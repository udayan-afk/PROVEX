import ipaddress
import re
import socket
from html.parser import HTMLParser
from typing import Optional, Tuple, Set
from urllib.parse import urlparse, urljoin
import httpx

# Blocked IP ranges and hostnames for SSRF protection
BLOCKED_HOSTNAMES: Set[str] = {
    "localhost",
    "127.0.0.1",
    "::1",
    "0.0.0.0",
    "metadata.google.internal",
    "metadata.internal",
    "instance-data",
}

# Maximum allowed page size: 2 MB
MAX_PAGE_BYTES = 2 * 1024 * 1024
MAX_REDIRECTS = 3
CONNECT_TIMEOUT = 8.0
READ_TIMEOUT = 10.0


def is_ip_blocked(ip_obj: ipaddress.IPv4Address | ipaddress.IPv6Address) -> bool:
    """Check if an IP address belongs to loopback, private, link-local, reserved or multicast ranges."""
    if (
        ip_obj.is_loopback
        or ip_obj.is_private
        or ip_obj.is_link_local
        or ip_obj.is_reserved
        or ip_obj.is_multicast
        or ip_obj.is_unspecified
    ):
        return True
    # Cloud metadata endpoint: 169.254.169.254
    if str(ip_obj) == "169.254.169.254":
        return True
    return False


def validate_url_and_resolve(url_str: str) -> Tuple[bool, str, Optional[str]]:
    """
    Validates scheme and resolves hostname, checking for SSRF / private IP access.
    Returns (is_valid, error_or_hostname, first_resolved_ip)
    """
    try:
        parsed = urlparse(url_str.strip())
    except Exception as e:
        return False, f"Malformed URL format: {str(e)}", None

    if parsed.scheme.lower() not in ("http", "https"):
        return False, f"Unsupported URL scheme '{parsed.scheme}'. Only HTTP and HTTPS are permitted.", None

    hostname = (parsed.hostname or "").strip().lower()
    if not hostname:
        return False, "URL is missing a valid hostname.", None

    if hostname in BLOCKED_HOSTNAMES:
        return False, f"Access to '{hostname}' is blocked for security (SSRF prevention).", None

    # Check if the hostname itself is a direct IP address literal
    try:
        ip = ipaddress.ip_address(hostname)
        if is_ip_blocked(ip):
            return False, f"Access to internal IP address '{hostname}' is prohibited.", None
        return True, hostname, str(ip)
    except ValueError:
        # Hostname is a domain name, proceed with DNS resolution
        pass

    port = parsed.port or (443 if parsed.scheme.lower() == "https" else 80)
    try:
        addr_info = socket.getaddrinfo(hostname, port, type=socket.SOCK_STREAM)
        if not addr_info:
            return False, f"Unable to resolve DNS for host '{hostname}'.", None

        # Check all resolved IP addresses
        first_ip = None
        for entry in addr_info:
            sockaddr = entry[4]
            ip_str = sockaddr[0]
            if not first_ip:
                first_ip = ip_str
            resolved_ip = ipaddress.ip_address(ip_str)
            if is_ip_blocked(resolved_ip):
                return False, f"Host '{hostname}' resolved to private/internal IP '{ip_str}', which is blocked.", None

        return True, hostname, first_ip
    except socket.gaierror as e:
        return False, f"DNS resolution failed for '{hostname}': {str(e)}", None
    except Exception as e:
        return False, f"Network validation error for '{hostname}': {str(e)}", None


class JobHtmlExtractor(HTMLParser):
    """
    Extracts human-readable text from HTML while ignoring navigation,
    scripts, styles, ads, headers, footers, and tracking widgets.
    """

    SKIPPED_TAGS = {
        "script",
        "style",
        "noscript",
        "svg",
        "nav",
        "footer",
        "header",
        "aside",
        "iframe",
        "form",
        "button",
        "select",
        "option",
    }

    HEADING_TAGS = {"h1", "h2", "h3", "h4", "h5", "h6"}
    BLOCK_TAGS = {"p", "div", "section", "article", "li", "tr"}

    def __init__(self):
        super().__init__()
        self._skip_depth = 0
        self._in_title = False
        self._title_buffer = []
        self.page_title: Optional[str] = None
        self.meta_og_title: Optional[str] = None
        self.meta_company: Optional[str] = None
        self._text_chunks = []
        self._last_tag = ""

    def handle_starttag(self, tag: str, attrs: list):
        tag_lower = tag.lower()
        attr_dict = {k.lower(): (v or "") for k, v in attrs}

        if tag_lower in self.SKIPPED_TAGS:
            self._skip_depth += 1
            return

        if self._skip_depth > 0:
            return

        if tag_lower == "title":
            self._in_title = True
        elif tag_lower == "meta":
            prop = attr_dict.get("property", "").lower()
            name = attr_dict.get("name", "").lower()
            content = attr_dict.get("content", "").strip()

            if prop in ("og:title", "twitter:title") and not self.meta_og_title:
                self.meta_og_title = content
            elif prop in ("og:site_name", "company", "employer") and not self.meta_company:
                self.meta_company = content
            elif name in ("company", "author", "hiringorganization") and not self.meta_company:
                self.meta_company = content

        if tag_lower in self.BLOCK_TAGS or tag_lower in self.HEADING_TAGS:
            self._text_chunks.append("\n")

        self._last_tag = tag_lower

    def handle_endtag(self, tag: str):
        tag_lower = tag.lower()
        if tag_lower in self.SKIPPED_TAGS:
            if self._skip_depth > 0:
                self._skip_depth -= 1
            return

        if self._skip_depth > 0:
            return

        if tag_lower == "title":
            self._in_title = False
            self.page_title = "".join(self._title_buffer).strip()

        if tag_lower in self.BLOCK_TAGS or tag_lower in self.HEADING_TAGS:
            self._text_chunks.append("\n")

    def handle_data(self, data: str):
        if self._skip_depth > 0:
            return

        cleaned = data.strip()
        if not cleaned:
            return

        if self._in_title:
            self._title_buffer.append(data)
        else:
            self._text_chunks.append(data)

    def get_text(self) -> str:
        raw_text = "".join(self._text_chunks)
        # Collapse excessive newlines and whitespace
        lines = [line.strip() for line in raw_text.splitlines()]
        filtered_lines = [l for l in lines if l]
        return "\n".join(filtered_lines)


class JobUrlReader:
    """Safe, SSRF-protected HTTP reader for job postings."""

    @staticmethod
    async def fetch_job_content(
        url: str,
    ) -> Tuple[Optional[str], Optional[str], Optional[str], Optional[str]]:
        """
        Fetches and parses a public job posting page.
        Returns:
            Tuple[job_title, company, readable_text, error_message]
        """
        current_url = url.strip()
        redirect_count = 0

        # Custom transport / client without automatic follow_redirects so we can revalidate each hop
        async with httpx.AsyncClient(
            verify=True,
            follow_redirects=False,
            timeout=httpx.Timeout(CONNECT_TIMEOUT, read=READ_TIMEOUT),
            headers={
                "User-Agent": (
                    "PROVEX-CareerIntelligence/1.0 (+https://provex.app; job skill analyzer bot)"
                ),
                "Accept": "text/html,application/xhtml+xml,text/plain;q=0.9",
                "Accept-Language": "en-US,en;q=0.8",
            },
        ) as client:
            while redirect_count <= MAX_REDIRECTS:
                # 1. SSRF and DNS revalidation on every hop
                valid, err_or_host, resolved_ip = validate_url_and_resolve(current_url)
                if not valid:
                    return None, None, None, err_or_host

                try:
                    resp = await client.get(current_url)
                except httpx.TimeoutException:
                    return (
                        None,
                        None,
                        None,
                        f"Connection timed out while fetching '{current_url}'. The server did not respond in time.",
                    )
                except httpx.ConnectError as ce:
                    return None, None, None, f"Failed to connect to '{err_or_host}': {str(ce)}"
                except Exception as e:
                    return None, None, None, f"Network request error: {str(e)}"

                # Check status code
                if resp.status_code in (301, 302, 303, 307, 308):
                    location = resp.headers.get("Location")
                    if not location:
                        return None, None, None, f"Received redirect {resp.status_code} without Location header."
                    current_url = urljoin(current_url, location)
                    redirect_count += 1
                    if redirect_count > MAX_REDIRECTS:
                        return None, None, None, "Too many redirects encountered while fetching the job posting."
                    continue

                if resp.status_code == 401:
                    return (
                        None,
                        None,
                        None,
                        "This job posting is behind a login wall. Please copy and paste the job description directly.",
                    )
                if resp.status_code in (403, 429):
                    return (
                        None,
                        None,
                        None,
                        f"Access was denied or rate-limited by the host (HTTP {resp.status_code}). Please copy and paste the job description text.",
                    )
                if resp.status_code == 404:
                    return (
                        None,
                        None,
                        None,
                        "The job posting was not found (HTTP 404). The position may have expired or the link is broken.",
                    )
                if resp.status_code >= 400:
                    return (
                        None,
                        None,
                        None,
                        f"The server returned an error (HTTP {resp.status_code}). Please paste the job description directly.",
                    )

                # Check Content-Type
                content_type = resp.headers.get("Content-Type", "").lower()
                if (
                    "text/html" not in content_type
                    and "text/plain" not in content_type
                    and "application/xhtml+xml" not in content_type
                ):
                    return (
                        None,
                        None,
                        None,
                        f"Unsupported content type '{content_type}'. Please paste the job description text.",
                    )

                # Check response size
                raw_bytes = resp.content
                if len(raw_bytes) > MAX_PAGE_BYTES:
                    raw_bytes = raw_bytes[:MAX_PAGE_BYTES]

                # Parse HTML content
                try:
                    encoding = resp.encoding or "utf-8"
                    html_text = raw_bytes.decode(encoding, errors="replace")
                except Exception:
                    html_text = raw_bytes.decode("utf-8", errors="replace")

                # Parse readable text
                parser = JobHtmlExtractor()
                try:
                    parser.feed(html_text)
                except Exception:
                    pass

                readable_text = parser.get_text()

                # Detect bot protection or empty page
                bot_signals = [
                    "enable javascript",
                    "attention required! | cloudflare",
                    "verify you are a human",
                    "access to this page has been denied",
                    "captcha",
                    "just a moment...",
                ]
                text_lower = readable_text.lower()
                if any(signal in text_lower for signal in bot_signals) and len(readable_text) < 500:
                    return (
                        None,
                        None,
                        None,
                        "This job website is protected by automated bot verification or requires JavaScript. Please copy and paste the job description directly into PROVEX.",
                    )

                if len(readable_text.strip()) < 80:
                    return (
                        None,
                        None,
                        None,
                        "No readable job description could be extracted from this page. Please paste the job description text directly.",
                    )

                # Derive title and company
                job_title = parser.meta_og_title or parser.page_title
                if job_title:
                    # Clean up common web suffixes like " | Careers at Company" or " - LinkedIn"
                    job_title = re.split(r"\s+[|\-–—•]\s+", job_title)[0].strip()

                company = parser.meta_company

                return job_title, company, readable_text, None

            return None, None, None, "Too many redirects encountered."

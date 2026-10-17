import json
import re
from typing import Dict, List, Optional, Set, Tuple
import httpx
from sqlalchemy.orm import Session

from app.core.config import settings
from app.models.profile import CareerProfile
from app.models.github import GitHubConnection
from app.models.user import User
from app.schemas.job_analyzer import (
    ExtractedSkill,
    JobAnalysisResponse,
    ProfileComparison,
    GitHubMatchResult,
)
from app.services.url_reader import JobUrlReader
from app.services.github_service import GitHubService
from app.services.github_skill_matcher import GitHubSkillMatcher

# Comprehensive grounded skill definitions
SKILL_TAXONOMY: Dict[str, Tuple[str, List[str]]] = {
    # Language: (Canonical Name, Category, [Aliases/Patterns])
    "kotlin": ("Kotlin", "Programming Language", [r"\bkotlin\b"]),
    "java": ("Java", "Programming Language", [r"\bjava\b(?!script)"]),
    "python": ("Python", "Programming Language", [r"\bpython\b"]),
    "javascript": ("JavaScript", "Programming Language", [r"\bjavascript\b", r"\bjs\b"]),
    "typescript": ("TypeScript", "Programming Language", [r"\btypescript\b", r"\bts\b"]),
    "swift": ("Swift", "Programming Language", [r"\bswift\b"]),
    "c++": ("C++", "Programming Language", [r"\bc\+\+\b", r"\bcpp\b"]),
    "c#": ("C#", "Programming Language", [r"\bc#\b", r"\bcsharp\b"]),
    "go": ("Go", "Programming Language", [r"\bgolang\b", r"\bgo\s+language\b"]),
    "rust": ("Rust", "Programming Language", [r"\brust\b"]),
    "sql": ("SQL", "Database & Storage", [r"\bsql\b"]),
    "html/css": ("HTML/CSS", "Frontend & UI", [r"\bhtml5?\b", r"\bcss3?\b"]),

    # Mobile & Android
    "jetpack compose": ("Jetpack Compose", "Mobile Framework", [r"\bjetpack\s+compose\b", r"\bcompose\b"]),
    "android sdk": ("Android SDK", "Mobile Framework", [r"\bandroid\s+sdk\b", r"\bandroid\s+development\b", r"\bandroid\s+os\b"]),
    "coroutines": ("Kotlin Coroutines", "Mobile Framework", [r"\bcoroutines\b", r"\bkotlinx\.coroutines\b"]),
    "flow": ("Kotlin Flow", "Mobile Framework", [r"\bkotlin\s+flow\b", r"\bstateflow\b", r"\bsharedflow\b"]),
    "retrofit": ("Retrofit", "Mobile Framework", [r"\bretrofit\b", r"\bretrofit2\b"]),
    "okhttp": ("OkHttp", "Mobile Framework", [r"\bokhttp\b"]),
    "room": ("Room Database", "Database & Storage", [r"\broom\s+db\b", r"\broom\s+database\b", r"\broom\s+persistence\b"]),
    "hilt": ("Hilt / Dagger", "Mobile Framework", [r"\bhilt\b", r"\bdagger2?\b", r"\bdagger-hilt\b"]),
    "koin": ("Koin", "Mobile Framework", [r"\bkoin\b"]),
    "material design": ("Material Design 3", "Frontend & UI", [r"\bmaterial\s+design\b", r"\bmaterial\s*3\b"]),
    "flutter": ("Flutter", "Mobile Framework", [r"\bflutter\b", r"\bdart\b"]),
    "react native": ("React Native", "Mobile Framework", [r"\breact\s+native\b"]),
    "ios sdk": ("iOS SDK", "Mobile Framework", [r"\bios\s+sdk\b", r"\buikit\b", r"\bswiftui\b"]),

    # Backend & Web
    "fastapi": ("FastAPI", "Backend Framework", [r"\bfastapi\b"]),
    "django": ("Django", "Backend Framework", [r"\bdjango\b"]),
    "flask": ("Flask", "Backend Framework", [r"\bflask\b"]),
    "spring boot": ("Spring Boot", "Backend Framework", [r"\bspring\s+boot\b", r"\bspring\s+framework\b"]),
    "node.js": ("Node.js", "Backend Framework", [r"\bnode\.?js\b", r"\bnodejs\b"]),
    "react": ("React", "Frontend & UI", [r"\breact(\.?js)?\b"]),
    "vue": ("Vue.js", "Frontend & UI", [r"\bvue(\.?js)?\b"]),
    "next.js": ("Next.js", "Frontend & UI", [r"\bnext\.?js\b"]),

    # Database & Storage
    "postgresql": ("PostgreSQL", "Database & Storage", [r"\bpostgres(ql)?\b"]),
    "mysql": ("MySQL", "Database & Storage", [r"\bmysql\b"]),
    "sqlite": ("SQLite", "Database & Storage", [r"\bsqlite\b", r"\bsqlite3\b"]),
    "mongodb": ("MongoDB", "Database & Storage", [r"\bmongodb\b", r"\bmongo\b"]),
    "redis": ("Redis", "Database & Storage", [r"\bredis\b"]),
    "sqlalchemy": ("SQLAlchemy", "Database & Storage", [r"\bsqlalchemy\b"]),

    # Cloud & DevOps
    "aws": ("AWS", "Cloud & DevOps", [r"\baws\b", r"\bamazon\s+web\s+services\b"]),
    "google cloud": ("Google Cloud (GCP)", "Cloud & DevOps", [r"\bgcp\b", r"\bgoogle\s+cloud\b"]),
    "azure": ("Microsoft Azure", "Cloud & DevOps", [r"\bazure\b"]),
    "docker": ("Docker", "Cloud & DevOps", [r"\bdocker\b", r"\bcontainer(ization)?\b"]),
    "kubernetes": ("Kubernetes", "Cloud & DevOps", [r"\bkubernetes\b", r"\bk8s\b"]),
    "ci/cd": ("CI/CD Pipelines", "Cloud & DevOps", [r"\bci\/cd\b", r"\bcontinuous\s+integration\b", r"\bgithub\s+actions\b"]),
    "linux": ("Linux", "Cloud & DevOps", [r"\blinux\b", r"\bbash\b", r"\bshell\s+scripting\b"]),

    # Architecture & Testing
    "mvvm": ("MVVM Architecture", "Architecture & Patterns", [r"\bmvvm\b", r"\bmodel-view-viewmodel\b"]),
    "clean architecture": ("Clean Architecture", "Architecture & Patterns", [r"\bclean\s+architecture\b", r"\bsolid\s+principles\b"]),
    "rest api": ("RESTful APIs", "Architecture & Patterns", [r"\brest(ful)?\s+apis?\b", r"\brestful\b"]),
    "graphql": ("GraphQL", "Architecture & Patterns", [r"\bgraphql\b"]),
    "unit testing": ("Unit Testing", "Testing & Quality", [r"\bunit\s+test(ing|s)?\b", r"\bjunit\b", r"\bmockk\b", r"\bpytest\b"]),
    "ui testing": ("UI Testing / Espresso", "Testing & Quality", [r"\bespresso\b", r"\bui\s+test(ing|s)?\b"]),

    # Tools & Collaboration
    "git": ("Git / Version Control", "Tools & Collaboration", [r"\bgit\b", r"\bgithub\b", r"\bgitlab\b"]),
    "gradle": ("Gradle Build System", "Tools & Collaboration", [r"\bgradle\b"]),
    "figma": ("Figma", "Tools & Collaboration", [r"\bfigma\b"]),
    "jira": ("Jira / Agile Tracking", "Tools & Collaboration", [r"\bjira\b"]),

    # Soft Skills
    "problem solving": ("Problem Solving", "Soft Skills", [r"\bproblem[\s-]solving\b", r"\banalytical\s+skills\b"]),
    "team collaboration": ("Team Collaboration", "Soft Skills", [r"\bcollaboration\b", r"\bteamwork\b", r"\bcross-functional\b"]),
    "communication": ("Written & Verbal Communication", "Soft Skills", [r"\bcommunication\s+skills\b", r"\barticulate\b"]),
    "agile": ("Agile / Scrum", "Soft Skills", [r"\bagile\b", r"\bscrum\b"]),
}


class JobAnalyzerService:
    @staticmethod
    async def analyze(
        description: Optional[str],
        url: Optional[str],
        user: Optional[User],
        db: Session,
    ) -> JobAnalysisResponse:
        """
        Main entry point for analyzing a job or internship posting.
        """
        warnings: List[str] = []
        source_type = "pasted_description"
        analyzed_text = ""
        job_title: Optional[str] = None
        company: Optional[str] = None
        source_url: Optional[str] = None

        has_desc = bool(description and description.strip())
        has_url = bool(url and url.strip())

        # Sensible handling when both are provided
        if has_desc and has_url:
            source_url = url.strip()
            analyzed_text = description.strip()
            warnings.append(
                f"Both description and URL were provided. Analyzed the provided job description text; the URL ({source_url}) was retained as reference."
            )
        elif has_url:
            source_url = url.strip()
            source_type = "url_extracted"
            fetched_title, fetched_company, fetched_text, fetch_error = await JobUrlReader.fetch_job_content(source_url)
            if fetch_error:
                # If URL fetch failed, return descriptive error
                raise ValueError(fetch_error)
            analyzed_text = fetched_text or ""
            job_title = fetched_title
            company = fetched_company
        elif has_desc:
            analyzed_text = description.strip()
        else:
            raise ValueError("Please provide either a job description or a valid job posting URL.")

        if len(analyzed_text.strip()) < 30:
            raise ValueError("The provided job description is too short to analyze. Please provide a more detailed posting.")

        # Check for optional AI configuration
        ai_key = getattr(settings, "GEMINI_API_KEY", "") or getattr(settings, "OPENAI_API_KEY", "")
        extracted_via_ai = False
        extraction_method = "deterministic_rules"

        # Attempt AI extraction if key is present
        if ai_key:
            try:
                ai_result = await JobAnalyzerService._extract_with_gemini(analyzed_text, ai_key)
                if ai_result:
                    extracted_via_ai = True
                    extraction_method = "ai_llm"
                    extracted_skills = ai_result.get("skills", [])
                    responsibilities = ai_result.get("responsibilities", [])
                    qualifications = ai_result.get("qualifications", [])
                    education_requirements = ai_result.get("education_requirements", [])
                    experience_requirements = ai_result.get("experience_requirements", [])
                    resume_keywords = ai_result.get("resume_keywords", [])
                    if not job_title and ai_result.get("job_title"):
                        job_title = ai_result.get("job_title")
                    if not company and ai_result.get("company"):
                        company = ai_result.get("company")
                    summary = ai_result.get("summary", "")
            except Exception as e:
                warnings.append(f"AI provider extraction failed ({str(e)}). Fell back to PROVEX deterministic extraction.")

        # Fallback / Deterministic grounded extraction
        if not extracted_via_ai:
            (
                extracted_skills,
                responsibilities,
                qualifications,
                education_requirements,
                experience_requirements,
                resume_keywords,
                summary,
                detected_title,
                detected_company,
            ) = JobAnalyzerService._deterministic_extraction(analyzed_text)

            if not job_title:
                job_title = detected_title
            if not company:
                company = detected_company

        # Build skill category counts
        skill_counts: Dict[str, int] = {}
        for skill in extracted_skills:
            cat = skill.category
            skill_counts[cat] = skill_counts.get(cat, 0) + 1

        # Profile Comparison
        profile_comparison = None
        github_matching = None
        if user:
            profile_comparison = JobAnalyzerService._compare_with_user_profile(user, extracted_skills, db)
            github_matching = await JobAnalyzerService._match_with_github(user, extracted_skills, db)

        return JobAnalysisResponse(
            job_title=job_title or "Job Role",
            company=company,
            source_type=source_type,
            source_url=source_url,
            summary=summary,
            skills=extracted_skills,
            skill_counts_by_category=skill_counts,
            responsibilities=responsibilities[:8],
            qualifications=qualifications[:8],
            education_requirements=education_requirements[:5],
            experience_requirements=experience_requirements[:5],
            resume_keywords=resume_keywords[:15],
            profile_comparison=profile_comparison,
            github_matching=github_matching,
            extraction_method=extraction_method,
            warnings=warnings,
        )

    @staticmethod
    def _deterministic_extraction(text: str) -> Tuple[
        List[ExtractedSkill],
        List[str],
        List[str],
        List[str],
        List[str],
        List[str],
        str,
        Optional[str],
        Optional[str],
    ]:
        """
        Thorough, deterministic skill extractor grounded strictly in the supplied text.
        """
        lines = [l.strip() for l in text.splitlines() if l.strip()]
        paragraphs = re.split(r"\n\s*\n", text)
        sentences = [s.strip() for s in re.split(r"(?<=[.!?])\s+|\n+", text) if len(s.strip()) > 5]

        # 1. Section mapping: label lines as 'required', 'preferred', or 'general'
        section_states: List[Tuple[str, str]] = []  # (line, section_type)
        current_section = "general"

        req_headers = re.compile(
            r"^(minimum|basic|key|required|must[\s-]have)?\s*(qualifications|requirements|what you('ll)? bring|who you are|what we look for)\b",
            re.IGNORECASE,
        )
        pref_headers = re.compile(
            r"^(preferred|nice[\s-]to[\s-]have|bonus|desired|what gives you an edge|good to have)\b",
            re.IGNORECASE,
        )
        resp_headers = re.compile(
            r"^(responsibilities|what you('ll)? do|your role|the role|key duties)\b",
            re.IGNORECASE,
        )

        detected_title = None
        detected_company = None
        responsibilities: List[str] = []
        qualifications: List[str] = []

        for line in lines:
            if not detected_title:
                m_title = re.search(r"^(job\s+title|position|role):\s*(.+)$", line, re.IGNORECASE)
                if m_title:
                    detected_title = m_title.group(2).strip()
                elif re.search(r"\b(engineer|developer|intern|internship|specialist|architect|manager)\b", line, re.IGNORECASE) and len(line) < 80:
                    detected_title = line.strip()

            if not detected_company:
                m_comp = re.search(r"^(company|employer|at):\s*(.+)$", line, re.IGNORECASE)
                if m_comp:
                    detected_company = m_comp.group(2).strip()

            if req_headers.search(line):
                current_section = "required"
                continue
            elif pref_headers.search(line):
                current_section = "preferred"
                continue
            elif resp_headers.search(line):
                current_section = "responsibilities"
                continue

            # Capture bullet points for responsibilities / qualifications
            is_bullet = line.startswith(("-", "•", "*", "–", "—")) or re.match(r"^\d+\.", line)
            clean_bullet = re.sub(r"^[-•*–—\d\.]+\s*", "", line).strip()

            if is_bullet and len(clean_bullet) > 15:
                if current_section == "responsibilities":
                    responsibilities.append(clean_bullet)
                elif current_section in ("required", "preferred"):
                    qualifications.append(clean_bullet)

            section_states.append((line, current_section))

        # 2. Extract Skills
        extracted_skills: Dict[str, ExtractedSkill] = {}

        for key, (canonical_name, category, patterns) in SKILL_TAXONOMY.items():
            for pat in patterns:
                regex = re.compile(pat, re.IGNORECASE)
                match = regex.search(text)
                if match:
                    # Find best sentence for evidence snippet
                    evidence = ""
                    importance = "Mentioned"

                    # Find containing sentence
                    for s in sentences:
                        if regex.search(s):
                            # Clean and truncate evidence
                            clean_s = re.sub(r"^[•\-\*\s]+", "", s).strip()
                            if len(clean_s) > 140:
                                clean_s = clean_s[:137] + "..."
                            evidence = clean_s
                            break

                    # Determine importance based on section and local cues
                    for line, sec in section_states:
                        if regex.search(line):
                            if sec == "required":
                                importance = "Required"
                                break
                            elif sec == "preferred" and importance != "Required":
                                importance = "Preferred"

                    # In-sentence cues
                    if evidence:
                        ev_lower = evidence.lower()
                        if any(w in ev_lower for w in ("must have", "required", "proficient", "strong experience", "minimum of")):
                            importance = "Required"
                        elif any(w in ev_lower for w in ("preferred", "plus", "bonus", "nice to have", "good to have")):
                            if importance != "Required":
                                importance = "Preferred"

                    # If not in specific section, default to Required for core requirements or Mentioned
                    if importance == "Mentioned" and any(w in text.lower() for w in ("requirements", "qualifications")):
                        # If mentioned in bullet points under general
                        importance = "Mentioned"

                    # Deduplicate and prioritize Required > Preferred > Mentioned
                    if canonical_name in extracted_skills:
                        prev = extracted_skills[canonical_name]
                        if prev.importance != "Required" and importance == "Required":
                            prev.importance = "Required"
                            if evidence:
                                prev.evidence = evidence
                    else:
                        extracted_skills[canonical_name] = ExtractedSkill(
                            name=canonical_name,
                            category=category,
                            importance=importance,
                            evidence=evidence or f"Mentioned in job description: '{match.group(0)}'",
                            confidence=0.95,
                        )
                    break

        # Sort skills: Required first, then Preferred, then Mentioned
        importance_rank = {"Required": 0, "Preferred": 1, "Mentioned": 2}
        sorted_skills = sorted(
            extracted_skills.values(),
            key=lambda x: (importance_rank.get(x.importance, 3), x.name.lower()),
        )

        # 3. Extract Education Requirements
        education_patterns = [
            r"bachelor['’]?s?\s+(degree\s+)?(in\s+[^,.\n]+)?",
            r"master['’]?s?\s+(degree\s+)?(in\s+[^,.\n]+)?",
            r"computer\s+science\s+(or\s+related\s+field)?",
            r"enrolled\s+in\s+a\s+(degree|university|college)\s+program",
            r"diploma|high\s+school\s+degree",
        ]
        education_requirements: List[str] = []
        for s in sentences:
            for ep in education_patterns:
                if re.search(ep, s, re.IGNORECASE):
                    clean = re.sub(r"^[•\-\*\s]+", "", s).strip()
                    if clean not in education_requirements:
                        education_requirements.append(clean[:120])
                    break

        # 4. Extract Experience Requirements
        experience_patterns = [
            r"\d+\+?\s*(years?|yrs?)\s+of\s+(experience|work)",
            r"internship\s+experience",
            r"entry[\s-]level",
            r"proven\s+track\s+record",
            r"experience\s+with\s+(shipping|building|deploying)",
        ]
        experience_requirements: List[str] = []
        for s in sentences:
            for xp in experience_patterns:
                if re.search(xp, s, re.IGNORECASE):
                    clean = re.sub(r"^[•\-\*\s]+", "", s).strip()
                    if clean not in experience_requirements:
                        experience_requirements.append(clean[:120])
                    break

        # 5. Extract Resume Keywords
        keywords: Set[str] = set()
        for sk in sorted_skills:
            keywords.add(sk.name)

        general_tech_terms = [
            "REST API", "CI/CD", "Git", "Clean Architecture", "Unit Testing",
            "Debugging", "Agile", "Scrum", "Code Review", "Data Structures",
            "Algorithms", "Performance Optimization", "Scalability", "Security"
        ]
        for term in general_tech_terms:
            if re.search(r"\b" + re.escape(term) + r"\b", text, re.IGNORECASE):
                keywords.add(term)

        # 6. Generate Summary
        role_desc = detected_title or "Software Developer / Engineer"
        comp_desc = f" at {detected_company}" if detected_company else ""
        req_count = sum(1 for s in sorted_skills if s.importance == "Required")
        pref_count = sum(1 for s in sorted_skills if s.importance == "Preferred")
        summary = (
            f"Opportunity for {role_desc}{comp_desc}. "
            f"Identified {len(sorted_skills)} relevant skills ({req_count} required, {pref_count} preferred). "
            "Evaluated using PROVEX grounded extraction."
        )

        return (
            sorted_skills,
            responsibilities,
            qualifications,
            education_requirements,
            experience_requirements,
            sorted(list(keywords)),
            summary,
            detected_title,
            detected_company,
        )

    @staticmethod
    def _compare_with_user_profile(
        user: User,
        job_skills: List[ExtractedSkill],
        db: Session,
    ) -> ProfileComparison:
        """
        Compares extracted job skills with the authenticated user's persistent career profile.
        Never marks skills as proven without verified repository citations.
        """
        profile = db.query(CareerProfile).filter(CareerProfile.user_id == user.id).first()
        if not profile:
            return ProfileComparison(
                user_has_profile=False,
                matching_skills=[],
                missing_skills=[s.name for s in job_skills],
                next_skills_to_learn=[s.name for s in job_skills if s.importance in ("Required", "Preferred")][:5],
                match_percentage=0,
            )

        # Parse user's profile skills
        user_skills_set: Set[str] = set()
        for json_field in (profile.skills_json, profile.languages_json, profile.frameworks_json):
            if json_field:
                try:
                    items = json.loads(json_field)
                    for item in items:
                        if isinstance(item, str):
                            user_skills_set.add(item.strip().lower())
                except Exception:
                    pass

        matching: List[str] = []
        missing: List[str] = []
        next_to_learn: List[str] = []

        # Compare
        for skill in job_skills:
            skill_norm = skill.name.strip().lower()
            # Direct or partial match
            if skill_norm in user_skills_set or any(u in skill_norm or skill_norm in u for u in user_skills_set):
                matching.append(skill.name)
            else:
                missing.append(skill.name)
                if skill.importance in ("Required", "Preferred"):
                    next_to_learn.append(skill.name)

        total_req_pref = sum(1 for s in job_skills if s.importance in ("Required", "Preferred"))
        if total_req_pref > 0:
            match_count = sum(1 for s in job_skills if s.importance in ("Required", "Preferred") and s.name in matching)
            match_pct = int((match_count / total_req_pref) * 100)
        else:
            match_pct = int((len(matching) / max(len(job_skills), 1)) * 100)

        return ProfileComparison(
            user_has_profile=True,
            matching_skills=matching,
            missing_skills=missing,
            next_skills_to_learn=next_to_learn[:6],
            match_percentage=min(max(match_pct, 0), 100),
            disclaimer=(
                "Profile match represents alignment with your self-reported skills. "
                "PROVEX code verification requires repository analysis to prove proficiency."
            ),
        )

    @staticmethod
    async def _extract_with_gemini(text: str, api_key: str) -> Optional[Dict]:
        """
        Calls Gemini API with structured JSON output if an API key is configured.
        """
        model = getattr(settings, "AI_MODEL_NAME", "gemini-2.5-flash")
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"

        prompt = (
            "You are the PROVEX Job Skill Analyzer. Analyze the following job posting and return "
            "a valid JSON object with the following schema:\n"
            "{\n"
            '  "job_title": "string or null",\n'
            '  "company": "string or null",\n'
            '  "summary": "string",\n'
            '  "skills": [\n'
            '    {\n'
            '      "name": "string",\n'
            '      "category": "string",\n'
            '      "importance": "Required | Preferred | Mentioned",\n'
            '      "evidence": "quote from text"\n'
            "    }\n"
            "  ],\n"
            '  "responsibilities": ["string"],\n'
            '  "qualifications": ["string"],\n'
            '  "education_requirements": ["string"],\n'
            '  "experience_requirements": ["string"],\n'
            '  "resume_keywords": ["string"]\n'
            "}\n\n"
            "Ground all skills strictly in the provided text. Do not invent requirements.\n\n"
            f"Job Posting Text:\n{text[:8000]}"
        )

        payload = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {"response_mime_type": "application/json"},
        }

        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.post(url, json=payload)
            if resp.status_code == 200:
                data = resp.json()
                content = data["candidates"][0]["content"]["parts"][0]["text"]
                return json.loads(content)
        return None

    @staticmethod
    async def _match_with_github(
        user: User,
        job_skills: List[ExtractedSkill],
        db: Session,
    ) -> GitHubMatchResult:
        """
        Retrieves user's GitHub repositories and evaluates deterministic skill matching.
        """
        conn = db.query(GitHubConnection).filter(GitHubConnection.user_id == user.id).first()
        if not conn:
            return GitHubMatchResult(
                connected=False,
                github_username=None,
                repositories_analyzed=0,
                score=None,
                warnings=["GitHub account is not connected. Connect your GitHub account to analyze repository evidence match."],
            )

        try:
            repos = await GitHubService.get_repositories(db, user)
            match_data = GitHubSkillMatcher.match(job_skills, repos)
            return GitHubMatchResult(
                connected=True,
                github_username=conn.github_username,
                repositories_analyzed=match_data["repositories_analyzed"],
                score=match_data["score"],
                score_label=match_data["score_label"],
                strong_count=match_data["strong_count"],
                partial_count=match_data["partial_count"],
                not_found_count=match_data["not_found_count"],
                preferred_match_count=match_data["preferred_match_count"],
                matched_skills=match_data["matched_skills"],
                skills_to_strengthen=match_data["skills_to_strengthen"],
                warnings=[],
            )
        except Exception as e:
            return GitHubMatchResult(
                connected=True,
                github_username=conn.github_username,
                repositories_analyzed=0,
                score=None,
                warnings=[f"Failed to fetch repositories from GitHub: {str(e)}"],
            )

    @staticmethod
    async def match_skills_with_github(
        user: User,
        skills: List[ExtractedSkill],
        db: Session,
    ) -> GitHubMatchResult:
        return await JobAnalyzerService._match_with_github(user, skills, db)

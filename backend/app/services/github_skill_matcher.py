import re
from typing import Dict, List, Optional, Tuple
from app.schemas.job_analyzer import ExtractedSkill
from app.schemas.github import GitHubRepositoryResponse

# Knowledge base mapping skill names to regex patterns and language associations
SKILL_PATTERNS: Dict[str, Tuple[List[str], List[str]]] = {
    # skill_key: ([name/desc regex patterns], [associated languages])
    "kotlin": ([r"\bkotlin\b"], ["kotlin"]),
    "java": ([r"\bjava\b(?!script)"], ["java"]),
    "python": ([r"\bpython\b"], ["python", "jupyter notebook"]),
    "javascript": ([r"\bjavascript\b", r"\bjs\b", r"\bnode\b"], ["javascript"]),
    "typescript": ([r"\btypescript\b", r"\bts\b"], ["typescript"]),
    "swift": ([r"\bswift\b", r"\bswiftui\b"], ["swift"]),
    "c++": ([r"\bc\+\+\b", r"\bcpp\b"], ["c++", "c"]),
    "c#": ([r"\bc#\b", r"\bcsharp\b", r"\b\.net\b"], ["c#"]),
    "go": ([r"\bgolang\b", r"\bgo\b"], ["go"]),
    "rust": ([r"\brust\b"], ["rust"]),
    "sql": ([r"\bsql\b", r"\bpostgres\b", r"\bmysql\b", r"\bsqlite\b"], ["sql", "plpgsql", "tsql"]),
    "html/css": ([r"\bhtml\b", r"\bcss\b"], ["html", "css", "scss"]),

    # Mobile Frameworks
    "jetpack compose": ([r"\bjetpack\s+compose\b", r"\bcompose\b"], ["kotlin"]),
    "android sdk": ([r"\bandroid\b", r"\bandroid\s+sdk\b", r"\bapk\b"], ["kotlin", "java"]),
    "coroutines": ([r"\bcoroutines\b", r"\bkotlinx\b"], ["kotlin"]),
    "flow": ([r"\bflow\b", r"\bstateflow\b"], ["kotlin"]),
    "retrofit": ([r"\bretrofit\b", r"\bretrofit2\b"], ["kotlin", "java"]),
    "okhttp": ([r"\bokhttp\b"], ["kotlin", "java"]),
    "room database": ([r"\broom\b", r"\broom\s+db\b"], ["kotlin", "java"]),
    "hilt / dagger": ([r"\bhilt\b", r"\bdagger\b"], ["kotlin", "java"]),
    "flutter": ([r"\bflutter\b", r"\bdart\b"], ["dart"]),
    "react native": ([r"\breact\s+native\b"], ["javascript", "typescript"]),
    "material design 3": ([r"\bmaterial\b", r"\bmaterial\s*3\b"], ["kotlin", "java", "dart"]),

    # Backend & Web
    "fastapi": ([r"\bfastapi\b", r"\buvicorn\b", r"\bstarlette\b"], ["python"]),
    "django": ([r"\bdjango\b"], ["python"]),
    "flask": ([r"\bflask\b"], ["python"]),
    "spring boot": ([r"\bspring\b", r"\bspring\s+boot\b"], ["java", "kotlin"]),
    "node.js": ([r"\bnode\.?js\b", r"\bexpress\b"], ["javascript", "typescript"]),
    "react": ([r"\breact\b", r"\breactjs\b"], ["javascript", "typescript"]),
    "vue.js": ([r"\bvue\b"], ["javascript", "typescript", "vue"]),
    "next.js": ([r"\bnext\.?js\b"], ["javascript", "typescript"]),

    # Databases
    "postgresql": ([r"\bpostgres(ql)?\b"], ["sql", "python", "go"]),
    "mysql": ([r"\bmysql\b"], ["sql"]),
    "sqlite": ([r"\bsqlite\b", r"\bsqlite3\b"], ["sql", "c", "python", "kotlin"]),
    "mongodb": ([r"\bmongodb\b", r"\bmongo\b"], []),
    "redis": ([r"\bredis\b"], []),
    "sqlalchemy": ([r"\bsqlalchemy\b", r"\balembic\b"], ["python"]),

    # Cloud & DevOps
    "docker": ([r"\bdocker\b", r"\bdockerfile\b", r"\bcompose\.yml\b"], ["dockerfile"]),
    "kubernetes": ([r"\bkubernetes\b", r"\bk8s\b"], []),
    "ci/cd pipelines": ([r"\bci\/cd\b", r"\bgithub\s+actions\b", r"\bworkflow\b"], []),
    "linux": ([r"\blinux\b", r"\bbash\b", r"\bshell\b"], ["shell"]),
    "aws": ([r"\baws\b", r"\blambda\b", r"\bs3\b"], []),
    "google cloud (gcp)": ([r"\bgcp\b", r"\bgoogle\s+cloud\b", r"\bbigquery\b"], []),

    # Architecture & Tools
    "mvvm architecture": ([r"\bmvvm\b", r"\bviewmodel\b"], ["kotlin", "java", "swift"]),
    "clean architecture": ([r"\bclean\s+architecture\b", r"\busecase\b"], []),
    "restful apis": ([r"\brest\b", r"\brestful\b", r"\bapi\b"], []),
    "graphql": ([r"\bgraphql\b"], []),
    "unit testing": ([r"\btest\b", r"\bjunit\b", r"\bpytest\b", r"\bmockk\b"], []),
    "git / version control": ([r"\bgit\b", r"\bgithub\b"], []),
    "gradle build system": ([r"\bgradle\b", r"\bbuild\.gradle\b"], ["groovy", "kotlin"]),
    "agile / scrum": ([r"\bagile\b", r"\bscrum\b"], []),
    "problem solving": ([r"\balgorithm\b", r"\bleetcode\b", r"\bdata\s+structure\b"], []),
}


class GitHubSkillMatcher:
    """
    Deterministically evaluates repository evidence against extracted job skills.
    Calculates exact weighted match score based on requirement importance and evidence tier.
    """

    @staticmethod
    def match(
        job_skills: List[ExtractedSkill],
        repositories: List[GitHubRepositoryResponse],
    ) -> Dict:
        """
        Matches job skills against user's actual GitHub repositories.
        """
        matched_items = []
        strong_count = 0
        partial_count = 0
        not_found_count = 0
        preferred_match_count = 0

        total_weight = 0.0
        weighted_credit = 0.0

        for skill in job_skills:
            importance = skill.importance.capitalize()
            # Requirement weights: Required = 3, Preferred = 2, Mentioned = 1
            weight = 3.0 if importance == "Required" else (2.0 if importance == "Preferred" else 1.0)
            total_weight += weight

            # Search repositories for evidence
            evidence_tier, credit, supporting_repo, reason = GitHubSkillMatcher._find_repo_evidence(
                skill.name, repositories
            )

            weighted_credit += weight * credit

            # Match level classification
            if credit >= 0.95:
                if importance == "Preferred":
                    match_level = "PREFERRED_MATCH"
                    symbol = "★"
                    preferred_match_count += 1
                else:
                    match_level = "STRONG"
                    symbol = "✓"
                    strong_count += 1
            elif credit >= 0.5:
                if importance == "Preferred":
                    match_level = "PREFERRED_MATCH"
                    symbol = "★"
                    preferred_match_count += 1
                else:
                    match_level = "PARTIAL"
                    symbol = "◐"
                    partial_count += 1
            else:
                match_level = "NOT_FOUND"
                symbol = "○"
                not_found_count += 1

            matched_items.append({
                "name": skill.name,
                "category": skill.category,
                "importance": skill.importance,
                "match_level": match_level,
                "symbol": symbol,
                "credit": credit,
                "supporting_repo": supporting_repo,
                "evidence_reason": reason,
            })

        # Match Score = 100 * sum(weight * credit) / sum(weights)
        if total_weight > 0 and len(job_skills) > 0:
            raw_score = (100.0 * weighted_credit) / total_weight
            final_score = round(min(max(raw_score, 0.0), 100.0), 1)
        else:
            final_score = None

        # Prioritize skills to strengthen (Not Found required skills first, then partial)
        skills_to_strengthen = []
        for item in matched_items:
            if item["match_level"] == "NOT_FOUND":
                skills_to_strengthen.append(f"{item['name']} ({item['importance']})")
        for item in matched_items:
            if item["match_level"] == "PARTIAL":
                skills_to_strengthen.append(f"{item['name']} (Partial - strengthen with projects)")

        return {
            "score": final_score,
            "score_label": "Repository Evidence Match",
            "repositories_analyzed": len(repositories),
            "strong_count": strong_count,
            "partial_count": partial_count,
            "not_found_count": not_found_count,
            "preferred_match_count": preferred_match_count,
            "matched_skills": matched_items,
            "skills_to_strengthen": skills_to_strengthen[:6],
        }

    @staticmethod
    def _find_repo_evidence(
        skill_name: str,
        repositories: List[GitHubRepositoryResponse],
    ) -> Tuple[str, float, Optional[str], Optional[str]]:
        """
        Returns (tier, credit, supporting_repo_name, reason)
        Tiers:
          STRONG: credit 1.0
          PARTIAL: credit 0.6
          LIMITED: credit 0.3
          NO_EVIDENCE: credit 0.0
        """
        skill_key = skill_name.strip().lower()

        # Find matching patterns in taxonomy or fallback to skill name itself
        patterns, associated_langs = SKILL_PATTERNS.get(skill_key, ([re.escape(skill_key)], []))

        # 1. First check for direct language match
        for repo in repositories:
            repo_lang = (repo.language or "").strip().lower()
            if repo_lang and (repo_lang == skill_key or repo_lang in associated_langs):
                return (
                    "STRONG",
                    1.0,
                    repo.full_name or repo.name,
                    f"Primary language in repository '{repo.name}' is {repo.language}",
                )

        # 2. Check repository name and description for explicit mentions
        for repo in repositories:
            text_to_search = f"{repo.name} {repo.description or ''}".lower()
            for pat in patterns:
                if re.search(pat, text_to_search, re.IGNORECASE):
                    return (
                        "STRONG",
                        1.0,
                        repo.full_name or repo.name,
                        f"Directly featured in repository '{repo.name}' (description or name)",
                    )

        # 3. Check for ecosystem / partial association
        for repo in repositories:
            repo_lang = (repo.language or "").strip().lower()
            if repo_lang:
                # E.g. Kotlin/Java repo provides partial support for general Android concepts/patterns
                if skill_key in ("android sdk", "coroutines", "flow", "retrofit", "room database", "mvvm architecture") and repo_lang in ("kotlin", "java"):
                    return (
                        "PARTIAL",
                        0.6,
                        repo.full_name or repo.name,
                        f"Supported by {repo.language} code in '{repo.name}'",
                    )
                # E.g. Python repo provides partial support for FastAPI, Django, Flask, SQLAlchemy
                if skill_key in ("fastapi", "django", "flask", "sqlalchemy") and repo_lang == "python":
                    return (
                        "PARTIAL",
                        0.6,
                        repo.full_name or repo.name,
                        f"Supported by Python codebase in '{repo.name}'",
                    )
                # E.g. TypeScript/JavaScript for web frontend
                if skill_key in ("react", "vue.js", "next.js", "node.js") and repo_lang in ("typescript", "javascript"):
                    return (
                        "PARTIAL",
                        0.6,
                        repo.full_name or repo.name,
                        f"Supported by {repo.language} codebase in '{repo.name}'",
                    )

        # 4. Check for general tool support (Git, Linux, CI/CD) if user has active repositories
        if skill_key in ("git / version control", "git") and len(repositories) > 0:
            first_repo = repositories[0]
            return (
                "STRONG",
                1.0,
                first_repo.full_name or first_repo.name,
                f"Demonstrated by active Git version-controlled repository '{first_repo.name}'",
            )

        if skill_key in ("ci/cd pipelines", "ci/cd") and len(repositories) > 0:
            # Check if any repo mentions workflows or actions
            for repo in repositories:
                if "action" in (repo.description or "").lower() or "ci" in (repo.name or "").lower():
                    return (
                        "STRONG",
                        1.0,
                        repo.full_name or repo.name,
                        f"CI/CD evidence found in repository '{repo.name}'",
                    )
            return (
                "LIMITED",
                0.3,
                repositories[0].full_name or repositories[0].name,
                "Hosted GitHub repository indicates familiarity with GitHub ecosystem",
            )

        return ("NO_EVIDENCE", 0.0, None, "No direct or indirect evidence found in analyzed repositories")

package com.provex.app.domain.usecase

import com.provex.app.domain.model.AssistantOpportunityCard
import com.provex.app.domain.model.AssistantPersonalizedGuidance
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.model.WorkMode
import java.util.UUID

object CareerOpportunityEngine {

    private enum class RoleCategory {
        ANDROID_MOBILE,
        PYTHON_BACKEND,
        AI_ML,
        JUNIOR_SOFTWARE_ENGINEER,
        FRONTEND_WEB,
        DEVOPS_CLOUD,
        GENERAL_TECH,
    }

    fun generateGuidance(query: String, profile: UserProfile): AssistantPersonalizedGuidance {
        val trimmed = query.trim()
        val normalized = trimmed.lowercase()

        val category = when {
            normalized.contains("android") || normalized.contains("kotlin") || normalized.contains("mobile") || normalized.contains("compose") ->
                RoleCategory.ANDROID_MOBILE

            normalized.contains("python") || normalized.contains("django") || normalized.contains("fastapi") || normalized.contains("flask") ->
                RoleCategory.PYTHON_BACKEND

            normalized.contains("ai") || normalized.contains("ml") || normalized.contains("machine learning") || normalized.contains("data science") || normalized.contains("deep learning") ->
                RoleCategory.AI_ML

            normalized.contains("front") || normalized.contains("react") || normalized.contains("vue") || normalized.contains("web") || normalized.contains("angular") || normalized.contains("javascript") || normalized.contains("typescript") ->
                RoleCategory.FRONTEND_WEB

            normalized.contains("devops") || normalized.contains("cloud") || normalized.contains("docker") || normalized.contains("kubernetes") || normalized.contains("aws") ->
                RoleCategory.DEVOPS_CLOUD

            normalized.contains("junior") || normalized.contains("software") || normalized.contains("developer") || normalized.contains("engineer") || normalized.contains("intern") || normalized.contains("backend") ->
                RoleCategory.JUNIOR_SOFTWARE_ENGINEER

            else -> RoleCategory.GENERAL_TECH
        }

        val locationText = if (profile.location.isNotBlank()) profile.location else "Open to relocation / Remote"
        val workModeText = when (profile.workMode) {
            WorkMode.REMOTE -> "Remote preference"
            WorkMode.HYBRID -> "Hybrid preference"
            WorkMode.ON_SITE -> "On-site preference"
            WorkMode.NO_PREFERENCE -> "Flexible work mode"
        }

        return when (category) {
            RoleCategory.ANDROID_MOBILE -> buildAndroidGuidance(trimmed, profile, locationText, workModeText)
            RoleCategory.PYTHON_BACKEND -> buildPythonGuidance(trimmed, profile, locationText, workModeText)
            RoleCategory.AI_ML -> buildAiMlGuidance(trimmed, profile, locationText, workModeText)
            RoleCategory.FRONTEND_WEB -> buildFrontendGuidance(trimmed, profile, locationText, workModeText)
            RoleCategory.DEVOPS_CLOUD -> buildDevopsGuidance(trimmed, profile, locationText, workModeText)
            RoleCategory.JUNIOR_SOFTWARE_ENGINEER -> buildJuniorSeGuidance(trimmed, profile, locationText, workModeText)
            RoleCategory.GENERAL_TECH -> buildGeneralGuidance(trimmed, profile, locationText, workModeText)
        }
    }

    private fun buildAndroidGuidance(
        query: String,
        profile: UserProfile,
        location: String,
        workMode: String,
    ): AssistantPersonalizedGuidance {
        val commonSkills = listOf("Kotlin", "Android", "Jetpack Compose", "Git", "REST APIs", "Unit testing", "Coroutines", "Room")
        val (matched, missing) = partitionSkills(commonSkills, profile)

        val cards = listOf(
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Android Developer Intern",
                category = "Internship",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Kotlin", "Jetpack Compose", "Android SDK", "Git"),
                relevanceExplanation = "High demand for native Kotlin developers with modern Jetpack Compose UI architecture.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Android Developer Intern
                    Requirements:
                    - Must have proficiency in Kotlin and modern Android SDK.
                    - Experience with Jetpack Compose for declarative UI.
                    - Familiarity with Coroutines and RESTful API consumption.
                    - Knowledge of Git version control and Unit testing.
                """.trimIndent(),
            ),
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Associate Mobile Application Engineer",
                category = "Entry-Level",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Kotlin", "Android", "Coroutines", "Room", "Unit testing"),
                relevanceExplanation = "Entry-level native mobile role focusing on offline persistence and robust test coverage.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Associate Mobile Application Engineer
                    Requirements:
                    - Bachelor's degree or equivalent in Computer Science or Software Engineering.
                    - Solid understanding of Kotlin language fundamentals and OOP.
                    - Experience building offline-first apps using Room database.
                    - Working knowledge of automated testing and CI/CD pipelines.
                """.trimIndent(),
            ),
        )

        return AssistantPersonalizedGuidance(
            roleQuery = query,
            summary = "Android engineering roles seek strong native Kotlin fundamentals, declarative UI (Jetpack Compose), and clean architecture.",
            suitableRoles = listOf("Android Developer Intern", "Associate Mobile Engineer", "Junior Kotlin Developer"),
            commonlyRequestedSkills = commonSkills,
            matchedProfileSkills = matched,
            skillsToStrengthen = missing,
            projectIdeas = listOf(
                "Build an offline-first app utilizing Jetpack Compose, Room database, and StateFlow.",
                "Implement a public GitHub repo integrating a REST API with Retrofit and full Unit test coverage.",
            ),
            preparationChecklist = listOf(
                "Verify that your connected GitHub repos showcase Kotlin code commits.",
                "Write automated unit tests for business logic to generate strong code credibility.",
                "Document MVVM / MVI architecture patterns in your repository README files.",
            ),
            opportunityCards = cards,
            searchOptionsExplanation = "Search verified portals like LinkedIn, Handshake, or company careers pages for 'Android Intern'. Copy real postings into PROVEX to evaluate your code evidence.",
            profileMissingPrompt = if (profile.claimedSkills.isEmpty()) "Add your skills in Career Profile to personalize gap detection." else null,
        )
    }

    private fun buildPythonGuidance(
        query: String,
        profile: UserProfile,
        location: String,
        workMode: String,
    ): AssistantPersonalizedGuidance {
        val commonSkills = listOf("Python", "FastAPI", "Django", "SQL", "PostgreSQL", "Docker", "Git", "REST APIs", "Unit testing")
        val (matched, missing) = partitionSkills(commonSkills, profile)

        val cards = listOf(
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Python Backend Developer Intern",
                category = "Internship",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Python", "FastAPI", "SQL", "Git", "REST APIs"),
                relevanceExplanation = "FastAPI and asynchronous Python backend services for high-throughput API endpoints.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Python Backend Developer Intern
                    Requirements:
                    - Strong fundamentals in Python 3, data structures, and algorithms.
                    - Experience building web APIs with FastAPI or Django REST Framework.
                    - Proficiency with SQL and relational databases (PostgreSQL or MySQL).
                    - Familiarity with Git version control and writing automated tests with pytest.
                """.trimIndent(),
            ),
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Junior Software Engineer (Python/Cloud)",
                category = "Entry-Level",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Python", "Docker", "PostgreSQL", "Unit testing"),
                relevanceExplanation = "Entry-level backend position developing containerized microservices and automated test suites.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Junior Software Engineer (Python/Cloud)
                    Requirements:
                    - Practical experience with Python and relational database modeling.
                    - Understanding of Docker containerization and cloud basics.
                    - Proven ability to write modular, well-tested Python code.
                """.trimIndent(),
            ),
        )

        return AssistantPersonalizedGuidance(
            roleQuery = query,
            summary = "Python backend roles prioritize robust API development, relational database modeling, containerization, and automated testing.",
            suitableRoles = listOf("Python Backend Intern", "Junior Python Developer", "API Engineer"),
            commonlyRequestedSkills = commonSkills,
            matchedProfileSkills = matched,
            skillsToStrengthen = missing,
            projectIdeas = listOf(
                "Create a secure REST/GraphQL API with FastAPI, SQLAlchemy, and PostgreSQL.",
                "Containerize a Python microservice with Docker and set up automated pytest in GitHub Actions.",
            ),
            preparationChecklist = listOf(
                "Showcase production-ready Python projects in your connected GitHub repositories.",
                "Ensure your database migration scripts and test files are committed.",
                "Include API documentation (Swagger/OpenAPI) in your project showcases.",
            ),
            opportunityCards = cards,
            searchOptionsExplanation = "Search for 'Python Intern' or 'Junior Backend Developer' on major hiring portals. Copy actual job requirements into PROVEX to check your code fit.",
            profileMissingPrompt = if (profile.claimedSkills.isEmpty()) "Add Python and your backend skills to Career Profile for personalized matching." else null,
        )
    }

    private fun buildAiMlGuidance(
        query: String,
        profile: UserProfile,
        location: String,
        workMode: String,
    ): AssistantPersonalizedGuidance {
        val commonSkills = listOf("Python", "Machine Learning", "PyTorch", "TensorFlow", "Pandas", "SQL", "Git", "Data Analysis", "Docker")
        val (matched, missing) = partitionSkills(commonSkills, profile)

        val cards = listOf(
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "AI / Machine Learning Intern",
                category = "Internship",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Python", "PyTorch", "Pandas", "Machine Learning", "Git"),
                relevanceExplanation = "Model training, dataset evaluation, and data pipeline development for machine learning applications.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: AI / Machine Learning Intern
                    Requirements:
                    - Proficient in Python and scientific computing libraries (NumPy, Pandas, Scikit-Learn).
                    - Experience with deep learning frameworks such as PyTorch or TensorFlow.
                    - Understanding of model evaluation metrics, loss functions, and data preprocessing.
                    - Clean code practices and Git version control for reproducible experiments.
                """.trimIndent(),
            ),
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Junior Data Scientist / ML Associate",
                category = "Entry-Level",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Python", "SQL", "Data Analysis", "Machine Learning", "Docker"),
                relevanceExplanation = "Bridging exploratory data analysis with production-ready machine learning pipelines.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Junior Data Scientist / ML Associate
                    Requirements:
                    - Degree in Computer Science, Data Science, Mathematics, or equivalent.
                    - Strong analytical skills, SQL querying capability, and Python programming.
                    - Experience training and validating predictive models on real-world datasets.
                """.trimIndent(),
            ),
        )

        return AssistantPersonalizedGuidance(
            roleQuery = query,
            summary = "AI and ML positions require demonstrable fluency in Python, deep learning frameworks (PyTorch/TensorFlow), and reproducible data pipelines.",
            suitableRoles = listOf("AI/ML Intern", "Junior Machine Learning Engineer", "Data Science Intern"),
            commonlyRequestedSkills = commonSkills,
            matchedProfileSkills = matched,
            skillsToStrengthen = missing,
            projectIdeas = listOf(
                "Train and deploy a computer vision or NLP model with clear precision/recall benchmarks.",
                "Build an end-to-end data pipeline with feature engineering, model versioning, and Docker deployment.",
            ),
            preparationChecklist = listOf(
                "Provide Jupyter notebooks and executable Python scripts in your connected GitHub repositories.",
                "Document data sources, validation methodology, and baseline comparisons.",
                "Demonstrate data wrangling and SQL competency in addition to model modeling.",
            ),
            opportunityCards = cards,
            searchOptionsExplanation = "Look for 'AI Intern' or 'Machine Learning Co-op' listings on specialized tech job boards. Run job requirements through PROVEX to check evidence.",
            profileMissingPrompt = if (profile.claimedSkills.isEmpty()) "Specify your machine learning tools in Career Profile for better matching." else null,
        )
    }

    private fun buildFrontendGuidance(
        query: String,
        profile: UserProfile,
        location: String,
        workMode: String,
    ): AssistantPersonalizedGuidance {
        val commonSkills = listOf("JavaScript", "TypeScript", "React", "Next.js", "HTML", "CSS", "Git", "REST APIs", "Unit testing")
        val (matched, missing) = partitionSkills(commonSkills, profile)

        val cards = listOf(
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Frontend Developer Intern",
                category = "Internship",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("React", "TypeScript", "HTML", "CSS", "Git"),
                relevanceExplanation = "Developing responsive user interfaces with modern React, TypeScript, and state management.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Frontend Developer Intern
                    Requirements:
                    - Strong foundation in HTML5, CSS3, modern JavaScript, and TypeScript.
                    - Hands-on experience developing single-page web applications with React.
                    - Understanding of responsive design, web performance, and state management.
                    - Familiarity with Git collaboration workflows.
                """.trimIndent(),
            ),
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Junior Web Engineer",
                category = "Entry-Level",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("TypeScript", "React", "Next.js", "REST APIs", "Unit testing"),
                relevanceExplanation = "Building server-rendered and responsive web applications with TypeScript.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Junior Web Engineer
                    Requirements:
                    - Bachelor's degree in Computer Science or demonstrable frontend portfolio.
                    - Experience with React, Next.js, and client-server REST communication.
                    - Familiarity with frontend automated testing (Jest, Playwright, or Cypress).
                """.trimIndent(),
            ),
        )

        return AssistantPersonalizedGuidance(
            roleQuery = query,
            summary = "Frontend engineering requires strong proficiency in TypeScript, component-driven UI frameworks (React), and responsive web standards.",
            suitableRoles = listOf("Frontend Developer Intern", "Junior UI Engineer", "React Developer Intern"),
            commonlyRequestedSkills = commonSkills,
            matchedProfileSkills = matched,
            skillsToStrengthen = missing,
            projectIdeas = listOf(
                "Build a complex data dashboard with TypeScript, React, responsive layouts, and dark mode.",
                "Implement automated end-to-end or unit tests with Jest / React Testing Library.",
            ),
            preparationChecklist = listOf(
                "Ensure your GitHub repositories contain clean TypeScript code without build errors.",
                "Provide live demo links in your repository README files.",
                "Showcase adherence to web accessibility standards (semantic HTML and ARIA).",
            ),
            opportunityCards = cards,
            searchOptionsExplanation = "Search for 'Frontend Intern' or 'Junior React Developer' on verified hiring boards. Paste requirement descriptions into PROVEX for evaluation.",
            profileMissingPrompt = if (profile.claimedSkills.isEmpty()) "List your frontend frameworks in Career Profile to highlight your strongest skills." else null,
        )
    }

    private fun buildDevopsGuidance(
        query: String,
        profile: UserProfile,
        location: String,
        workMode: String,
    ): AssistantPersonalizedGuidance {
        val commonSkills = listOf("Docker", "Kubernetes", "Linux", "Git", "CI/CD", "Python", "Go", "AWS", "Cloud")
        val (matched, missing) = partitionSkills(commonSkills, profile)

        val cards = listOf(
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Cloud & DevOps Intern",
                category = "Internship",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Docker", "Linux", "CI/CD", "Git", "Python"),
                relevanceExplanation = "Automating deployment pipelines, containerization, and infrastructure configuration.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Cloud & DevOps Intern
                    Requirements:
                    - Familiarity with Linux shell scripting and command line environments.
                    - Hands-on experience containerizing applications using Docker.
                    - Understanding of CI/CD pipeline automation (GitHub Actions, GitLab CI).
                    - Basic familiarity with cloud infrastructure concepts (AWS, GCP, or Azure).
                """.trimIndent(),
            ),
        )

        return AssistantPersonalizedGuidance(
            roleQuery = query,
            summary = "DevOps positions focus on containerization, infrastructure as code, CI/CD pipeline automation, and Linux systems administration.",
            suitableRoles = listOf("DevOps Intern", "Cloud Infrastructure Intern", "Site Reliability Associate"),
            commonlyRequestedSkills = commonSkills,
            matchedProfileSkills = matched,
            skillsToStrengthen = missing,
            projectIdeas = listOf(
                "Create a multi-container Docker Compose setup with automated GitHub Actions CI/CD deployment.",
                "Author Terraform or Ansible scripts configuring a cloud-hosted virtual environment.",
            ),
            preparationChecklist = listOf(
                "Commit Dockerfiles and GitHub Actions workflow YAML configurations to your repositories.",
                "Demonstrate monitoring, logging, and security best practices in your infrastructure code.",
            ),
            opportunityCards = cards,
            searchOptionsExplanation = "Search 'DevOps Intern' on tech job listings. Paste posting requirements into PROVEX to compare against your repo evidence.",
            profileMissingPrompt = if (profile.claimedSkills.isEmpty()) "Add DevOps and cloud skills in Career Profile to tailor your preparation." else null,
        )
    }

    private fun buildJuniorSeGuidance(
        query: String,
        profile: UserProfile,
        location: String,
        workMode: String,
    ): AssistantPersonalizedGuidance {
        val commonSkills = listOf("Java", "Python", "SQL", "Git", "Data Structures", "Algorithms", "Unit testing", "REST APIs", "Docker")
        val (matched, missing) = partitionSkills(commonSkills, profile)

        val cards = listOf(
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Junior Software Developer",
                category = "Entry-Level",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Java", "SQL", "Git", "Unit testing", "REST APIs"),
                relevanceExplanation = "Core software development role emphasizing solid computer science fundamentals, clean code, and team collaboration.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Junior Software Developer
                    Requirements:
                    - Bachelor's degree in Computer Science, Software Engineering, or related technical field.
                    - Proficiency in at least one object-oriented language (Java, C++, C#, or Python).
                    - Strong understanding of algorithms, data structures, and relational databases.
                    - Experience with Git version control and writing automated unit tests.
                """.trimIndent(),
            ),
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = "Software Engineering Intern",
                category = "Internship",
                workMode = workMode,
                location = location,
                relevantSkills = listOf("Python", "Java", "Git", "REST APIs"),
                relevanceExplanation = "University internship focused on contributing to full-lifecycle software features alongside senior mentors.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: Software Engineering Intern
                    Requirements:
                    - Currently pursuing a degree in Computer Science or related STEM program.
                    - Solid coding proficiency and problem-solving abilities.
                    - Eagerness to learn new languages, frameworks, and engineering best practices.
                    - Strong written and verbal technical communication skills.
                """.trimIndent(),
            ),
        )

        return AssistantPersonalizedGuidance(
            roleQuery = query,
            summary = "General software engineering roles focus on computer science fundamentals: algorithms, OOP design, relational data modeling, and automated testing.",
            suitableRoles = listOf("Junior Software Developer", "Software Engineer Intern", "Associate Applications Developer"),
            commonlyRequestedSkills = commonSkills,
            matchedProfileSkills = matched,
            skillsToStrengthen = missing,
            projectIdeas = listOf(
                "Build a full-stack application featuring a database backend, authentication, and comprehensive unit tests.",
                "Contribute to an open-source library or create a utility package with documented test suites.",
            ),
            preparationChecklist = listOf(
                "Showcase clean coding patterns and consistent commit histories across your connected repositories.",
                "Include unit tests and integration tests in your project codebases.",
                "Write detailed documentation explaining setup instructions and architecture decisions.",
            ),
            opportunityCards = cards,
            searchOptionsExplanation = "Search for 'Software Engineer Intern' or 'New Grad Software Engineer'. Once you find an interesting posting, paste the text into PROVEX to evaluate requirements.",
            profileMissingPrompt = if (profile.claimedSkills.isEmpty()) "Fill in your known languages and target role in Career Profile for customized gap analysis." else null,
        )
    }

    private fun buildGeneralGuidance(
        query: String,
        profile: UserProfile,
        location: String,
        workMode: String,
    ): AssistantPersonalizedGuidance {
        val commonSkills = listOf("Git", "Problem Solving", "Unit testing", "Algorithms", "REST APIs")
        val (matched, missing) = partitionSkills(commonSkills, profile)

        val cards = listOf(
            AssistantOpportunityCard(
                id = UUID.randomUUID().toString(),
                roleTitle = if (query.isNotBlank()) query.replaceFirstChar { it.uppercase() } else "Software Developer Intern",
                category = "Role Archetype",
                workMode = workMode,
                location = location,
                relevantSkills = if (profile.claimedSkills.isNotEmpty()) profile.claimedSkills.take(4) else listOf("Git", "Unit testing", "Problem Solving"),
                relevanceExplanation = "Target role guidance tailored to your query. Focus on building demonstrable code artifacts.",
                sourceLabel = "PROVEX Role Archetype",
                sampleJobDescription = """
                    Job Title: $query
                    Requirements:
                    - Passion for software engineering and problem solving.
                    - Demonstrable projects or coursework in relevant technologies.
                    - Familiarity with version control using Git.
                    - Ability to write clean, maintainable, and tested code.
                """.trimIndent(),
            ),
        )

        return AssistantPersonalizedGuidance(
            roleQuery = query,
            summary = "For '$query', employers value demonstrable project evidence, strong software development fundamentals, and clean version-controlled repositories.",
            suitableRoles = listOf("$query Intern", "Junior $query", "Associate Developer"),
            commonlyRequestedSkills = commonSkills,
            matchedProfileSkills = matched,
            skillsToStrengthen = missing,
            projectIdeas = listOf(
                "Develop a public portfolio project specifically demonstrating the core technical requirements for '$query'.",
                "Ensure your code repository has clean commits, automated tests, and a clear architectural diagram.",
            ),
            preparationChecklist = listOf(
                "Inspect real employer postings for '$query' to extract mandatory skills.",
                "Paste real job descriptions into PROVEX's 'Check your fit' analyzer to verify code evidence alignment.",
            ),
            opportunityCards = cards,
            searchOptionsExplanation = "Find genuine vacancies on LinkedIn, Handshake, or company careers pages for '$query'. Paste the job requirements into PROVEX to evaluate evidence.",
            profileMissingPrompt = if (profile.isEmpty) "Complete your Career Profile details (skills, target role, education) for more specific recommendations." else null,
        )
    }

    private fun partitionSkills(
        catalogSkills: List<String>,
        profile: UserProfile,
    ): Pair<List<String>, List<String>> {
        val matched = catalogSkills.filter { skill -> profile.claims(skill) }
        val missing = catalogSkills.filterNot { skill -> profile.claims(skill) }
        return Pair(matched, missing)
    }
}

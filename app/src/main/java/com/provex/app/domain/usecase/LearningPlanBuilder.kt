package com.provex.app.domain.usecase

import com.provex.app.domain.model.LearningPlanItem
import com.provex.app.domain.model.LearningResource
import com.provex.app.domain.model.ResourceType
import com.provex.app.domain.model.SkillAssessment

/**
 * Builds a learning plan from template text. Links are well-known official documentation
 * homepages, but they are NOT verified by a PROVEX backend, so they are flagged unverified
 * and presented as illustrative.
 */
object LearningPlanBuilder {

    private val officialDocs: Map<String, Pair<String, String>> = mapOf(
        "kotlin" to ("Kotlin documentation" to "https://kotlinlang.org/docs/home.html"),
        "java" to ("Dev.java learning" to "https://dev.java/learn/"),
        "python" to ("The Python Tutorial" to "https://docs.python.org/3/tutorial/"),
        "javascript" to ("MDN JavaScript guide" to "https://developer.mozilla.org/en-US/docs/Web/JavaScript"),
        "typescript" to ("TypeScript documentation" to "https://www.typescriptlang.org/docs/"),
        "go" to ("Go documentation" to "https://go.dev/doc/"),
        "rust" to ("The Rust Book" to "https://doc.rust-lang.org/book/"),
        "react" to ("React: Learn" to "https://react.dev/learn"),
        "node.js" to ("Node.js: Learn" to "https://nodejs.org/en/learn"),
        "android" to ("Android developer courses" to "https://developer.android.com/courses"),
        "jetpack compose" to ("Jetpack Compose" to "https://developer.android.com/compose"),
        "django" to ("Django documentation" to "https://docs.djangoproject.com/"),
        "flask" to ("Flask documentation" to "https://flask.palletsprojects.com/"),
        "fastapi" to ("FastAPI tutorial" to "https://fastapi.tiangolo.com/tutorial/"),
        "spring boot" to ("Spring guides" to "https://spring.io/guides"),
        "docker" to ("Docker: Get started" to "https://docs.docker.com/get-started/"),
        "kubernetes" to ("Kubernetes tutorials" to "https://kubernetes.io/docs/tutorials/"),
        "postgresql" to ("PostgreSQL tutorial" to "https://www.postgresql.org/docs/current/tutorial.html"),
        "mongodb" to ("MongoDB documentation" to "https://www.mongodb.com/docs/"),
        "git" to ("Git documentation" to "https://git-scm.com/doc"),
        "aws" to ("AWS documentation" to "https://docs.aws.amazon.com/"),
        "github actions" to ("GitHub Actions docs" to "https://docs.github.com/en/actions"),
    )

    fun build(jobTitle: String, assessments: List<SkillAssessment>): List<LearningPlanItem> =
        assessments.filter { AssessmentEngine.isGap(it) }.map { a ->
            val docs = officialDocs[a.skill.lowercase()]
            val resources = buildList {
                if (docs != null) {
                    add(LearningResource(docs.first, docs.second, ResourceType.DOCS, verified = false))
                } else {
                    add(LearningResource("Search the official ${a.skill} documentation", null, ResourceType.DOCS, verified = false))
                }
                add(LearningResource("Find a beginner tutorial for ${a.skill}", null, ResourceType.TUTORIAL, verified = false))
            }
            LearningPlanItem(
                skill = a.skill,
                whyItMatters = "${a.skill} is ${a.importance.label.lowercase()} in \"$jobTitle\". " +
                    "Current status: ${a.status.label}.",
                resources = resources,
                taskTitle = "Ship a small tested feature using ${a.skill}",
                taskDescription = "Choose one of your own repositories and add a small, focused feature that genuinely " +
                    "uses ${a.skill}. Keep it realistic enough that another developer could review it.",
                acceptanceCriteria = listOf(
                    "The ${a.skill} code is committed to a repository you have connected to PROVEX.",
                    "It includes at least one automated test (or a documented manual test).",
                    "The README explains how ${a.skill} is used and how to run the project.",
                    "A fresh repository analysis finds validated code evidence for ${a.skill}.",
                ),
                illustrative = true,
            )
        }.sortedWith(compareBy { it.skill })
}

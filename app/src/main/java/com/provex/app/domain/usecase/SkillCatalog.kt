package com.provex.app.domain.usecase

import com.provex.app.domain.model.RequirementCategory
import com.provex.app.domain.model.RequirementCategory.CLOUD_DEPLOY
import com.provex.app.domain.model.RequirementCategory.DATABASE
import com.provex.app.domain.model.RequirementCategory.FRAMEWORK
import com.provex.app.domain.model.RequirementCategory.GENERAL
import com.provex.app.domain.model.RequirementCategory.LANGUAGE
import com.provex.app.domain.model.RequirementCategory.TESTING

class CatalogSkill(
    val name: String,
    val category: RequirementCategory,
    aliases: List<String>,
    caseSensitive: Boolean = false,
) {
    val regex: Regex = Regex(
        aliases.joinToString("|", prefix = "(?<![A-Za-z0-9_+#])(?:", postfix = ")(?![A-Za-z0-9_+#])") {
            Regex.escape(it)
        },
        if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE),
    )
}

/**
 * Vocabulary used by on-device keyword extraction and by the profile skill pickers.
 * Extraction only reports a skill when one of its aliases literally appears in the text.
 */
object SkillCatalog {
    val all: List<CatalogSkill> = listOf(
        CatalogSkill("Kotlin", LANGUAGE, listOf("Kotlin")),
        CatalogSkill("Java", LANGUAGE, listOf("Java")),
        CatalogSkill("Python", LANGUAGE, listOf("Python")),
        CatalogSkill("JavaScript", LANGUAGE, listOf("JavaScript", "JS", "ECMAScript")),
        CatalogSkill("TypeScript", LANGUAGE, listOf("TypeScript")),
        CatalogSkill("C++", LANGUAGE, listOf("C++")),
        CatalogSkill("C#", LANGUAGE, listOf("C#")),
        CatalogSkill("Go", LANGUAGE, listOf("Golang", "Go"), caseSensitive = true),
        CatalogSkill("Rust", LANGUAGE, listOf("Rust")),
        CatalogSkill("Swift", LANGUAGE, listOf("Swift")),
        CatalogSkill("SQL", LANGUAGE, listOf("SQL")),
        CatalogSkill("PHP", LANGUAGE, listOf("PHP")),
        CatalogSkill("Ruby", LANGUAGE, listOf("Ruby")),
        CatalogSkill("Dart", LANGUAGE, listOf("Dart")),
        CatalogSkill("HTML", LANGUAGE, listOf("HTML", "HTML5")),
        CatalogSkill("CSS", LANGUAGE, listOf("CSS", "CSS3")),

        CatalogSkill("Android", FRAMEWORK, listOf("Android")),
        CatalogSkill("Jetpack Compose", FRAMEWORK, listOf("Jetpack Compose", "Compose")),
        CatalogSkill("React", FRAMEWORK, listOf("React", "React.js", "ReactJS")),
        CatalogSkill("React Native", FRAMEWORK, listOf("React Native")),
        CatalogSkill("Angular", FRAMEWORK, listOf("Angular", "AngularJS")),
        CatalogSkill("Vue", FRAMEWORK, listOf("Vue", "Vue.js")),
        CatalogSkill("Next.js", FRAMEWORK, listOf("Next.js", "NextJS")),
        CatalogSkill("Node.js", FRAMEWORK, listOf("Node.js", "NodeJS")),
        CatalogSkill("Express", FRAMEWORK, listOf("Express.js", "ExpressJS")),
        CatalogSkill("Spring Boot", FRAMEWORK, listOf("Spring Boot", "Spring")),
        CatalogSkill("Django", FRAMEWORK, listOf("Django")),
        CatalogSkill("Flask", FRAMEWORK, listOf("Flask")),
        CatalogSkill("FastAPI", FRAMEWORK, listOf("FastAPI")),
        CatalogSkill("Flutter", FRAMEWORK, listOf("Flutter")),
        CatalogSkill(".NET", FRAMEWORK, listOf(".NET", "ASP.NET")),

        CatalogSkill("PostgreSQL", DATABASE, listOf("PostgreSQL", "Postgres")),
        CatalogSkill("MySQL", DATABASE, listOf("MySQL")),
        CatalogSkill("MongoDB", DATABASE, listOf("MongoDB")),
        CatalogSkill("SQLite", DATABASE, listOf("SQLite")),
        CatalogSkill("Redis", DATABASE, listOf("Redis")),
        CatalogSkill("Firebase", DATABASE, listOf("Firebase", "Firestore")),

        CatalogSkill("Unit testing", TESTING, listOf("unit testing", "unit tests", "unit test")),
        CatalogSkill("Integration testing", TESTING, listOf("integration testing", "integration tests")),
        CatalogSkill("Test-driven development", TESTING, listOf("TDD", "test-driven development")),
        CatalogSkill("JUnit", TESTING, listOf("JUnit")),
        CatalogSkill("Mockito", TESTING, listOf("Mockito")),
        CatalogSkill("pytest", TESTING, listOf("pytest")),
        CatalogSkill("Jest", TESTING, listOf("Jest")),
        CatalogSkill("Espresso", TESTING, listOf("Espresso")),
        CatalogSkill("Selenium", TESTING, listOf("Selenium")),

        CatalogSkill("Docker", CLOUD_DEPLOY, listOf("Docker")),
        CatalogSkill("Kubernetes", CLOUD_DEPLOY, listOf("Kubernetes", "K8s")),
        CatalogSkill("AWS", CLOUD_DEPLOY, listOf("AWS", "Amazon Web Services")),
        CatalogSkill("Google Cloud", CLOUD_DEPLOY, listOf("Google Cloud", "GCP")),
        CatalogSkill("Azure", CLOUD_DEPLOY, listOf("Azure")),
        CatalogSkill("CI/CD", CLOUD_DEPLOY, listOf("CI/CD", "continuous integration", "continuous delivery")),
        CatalogSkill("GitHub Actions", CLOUD_DEPLOY, listOf("GitHub Actions")),
        CatalogSkill("Terraform", CLOUD_DEPLOY, listOf("Terraform")),
        CatalogSkill("Linux", CLOUD_DEPLOY, listOf("Linux")),

        CatalogSkill("Git", GENERAL, listOf("Git", "GitHub")),
        CatalogSkill("REST APIs", GENERAL, listOf("REST", "RESTful", "REST API", "REST APIs")),
        CatalogSkill("GraphQL", GENERAL, listOf("GraphQL")),
        CatalogSkill("Data structures & algorithms", GENERAL, listOf("data structures", "algorithms")),
        CatalogSkill("Object-oriented programming", GENERAL, listOf("OOP", "object-oriented", "object oriented")),
        CatalogSkill("MVVM", GENERAL, listOf("MVVM")),
        CatalogSkill("Microservices", GENERAL, listOf("microservices")),
        CatalogSkill("Machine learning", GENERAL, listOf("machine learning")),
        CatalogSkill("Agile", GENERAL, listOf("Agile", "Scrum")),
    )

    val names: List<String> get() = all.map { it.name }
    val languageNames: List<String> get() = all.filter { it.category == LANGUAGE }.map { it.name }
    val frameworkNames: List<String> get() = all.filter { it.category == FRAMEWORK || it.category == DATABASE }.map { it.name }
    val generalNames: List<String> get() = all.filter { it.category !in setOf(LANGUAGE, FRAMEWORK, DATABASE) }.map { it.name }
}

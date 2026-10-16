package com.provex.app.navigation

sealed class Destination(val route: String) {
    data object Splash : Destination("splash")
    data object Login : Destination("login")
    data object Register : Destination("register")
    data object Home : Destination("home")
    data object Profile : Destination("profile")
    data object Repositories : Destination("repositories")
    data object Extraction : Destination("extraction/{analysisId}") {
        fun createRoute(analysisId: String) = "extraction/$analysisId"
    }
    data object Comparison : Destination("comparison/{analysisId}") {
        fun createRoute(analysisId: String) = "comparison/$analysisId"
    }
    data object EvidenceDetail : Destination("evidence/{analysisId}/{skill}") {
        fun createRoute(analysisId: String, skill: String) = "evidence/$analysisId/$skill"
    }
    data object SkepticReview : Destination("skeptic")
    data object ProofPath : Destination("proofpath/{analysisId}") {
        fun createRoute(analysisId: String) = "proofpath/$analysisId"
    }
    data object ProofLoop : Destination("proofloop")
    data object History : Destination("history")
    data object Opportunities : Destination("opportunities")
    data object Settings : Destination("settings")
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val iconSymbol: String,
)

val bottomNavItems = listOf(
    BottomNavItem(Destination.Home.route, "Home", "⌂"),
    BottomNavItem(Destination.Opportunities.route, "Opportunities", "✦"),
    BottomNavItem(Destination.History.route, "Assessments", "⏱"),
    BottomNavItem(Destination.Profile.route, "Profile", "👤"),
)

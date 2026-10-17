package com.provex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.provex.app.domain.model.AuthState
import com.provex.app.navigation.Destination
import com.provex.app.navigation.bottomNavItems
import com.provex.app.ui.screens.assessment.ComparisonScreen
import com.provex.app.ui.screens.assessment.ComparisonViewModel
import com.provex.app.ui.screens.auth.LoginScreen
import com.provex.app.ui.screens.auth.LoginViewModel
import com.provex.app.ui.screens.auth.RegisterScreen
import com.provex.app.ui.screens.auth.RegisterViewModel
import com.provex.app.ui.screens.splash.SplashScreen
import com.provex.app.ui.screens.splash.SplashViewModel
import com.provex.app.ui.screens.evidence.EvidenceDetailScreen
import com.provex.app.ui.screens.evidence.EvidenceDetailViewModel
import com.provex.app.ui.screens.history.HistoryScreen
import com.provex.app.ui.screens.history.HistoryViewModel
import com.provex.app.ui.screens.home.HomeScreen
import com.provex.app.ui.screens.home.HomeViewModel
import com.provex.app.ui.screens.opportunity.ExtractionScreen
import com.provex.app.ui.screens.opportunity.ExtractionViewModel
import android.os.SystemClock
import androidx.compose.material3.FabPosition
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.provex.app.ui.components.RoleAdvisorFloatingBubble
import com.provex.app.ui.screens.explore.ExploreScreen
import com.provex.app.ui.screens.explore.ExploreViewModel
import com.provex.app.ui.screens.analyzer.JobSkillAnalyzerScreen
import com.provex.app.ui.screens.analyzer.JobSkillAnalyzerViewModel
import com.provex.app.ui.screens.opportunity.OpportunitiesScreen
import com.provex.app.ui.screens.opportunity.OpportunitiesViewModel
import com.provex.app.ui.screens.profile.ProfileScreen
import com.provex.app.ui.screens.profile.ProfileViewModel
import com.provex.app.ui.screens.proofloop.ProofLoopScreen
import com.provex.app.ui.screens.proofpath.ProofPathScreen
import com.provex.app.ui.screens.proofpath.ProofPathViewModel
import com.provex.app.ui.screens.repositories.RepositoriesScreen
import com.provex.app.ui.screens.repositories.RepositoriesViewModel
import com.provex.app.ui.screens.settings.SettingsScreen
import com.provex.app.ui.screens.settings.SettingsViewModel
import com.provex.app.ui.screens.skeptic.SkepticReviewScreen
import com.provex.app.ui.theme.DarkGreen
import com.provex.app.ui.theme.MintGreen
import com.provex.app.ui.theme.NavyDark
import com.provex.app.ui.theme.NavyPrimary
import com.provex.app.ui.theme.ProvexTheme
import com.provex.app.ui.theme.SoftBlue
import com.provex.app.ui.theme.SurfaceAlt
import com.provex.app.ui.theme.SurfaceWhite
import com.provex.app.ui.theme.TextPrimary
import com.provex.app.ui.theme.TextSecondary

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var appContainer: com.provex.app.di.AppContainer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ProvexApplication).container
        appContainer = container

        handleIntent(intent)

        setContent {
            ProvexTheme {
                ProvexApp(container = container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            appContainer?.githubRepository?.refresh()
        }
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.data
        if (data?.scheme == "provex" && data.host == "github-callback") {
            lifecycleScope.launch {
                appContainer?.githubRepository?.refresh()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvexApp(container: com.provex.app.di.AppContainer) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authState by container.authRepository.authState.collectAsState(initial = null)
    val isSignedOut = authState == AuthState.SignedOut

    // Retained at ProvexApp level to preserve chat conversation state across navigation
    val roleAdvisorViewModel: OpportunitiesViewModel = viewModel {
        OpportunitiesViewModel(
            profileRepository = container.profileRepository,
            jobDraftHolder = container.jobDraftHolder,
        )
    }

    var lastAdvisorClickTime by remember { mutableLongStateOf(0L) }
    val onOpenRoleAdvisor: () -> Unit = {
        val now = SystemClock.elapsedRealtime()
        if (now - lastAdvisorClickTime > 750L) {
            lastAdvisorClickTime = now
            navController.navigate(Destination.RoleAdvisor.route) {
                launchSingleTop = true
            }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Destination.Home.route,
        Destination.Explore.route,
        Destination.History.route,
        Destination.Profile.route,
    )

    val unauthenticatedRoutes = setOf(
        Destination.Splash.route,
        Destination.Login.route,
        Destination.Register.route,
    )

    val hideFloatingBubbleRoutes = setOf(
        Destination.Splash.route,
        Destination.Login.route,
        Destination.Register.route,
        Destination.RoleAdvisor.route,
    )
    val showFloatingAdvisor = currentRoute !in hideFloatingBubbleRoutes && !isSignedOut

    Scaffold(
        floatingActionButton = {
            if (showFloatingAdvisor) {
                RoleAdvisorFloatingBubble(
                    onClick = onOpenRoleAdvisor,
                    modifier = Modifier.padding(bottom = if (showBottomBar) 0.dp else 12.dp),
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        topBar = {
            if (currentRoute !in unauthenticatedRoutes) {
                TopAppBar(
                    title = {
                        Text(
                            text = "PROVEX",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = SurfaceWhite,
                            ),
                        )
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Destination.Settings.route) }) {
                            Text(
                                text = "⚙",
                                fontSize = 18.sp,
                                color = SurfaceWhite,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavyPrimary,
                        titleContentColor = SurfaceWhite,
                        navigationIconContentColor = SurfaceWhite,
                        actionIconContentColor = SurfaceWhite,
                    ),
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = NavyDark,
                    tonalElevation = 4.dp,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Text(
                                    text = item.iconSymbol,
                                    fontSize = 18.sp,
                                    color = if (selected) MintGreen else TextSecondary,
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (selected) SurfaceWhite else TextSecondary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    ),
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = NavyPrimary,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Splash.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(Destination.Splash.route) {
                val vm = viewModel { SplashViewModel(container.authRepository) }
                SplashScreen(
                    viewModel = vm,
                    onNavigateToHome = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Destination.Login.route) {
                            popUpTo(Destination.Splash.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Destination.Login.route) {
                val vm = viewModel { LoginViewModel(container.authRepository) }
                LoginScreen(
                    viewModel = vm,
                    onNavigateToHome = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Destination.Register.route)
                    },
                )
            }

            composable(Destination.Register.route) {
                val vm = viewModel { RegisterViewModel(container.authRepository) }
                RegisterScreen(
                    viewModel = vm,
                    onNavigateToHome = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(Destination.Register.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Destination.Login.route) {
                            popUpTo(Destination.Register.route) { inclusive = true }
                        }
                    },
                )
            }



            composable(Destination.Home.route) {
                val vm = viewModel {
                    HomeViewModel(
                        analysisRepository = container.analysisRepository,
                        profileRepository = container.profileRepository,
                        githubRepository = container.githubRepository,
                        insightsRepository = container.insightsRepository,
                        authRepository = container.authRepository,
                        jobDraftHolder = container.jobDraftHolder,
                    )
                }
                HomeScreen(
                    viewModel = vm,
                    onNavigateToExtraction = { id -> navController.navigate(Destination.Extraction.createRoute(id)) },
                    onNavigateToProfile = { navController.navigate(Destination.Profile.route) },
                    onNavigateToRepositories = { navController.navigate(Destination.Repositories.route) },
                    onNavigateToComparison = { id -> navController.navigate(Destination.Comparison.createRoute(id)) },
                    onNavigateToProofPath = { id -> navController.navigate(Destination.ProofPath.createRoute(id)) },
                    onNavigateToOpportunities = { navController.navigate(Destination.Explore.route) },
                    onNavigateToRoleAdvisor = onOpenRoleAdvisor,
                )
            }

            composable(Destination.Profile.route) {
                val vm = viewModel { ProfileViewModel(container.profileRepository, container.githubRepository) }
                ProfileScreen(
                    viewModel = vm,
                    onNavigateToSettings = { navController.navigate(Destination.Settings.route) },
                    onNavigateToRepositories = { navController.navigate(Destination.Repositories.route) },
                )
            }

            composable(Destination.Repositories.route) {
                val vm = viewModel { RepositoriesViewModel(container.githubRepository) }
                RepositoriesScreen(viewModel = vm)
            }

            composable(
                route = Destination.Extraction.route,
                arguments = listOf(navArgument("analysisId") { type = NavType.StringType }),
            ) { backStack ->
                val analysisId = backStack.arguments?.getString("analysisId") ?: ""
                val vm = viewModel {
                    ExtractionViewModel(
                        analysisId = analysisId,
                        analysisRepository = container.analysisRepository,
                    )
                }
                ExtractionScreen(
                    viewModel = vm,
                    onNavigateToComparison = { id ->
                        navController.navigate(Destination.Comparison.createRoute(id)) {
                            popUpTo(Destination.Home.route)
                        }
                    },
                )
            }

            composable(
                route = Destination.Comparison.route,
                arguments = listOf(navArgument("analysisId") { type = NavType.StringType }),
            ) { backStack ->
                val analysisId = backStack.arguments?.getString("analysisId") ?: ""
                val vm = viewModel {
                    ComparisonViewModel(
                        analysisId = analysisId,
                        analysisRepository = container.analysisRepository,
                    )
                }
                ComparisonScreen(
                    viewModel = vm,
                    onNavigateToEvidenceDetail = { id, skill ->
                        navController.navigate(Destination.EvidenceDetail.createRoute(id, skill))
                    },
                    onNavigateToProofPath = { id ->
                        navController.navigate(Destination.ProofPath.createRoute(id))
                    },
                    onNavigateToSkeptic = {
                        navController.navigate(Destination.SkepticReview.route)
                    },
                )
            }

            composable(
                route = Destination.EvidenceDetail.route,
                arguments = listOf(
                    navArgument("analysisId") { type = NavType.StringType },
                    navArgument("skill") { type = NavType.StringType },
                ),
            ) { backStack ->
                val analysisId = backStack.arguments?.getString("analysisId") ?: ""
                val skill = backStack.arguments?.getString("skill") ?: ""
                val vm = viewModel {
                    EvidenceDetailViewModel(
                        analysisId = analysisId,
                        skill = skill,
                        analysisRepository = container.analysisRepository,
                    )
                }
                EvidenceDetailScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Destination.SkepticReview.route) {
                SkepticReviewScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Destination.ProofPath.route,
                arguments = listOf(navArgument("analysisId") { type = NavType.StringType }),
            ) { backStack ->
                val analysisId = backStack.arguments?.getString("analysisId") ?: ""
                val vm = viewModel {
                    ProofPathViewModel(
                        analysisId = analysisId,
                        analysisRepository = container.analysisRepository,
                        learningRepository = container.learningRepository,
                    )
                }
                ProofPathScreen(
                    viewModel = vm,
                    onNavigateToProofLoop = { navController.navigate(Destination.ProofLoop.route) },
                )
            }

            composable(Destination.ProofLoop.route) {
                ProofLoopScreen(onBack = { navController.popBackStack() })
            }

            composable(Destination.History.route) {
                val vm = viewModel { HistoryViewModel(container.analysisRepository) }
                HistoryScreen(
                    viewModel = vm,
                    onNavigateToComparison = { id -> navController.navigate(Destination.Comparison.createRoute(id)) },
                )
            }

            composable(Destination.Explore.route) {
                val exploreVm = viewModel {
                    ExploreViewModel(
                        profileRepository = container.profileRepository,
                        insightsRepository = container.insightsRepository,
                        jobDraftHolder = container.jobDraftHolder,
                    )
                }
                val analyzerVm = viewModel {
                    JobSkillAnalyzerViewModel(
                        repository = container.jobAnalyzerRepository,
                        draftHolder = container.jobDraftHolder,
                    )
                }
                ExploreScreen(
                    viewModel = exploreVm,
                    analyzerViewModel = analyzerVm,
                    onNavigateToHome = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRoleAdvisor = { prompt ->
                        if (!prompt.isNullOrBlank()) {
                            roleAdvisorViewModel.onSelectPrompt(prompt)
                        }
                        onOpenRoleAdvisor()
                    },
                )
            }

            composable(Destination.Opportunities.route) {
                val exploreVm = viewModel {
                    ExploreViewModel(
                        profileRepository = container.profileRepository,
                        insightsRepository = container.insightsRepository,
                        jobDraftHolder = container.jobDraftHolder,
                    )
                }
                val analyzerVm = viewModel {
                    JobSkillAnalyzerViewModel(
                        repository = container.jobAnalyzerRepository,
                        draftHolder = container.jobDraftHolder,
                    )
                }
                ExploreScreen(
                    viewModel = exploreVm,
                    analyzerViewModel = analyzerVm,
                    onNavigateToHome = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRoleAdvisor = { prompt ->
                        if (!prompt.isNullOrBlank()) {
                            roleAdvisorViewModel.onSelectPrompt(prompt)
                        }
                        onOpenRoleAdvisor()
                    },
                    onConnectGitHub = {
                        navController.navigate(Destination.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Destination.JobAnalyzer.route) {
                val analyzerVm = viewModel {
                    JobSkillAnalyzerViewModel(
                        repository = container.jobAnalyzerRepository,
                        draftHolder = container.jobDraftHolder,
                    )
                }
                JobSkillAnalyzerScreen(
                    viewModel = analyzerVm,
                    onBack = { navController.popBackStack() },
                    onConnectGitHub = {
                        navController.navigate(Destination.Profile.route) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Destination.RoleAdvisor.route) {
                OpportunitiesScreen(
                    viewModel = roleAdvisorViewModel,
                    onNavigateToHome = {
                        navController.navigate(Destination.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProfile = { navController.navigate(Destination.Profile.route) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Destination.Settings.route) {
                val vm = viewModel {
                    SettingsViewModel(
                        authRepository = container.authRepository,
                        githubRepository = container.githubRepository,
                        settingsRepository = container.settingsRepository,
                    )
                }
                SettingsScreen(
                    viewModel = vm,
                    onNavigateToProfile = { navController.navigate(Destination.Profile.route) },
                    onNavigateToRepositories = { navController.navigate(Destination.Repositories.route) },
                    onSignOut = {
                        navController.navigate(Destination.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}

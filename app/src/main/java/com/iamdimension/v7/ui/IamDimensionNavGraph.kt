@file:Suppress("unused")

package com.iamdimension.v7.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * ====================================================================================
 * LROS VR26.10 — I.A.M DIMENSION V.7 (0519 PORTAL ZONE)
 * NAVIGATION GRAPH & MAIN ACTIVITY HOST (`IamDimensionNavGraph.kt`)
 * ====================================================================================
 * Author  : Gemini Argon (For Commander Rex the Dopelganger)
 * Protocol: VISUAL TRUTH · MODULAR ENVELOPING · ZERO-ERROR PROTOCOL
 *
 * WIRES ALL 10 JETPACK COMPOSE SCREENS + VAULT SWITCH MODAL (19 MAPPED IMAGES):
 * 1. [IamDimensionRoute.Auth]                 -> `AuthScreen.kt` (IMG 1, IMG 2, IMG 4)
 * 2. [IamDimensionRoute.PortalTransition]     -> `PortalTransitionScreen.kt` (IMG 3)
 * 3. [IamDimensionRoute.VaultDashboard]       -> `VaultDashboardScreen.kt` (IMG 5 & IMG 7 Modal)
 * 4. [IamDimensionRoute.Settings]             -> `SettingsScreen.kt` (IMG 6 - 9-Language Strict Mode)
 * 5. [IamDimensionRoute.LrosVault]            -> `LrosVaultScreen.kt` (IMG 8, IMG 9 w/ Module 4 = SAKUNA)
 * 6. [IamDimensionRoute.ForensicLedger]       -> `ForensicLedgerScreen.kt` (IMG 10)
 * 7. [IamDimensionRoute.RxmVault]             -> `RxmVaultScreen.kt` (IMG 11, IMG 12)
 * 8. [IamDimensionRoute.ContactRadar]         -> `ContactRadarScreen.kt` (IMG 13)
 * 9. [IamDimensionRoute.WolfsDen]             -> `WolfsDenScreen.kt` (IMG 14, IMG 15, IMG 16)
 * 10. [IamDimensionRoute.BlackCase]           -> `BlackCaseScreen.kt` (IMG 17 Locked 60% -> IMG 18 Unlocked 100%)
 * 11. [IamDimensionRoute.SecurityVerification]-> `SecurityVerificationScreen.kt` (IMG 19 SHA-256 Seal)
 * ====================================================================================
 */

sealed class IamDimensionRoute(val route: String) {
    data object Auth : IamDimensionRoute("auth_exterior_portal")
    data object PortalTransition : IamDimensionRoute("portal_black_hole_transition")
    data object VaultDashboard : IamDimensionRoute("interior_vault_dashboard")
    data object Settings : IamDimensionRoute("settings_nine_language_selector")
    data object LrosVault : IamDimensionRoute("lros_juggernaut_vault_1293")
    data object ForensicLedger : IamDimensionRoute("lros_forensic_ledger_scanner")
    data object RxmVault : IamDimensionRoute("rxm_phylax_vault_0519")
    data object ContactRadar : IamDimensionRoute("rxm_contact_radar_4lang")
    data object WolfsDen : IamDimensionRoute("wolfs_den_deepmotion_glb")
    data object BlackCase : IamDimensionRoute("the_black_case_master_node")
    data object SecurityVerification : IamDimensionRoute("sha256_security_verification_seal")
}

private val IamDimensionMaterial3DarkScheme = darkColorScheme(
    primary = IamDimensionPalette.CyberGold,
    onPrimary = IamDimensionPalette.VoidBlack,
    secondary = IamDimensionPalette.NeonMagenta,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = IamDimensionPalette.PhylaxCyan,
    background = IamDimensionPalette.VoidBlack,
    surface = IamDimensionPalette.DeepNebula,
    error = IamDimensionPalette.CrimsonAlert
)

/**
 * Material 3 Dark Theme wrapper replicating the exact Cyberpunk / Neon / Galactic
 * visual truth across all 19 screens.
 */
@Composable
fun IamDimensionV7Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IamDimensionMaterial3DarkScheme,
        content = content
    )
}

/**
 * Root Navigation Host connecting all 19 mapped UI states and preserving the single
 * shared [IamDimensionAppState] (9-language strict state + Black Case 60%/100% Catch Basin Overflow).
 */
@Composable
fun IamDimensionNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    appState: IamDimensionAppState = remember { IamDimensionAppState() },
    startDestination: String = IamDimensionRoute.Auth.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        enterTransition = {
            fadeIn(animationSpec = tween(350)) + slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(350)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        // ----------------------------------------------------------------------
        // PHASE 1: EXTERIOR BOOK COVER UI (IMG 1, IMG 2, IMG 3, IMG 4)
        // ----------------------------------------------------------------------
        composable(IamDimensionRoute.Auth.route) {
            AuthScreen(
                onPortalActivated = {
                    navController.navigate(IamDimensionRoute.PortalTransition.route) {
                        popUpTo(IamDimensionRoute.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(IamDimensionRoute.PortalTransition.route) {
            PortalTransitionScreen(
                onTransitionComplete = {
                    navController.navigate(IamDimensionRoute.VaultDashboard.route) {
                        popUpTo(IamDimensionRoute.PortalTransition.route) { inclusive = true }
                    }
                }
            )
        }

        // ----------------------------------------------------------------------
        // PHASE 2: INTERIOR AVATAR DASHBOARD (IMG 5, IMG 6, IMG 7)
        // ----------------------------------------------------------------------
        composable(IamDimensionRoute.VaultDashboard.route) {
            VaultDashboardScreen(
                appState = appState,
                onNavigateToLros = {
                    appState.activeVaultId = "LROS_1293"
                    navController.navigate(IamDimensionRoute.LrosVault.route)
                },
                onNavigateToRxm = {
                    appState.activeVaultId = "RXM_0519"
                    navController.navigate(IamDimensionRoute.RxmVault.route)
                },
                onNavigateToWolfsDen = {
                    appState.activeVaultId = "WOLFS_DEN"
                    navController.navigate(IamDimensionRoute.WolfsDen.route)
                },
                onNavigateToBlackCase = {
                    navController.navigate(IamDimensionRoute.BlackCase.route)
                },
                onNavigateToSettings = {
                    navController.navigate(IamDimensionRoute.Settings.route)
                },
                onNavigateToForensicLedger = {
                    navController.navigate(IamDimensionRoute.ForensicLedger.route)
                },
                onNavigateToSecuritySeal = {
                    navController.navigate(IamDimensionRoute.SecurityVerification.route)
                },
                onLogout = {
                    navController.navigate(IamDimensionRoute.Auth.route) {
                        popUpTo(IamDimensionRoute.VaultDashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(IamDimensionRoute.Settings.route) {
            SettingsScreen(
                appState = appState,
                onBack = { navController.popBackStack() }
            )
        }

        // ----------------------------------------------------------------------
        // PHASE 3: LROS JUGGERNAUT VAULT 1293 (IMG 8, IMG 9 [SAKUNA], IMG 10)
        // ----------------------------------------------------------------------
        composable(IamDimensionRoute.LrosVault.route) {
            LrosVaultScreen(
                appState = appState,
                onOpenForensicLedger = {
                    navController.navigate(IamDimensionRoute.ForensicLedger.route)
                },
                onOpenContactRadar = {
                    navController.navigate(IamDimensionRoute.ContactRadar.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(IamDimensionRoute.ForensicLedger.route) {
            ForensicLedgerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ----------------------------------------------------------------------
        // PHASE 4: RXM PHYLAX VAULT 0519 (IMG 11, IMG 12, IMG 13)
        // ----------------------------------------------------------------------
        composable(IamDimensionRoute.RxmVault.route) {
            RxmVaultScreen(
                onOpenContactRadar = {
                    navController.navigate(IamDimensionRoute.ContactRadar.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(IamDimensionRoute.ContactRadar.route) {
            ContactRadarScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ----------------------------------------------------------------------
        // PHASE 5: WOLF'S DEN (IMG 14, IMG 15, IMG 16)
        // ----------------------------------------------------------------------
        composable(IamDimensionRoute.WolfsDen.route) {
            WolfsDenScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ----------------------------------------------------------------------
        // PHASE 6: THE BLACK CASE MASTER NODE & SHA-256 SEAL (IMG 17, IMG 18, IMG 19)
        // ----------------------------------------------------------------------
        composable(IamDimensionRoute.BlackCase.route) {
            BlackCaseScreen(
                appState = appState,
                onNavigateToSecuritySeal = {
                    navController.navigate(IamDimensionRoute.SecurityVerification.route)
                },
                onBackToDashboard = { navController.popBackStack() }
            )
        }

        composable(IamDimensionRoute.SecurityVerification.route) {
            SecurityVerificationScreen(
                appState = appState,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Drop-in MainActivity entry point for the Android application module.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            IamDimensionV7Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = IamDimensionPalette.VoidBlack
                ) {
                    val navController = rememberNavController()
                    val appState = remember { IamDimensionAppState() }
                    IamDimensionNavGraph(
                        navController = navController,
                        appState = appState
                    )
                }
            }
        }
    }
}

package com.diprotec.inventario.ui.main

import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.diprotec.inventario.core.config.SettingsManager
import com.diprotec.inventario.core.session.SessionManager
import com.diprotec.inventario.ui.about.AboutScreen
import com.diprotec.inventario.ui.common.AppFloatingMessageHost
import com.diprotec.inventario.ui.datausage.DataUsageScreen
import com.diprotec.inventario.ui.inventory.capture.CaptureInventoryScreen
import com.diprotec.inventario.ui.inventory.create.CreateInventoryScreen
import com.diprotec.inventario.ui.inventory.create.CreateInventoryViewModel
import com.diprotec.inventario.ui.inventory.list.InventoryListScreen
import com.diprotec.inventario.ui.inventory.pending.PendingInventoriesScreen
import com.diprotec.inventario.ui.login.LoginScreen
import com.diprotec.inventario.ui.menu.MainMenuScreen
import com.diprotec.inventario.ui.settings.SettingsScreen
import com.diprotec.inventario.ui.startup.StartupGateScreen
import com.diprotec.inventario.ui.synclog.SyncLogScreen
import kotlinx.coroutines.launch

private const val SESSION_ACTIVITY_TOUCH_THROTTLE_MS = 1_000L

sealed class Screen(val route: String) {
    data object StartupGate : Screen("startup_gate")
    data object Login : Screen("login")
    data object Settings : Screen("settings")
    data object MainMenu : Screen("main_menu")
    data object CreateInventory : Screen("create_inventory")
    data object About : Screen("about")
    data object PendingInventories : Screen("pending_inventories")
    data object SyncLogs : Screen("sync_logs")
    data object DataUsage : Screen("data_usage")
    data object CaptureInventory : Screen("capture_inventory/{inventoryId}")
    data object InventoryList : Screen("inventory_list/{inventoryId}")

    fun captureRoute(inventoryId: Long): String =
        "capture_inventory/$inventoryId"

    fun inventoryListRoute(inventoryId: Long): String =
        "inventory_list/$inventoryId"
}

@Composable
fun AppNavHost(
    settings: SettingsManager,
    session: SessionManager
) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val loginRut by session.loginRut.collectAsState()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val latestLoginRut by rememberUpdatedState(loginRut)
    val latestCurrentRoute by rememberUpdatedState(currentRoute)

    LaunchedEffect(Unit) {
        Log.d("STARTUP", "AppNavHost composed start=${Screen.StartupGate.route}")
    }

    LaunchedEffect(currentRoute) {
        CurrentScreenTracker.update(currentRoute)
    }

    LaunchedEffect(loginRut, currentRoute) {
        val isPublicRoute =
            currentRoute == Screen.StartupGate.route ||
                    currentRoute == Screen.Login.route ||
                    currentRoute == Screen.Settings.route ||
                    currentRoute == null

        if (loginRut == null && !isPublicRoute) {
            nav.navigate(Screen.Login.route) {
                popUpTo(Screen.StartupGate.route) {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(session) {
                var lastTouchRegisteredAt = 0L

                awaitEachGesture {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val hasDown = event.changes.any { it.changedToDownIgnoreConsumed() }

                    if (hasDown) {
                        val route = latestCurrentRoute

                        val isPublicRoute =
                            route == Screen.StartupGate.route ||
                                    route == Screen.Login.route ||
                                    route == Screen.Settings.route ||
                                    route == null

                        if (latestLoginRut != null && !isPublicRoute) {
                            val now = System.currentTimeMillis()

                            if (now - lastTouchRegisteredAt >= SESSION_ACTIVITY_TOUCH_THROTTLE_MS) {
                                lastTouchRegisteredAt = now

                                scope.launch {
                                    session.notifyUserActivity()
                                }
                            }
                        }
                    }
                }
            }
    ) {
        NavHost(
            navController = nav,
            startDestination = Screen.StartupGate.route
        ) {
            composable(Screen.StartupGate.route) {
                StartupGateScreen(
                    onGoLogin = {
                        nav.navigate(Screen.Login.route) {
                            popUpTo(Screen.StartupGate.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onGoMainMenu = {
                        nav.navigate(Screen.MainMenu.route) {
                            popUpTo(Screen.StartupGate.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onGoSettings = {
                        nav.navigate(Screen.Settings.route) {
                            popUpTo(Screen.StartupGate.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onLoggedIn = {
                        nav.navigate(Screen.MainMenu.route) {
                            popUpTo(Screen.Login.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onGoSettings = {
                        nav.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onDone = {
                        nav.navigate(Screen.StartupGate.route) {
                            popUpTo(Screen.Settings.route) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    },
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }

            composable(Screen.MainMenu.route) {
                MainMenuScreen(
                    onRealizarInventario = {
                        nav.navigate(Screen.CreateInventory.route) {
                            launchSingleTop = true
                        }
                    },
                    onContinuarInventario = {
                        nav.navigate(Screen.PendingInventories.route) {
                            launchSingleTop = true
                        }
                    },
                    onHistorialEnvios = {
                        nav.navigate(Screen.SyncLogs.route) {
                            launchSingleTop = true
                        }
                    },
                    onConsumoDatos = {
                        nav.navigate(Screen.DataUsage.route) {
                            launchSingleTop = true
                        }
                    },
                    onAcercaDe = {
                        nav.navigate(Screen.About.route) {
                            launchSingleTop = true
                        }
                    },
                    onSalir = {
                        scope.launch {
                            session.logout()

                            nav.navigate(Screen.Login.route) {
                                popUpTo(Screen.MainMenu.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(Screen.SyncLogs.route) {
                SyncLogScreen(
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }

            composable(Screen.DataUsage.route) {
                DataUsageScreen(
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }

            composable(Screen.CreateInventory.route) {
                val viewModel: CreateInventoryViewModel = hiltViewModel()

                CreateInventoryScreen(
                    onAccept = {
                        viewModel.createSelectedInventory { inventoryId ->
                            nav.navigate(Screen.CaptureInventory.captureRoute(inventoryId)) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onBack = {
                        nav.popBackStack()
                    },
                    viewModel = viewModel
                )
            }

            composable(Screen.About.route) {
                AboutScreen(
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }

            composable(Screen.PendingInventories.route) {
                PendingInventoriesScreen(
                    onBack = {
                        nav.popBackStack()
                    },
                    onOpenPendingInventory = { inventoryId ->
                        nav.navigate(Screen.CaptureInventory.captureRoute(inventoryId)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenFinishedInventory = { inventoryId ->
                        nav.navigate(Screen.InventoryList.inventoryListRoute(inventoryId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Screen.CaptureInventory.route,
                arguments = listOf(
                    navArgument("inventoryId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val inventoryId =
                    backStackEntry.arguments?.getLong("inventoryId") ?: 0L

                CaptureInventoryScreen(
                    inventoryId = inventoryId,
                    onBack = {
                        nav.popBackStack()
                    },
                    onViewList = {
                        nav.navigate(Screen.InventoryList.inventoryListRoute(inventoryId)) {
                            launchSingleTop = true
                        }
                    },
                    onLeavePending = {
                        nav.navigate(Screen.MainMenu.route) {
                            popUpTo(Screen.MainMenu.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },
                    onFinishInventory = {
                        nav.navigate(Screen.MainMenu.route) {
                            popUpTo(Screen.MainMenu.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = Screen.InventoryList.route,
                arguments = listOf(
                    navArgument("inventoryId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->
                val inventoryId =
                    backStackEntry.arguments?.getLong("inventoryId") ?: 0L

                InventoryListScreen(
                    inventoryId = inventoryId,
                    onBack = {
                        nav.popBackStack()
                    }
                )
            }
        }

        AppFloatingMessageHost(
            durationMillis = 2_000L
        )
    }
}
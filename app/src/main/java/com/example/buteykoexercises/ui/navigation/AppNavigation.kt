package com.example.buteykoexercises.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.buteykoexercises.ui.cp.ControlPauseScreen
import com.example.buteykoexercises.ui.exercise.ExerciseScreen
import com.example.buteykoexercises.ui.history.HistoryScreen
import com.example.buteykoexercises.ui.charts.ChartsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.buteykoexercises.ui.history.HistoryDetailScreen
import kotlinx.coroutines.launch

// Define strict routes
object NavRoutes {
    const val CONTROL_PAUSE = "control_pause"
    const val EXERCISE = "exercise"
    const val HISTORY = "history"
    const val CHARTS = "charts"
    // NEW ROUTE with arguments
    const val HISTORY_DETAIL = "history_detail/{type}/{id}"

    // Helper to build the route string
    fun historyDetail(type: String, id: Long) = "history_detail/$type/$id"
}

data class DrawerItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppShell() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Define Menu Items
    val items = listOf(
        DrawerItem(NavRoutes.CONTROL_PAUSE, "Control Pause", Icons.Default.Home),
        DrawerItem(NavRoutes.EXERCISE, "Breathing Exercise", Icons.Default.PlayArrow),
        DrawerItem(NavRoutes.HISTORY, "History", Icons.Default.DateRange),
        DrawerItem(NavRoutes.CHARTS, "Charts", Icons.Default.Info)
    )

    // Select the first item by default
    var selectedItem by remember { mutableStateOf(items[0]) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Buteyko Breathing",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()

                items.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = item == selectedItem,
                        onClick = {
                            selectedItem = item
                            scope.launch { drawerState.close() }

                            navController.navigate(item.route) {
                                popUpTo(NavRoutes.CONTROL_PAUSE) {
                                    saveState = true
                                }
                                launchSingleTop = true

                                // Only restore state if it is NOT History.
                                // If it IS History, false means we reset to the top of that stack (the list).
                                restoreState = (item.route != NavRoutes.HISTORY)
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(selectedItem.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            // Host the screens
            NavHost(
                navController = navController,
                startDestination = NavRoutes.CONTROL_PAUSE,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(NavRoutes.CONTROL_PAUSE) {
                    ControlPauseScreen()
                }
                composable(NavRoutes.EXERCISE) {
                    ExerciseScreen()
                }
                composable(NavRoutes.HISTORY) {
                    HistoryScreen(
                        onItemClick = { type, id ->
                            navController.navigate(NavRoutes.historyDetail(type, id))
                        }
                    )
                }
                composable(NavRoutes.CHARTS) {
                    ChartsScreen()
                }
                composable(
                    route = NavRoutes.HISTORY_DETAIL,
                    arguments = listOf(
                        navArgument("type") { type = NavType.StringType },
                        navArgument("id") { type = NavType.LongType }
                    )
                ) {
                    HistoryDetailScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

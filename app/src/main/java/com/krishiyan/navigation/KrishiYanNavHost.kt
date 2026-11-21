package com.krishiyan.navigation

import android.app.Application
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krishiyan.KrishiYanApp
import com.krishiyan.R
import com.krishiyan.equipment.AddEquipmentScreen
import com.krishiyan.equipment.AddEquipmentViewModel
import com.krishiyan.ui.components.BigButton
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object MapRoute
@Serializable object MyRentalsRoute
@Serializable object ProfileRoute
@Serializable object AddEquipmentRoute

private data class Tab(val route: Any, @StringRes val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(HomeRoute, R.string.tab_home, Icons.Filled.Home),
    Tab(MapRoute, R.string.tab_map, Icons.Filled.Map),
    Tab(MyRentalsRoute, R.string.tab_my_rentals, Icons.Filled.Agriculture),
    Tab(ProfileRoute, R.string.tab_profile, Icons.Filled.Person),
)

@Composable
fun KrishiYanNavHost(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination

    val onTab = tabs.any { destination?.hasRoute(it.route::class) == true }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (onTab) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = destination?.hasRoute(tab.route::class) == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondary,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<HomeRoute> { PlaceholderScreen(R.string.tab_home) }
            composable<MapRoute> { PlaceholderScreen(R.string.tab_map) }
            composable<MyRentalsRoute> { PlaceholderScreen(R.string.tab_my_rentals) }
            composable<ProfileRoute> {
                ProfilePlaceholder(onAddEquipment = { navController.navigate(AddEquipmentRoute) }, onSignOut = onSignOut)
            }
            composable<AddEquipmentRoute> {
                val context = LocalContext.current
                val container = (context.applicationContext as KrishiYanApp).container
                AddEquipmentScreen(
                    viewModel = viewModel {
                        AddEquipmentViewModel(
                            context.applicationContext as Application,
                            container.equipmentRepository,
                            container.locationProvider,
                        )
                    },
                    onBack = { navController.popBackStack() },
                    onPublished = {
                        Toast.makeText(context, R.string.equipment_published, Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(@StringRes title: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "${stringResource(title)}\n${stringResource(R.string.coming_soon)}",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfilePlaceholder(onAddEquipment: () -> Unit, onSignOut: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = "${stringResource(R.string.tab_profile)}\n${stringResource(R.string.coming_soon)}",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton(stringResource(R.string.add_equipment), onAddEquipment)
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.log_out), style = MaterialTheme.typography.labelLarge)
        }
    }
}

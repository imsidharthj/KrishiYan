package com.krishiyan.navigation

import android.app.Application
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.lifecycle.viewmodel.compose.viewModel
import com.krishiyan.KrishiYanApp
import com.krishiyan.R
import com.krishiyan.booking.MyRentalsScreen
import com.krishiyan.booking.MyRentalsViewModel
import com.krishiyan.booking.OwnerRequestsScreen
import com.krishiyan.booking.OwnerRequestsViewModel
import com.krishiyan.booking.RequestBookingScreen
import com.krishiyan.booking.RequestBookingViewModel
import com.krishiyan.equipment.AddEquipmentScreen
import com.krishiyan.equipment.AddEquipmentViewModel
import com.krishiyan.equipment.EquipmentDetailsScreen
import com.krishiyan.equipment.EquipmentDetailsViewModel
import com.krishiyan.home.HomeScreen
import com.krishiyan.home.HomeViewModel
import com.krishiyan.map.MapScreen
import com.krishiyan.profile.ProfileScreen
import com.krishiyan.profile.ProfileViewModel
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object MapRoute
@Serializable object MyRentalsRoute
@Serializable object ProfileRoute
@Serializable object AddEquipmentRoute
@Serializable data class EditEquipmentRoute(val id: String)
@Serializable data class EquipmentDetailsRoute(val id: String)
@Serializable data class RequestBookingRoute(val equipmentId: String)
@Serializable object OwnerRequestsRoute

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
    val container = (LocalContext.current.applicationContext as KrishiYanApp).container
    // Home and Map show the same search, so they share one ViewModel.
    val discover = viewModel {
        HomeViewModel(container.equipmentRepository, container.locationProvider, container.savedLocation, container.placeSearch, container.profileRepository)
    }
    val goToTab = { route: Any ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (onTab) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = destination?.hasRoute(tab.route::class) == true,
                        onClick = { goToTab(tab.route) },
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
            composable<HomeRoute> {
                HomeScreen(
                    viewModel = discover,
                    placeSearch = container.placeSearch,
                    onOpenEquipment = { navController.navigate(EquipmentDetailsRoute(it)) },
                )
            }
            composable<MapRoute> {
                MapScreen(
                    viewModel = discover,
                    onOpenEquipment = { navController.navigate(EquipmentDetailsRoute(it)) },
                    onChooseLocation = { goToTab(HomeRoute) },
                )
            }
            composable<MyRentalsRoute> {
                MyRentalsScreen(viewModel { MyRentalsViewModel(container.bookingRepository) })
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    viewModel = viewModel {
                        ProfileViewModel(container.profileRepository, container.equipmentRepository, container.bookingRepository)
                    },
                    onAddEquipment = { navController.navigate(AddEquipmentRoute) },
                    onOpenRequests = { navController.navigate(OwnerRequestsRoute) },
                    onEditEquipment = { navController.navigate(EditEquipmentRoute(it)) },
                    onSignOut = onSignOut,
                )
            }
            composable<EquipmentDetailsRoute> { entry ->
                val id = entry.toRoute<EquipmentDetailsRoute>().id
                EquipmentDetailsScreen(
                    viewModel = viewModel {
                        EquipmentDetailsViewModel(id, container.equipmentRepository, container.authRepository, container.savedLocation)
                    },
                    onBack = { navController.popBackStack() },
                    onRequestBooking = { navController.navigate(RequestBookingRoute(it)) },
                )
            }
            composable<RequestBookingRoute> { entry ->
                val equipmentId = entry.toRoute<RequestBookingRoute>().equipmentId
                val context = LocalContext.current
                RequestBookingScreen(
                    viewModel = viewModel {
                        RequestBookingViewModel(equipmentId, container.equipmentRepository, container.bookingRepository)
                    },
                    onBack = { navController.popBackStack() },
                    onSent = {
                        Toast.makeText(context, R.string.request_sent, Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    },
                )
            }
            composable<OwnerRequestsRoute> {
                OwnerRequestsScreen(
                    viewModel = viewModel { OwnerRequestsViewModel(container.bookingRepository) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<EditEquipmentRoute> { entry ->
                val id = entry.toRoute<EditEquipmentRoute>().id
                val context = LocalContext.current
                AddEquipmentScreen(
                    viewModel = viewModel {
                        AddEquipmentViewModel(
                            context.applicationContext as Application,
                            container.equipmentRepository,
                            container.locationProvider,
                            editingId = id,
                        )
                    },
                    placeSearch = container.placeSearch,
                    onBack = { navController.popBackStack() },
                    onPublished = {
                        Toast.makeText(context, R.string.changes_saved, Toast.LENGTH_LONG).show()
                        navController.popBackStack()
                    },
                )
            }
            composable<AddEquipmentRoute> {
                val context = LocalContext.current
                AddEquipmentScreen(
                    viewModel = viewModel {
                        AddEquipmentViewModel(
                            context.applicationContext as Application,
                            container.equipmentRepository,
                            container.locationProvider,
                        )
                    },
                    placeSearch = container.placeSearch,
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

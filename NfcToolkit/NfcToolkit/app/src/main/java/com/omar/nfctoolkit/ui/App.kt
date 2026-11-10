package com.omar.nfctoolkit.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.omar.nfctoolkit.ui.screens.CopyScreen
import com.omar.nfctoolkit.ui.screens.ReadScreen
import com.omar.nfctoolkit.ui.screens.SettingsScreen
import com.omar.nfctoolkit.ui.screens.WalletScreen
import com.omar.nfctoolkit.ui.screens.WriteScreen
import com.omar.nfctoolkit.viewmodel.MainViewModel

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Read : Dest("read", "Read", Icons.Outlined.Sensors)
    data object Write : Dest("write", "Write", Icons.Filled.Edit)
    data object Copy : Dest("copy", "Copy", Icons.Filled.ContentCopy)
    data object Wallet : Dest("wallet", "Wallet", Icons.Filled.CreditCard)
    data object Settings : Dest("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun NfcApp(vm: MainViewModel, nfcSupported: Boolean, nfcEnabled: Boolean) {
    val nav = rememberNavController()
    val items = listOf(Dest.Read, Dest.Write, Dest.Copy, Dest.Wallet, Dest.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by nav.currentBackStackEntryAsState()
                val current = backStack?.destination?.route
                items.forEach { dest ->
                    NavigationBarItem(
                        selected = current == dest.route,
                        onClick = {
                            vm.resetToRead()
                            nav.navigate(dest.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Dest.Read.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Read.route) { ReadScreen(vm, nfcSupported, nfcEnabled) }
            composable(Dest.Write.route) { WriteScreen(vm) }
            composable(Dest.Copy.route) { CopyScreen(vm) }
            composable(Dest.Wallet.route) { WalletScreen(vm) }
            composable(Dest.Settings.route) { SettingsScreen(vm, nfcSupported, nfcEnabled) }
        }
    }
}

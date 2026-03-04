package com.whispervault.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.whispervault.ui.auth.*
import com.whispervault.ui.chat.ChatScreen
import com.whispervault.ui.chat.ChatListScreen
import com.whispervault.ui.contacts.ContactsScreen
import com.whispervault.ui.profile.ProfileScreen
import com.whispervault.ui.splash.SplashScreen
import com.whispervault.ui.tor.TorStatusScreen
import com.whispervault.ui.vault.PanicScreen
import com.whispervault.ui.settings.SettingsScreen
import com.whispervault.viewmodel.AppViewModel

sealed class Route(val path: String) {
    object Splash      : Route("splash")
    object Welcome     : Route("welcome")
    object Register    : Route("register")
    object Login       : Route("login")
    object ChatList    : Route("chat_list")
    object Contacts    : Route("contacts")
    object Profile     : Route("profile")
    object TorStatus   : Route("tor_status")
    object Panic       : Route("panic")
    object Settings    : Route("settings")
    object VerifyOtp : Route("verify_otp/{email}") {
        fun go(email: String) = "verify_otp/$email"
    }
    object SetupProfile : Route("setup_profile/{email}") {
        fun go(email: String) = "setup_profile/$email"
    }
    object Chat : Route("chat/{userId}") {
        fun go(userId: String) = "chat/$userId"
    }
}

@Composable
fun WhisperVaultNavHost() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val isLoggedIn by appViewModel.isLoggedIn.collectAsState()

    NavHost(navController = navController, startDestination = Route.Splash.path) {

        composable(Route.Splash.path) {
            SplashScreen(onReady = { loggedIn ->
                if (loggedIn) navController.navigate(Route.ChatList.path) { popUpTo(0) }
                else navController.navigate(Route.Welcome.path) { popUpTo(0) }
            })
        }

        composable(Route.Welcome.path) {
            WelcomeScreen(
                onLoginClick    = { navController.navigate(Route.Login.path) },
                onRegisterClick = { navController.navigate(Route.Register.path) },
                onGuestClick    = {
                    appViewModel.continueAsGuest()
                    navController.navigate(Route.ChatList.path) { popUpTo(0) }
                }
            )
        }

        composable(Route.Register.path) {
            RegisterScreen(
                onSuccess    = { email -> navController.navigate(Route.VerifyOtp.go(email)) },
                onLoginClick = { navController.navigate(Route.Login.path) }
            )
        }

        composable(Route.VerifyOtp.path,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { back ->
            val email = back.arguments?.getString("email") ?: ""
            VerifyOtpScreen(email = email,
                onVerified = { navController.navigate(Route.SetupProfile.go(email)) { popUpTo(Route.Register.path) { inclusive = true } } })
        }

        composable(Route.SetupProfile.path,
            arguments = listOf(navArgument("email") { type = NavType.StringType })
        ) { back ->
            val email = back.arguments?.getString("email") ?: ""
            SetupProfileScreen(email = email,
                onComplete = { navController.navigate(Route.Login.path) { popUpTo(0) } })
        }

        composable(Route.Login.path) {
            LoginScreen(
                onLoginSuccess  = { navController.navigate(Route.ChatList.path) { popUpTo(0) } },
                onRegisterClick = { navController.navigate(Route.Register.path) }
            )
        }

        composable(Route.ChatList.path) {
            ChatListScreen(
                onChatClick     = { userId -> navController.navigate(Route.Chat.go(userId)) },
                onContactsClick = { navController.navigate(Route.Contacts.path) },
                onProfileClick  = { navController.navigate(Route.Profile.path) },
                onSettingsClick = { navController.navigate(Route.Settings.path) }
            )
        }

        composable(Route.Chat.path,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { back ->
            val userId = back.arguments?.getString("userId") ?: ""
            ChatScreen(withUserId = userId, onBack = { navController.popBackStack() })
        }

        composable(Route.Contacts.path) {
            ContactsScreen(
                onUserFound = { userId -> navController.navigate(Route.Chat.go(userId)) },
                onBack      = { navController.popBackStack() }
            )
        }

        composable(Route.Profile.path) {
            ProfileScreen(
                onLogout = { navController.navigate(Route.Welcome.path) { popUpTo(0) } },
                onBack   = { navController.popBackStack() }
            )
        }

        composable(Route.TorStatus.path) {
            TorStatusScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.Panic.path) {
            PanicScreen(onWipeComplete = { navController.navigate(Route.Welcome.path) { popUpTo(0) } })
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onBack      = { navController.popBackStack() },
                onTorClick  = { navController.navigate(Route.TorStatus.path) },
                onPanicClick = { navController.navigate(Route.Panic.path) },
                onLogout    = { navController.navigate(Route.Welcome.path) { popUpTo(0) } }
            )
        }
    }
}

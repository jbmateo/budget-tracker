package app.budgettracker.android.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.budgettracker.android.ui.budgets.BudgetsScreen
import app.budgettracker.android.ui.expenses.ExpensesScreen
import app.budgettracker.android.ui.form.ExpenseFormScreen
import app.budgettracker.android.ui.home.HomeScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", Icons.Rounded.Home),
    Expenses("expenses", "Expenses", Icons.AutoMirrored.Rounded.ReceiptLong),
    Budgets("budgets", "Budgets", Icons.Rounded.PieChart),
}

/** Add (no id) or edit an expense. The form ViewModel lives in this destination's back-stack entry. */
private const val FORM = "expense?id={id}"
private fun formRoute(id: String?) = if (id == null) "expense" else "expense?id=$id"

private fun NavController.switchTo(tab: Tab) = navigate(tab.route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = Tab.entries.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }
    val openForm: (String?) -> Unit = { nav.navigate(formRoute(it)) }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = { nav.switchTo(tab) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("tab_${tab.route}"),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.Home.route) {
            composable(Tab.Home.route) {
                HomeScreen(
                    contentPadding = padding,
                    onAdd = { openForm(null) },
                    onOpen = openForm,
                    onSeeAll = { nav.switchTo(Tab.Expenses) },
                    onSetBudget = { nav.switchTo(Tab.Budgets) },
                )
            }
            composable(Tab.Expenses.route) { ExpensesScreen(padding, onAdd = { openForm(null) }, onOpen = openForm) }
            composable(Tab.Budgets.route) { BudgetsScreen(padding) }
            composable(FORM, arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true })) { entry ->
                ExpenseFormScreen(expenseId = entry.arguments?.getString("id"), onClose = { nav.popBackStack() })
            }
        }
    }
}

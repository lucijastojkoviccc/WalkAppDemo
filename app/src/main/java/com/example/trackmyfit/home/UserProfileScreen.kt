package com.example.trackmyfit.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.navigation.NavController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.Text
import com.example.trackmyfit.BottomNavItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import com.google.firebase.firestore.Query
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import java.util.*


@Composable
fun UserProfileScreen(navController: NavController) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        // Prikazivanje toast poruke ako je currentUser null
        Log.d("AuthCheck", "User is null")
    }
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("needRefresh")) {
        if (navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>("needRefresh") == true) {
            // Ponovo učitajte podatke
            userId?.let {
                val db = FirebaseFirestore.getInstance()
                val userDocument = db.collection("users").document(it).get().await()
                userData = userDocument.toObject(UserData::class.java)
                isLoading = false
            }
            navController.currentBackStackEntry?.savedStateHandle?.set("needRefresh", false)
        }
    }

    LaunchedEffect(userId) {
        userId?.let {
            val db = FirebaseFirestore.getInstance()
            val userDocument = db.collection("users").document(it).get().await()
            userData = userDocument.toObject(UserData::class.java)
            isLoading = false
        }
    }
    val items = listOf(
        BottomNavItem.Search,
        BottomNavItem.Add,
        BottomNavItem.Map,
        BottomNavItem.Profile
    )


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    IconButton(onClick = { navController.navigate("editProfile") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Log Out")
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigation {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    BottomNavigationItem(
                        icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                        label = { Text(text = item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationRoute ?: "home") {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else {
            userData?.let { user ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profilna slika
                    Image(
                        painter = rememberImagePainter(user.profilePictureUrl),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ime i prezime
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "${user.gender}",
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Visina i težina
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Visina: ${user.height} cm", fontSize = 16.sp)
                        Text(text = "Težina: ${user.weight} kg", fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Tabs for Walking, Sleeping, Activities
                    var selectedTabIndex by remember { mutableStateOf(0) }
                    val tabs = listOf("Walking", "Sleeping", "Activities")

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedTabIndex) {
                        0 -> WalkingTabContent(user)
                        1 -> SleepingTabContent()
                        2 -> ActivitiesTabContent(user)
                    }
                }
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out") },
                text = { Text("Are you sure you want to log out?") },
                confirmButton = {
                    Button(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showLogoutDialog = false }
                    ) {
                        Text("No")
                    }
                }
            )
        }
    }
}
@Composable
fun WalkingTabContent(user: UserData) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var walksData by remember { mutableStateOf<List<WalkData>>(emptyList()) }

    // Fetch data from Firestore
    LaunchedEffect(userId) {
        val db = FirebaseFirestore.getInstance()
        val walksSnapshot = db.collection("walks")
            .whereEqualTo("userId", userId)
            .orderBy("date")
            .get()
            .await()

        val fetchedWalks = walksSnapshot.documents.map { document ->
            val steps = document.getLong("steps")?.toInt() ?: 0
            val date = document.getDate("date") ?: Date()
            WalkData(steps, date)
        }
        walksData = fetchedWalks
    }

    // Display the graph with scrollable bars
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (walksData.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                items(walksData.size) { index ->
                    val walk = walksData[index]
                    WalkBar(walk.steps, walk.date)
                }
            }
        } else {
            Text("No walking data available")
        }
    }
}
@Composable
fun WalkBar(steps: Int, date: Date) {
    val maxSteps = 40000 // Define the max height for the bar based on a maximum step count
    val barHeightRatio = steps.toFloat() / maxSteps

    // Format the date to day/month
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val formattedDate = dateFormat.format(date)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .height(150.dp)
                .width(30.dp)
        ) {
            // Draw the bar based on the step count
            drawRect(
                color = Color(0xFFD7BDE2),
                size = size.copy(height = size.height * barHeightRatio)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Display the formatted date under the bar
        Text(text = formattedDate, fontSize = 12.sp)
    }

}

data class WalkData(
    val steps: Int,
    val date: Date
)

@Composable
fun SleepingTabContent() {
    val context = LocalContext.current
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    var sleepSessions by remember { mutableStateOf<Map<String, Int>>(emptyMap()) } // Map of date to total minutes

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val querySnapshot = db.collection("sleep")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .await()

        // Group sleep sessions by date and sum their lengths
        val groupedSessions = querySnapshot.documents
            .groupBy { document ->
                document.getString("date") ?: "" // Group by the date
            }.mapValues { entry ->
                entry.value.sumBy { document ->
                    extractMinutesFromLength(document.getString("length") ?: "0min") // Sum the sleep lengths
                }
            }.filterKeys { it.isNotEmpty() } // Remove empty dates if any

        sleepSessions = groupedSessions.toSortedMap(compareByDescending { it }) // Sort by descending date
    }

    // Horizontal scrolling for the sleep bars
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Using the `items` function that takes a map
        items(sleepSessions.toList()) { (dateString, totalMinutes) ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString) ?: Date()
            SleepBar(totalMinutes = totalMinutes, date = date)
        }
    }
}

@Composable
fun SleepBar(totalMinutes: Int, date: Date) {
    val maxMinutes = 24 * 60f // Maximum height for the bar based on 24 hours in minutes
    val barHeightRatio = totalMinutes / maxMinutes

    // Format the date to day/month
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val formattedDate = dateFormat.format(date)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        // The Canvas will draw the bar from the bottom up with rounded corners
        Canvas(
            modifier = Modifier
                .height(150.dp) // Total height for the bars
                .width(15.dp)   // Reduced width for the bars (half the original)
        ) {
            val barHeight = size.height * barHeightRatio
            val cornerRadius = 8.dp.toPx() // Set the corner radius for the bars

            drawRoundRect(
                color = Color(0xFFD7BDE2), // Using the requested color
                topLeft = Offset(0f, size.height - barHeight), // Start from bottom
                size = Size(size.width, barHeight), // Adjust height dynamically
                cornerRadius = CornerRadius(cornerRadius, cornerRadius) // Rounded corners
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Display the formatted date under the bar
        Text(text = formattedDate, fontSize = 12.sp)
    }
}

// Function to extract total minutes from a string like "8h 30min"
fun extractMinutesFromLength(length: String): Int {
    val hoursRegex = "(\\d+)h".toRegex() // Match the hours
    val minutesRegex = "(\\d+)min".toRegex() // Match the minutes

    val hoursMatch = hoursRegex.find(length)
    val minutesMatch = minutesRegex.find(length)

    val hours = hoursMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val minutes = minutesMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

    return (hours * 60) + minutes // Convert hours to minutes and add
}


data class SleepSession(val length: String, val date: Date)

@Composable
fun ActivitiesTabContent(user: UserData) {
    // Ovdje možete prikazati podatke ili funkcionalnosti vezane za druge aktivnosti korisnika
    Text("Activities content goes here")
}
data class UserData(
    val firstName: String = "",
    val lastName: String = "",
    val gender: String = "",
    val email : String = "",
    val profilePictureUrl: String? = null,
    val height: Int = 0,
    val weight: Int = 0
)

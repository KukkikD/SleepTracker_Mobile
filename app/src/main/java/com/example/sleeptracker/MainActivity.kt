package com.example.sleeptracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore

// Data class to store one sleep record
data class SleepRecord(val id: String = "",
                       val userId: String = "",
                       val day: Int = 0,
                       val hours: Int = 0,
                       val category: String = "",
                       val createdAt: String = "")

// Function to determine sleep quality based on hours
fun getSleepCategory(hours: Int): String {
    return when {
        hours >= 8 -> "Great Sleep!"
        hours >= 6 -> "Good Sleep!"
        hours >= 5 -> "Fair Sleep!"
        hours >= 4 -> "Poor Sleep!"
        else -> "Deprived Sleep!"
    }
}

// Shared sleep records list for both screens
val sleepHistory = mutableStateListOf<SleepRecord>()

// Main activity is the entry point of the Android app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the app with navigation
        setContent {
            SleepTrackerNavigation()
        }
    }
}

// Navigation function for switching between screens
@Composable
fun SleepTrackerNavigation() {
    // Create navigation controller
    val navController = rememberNavController()

    // Define app screens
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // Main tracker screen
        composable("main") {
            SleepTrackerApp(navController)
        }

        // Sleep history screen
        composable("history") {
            SleepHistoryScreen(navController)
        }
    }
}

// Main composable function for the sleep tracker screen
@Composable
fun SleepTrackerApp(navController: NavController) {
    // Store the user's input from the TextField
    var sleepInput by remember { mutableStateOf("") }

    // Store the sleep result message
    var resultText by remember { mutableStateOf("") }

    // Calculate average sleep hours from the shared history list
    val average =
        if (sleepHistory.isNotEmpty()) {
            sleepHistory.map { it.hours }.average()
        } else {
            0.0
        }

    // Arrange UI components vertically
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // App title
        Text(
            text = "Sleep Tracker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // Add space between components
        Spacer(modifier = Modifier.height(16.dp))

        // Input field for sleep hours
        TextField(
            value = sleepInput,

            // Update input state when user types
            onValueChange = {
                sleepInput = it
            },

            // Input label
            label = {
                Text("Enter sleep hours")
            },

            // Show numeric keyboard
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),

            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Button to check sleep result
        Button(
            onClick = {
                // Convert input text to integer safely
                val hours = sleepInput.toIntOrNull()

                // Show error if input is invalid
                if (hours == null) {
                    resultText = "Please enter a valid number."
                } else {
                    // Get sleep category result
                    val category = getSleepCategory(hours)

                    // Create one new sleep record
                    val newRecord = SleepRecord(
                        userId = "test123",
                        day = sleepHistory.size + 1,
                        hours = hours,
                        category = category,
                        createdAt = System.currentTimeMillis().toString()
                    )

                    // Add the new record to the shared history list
                    sleepHistory.add(newRecord)

                    // Save history to cloud database (Firestore)
                    val db = FirebaseFirestore.getInstance()

                    //Firestore created id
                    val documentRef = db.collection("sleepRecords").document()

                    val recordWithId = newRecord.copy(
                        id = documentRef.id
                    )

                    documentRef.set(recordWithId)
                        .addOnSuccessListener  {
                            // Update result message
                            resultText = "Saved to Firestore!\nYou slept $hours hours.\n$category"
                        }
                        .addOnFailureListener {
                            resultText = "Error saving to Firestore."
                        }

                    // Clear the input field after submission
                    sleepInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Sleep")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Button to navigate to the history screen
        Button(
            onClick = {
                navController.navigate("history")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View History")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show sleep result
        Text(resultText)

        Spacer(modifier = Modifier.height(16.dp))

        // Show total number of entries
        Text("Total entries: ${sleepHistory.size}")

        // Show average sleep hours with one decimal place
        Text("Average sleep: %.1f hours".format(average))
    }
}

// Composable function for the sleep history screen
@Composable
fun SleepHistoryScreen(navController: NavController) {
    var records by remember { mutableStateOf<List<SleepRecord>>(emptyList()) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("sleepRecords")
            .get()
            .addOnSuccessListener { result ->
                val recordList = result.documents.mapNotNull { document ->
                    document.toObject(SleepRecord::class.java)
                }

                records = recordList
            }
            .addOnFailureListener {
                errorMessage = "Error loading sleep records."
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Sleep History",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage)
        } else if (records.isEmpty()) {
            Text("No sleep records yet.")
        } else {
            for (record in records) {

                Text(
                    "Day ${record.day}: ${record.hours} hours - ${record.category}"
                )

                //Add "Edit" button & "Delete" button in the same row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Edit" button from DB by +1
                    Button(
                        onClick = {
                            if (record.id.isBlank()) {
                                errorMessage = "This old record cannot be updated because it has no document ID."
                                return@Button
                            }

                            val db = FirebaseFirestore.getInstance()
                            val newHours = record.hours + 1
                            val newCategory = getSleepCategory(newHours)

                            db.collection("sleepRecords")
                                .document(record.id)
                                .update(
                                    mapOf(
                                        "hours" to newHours,
                                        "category" to newCategory
                                    )
                                )
                                .addOnSuccessListener {
                                    records = records.map {
                                        if (it.id == record.id) {
                                            it.copy(hours = newHours, category = newCategory)
                                        } else {
                                            it
                                        }
                                    }
                                }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update")
                    }

                    // "Delete" button and delete from DB
                    Button(
                        onClick = {
                            if (record.id.isBlank()) {
                                errorMessage = "This old record cannot be deleted because it has no document ID."
                                return@Button
                            }

                            val db = FirebaseFirestore.getInstance()

                            db.collection("sleepRecords")
                                .document(record.id)
                                .delete()
                                .addOnSuccessListener {
                                    records = records.filter {
                                        it.id != record.id
                                    }
                                }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Delete")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                navController.popBackStack()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }
    }
}

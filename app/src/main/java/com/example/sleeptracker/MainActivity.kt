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
import androidx.compose.ui.unit.dp

// Data class to store one sleep record
data class SleepRecord(val day: Int, val hours: Int)

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

// Main activity is the entry point of the Android app
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the app UI
        setContent {
            SleepTrackerApp()
        }
    }
}

// Main composable function for the app screen
@Composable
fun SleepTrackerApp() {
    // Store the user's input from the TextField
    var sleepInput by remember { mutableStateOf("") }

    // Store the sleep result message
    var resultText by remember { mutableStateOf("") }

    // Store all sleep records
    var records by remember {
        mutableStateOf(listOf<SleepRecord>())
    }

    // Calculate average sleep hours
    val average =
        if (records.isNotEmpty()) {
            records.map { it.hours }.average()
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
        Text("Sleep Tracker")

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

                    // Add a new sleep record
                    records = records + SleepRecord(
                        day = records.size + 1,
                        hours = hours
                    )

                    // Update result message
                    resultText = "You slept $hours hours.\n$category"

                    // Clear the input field after submission
                    sleepInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Sleep")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show sleep result
        Text(resultText)

        Spacer(modifier = Modifier.height(16.dp))

        // Show total number of entries
        Text("Total entries: ${records.size}")

        // Show average sleep hours
        Text("Average sleep: %.1f hours".format(average))
    }
}
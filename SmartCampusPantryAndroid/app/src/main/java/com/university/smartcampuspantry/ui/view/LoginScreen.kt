package com.university.smartcampuspantry.ui.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.smartcampuspantry.service.APIService
import com.university.smartcampuspantry.service.FirebaseService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var isRegisterMode by remember { mutableStateOf(false) }
    
    // Form Inputs
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var studentId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val firebaseService = FirebaseService.shared
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FC)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Logo
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.size(80.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(text = "🍽️", fontSize = 38.sp)
                }
            }

            Text(
                text = "UiTM Kolej Mawar Pantry",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF101A2C)
            )

            Text(
                text = "ITT632 Mobile Cloud Computing",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF007AFF)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Credentials Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Material 3 Tabs Switcher
                    TabRow(
                        selectedTabIndex = if (isRegisterMode) 1 else 0,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF101A2C),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = !isRegisterMode,
                            onClick = { 
                                isRegisterMode = false 
                                errorMessage = ""
                            },
                            text = { Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        )
                        Tab(
                            selected = isRegisterMode,
                            onClick = { 
                                isRegisterMode = true 
                                errorMessage = ""
                            },
                            text = { Text("Register", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        )
                    }

                    // Email Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "CAMPUS EMAIL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.trim() },
                            placeholder = { Text("username@student.uitm.edu.my", color = Color.Gray) },
                            singleLine = true,
                            maxLines = 1,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Additional registration fields (Name & Phone)
                    if (isRegisterMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "FULL NAME",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                placeholder = { Text("Muhammad Ali", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "PHONE NUMBER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                placeholder = { Text("+6012-3456789", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Student ID Field (Only for registration)
                    if (isRegisterMode) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "STUDENT ID",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            OutlinedTextField(
                                value = studentId,
                                onValueChange = { studentId = it },
                                placeholder = { Text("std_1001", color = Color.Gray) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Password Field
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PASSWORD",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("********", color = Color.Gray) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(if (passwordVisible) "🙈" else "👁️")
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (errorMessage.isNotEmpty()) {
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Action Button
                    Button(
                        onClick = {
                            errorMessage = ""
                            if (isRegisterMode) {
                                // Registration path
                                if (email.isBlank() || name.isBlank() || phone.isBlank() || studentId.isBlank() || password.isBlank()) {
                                    errorMessage = "Please fill in all registration fields."
                                    return@Button
                                }
                                val fullEmail = email
                                
                                isLoading = true
                                // 1. Register student in SQLite backend
                                APIService.shared.registerStudent(studentId, name, phone, fullEmail, password) { regResult ->
                                    if (regResult.isSuccess) {
                                        // 2. Sign in/create in Firebase Auth
                                        firebaseService.signIn(fullEmail, password) { authResult ->
                                            isLoading = false
                                            if (authResult.isSuccess) {
                                                firebaseService.setSession(fullEmail, studentId)
                                                Toast.makeText(context, "Registration Successful!", Toast.LENGTH_LONG).show()
                                                onLoginSuccess()
                                            } else {
                                                errorMessage = authResult.exceptionOrNull()?.message ?: "Firebase creation failed."
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        errorMessage = regResult.exceptionOrNull()?.message ?: "Backend registration failed."
                                    }
                                }
                            } else {
                                // Sign-in path
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter both Email and Password."
                                    return@Button
                                }
                                val fullEmail = email
                                
                                isLoading = true
                                firebaseService.signIn(fullEmail, password) { result ->
                                    if (result.isSuccess) {
                                        APIService.shared.fetchStudentProfileByEmail(fullEmail) { profileResult ->
                                            isLoading = false
                                            if (profileResult.isSuccess) {
                                                val profile = profileResult.getOrThrow()
                                                firebaseService.setSession(fullEmail, profile.studentId)
                                                onLoginSuccess()
                                            } else {
                                                firebaseService.signOut()
                                                errorMessage = profileResult.exceptionOrNull()?.message ?: "SQLite verification failed."
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        errorMessage = result.exceptionOrNull()?.message ?: "Authentication failed."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101A2C)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(
                                text = if (isRegisterMode) "Create Student Account" else "Access Pantry Dashboard",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = "🛡️", fontSize = 14.sp)
                Text(
                    text = "Secured via Firebase Authentication Shield",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

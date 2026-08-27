package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun LoginRegisterScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current

    // Inputs
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val blueSecondaryColor = Color(0xFF1B6CA8)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Aesthetic Top Decorative Logo Background Accent
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            val width = size.width
            val height = size.height

            // Clean modern abstract curve in brand Burgundy red
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                lineTo(width, height * 0.7f)
                quadraticTo(width * 0.5f, height, 0f, height * 0.7f)
                close()
            }
            drawPath(path = path, color = primaryColor)

            // Dynamic blue secondary abstract line to match the logo
            val accentPath = Path().apply {
                moveTo(0f, height * 0.7f)
                quadraticTo(width * 0.5f, height * 0.95f, width, height * 0.7f)
                lineTo(width, height * 0.74f)
                quadraticTo(width * 0.5f, height * 0.99f, 0f, height * 0.74f)
                close()
            }
            drawPath(path = accentPath, color = blueSecondaryColor)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // BRAND LOGO IMAGE - Displays actual company logo
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .width(180.dp)
                    .height(84.dp)
                    .padding(4.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_uk_logo_1780051906949),
                        contentDescription = "UK Security Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "UK SECURITY SOLUTIONS",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 1.sp,
                color = Color.White
            )

            Text(
                text = "Secure Staff Presence & Field Claims Portal",
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Authentication Form Card (SIGN IN ONLY)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Access Your Portal",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Left
                    )

                    Text(
                        text = "Sign in with registered credentials to clock working presence and view expenses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        textAlign = TextAlign.Left
                    )

                    // Username Input
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = primaryColor) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_username"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (Secret)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = primaryColor
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            focusedLabelColor = primaryColor
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Submission Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                Toast.makeText(context, "Please enter both credentials!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.loginUser(context, username, password) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button")
                    ) {
                        Text(
                            text = "SIGN IN TO PORTAL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Card showing admin control details
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = blueSecondaryColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Portal Authentication Info:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = blueSecondaryColor
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Registration is closed for regular users.\nOnly Administrator accounts can generate or delete staff access credentials from the system portal panel.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

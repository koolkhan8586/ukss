package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserMgmtDialog(
    viewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val context = LocalContext.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val isUserAdmin = loggedInUser?.role?.equals("Admin", ignoreCase = true) == true

    // Creation States for Admin User Signup
    var newUsername by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newFullName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("Staff") }
    var createPasswordVisible by remember { mutableStateOf(false) }

    // Map of password visibility triggers for each user ID
    val visiblePasswords = remember { mutableStateMapOf<Long, Boolean>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    viewModel.logoutUser(context)
                    onDismiss()
                },
                border = BorderStroke(1.dp, Color(0xFFC62828)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("LOGOUT", fontWeight = FontWeight.Black)
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "UK SYSTEM PORTAL PANEL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Connected Profile Details
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "CONNECTED USER IDENTITY (آپ کا پروفائل)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryColor,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = loggedInUser?.fullName?.take(2)?.uppercase() ?: "UG",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                            }
                            Column {
                                Text(
                                    text = loggedInUser?.fullName ?: "Staff Member",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Badge(
                                        containerColor = if (isUserAdmin) primaryColor else Color(0xFF1B6CA8),
                                        contentColor = Color.White
                                    ) {
                                        Text(
                                            text = loggedInUser?.role?.uppercase() ?: "GUEST",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "@${loggedInUser?.username ?: "username"}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Account administration list (Only visible to ADMINS!)
                if (isUserAdmin) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "REGISTERED ACCOUNTS LIST (${allUsers.size})",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allUsers.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = user.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "(${user.role})",
                                            fontSize = 10.sp,
                                            color = if (user.role == "Admin") primaryColor else Color(0xFF1B6CA8),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Password obscuring toggle for security
                                    val isPassVisible = visiblePasswords[user.id] ?: false
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "User: @${user.username} | Pass: " + 
                                                    if (isPassVisible) user.password else "••••••",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        IconButton(
                                            onClick = { visiblePasswords[user.id] = !isPassVisible },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPassVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = Color.Gray,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                                
                                // Do not let admin delete their own current account
                                if (user.username != loggedInUser?.username) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteUser(user.id)
                                            Toast.makeText(context, "Account for ${user.fullName} deleted!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Staff",
                                            tint = Color(0xFFC62828),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }

                    // Section 3: Add New Staff Member Form (Only visible to ADMINS!)
                    Spacer(modifier = Modifier.height(4.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "REGISTER NEW STAFF (نیا اسٹاف شامل کریں)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor,
                                letterSpacing = 0.5.sp
                            )
                            
                            OutlinedTextField(
                                value = newFullName,
                                onValueChange = { newFullName = it },
                                label = { Text("Full Name (پورا نام)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    focusedLabelColor = primaryColor
                                )
                            )
                            
                            OutlinedTextField(
                                value = newUsername,
                                onValueChange = { newUsername = it },
                                label = { Text("Username (صارف نام)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    focusedLabelColor = primaryColor
                                )
                            )
                            
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("Password (پاس ورڈ)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                trailingIcon = {
                                    IconButton(onClick = { createPasswordVisible = !createPasswordVisible }) {
                                        Icon(
                                            imageVector = if (createPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                visualTransformation = if (createPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = primaryColor,
                                    focusedLabelColor = primaryColor
                                )
                            )

                            // Role Selection Selector Row or Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Assign Role:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = newRole == "Staff",
                                        onClick = { newRole = "Staff" },
                                        label = { Text("Staff", fontSize = 11.sp) }
                                    )
                                    FilterChip(
                                        selected = newRole == "Admin",
                                        onClick = { newRole = "Admin" },
                                        label = { Text("Admin", fontSize = 11.sp) }
                                    )
                                }
                            }
                            
                            Button(
                                onClick = {
                                    val trimmedUsername = newUsername.trim()
                                    val trimmedName = newFullName.trim()
                                    if (trimmedName.isEmpty() || trimmedUsername.isEmpty() || newPassword.isEmpty()) {
                                        Toast.makeText(context, "Please fill all details!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.registerUser(
                                            context = context,
                                            usernameText = trimmedUsername,
                                            passwordText = newPassword,
                                            fullNameText = trimmedName,
                                            roleText = newRole
                                        ) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            if (success) {
                                                // Reset fields
                                                newFullName = ""
                                                newUsername = ""
                                                newPassword = ""
                                                newRole = "Staff"
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Registered Staff", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                            Text(
                                text = "Personal isolation mode active. Regular staff can only access and view their own entries. Admins retain full view control.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

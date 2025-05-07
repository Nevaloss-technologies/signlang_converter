package com.nevaloss.sign_lang_app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Signup extends AppCompatActivity {

    EditText fullName, email, password;
    Button signupButton;
    FirebaseAuth mAuth;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase instances
        FirebaseApp.initializeApp(this);
        Log.d("Firebase", "Firebase Initialized Successfully");
        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Connect UI elements
        fullName = findViewById(R.id.etName);
        email = findViewById(R.id.etEmail);
        password = findViewById(R.id.etPassword);
        signupButton = findViewById(R.id.btnSignup);

        // Handle Signup button click
        signupButton.setOnClickListener(view -> {
            String name = fullName.getText().toString().trim();
            String userEmail = email.getText().toString().trim();
            String userPassword = password.getText().toString().trim();

            // Basic validation
            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(userEmail) || TextUtils.isEmpty(userPassword)) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userPassword.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create user with Firebase Authentication
            Log.d("FIREBASE", "Trying to create user...");

            mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d("FIREBASE", "User created successfully");
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null) {
                                String userId = currentUser.getUid();
                                Log.d("FIREBASE", "User ID: " + userId);

                                // Update user profile with display name
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();

                                currentUser.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                Log.d("FIREBASE", "User profile updated with name");
                                            }
                                        });

                                // Save user data to database
                                User user = new User(name, userEmail);
                                databaseReference.child(userId).setValue(user)
                                        .addOnCompleteListener(dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                Log.d("FIREBASE", "User data saved to database");
                                                Toast.makeText(Signup.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(Signup.this, MainActivity.class));
                                            } else {
                                                Log.e("FIREBASE", "Failed to save user data", dbTask.getException());
                                                Toast.makeText(Signup.this, "Failed to save user data: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Log.e("FIREBASE", "Signup failed", task.getException());
                            Toast.makeText(Signup.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FIREBASE", "Error creating user", e);
                        Toast.makeText(Signup.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        });
    }

    // User model class
    public static class User {
        public String fullName, email;

        public User() {} // Needed for Firebase

        public User(String fullName, String email) {
            this.fullName = fullName;
            this.email = email;
        }
    }
}

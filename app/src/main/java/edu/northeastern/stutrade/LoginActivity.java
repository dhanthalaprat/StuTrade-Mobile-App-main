package edu.northeastern.stutrade;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnSignup;
    private UserSessionManager sessionManager;
    private FirebaseAuth firebaseAuth;

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        firebaseAuth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.username_edit_text);
        etPassword = findViewById(R.id.password_edit_text);
        btnLogin = findViewById(R.id.login_button);
        btnSignup = findViewById(R.id.signup_button);

        // Check if the user is already logged in
        sessionManager = new UserSessionManager(getApplicationContext());
        if (sessionManager.isUserLoggedIn()) {
            // User is logged in, navigate to the main activity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        btnLogin.setOnClickListener(v -> {
            email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            loginUser(email, password);
        });

        btnSignup.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    private void loginUser(String email, String password) {

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email cannot be empty");
            return;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Invalid email format");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password cannot be empty");
            return;
        }

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                        String userId = email.substring(0, email.indexOf("@")); // Extract user ID from the email

                        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
                        DatabaseReference userRef = usersRef.child(userId);
                        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    String username = dataSnapshot.child("name").getValue(String.class);
                                    if (username != null) {
                                        // Username found, save user details and proceed
                                        UserSessionManager sessionManager = new UserSessionManager(getApplicationContext());
                                        sessionManager.saveUserDetails(username, email);
                                        sessionManager.setLoggedIn(true);

                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();

                                    } else {
                                        // 'username' value is null, handle the error
                                        Toast.makeText(LoginActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // User node does not exist, handle the error
                                    Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Error occurred, handle the error
                                Toast.makeText(LoginActivity.this, "Database Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                        });
                    } else {
                        // Login failed
                        Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }
}
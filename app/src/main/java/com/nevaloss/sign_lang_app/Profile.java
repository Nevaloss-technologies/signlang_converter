package com.nevaloss.sign_lang_app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Profile extends AppCompatActivity {

    TextView txtName, txtEmail, txtUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        txtName = findViewById(R.id.txtName);
        txtEmail = findViewById(R.id.txtEmail);
        txtUid = findViewById(R.id.txtUid);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            txtName.setText((user.getDisplayName() != null ? user.getDisplayName() : "N/A"));
            txtEmail.setText(user.getEmail());
            txtUid.setText(user.getUid());
        }
    }
}

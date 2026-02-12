package protectedwp.safespace;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.WindowManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.security.MessageDigest;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class SecurityActivity extends Activity {

    private SharedPreferences bfuPrefs;
    private EditText passwordInput;
    private TextView instructionText;
    private Button actionButton;
    private boolean isSetupMode = false;
    private String tempMainHash = null;

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );    
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
		getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );    

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int top = (int) (getResources().getDisplayMetrics().heightPixels * 0.20);
        layout.setPadding(60, top, 60, 60);
        layout.setBackgroundColor(0xFF000000); 
        layout.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);

        instructionText = new TextView(this);
        instructionText.setTextSize(20);
        instructionText.setTextColor(0xFFFFFFFF); 
        layout.addView(instructionText);

        passwordInput = new EditText(this);
        passwordInput.setHint("Enter password here");
        passwordInput.setHintTextColor(0xFF404040);
        passwordInput.setTextColor(0xFF000000);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);
        passwordInput.requestFocus();

        actionButton = new Button(this);
        actionButton.setBackgroundColor(0xFFFFFFFF);
        actionButton.setTextColor(0xFF000000);        
        layout.addView(actionButton);

        setContentView(layout);
		
        Context bfuContext = createDeviceProtectedStorageContext();
        bfuPrefs = bfuContext.getSharedPreferences("secure_prefs", MODE_PRIVATE);

        actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClick();
            }
        });

        checkState();
    }

    private void checkState() {
        if (!bfuPrefs.contains("pass_hash")) {
            isSetupMode = true;
            instructionText.setText("Set Unlock Password (that starts autoconfiguration timer, which includes shows apps). Please use a unique password, different from your system password, so that if it is compromised, an attacker will not have access to other data sections.\n");
            actionButton.setText("Next");
        } else {
            isSetupMode = false;
            instructionText.setText("Enter Password");
            actionButton.setText("Unlock");
        }
    }

    private void handleButtonClick() {
        String input = passwordInput.getText().toString();
        if (input.isEmpty()) return;

        if (isSetupMode) {
            String hashed = hashPassword(input);
            if (tempMainHash == null) {
                tempMainHash = hashed;
                instructionText.setText("Set Duress Password (that wipes data). Please use a unique password, different from your system password, so that if it is compromised, an attacker will not have access to other data sections.\n");
                actionButton.setText("Finish Setup");
            } else {
                bfuPrefs.edit()
                        .putString("pass_hash", tempMainHash)
                        .putString("duress_hash", hashed)
                        .apply();
                isSetupMode = false;
                checkState();
            }
        } else {
            String storedDuress = bfuPrefs.getString("duress_hash", "");
            String storedPass = bfuPrefs.getString("pass_hash", "");

            if (verifyPassword(input, storedDuress)) {
                wipe.wipe(this);
            } else if (verifyPassword(input, storedPass)) {
                try {
                    startActivity(new Intent(this, ZeroActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK));
				    finishAndRemoveTask();
                } catch (Throwable StateErr) {
                    Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
                }
            }  else {
                Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
            }
        }
        passwordInput.setText("");
    }

    private String hashPassword(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] result = new byte[32];
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withParallelism(1)
                .withMemoryAsKB(65536) 
                .withIterations(5)
                .build();
        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params);
        gen.generateBytes(password.getBytes(StandardCharsets.UTF_8), result);
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + Base64.encodeToString(result, Base64.NO_WRAP);
    }

    private boolean verifyPassword(String input, String record) {
        if (record == null || !record.contains(":")) return false;
        try {
            String[] parts = record.split(":");
            byte[] salt = Base64.decode(parts[0], Base64.NO_WRAP);
            byte[] stored = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] test = new byte[32];
            Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withSalt(salt).withParallelism(1).withMemoryAsKB(65536).withIterations(5).build();
            Argon2BytesGenerator gen = new Argon2BytesGenerator();
            gen.init(params);
            gen.generateBytes(input.getBytes(StandardCharsets.UTF_8), test);
            return MessageDigest.isEqual(test, stored);
        } catch (Exception e) { return false; }
    }
}

package es.udc.psi.ttps;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String TAG = "_TAG";
    EditText et_code;
    Button but_create, but_join;

    DatabaseReference database;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        signInAnonymously();

        et_code = findViewById(R.id.et_code);
        but_create = findViewById(R.id.but_create);
        but_join = findViewById(R.id.but_join);

        database = FirebaseDatabase.getInstance().getReference();

        but_create.setOnClickListener(v -> createGame());
        but_join.setOnClickListener(v -> joinGame());
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG,"Authentication success");
            } else {
                Log.d(TAG,"Authentication failed");
            }
        });
    }

    private void createGame() {
        String code = generateCode();
        database.child("games").child(code).child("status").setValue("waiting").addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Game Code", code);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(this, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, GameActivity.class);
                intent.putExtra("GAME_CODE", code);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Error al crear la sala", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinGame() {
        String code = et_code.getText().toString();

        database.child("games").child(code).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Intent intent = new Intent(this, GameActivity.class);
                intent.putExtra("GAME_CODE", code);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Código de sala no válido", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}

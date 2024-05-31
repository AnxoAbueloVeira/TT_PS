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
    Button but_create_tictactoe, but_create_rps, but_join;

    DatabaseReference database;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        signInAnonymously();

        et_code = findViewById(R.id.et_code);
        but_create_tictactoe = findViewById(R.id.but_create_tictactoe);
        but_create_rps = findViewById(R.id.but_create_rps);
        but_join = findViewById(R.id.but_join);

        database = FirebaseDatabase.getInstance().getReference();

        but_create_tictactoe.setOnClickListener(v -> createGame("tictactoe"));
        but_create_rps.setOnClickListener(v -> createGame("rps"));
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

    private void createGame(String gameType) {
        String code = generateCode();
        database.child("games").child(code).child("status").setValue("waiting");
        database.child("games").child(code).child("type").setValue(gameType).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Game Code", code);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(this, getString(R.string.toast_clippboard), Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, gameType.equals("tictactoe") ? GameActivity.class : RPSGameActivity.class);
                intent.putExtra("GAME_CODE", code);
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.toast_error_room), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void joinGame() {
        String code = et_code.getText().toString();

        database.child("games").child(code).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String gameType = task.getResult().child("type").getValue(String.class);
                Intent intent = new Intent(this, gameType.equals("tictactoe") ? GameActivity.class : RPSGameActivity.class);
                intent.putExtra("GAME_CODE", code);
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.toast_wrong_code), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}

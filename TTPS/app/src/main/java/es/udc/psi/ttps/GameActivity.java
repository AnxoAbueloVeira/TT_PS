package es.udc.psi.ttps;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class GameActivity extends AppCompatActivity {

    DatabaseReference gameRef;
    TextView tv_code;
    Button[] buttons = new Button[9];
    boolean isPlayerOneTurn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tv_code = findViewById(R.id.tv_code);

        String code = getIntent().getStringExtra("GAME_CODE");
        gameRef = FirebaseDatabase.getInstance().getReference().child("games").child(code);

        tv_code.setText("Código de la sala: " + code);

        initButtons();
        setButtons();
        listenForGameChanges();
    }

    private void initButtons() {
        for (int i = 0; i < 9; i++){
            String butID = "but_" + i;
            int resID = getResources().getIdentifier(butID,"id",getPackageName());
            buttons[i] = findViewById(resID);
        }
    }

    private void setButtons() {
        for (int i = 0; i < 9; i++) {
            int finalI = i;
            buttons[i].setOnClickListener(v -> makeMove(finalI));
        }
    }

    private void makeMove(int index) {
        if (buttons[index].getText().toString().isEmpty()) {
            if (isPlayerOneTurn) {
                buttons[index].setText("X");
                gameRef.child("moves").child(String.valueOf(index)).setValue("X");
            } else {
                buttons[index].setText("O");
                gameRef.child("moves").child(String.valueOf(index)).setValue("O");
            }
            isPlayerOneTurn = !isPlayerOneTurn;
        }
    }

    private void listenForGameChanges() {
        gameRef.child("moves").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                int index = Integer.parseInt(snapshot.getKey());
                String value = snapshot.getValue(String.class);
                buttons[index].setText(value);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}

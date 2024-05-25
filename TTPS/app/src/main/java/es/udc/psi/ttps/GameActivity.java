package es.udc.psi.ttps;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class GameActivity extends AppCompatActivity {

    DatabaseReference gameRef;
    TextView tv_code, tv_turn;
    Button[] buttons = new Button[9];
    String playerSymbol;
    boolean isGameActive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tv_code = findViewById(R.id.tv_code);
        tv_turn = findViewById(R.id.tv_turn);

        String code = getIntent().getStringExtra("GAME_CODE");
        gameRef = FirebaseDatabase.getInstance().getReference().child("games").child(code);

        tv_code.setText("Código de la sala: " + code);

        initButtons();
        setButtons();
        listenForGameChanges();

        gameRef.child("players").child(FirebaseAuth.getInstance().getUid()).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        gameRef.child("players").get().addOnCompleteListener(playerTask -> {
                            if (playerTask.isSuccessful()) {
                                long playerCount = playerTask.getResult().getChildrenCount();
                                if (playerCount == 1) {
                                    playerSymbol = "X";
                                    gameRef.child("turn").setValue("X");
                                } else if (playerCount == 2) {
                                    playerSymbol = "O";
                                } else {
                                    isGameActive = false;
                                    Toast.makeText(this, "Sala llena", Toast.LENGTH_SHORT).show();
                                    for (Button button : buttons) {
                                        button.setEnabled(false);
                                    }
                                }
                                tv_turn.setText(playerSymbol.equals("X") ? "Turno: X" : "Turno: O");
                            }
                        });
                    } else {
                        Toast.makeText(this, "Error al unirse a la sala", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void initButtons() {
        for (int i = 0; i < 9; i++) {
            String butID = "but_" + i;
            int resID = getResources().getIdentifier(butID, "id", getPackageName());
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
        if (isGameActive && buttons[index].getText().toString().isEmpty()) {
            gameRef.child("turn").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String currentTurn = task.getResult().getValue(String.class);
                    if (currentTurn != null && currentTurn.equals(playerSymbol)) {
                        buttons[index].setText(playerSymbol);
                        gameRef.child("moves").child(String.valueOf(index)).setValue(playerSymbol);
                        gameRef.child("turn").setValue(playerSymbol.equals("X") ? "O" : "X");
                        checkWinner();
                    } else {
                        Toast.makeText(this, "No es tu turno", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void checkWinner() {
        String[][] field = new String[3][3];
        for (int i = 0; i < 9; i++) {
            field[i / 3][i % 3] = buttons[i].getText().toString();
        }

        for (int i = 0; i < 3; i++) {

            if (field[i][0].equals(field[i][1]) && field[i][1].equals(field[i][2]) && !field[i][0].isEmpty()) {
                endGame(field[i][0]);
                return;
            }

            if (field[0][i].equals(field[1][i]) && field[1][i].equals(field[2][i]) && !field[0][i].isEmpty()) {
                endGame(field[0][i]);
                return;
            }
        }

        if (field[0][0].equals(field[1][1]) && field[1][1].equals(field[2][2]) && !field[0][0].isEmpty()) {
            endGame(field[0][0]);
        } else if (field[0][2].equals(field[1][1]) && field[1][1].equals(field[2][0]) && !field[0][2].isEmpty()) {
            endGame(field[0][2]);
        } else if (isBoardFull()) {
            endGame(null);
        }
    }

    private boolean isBoardFull() {
        for (Button button : buttons) {
            if (button.getText().toString().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void endGame(String winner) {
        isGameActive = false;
        String message;
        if (winner != null) {
            message = "Ganador: " + winner;
        } else {
            message = "Empate";
        }
        gameRef.child("status").setValue(message);

        for (Button button : buttons) {
            button.setEnabled(false);
        }
    }

    private void listenForGameChanges() {
        gameRef.child("moves").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                int index = Integer.parseInt(snapshot.getKey());
                String value = snapshot.getValue(String.class);
                buttons[index].setText(value);
                checkWinner(); // Check winner after every move
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                int index = Integer.parseInt(snapshot.getKey());
                String value = snapshot.getValue(String.class);
                buttons[index].setText(value);
                checkWinner(); // Check winner after every move
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

        gameRef.child("status").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String status = snapshot.getValue(String.class);
                if (status != null && !status.equals("waiting")) {
                    isGameActive = false;
                    Toast.makeText(GameActivity.this, status, Toast.LENGTH_LONG).show();
                    for (Button button : buttons) {
                        button.setEnabled(false);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        gameRef.child("turn").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String turn = snapshot.getValue(String.class);
                if (turn != null) {
                    tv_turn.setText("Turno: " + turn);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}

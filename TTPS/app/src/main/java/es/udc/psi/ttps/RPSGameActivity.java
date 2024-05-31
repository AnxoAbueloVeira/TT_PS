package es.udc.psi.ttps;

import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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

public class RPSGameActivity extends AppCompatActivity {

    DatabaseReference gameRef;
    TextView tv_code, tv_status;
    Button btn_rock, btn_paper, btn_scissors;
    String playerMove;

    int playerId;
    boolean isGameActive = true;
    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    String currentUserUid = mAuth.getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rps);




        tv_code = findViewById(R.id.tv_code);
        tv_status = findViewById(R.id.tv_status);
        btn_rock = findViewById(R.id.btn_rock);
        btn_paper = findViewById(R.id.btn_paper);
        btn_scissors = findViewById(R.id.btn_scissors);

        String code = getIntent().getStringExtra("GAME_CODE");
        gameRef = FirebaseDatabase.getInstance().getReference().child("games").child(code);

        tv_code.setText(tv_code.getText().toString() + code);

        btn_rock.setOnClickListener(v -> makeMove("rock"));
        btn_paper.setOnClickListener(v -> makeMove("paper"));
        btn_scissors.setOnClickListener(v -> makeMove("scissors"));

        gameRef.child("players").child(currentUserUid).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        gameRef.child("players").get().addOnCompleteListener(playerTask -> {
                            if (playerTask.isSuccessful()) {
                                long playerCount = playerTask.getResult().getChildrenCount();
                                if (playerCount == 1){
                                    //gameRef.child("playerId").child(currentUserUid).setValue(1);

                                }
                                if (playerCount == 2){
                                    //gameRef.child("playerId").child(currentUserUid).setValue(2);

                                }
                                if (playerCount > 2) {
                                    isGameActive = false;
                                    Toast.makeText(this, "Sala llena", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(this, "Error al unirse a la sala", Toast.LENGTH_SHORT).show();
                    }
                });


            DatabaseReference playerIdRef = gameRef.child("playerId").child(currentUserUid);
            playerIdRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        playerId = snapshot.getValue(Integer.class);
                    } else {
                        // Manejar el caso donde no se encuentra ningún playerId asociado al usuario actual.
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Manejar errores de Firebase Database.
                }
            });


        listenForGameChanges();
    }


    private void makeMove(String move) {
        if (isGameActive) {
            playerMove = move;
            gameRef.child("moves").child(FirebaseAuth.getInstance().getUid()).setValue(move).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    checkWinner();
                } else {
                    Toast.makeText(this, "Error al realizar el movimiento", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkWinner() {
        gameRef.child("moves").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() == 2) {
                    String opponentMove = null;
                    for (DataSnapshot moveSnapshot : snapshot.getChildren()) {
                        if (!moveSnapshot.getKey().equals(currentUserUid)) {
                            opponentMove = moveSnapshot.getValue(String.class);
                        }
                    }
                    if (opponentMove != null && !opponentMove.equals("waiting")) {
                        determineWinner(playerMove, opponentMove);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Manejar errores de Firebase Database.
            }
        });
    }

    private void determineWinner(String playerMove, String opponentMove) {
        String result;
        if (playerMove.equals(opponentMove)) {
            result = "Empate";
        } else if (
                (playerMove.equals("rock") && opponentMove.equals("scissors")) ||
                        (playerMove.equals("scissors") && opponentMove.equals("paper")) ||
                        (playerMove.equals("paper") && opponentMove.equals("rock"))
        ) {
            result = "¡Ganador!";
        } else {
            result = "¡Perdedor!";
        }

        endGame(result);
    }

    private void endGame(String result) {
        isGameActive = false;
        gameRef.child("status").setValue(result); // Establece el resultado para ambos jugadores

        tv_status.setText(result);
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }





    private void listenForGameChanges() {
        gameRef.child("status").addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String value = snapshot.getValue(String.class);
                // Verifica si el estado ha cambiado para el jugador actual
                if (snapshot.getKey().equals(currentUserUid)) {
                    checkWinner(); // Comprobar si hay un ganador
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Manejar errores de Firebase Database.
            }
        });
    }
}
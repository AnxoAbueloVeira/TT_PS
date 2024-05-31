package es.udc.psi.ttps;

import android.os.Bundle;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
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
    ImageButton btn_rock, btn_paper, btn_scissors;
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
                                if (playerCount > 2) {
                                    isGameActive = false;
                                    Toast.makeText(this, getString(R.string.toast_room_full), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    } else {
                        Toast.makeText(this, getString(R.string.toast_error_joining), Toast.LENGTH_SHORT).show();
                    }
                });

        DatabaseReference playerIdRef = gameRef.child("playerId").child(currentUserUid);
        playerIdRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    playerId = snapshot.getValue(Integer.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        listenForGameChanges();
    }

    private void makeMove(String move) {
        if (isGameActive) {
            playerMove = move;
            // Llamada para deshabilitar los botones
            disableButtons();
            gameRef.child("moves").child(FirebaseAuth.getInstance().getUid()).setValue(move).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    checkWinner();
                } else {
                    Toast.makeText(this, getString(R.string.toast_error_move), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void disableButtons() {
        btn_rock.setEnabled(false);
        btn_paper.setEnabled(false);
        btn_scissors.setEnabled(false);
    }

    private void checkWinner() {
        gameRef.child("moves").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getChildrenCount() == 2) {
                    String opponentMove = null;
                    String opponentUid = null;
                    for (DataSnapshot moveSnapshot : snapshot.getChildren()) {
                        if (!moveSnapshot.getKey().equals(currentUserUid)) {
                            opponentMove = moveSnapshot.getValue(String.class);
                            opponentUid = moveSnapshot.getKey();
                        }
                    }
                    if (opponentMove != null && !opponentMove.equals("waiting")) {
                        determineWinner(playerMove, opponentMove, opponentUid);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void determineWinner(String playerMove, String opponentMove, String opponentUid) {
        //Lógica del Piedra, papel o tijeras
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

        endGame(result, opponentUid, playerMove, opponentMove);
    }

    private void endGame(String result, String opponentUid, String playerMove, String opponentMove) {
        isGameActive = false;
        String opponentResult;
        TextView animation;

        // Determinamos el resultado del oponente en base al del jugador
        if (result.equals("¡Ganador!")) {
            animation = findViewById(R.id.tv_winner);
            opponentResult = "¡Perdedor!";
        } else if (result.equals("¡Perdedor!")) {
            animation = findViewById(R.id.tv_loser);
            opponentResult = "¡Ganador!";
        } else {
            animation = null;
            opponentResult = "Empate";
        }

        // Actualizamos el estado de los jugadores
        gameRef.child("state").child(currentUserUid).setValue(result);
        gameRef.child("state").child(opponentUid).setValue(opponentResult);

        // Mostramos la animación correspondiente a los estados anteriores
        animateResult(animation);

        tv_status.setText(result);

    }

    private void animateResult(TextView animation) {
        if (animation != null) {
            animation.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            animation.startAnimation(fadeIn);
        }
    }



    private void listenForGameChanges() {
        gameRef.child("state").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateGameState(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                updateGameState(snapshot);
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

    private void updateGameState(@NonNull DataSnapshot snapshot) {
        String value = snapshot.getValue(String.class);
        String uid = snapshot.getKey();
        if (uid != null && value != null) {
            if (uid.equals(currentUserUid)) {
                // Actualizamos IU del jugador
                tv_status.setText(value);
                animateResultBasedOnValue(value);
            }
        }
    }

    private void animateResultBasedOnValue(String result) {
        TextView animation = null;
        if (result.equals("¡Ganador!")) {
            animation = findViewById(R.id.tv_winner);
        } else if (result.equals("¡Perdedor!")) {
            animation = findViewById(R.id.tv_loser);
        }

        if (animation != null) {
            animation.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            animation.startAnimation(fadeIn);
        }
    }
}
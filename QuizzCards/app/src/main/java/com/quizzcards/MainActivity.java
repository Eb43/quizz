package com.quizzcards;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private boolean isBackVisible = false;
    private View cardFront, cardBack;
    private TextView frontTextView, backTextView;
    private List<QuestionAnswerPair> quizData = new ArrayList<>();
    private Random random = new Random();

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    loadQuizData(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cardFront = findViewById(R.id.card_front);
        cardBack = findViewById(R.id.card_back);
        frontTextView = findViewById(R.id.front_text);
        backTextView = findViewById(R.id.back_text);
        Button loadQuizButton = findViewById(R.id.load_quiz_button);

        cardFront.setOnClickListener(v -> flipCard());
        cardBack.setOnClickListener(v -> flipCard());


        loadQuizButton.setOnClickListener(v -> {
            // If the cardBack is visible, flip it to cardFront
            if (isBackVisible) {
                flipCard();
            }
            // Then open the file picker
            openFilePicker();
        });
    }

    private void flipCard() {
        Animation flipOut = AnimationUtils.loadAnimation(this, R.anim.flip_out);
        Animation flipIn = AnimationUtils.loadAnimation(this, R.anim.flip_in);

        if (isBackVisible) {
            cardBack.startAnimation(flipOut);
            flipOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    cardBack.setVisibility(View.GONE);
                    cardFront.setVisibility(View.VISIBLE);
                    cardFront.startAnimation(flipIn);
                    // Display a new random question-answer pair only after the flip animation is complete
                    displayRandomQuiz();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        } else {
            cardFront.startAnimation(flipOut);
            flipOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    cardFront.setVisibility(View.GONE);
                    cardBack.setVisibility(View.VISIBLE);
                    cardBack.startAnimation(flipIn);


                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        }

        isBackVisible = !isBackVisible;
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/primary:"));
        filePickerLauncher.launch(intent);
    }

    private void loadQuizData(Uri uri) {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            quizData.clear();
            String line;
            String question = null, answer = null;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Q: ")) {
                    question = line.substring(3).trim();
                } else if (line.startsWith("A: ")) {
                    answer = line.substring(3).trim();
                } else if (line.equals("]") && question != null && answer != null) {
                    quizData.add(new QuestionAnswerPair(question, answer));
                    question = null;
                    answer = null;
                }
            }

            if (!quizData.isEmpty()) {
                Toast.makeText(this, "Quiz loaded successfully!", Toast.LENGTH_SHORT).show();
                displayRandomQuiz();
            } else {
                Toast.makeText(this, "No valid quiz data found in the file.", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this, "Failed to load file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void displayRandomQuiz() {
        if (!quizData.isEmpty()) {
            QuestionAnswerPair randomPair = quizData.get(random.nextInt(quizData.size()));
            frontTextView.setText(randomPair.getQuestion());
            backTextView.setText(randomPair.getAnswer());
        }
    }

    private static class QuestionAnswerPair {
        private final String question;
        private final String answer;

        public QuestionAnswerPair(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        public String getAnswer() {
            return answer;
        }
    }
}
package com.example.multilangstttts;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSION = 1;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private EditText editText;
    private Spinner languageSpinner;
    private Button btnSpeak, btnConvert;

    // TensorFlow Lite Interpreters for dummy models
    private Interpreter sttInterpreter;
    private Interpreter ttsInterpreter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        editText = findViewById(R.id.editText);
        languageSpinner = findViewById(R.id.languageSpinner);
        btnSpeak = findViewById(R.id.btnSpeak);
        btnConvert = findViewById(R.id.btnConvert);

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.ENGLISH); // Set default language to English
            }
        });

        // Load dummy TensorFlow Lite models
        try {
            sttInterpreter = new Interpreter(loadModelFile("stt_model.tflite"));
            ttsInterpreter = new Interpreter(loadModelFile("tts_model.tflite"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Handle runtime permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION);
        }

        // Set up SpeechRecognizer
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Toast.makeText(MainActivity.this, "Listening...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float rmsdB) {}

            @Override
            public void onBufferReceived(byte[] buffer) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int error) {
                Toast.makeText(MainActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    editText.setText(matches.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {}

            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        // Start STT on button click
        btnSpeak.setOnClickListener(v -> startSpeechRecognition());

        // Convert text to speech on button click
        btnConvert.setOnClickListener(v -> convertTextToSpeech());
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Start speech recognition based on the selected language
    private void startSpeechRecognition() {
        String selectedLanguage = getSelectedLanguage();
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        speechRecognizer.startListening(intent);
    }

    // Convert the text in EditText to speech
    private void convertTextToSpeech() {
        String text = editText.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter some text", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedLanguage = getSelectedLanguage();
        Locale locale = new Locale(selectedLanguage);
        textToSpeech.setLanguage(locale);

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    // Get selected language code from the spinner
    private String getSelectedLanguage() {
        String language = languageSpinner.getSelectedItem().toString();
        switch (language) {
            case "Bhojpuri":
                return "bho-IN";  // Bhojpuri language (assuming locale bho-IN)
            case "Hindi":
                return "hi-IN";   // Hindi
            case "Marathi":
                return "mr-IN";   // Marathi
            case "Assamese":
                return "as-IN";   // Assamese (North East Indian)
            case "Manipuri":
                return "mni-IN";  // Manipuri (North East Indian)
            case "Tamil":
                return "ta-IN";   // Sri Lankan Tamil
            default:
                return "EN-IN";  // Default to Hindi if not found
        }
    }

    // Load the TensorFlow Lite model file
    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(modelPath);
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
        if (sttInterpreter != null) {
            sttInterpreter.close();
        }
        if (ttsInterpreter != null) {
            ttsInterpreter.close();
        }
    }
}

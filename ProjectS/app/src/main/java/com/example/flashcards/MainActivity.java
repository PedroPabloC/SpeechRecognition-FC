package com.example.flashcards;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


//Necesarias para el asistente:
import com.example.flashcards.helpers.SendMessageInBG;
import com.example.flashcards.interfaces.BotReply;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.common.collect.Lists;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements BotReply {

    //Generales
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;
    TextView mTextView;
    ImageButton mVoiceButton;

    //Necesarios para Dialogflow
    private SessionsClient sessionsClient;
    private SessionName sessionName;
    private String uuid = UUID.randomUUID().toString();
    private String TAG = "mainactivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.textTv);
        mVoiceButton = findViewById(R.id.voiceBtn);

        setUpBot();//Se establece el bot en el programa

        mVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak();
            }
        });
    }



    /**
     * Esta clase establece el bot en el proyecto. Mediante las credenciales de dialogflow, obtiene las configuraciones propias de las
     * credenciales y crea una session única para cada servicio con la ayuda de uuid(identificador único universal)
     */
    private void setUpBot() {
        try {
            InputStream stream = this.getResources().openRawResource(R.raw.credential);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);

            Log.d(TAG, "projectId : " + projectId);
        } catch (Exception e) {
            Log.d(TAG, "setUpBot: " + e.getMessage());
        }
    }


    /**
     * Este método se encarga de mandar los mensajes con las configuraciones necesarias hechas por el setUpBot() y a su vez configura
     * la consulta en español
     * @param message
     */
    private void sendMessageToBot(String message) {
        QueryInput input = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("es")).build();
        new SendMessageInBG(this, sessionName, sessionsClient, input);
    }




    /**
     * Después de mandar el mensaje, BotReply lo devuelve el intentDetectado como respuesta y organiza según sea el caso
     * @param returnResponse
     */
    @Override
    public void callback(DetectIntentResponse returnResponse) {

        if(returnResponse!=null) {
            String botReply = returnResponse.getQueryResult().getFulfillmentText();
            if(!botReply.isEmpty()){
                switch (botReply){
                    case "1": {
                        mTextView.setText("Pasar pregunta");
                    }

                    case "2": {
                        mTextView.setText("Repetir pregunta");
                    }
                }
            }else {
                Toast.makeText(this, "No se ha dicho ningún mensaje", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Surgió un error en el sistema", Toast.LENGTH_SHORT).show();
        }
    }



    /**
     * Método necesario para configurar el idioma e intent del reconocimiento de voz de Google
     */
    private void speak() {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Hola, intenta decir algo");

        try {

            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        }
        catch (Exception e){

            Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }




    /**
     * Método de Speech to Text
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case REQUEST_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && data != null){
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    compararRespuesta(result.get(0));
                }
                break;
            }
        }
    }


    public void compararRespuesta(String userResult){
        if(userResult.equals("Respuesta número 1") || userResult.equals("Respuesta número 2")){
            respuestaCorrecta();
        }else if(userResult.equals("Respuesta número 3") || userResult.equals("Respuesta número 4")){
            respuestaCorrecta();
        }else {
            sendMessageToBot(userResult);
        }
    }


    public void respuestaCorrecta(){
        mTextView.setText("Respuesta correcta");
    }

}

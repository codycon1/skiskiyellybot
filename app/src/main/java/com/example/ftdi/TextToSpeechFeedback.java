package com.example.ftdi;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

public class TextToSpeechFeedback {
    public int speechStatus;
    public TextToSpeech textToSpeech;

    public String[] insultList = {"Disappointment", "Melon", "Thundercunt", "Asshat", "Assclown", "Dingus", "Peasant", "Douchecanoe", "Troglodite", "Neanderthal"};
    public String[] encouragementList = {"Good", "Great", "Perfect", "Nice", "Marvelous", "Superb", "Rad", "Prime", "Super", "Neat", "Gnarly"};
    public String[] badList = {"Bad", "Terrible", "Garbage", "Boo"};

    TextToSpeechFeedback(Context context){ // Initializer
        textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int ttsLang = textToSpeech.setLanguage(Locale.US);

                    if (ttsLang == TextToSpeech.LANG_MISSING_DATA
                            || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The Language is not supported!");
                    } else {
                        Log.i("TTS", "Language Supported.");
                    }
                    Log.i("TTS", "Initialization success.");
                } else {
                    Log.i("TTS", "Something fucked up. TTS not init");
                }
            }
        });
    }

    public void output(String input){
        Log.i("TTS", "button clicked: " + input);
        textToSpeech.speak(input, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void feedback(int mode){
        if(mode == 0){
            good();
        }
        else if(mode == 1){
            insult();
        }
    }

    public void intro(boolean cuss){
        if(cuss) {
            textToSpeech.speak("Welcome. My name is Schmitty! I'm your personal ski coach! Prepare to shred, bitch!", TextToSpeech.QUEUE_FLUSH, null);
        }
        else{
            textToSpeech.speak("Welcome. My name is Schmitty! I'm your personal ski coach! Prepare to shred!", TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    public void insult(){
        Log.i("TTS", "Insult sent");
        textToSpeech.speak(getRandom(insultList), TextToSpeech.QUEUE_FLUSH, null);
    }

    public void good(){
        Log.i("TTS", "Encouragement sent");
        textToSpeech.speak(getRandom(encouragementList), TextToSpeech.QUEUE_FLUSH, null);
    }

    public void bad(){
        Log.i("TTS", "Encouragement sent");
        textToSpeech.speak(getRandom(badList), TextToSpeech.QUEUE_FLUSH, null);
    }

    private String getRandom(String[] array) {
        int rnd = new Random().nextInt(array.length);
        return array[rnd];
    }
}

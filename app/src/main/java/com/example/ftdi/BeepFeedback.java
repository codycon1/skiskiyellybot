package com.example.ftdi;

import android.app.Activity;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;
import java.util.Random;

import in.excogitation.zentone.library.ToneStoppedListener;
import in.excogitation.zentone.library.ZenTone;

public class BeepFeedback {
    BeepFeedback(Context context){
        int freq = 440;
        ZenTone.getInstance().generate(freq, 1, 0, new ToneStoppedListener() {
            @Override
            public void onToneStopped() {
                // Do something when the tone has stopped playing
            }
        });
    }
    public void beep(int freq){
        ZenTone.getInstance().generate(freq, 1, 1, new ToneStoppedListener() {
            @Override
            public void onToneStopped() {

            }
        });
    }
}

package com.pavel.dolbik.castdisplay;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.gms.cast.CastPresentation;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;

/**
 * Created by Pavel on 17.04.2016.
 */
public class PresentationService extends CastRemoteDisplayLocalService {

    private MyPresentation myPresentation;
    private MediaPlayer    mediaPlayer;
    private ImageView      imageView;

    @Override
    public void onCreate() {
        super.onCreate();

        // Audio
        mediaPlayer = MediaPlayer.create(this, R.raw.sound);
        mediaPlayer.setVolume((float) 0.1, (float) 0.1);
        mediaPlayer.setLooping(true);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void onCreatePresentation(Display display) {
        Log.d("Pasha", "onCreatePresentation");
        dismissPresentation();
        myPresentation = new MyPresentation(this, display);

        try {
            myPresentation.show();
            mediaPlayer.start();
            imageView.setImageResource(R.drawable.image_1);
        } catch (WindowManager.InvalidDisplayException ex) {
            Log.e("Pasha", "Unable to show presentation, display was removed.", ex);
            dismissPresentation();
        }
    }


    private class MyPresentation extends CastPresentation {

        public MyPresentation(Context serviceContext, Display display) {
            super(serviceContext, display);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.presentation);

            imageView = (ImageView) findViewById(R.id.image);
        }
    }


    public void changeImage(int res) {
        imageView.setImageResource(res);
    }


    @Override
    public void onDismissPresentation() {
        Log.d("Pasha", "onDismissPresentation");
        dismissPresentation();
    }

    private void dismissPresentation() {
        if (myPresentation != null) {
            mediaPlayer.stop();
            myPresentation.dismiss();
            myPresentation = null;
        }
    }
}

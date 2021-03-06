package za.co.twinc.a9letterjumble;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Created by wilco on 2019/08/30.
 * 9LetterJumble
 */

public class Sounds {

    public void play(Context context, int sound){
        float volume = context.getSharedPreferences(
                MainActivity.MAIN_PREFS, 0).getFloat("volume", 0.5f);

        if (volume==0f)
            return;

        MediaPlayer mediaPlayer = MediaPlayer.create(context, sound);
        mediaPlayer.setVolume(volume, volume);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });
        mediaPlayer.start();
    }

    void playDing(Context context){
        play(context, R.raw.accept);
    }

    void playClick(Context context){
        play(context, R.raw.click02);
    }

}

package org.khacks.singandlearn;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.khacks.singandlearn.datastore.Song;
import org.khacks.singandlearn.datastore.SongsDatastore;
import org.khacks.singandlearn.datastore.Word;
import org.khacks.singandlearn.datastore.WordsDatastore;
import org.khacks.singandlearn.fragments.GapsFragment;
import org.khacks.singandlearn.fragments.LyricsFragment;
import org.khacks.singandlearn.fragments.MediaPlayerFragment;

import java.util.Random;

/**
 * Created by gus on 28/02/15.
 */
public class TestActivity extends Activity {

    private MediaPlayer mediaPlayer;
    private Song song;




    GapsFragment gapsFragment;
    LyricsFragment lyricsFragment;
    MediaPlayerFragment mediaPlayerFragment;
    FixedTimer timer;


    public static final String SONG_ID = "SONG_ID";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("testActivity", "onCreate");
        setContentView(R.layout.test_layout);

        gapsFragment = (GapsFragment) getFragmentManager().findFragmentById(R.id.gaps);
        lyricsFragment = (LyricsFragment) getFragmentManager().findFragmentById(R.id.lyrics);
        mediaPlayerFragment = (MediaPlayerFragment) getFragmentManager().findFragmentById(R.id.media_player);


        String songId = getIntent().getExtras().getString(SONG_ID);

        SongsDatastore datastore = new SongsDatastore(this);
        song = datastore.getSong(songId);



        mediaPlayer = MediaPlayer.create(this, song.res_id);
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });
        mediaPlayer.start();
        mediaPlayerFragment.setRewindPoint(0);


        timer = new FixedTimer(new Handler(Looper.getMainLooper()),new Runnable() {
            @Override
            public void run() {
                try {
                    Song.LyricsResult result = null;
                    int position = mediaPlayer.getCurrentPosition() / 1000;
                    result = song.getLyricsAtPosition(position);
                    String lyricsString = result.getLyrics().getText();
                    lyricsFragment.setLyrics(lyricsString);
                }catch (IllegalStateException e){
                    Log.d("TestAct", "illegalState: "+e.getMessage());
                }
            }
        } , 500);
        timer.start();

    }

    private void setGapWords(Word correctOne){
        // I'm not sure about this method
        WordsDatastore wordsDatastore = new WordsDatastore(this);
        Word[] incorrectOnes = wordsDatastore.getRandomWords(3, song.name);
        Random random = new Random();
        int luck = random.nextInt(4);
        gapsFragment.setCorrectAnswer(luck);
        int i = 0;
        while (i < 3){
            if(i == luck){
                gapsFragment.putWord(correctOne);
            }
            else{
                gapsFragment.putWord(incorrectOnes[i]);
                i++;
            }
        }
        if(luck == 3){
            gapsFragment.putWord(correctOne);
        }

    }


    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public Song getSong() {
        return song;
    }


    @Override
    protected void onPause() {
        super.onPause();
        timer.stop();
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }
}

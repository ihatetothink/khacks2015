package org.khacks.singandlearn;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.khacks.singandlearn.datastore.RawLyricsData;
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
    private SongsDatastore datastore;
    private WordsDatastore wordsDatastore;


    Song.LyricsResult lastResult;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("testActivity", "onCreate");
        setContentView(R.layout.test_layout);

        gapsFragment = (GapsFragment) getFragmentManager().findFragmentById(R.id.gaps);
        lyricsFragment = (LyricsFragment) getFragmentManager().findFragmentById(R.id.lyrics);
        mediaPlayerFragment = (MediaPlayerFragment) getFragmentManager().findFragmentById(R.id.media_player);




        String songId = getIntent().getExtras().getString(SONG_ID);



        datastore = new SongsDatastore(this);
        song = datastore.getSong(songId);

        mediaPlayerFragment.setSongTitle(song.name);
        mediaPlayerFragment.setArtist(song.artist);


        mediaPlayer = MediaPlayer.create(this, song.res_id);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                timer.stop();
            }
        });

        mediaPlayer.start();
        mediaPlayerFragment.setRewindPoint(0);

        wordsDatastore = new WordsDatastore(this);
        

        timer = new FixedTimer(new Handler(Looper.getMainLooper()),new Runnable() {
            @Override
            public void run() {
                try {
                    int position = mediaPlayer.getCurrentPosition() / 1000;
                    Song.LyricsResult result = song.getLyricsAtPosition(position);
                    String lyricsString = result.getLyrics().getText();


                    if(lastResult != result) {
                        gapsFragment.clear();
                    }
                    lastResult = result;


                    StringBuilder sb = new StringBuilder();
                    String[] parts = lyricsString.split(" ");
                    String word_longest = "";
                    //if(result.getLyrics().getList() == null)return;

                    //if (result.getLyrics().getList().size() > 0 && result.getLyrics().getList().size() == parts.length) {
                        int index2 = 0;
                        int word_longest_index = 0;
                        for (String word : parts) {
                            if (word.length() > word_longest.length()) {
                                word_longest = word;
                                word_longest_index = index2;
                            }
                            index2++;
                        }

                        parts[word_longest_index] = "_____";

                        for (String part : parts) {
                            sb.append(part);
                            sb.append(' ');
                        }

                    //} else {
                        sb.append(' ');
                    //}

                    //Word longestWord = wordsDatastore.getWord(song._id, word_longest);

                    lyricsFragment.setLyrics(sb.toString());

                    setGapWords();

                    int index = result.getLyrics().getIndex() + 1;
                    RawLyricsData nextSegmentData = song.getLineAtIndex(index);
                    int size = (int) (nextSegmentData.getTime() - result.getLyrics().getTime());
                    float percentage = (float) ((position-result.getLyrics().getTime())/size );
                    int highlightTo = (int) (lyricsString.length()*percentage);

                    if(highlightTo == 0)
                        return;

                    lyricsFragment.setLyricsHighlight(highlightTo);


                }catch (IllegalStateException e){
                    Log.d("TestAct", "illegalState: "+e.getMessage());
                }
            }
        } , 100);
        timer.start();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(mediaPlayer != null)
        outState.putInt("currentpos", mediaPlayer.getCurrentPosition());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(savedInstanceState != null) {
            int currentPo = savedInstanceState.getInt("currentpos", -1);
            if (currentPo == -1)
                return;
            mediaPlayer.seekTo(currentPo);
        }
    }

    private void setGapWords(/*Word correctOne*/){
        // I'm not sure about this method
        Word[] incorrectOnes = wordsDatastore.getRandomWords(4, song._id);
        Random random = new Random();
        int luck = random.nextInt(4);
        //gapsFragment.setCorrectAnswer(luck);
        int i = 0;

        while (i < 4){
            if(false && i == luck){
                //gapsFragment.putWord(correctOne);
            }
            else{
                gapsFragment.putWord(incorrectOnes[i]);

            }
            i++;
        }
        if(luck == 3){
            //gapsFragment.putWord(correctOne);
        }
        //Song song = datastore.getSong(correctOne.getSong());
        //Word.QuizInfo quizInfo = correctOne.getQuiz(datastore, song);
        //lyricsFragment.setLyrics(quizInfo.blankedText);

    }

    public void publishScore(boolean isGood){
        if(isGood){
            mediaPlayerFragment.incrementGoodScore();
        }
        else{
            mediaPlayerFragment.incrementWrongScore();
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
        if (timer != null)
        timer.stop();
        if(mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

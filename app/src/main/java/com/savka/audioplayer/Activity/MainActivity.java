package com.savka.audioplayer.Activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.savka.audioplayer.R;
import com.savka.audioplayer.Service.BackgroundAudioService;
import com.savka.audioplayer.Utils.GlobalVariables;
import com.savka.audioplayer.Utils.SeekbarHelper;

public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    final String LOG_TAG = "MainActivity";

    private ImageButton btnPlay;
    private ImageButton btnForward;
    private ImageButton btnBackward;
    private ImageButton btnNext;
    private ImageButton btnPrevious;
    private ImageButton btnPlaylist;
    private ImageButton btnRepeat;
    private ImageButton btnShuffle;
    private SeekBar songProgressBar;
    private TextView songTitleLabel;
    private TextView songCurrentDurationLabel;
    private TextView songTotalDurationLabel;
    //Service
    Intent playbackServiceIntent;
    BackgroundAudioService backgroundAudioService;
    ServiceConnection sConn;
    //Broadcast Receiver
    public final static String BROADCAST_ACTION = "com.savka.audioplayer";
    BroadcastReceiver br;
    public final static String PARAM_TASK = "task";
    public final static String PARAM_TITLE = "title";
    public final static String PARAM_TOTAL_DURATION = "duration";
    public final static String PARAM_CORENT_DURATION = "time";

    final int TASK1_CODE = 1; // update song title task
    final int TASK2_CODE = 2;// update progress bar task
    //Binder
    boolean bound = false;

    // Handler to update UI timer, progress bar etc,.

    String songTitle;
    private SeekbarHelper seekbarHelper;
    private int currentSongIndex;
    private long totalDuration;
    boolean isPlaying = true;
    boolean isShuffle;
    boolean isRepeat;

    SharedPreferences preferences;
    @Override
    protected void onStart() {
        super.onStart();
        bindService(playbackServiceIntent, sConn, 0);
        Log.v(LOG_TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!bound) return;
        unbindService(sConn);
        bound = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(LOG_TAG, "onPause") ;
        // Store values between instances

        SharedPreferences.Editor editor = preferences.edit();

        editor.putInt("currentSongIndex", currentSongIndex);
        editor.putString("songTitle", songTitle);
        editor.putBoolean("isShuffle", isShuffle);
        editor.putBoolean("isRepeat", isRepeat);
        // Commit to storage
        editor.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(LOG_TAG, "onResume") ;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(LOG_TAG, "onCreate");

        setContentView(R.layout.player);
        // All player buttons
        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnForward = (ImageButton) findViewById(R.id.btnForward);
        btnBackward = (ImageButton) findViewById(R.id.btnBackward);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        btnPrevious = (ImageButton) findViewById(R.id.btnPrevious);
        btnPlaylist = (ImageButton) findViewById(R.id.btnPlaylist);
        btnRepeat = (ImageButton) findViewById(R.id.btnRepeat);
        btnShuffle = (ImageButton) findViewById(R.id.btnShuffle);
        songProgressBar = (SeekBar) findViewById(R.id.songProgressBar);
        songTitleLabel = (TextView) findViewById(R.id.songTitle);
        songCurrentDurationLabel = (TextView) findViewById(R.id.songCurrentDurationLabel);
        songTotalDurationLabel = (TextView) findViewById(R.id.songTotalDurationLabel);

        restoreState();

        playbackServiceIntent = new Intent(this, BackgroundAudioService.class);

        sConn = new ServiceConnection() {

            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(LOG_TAG, "MainActivity onServiceConnected");
                backgroundAudioService = ((BackgroundAudioService.MyBinder) binder).getService();
                bound = true;
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d(LOG_TAG, "MainActivity onServiceDisconnected");
                bound = false;
            }
        };

        seekbarHelper = new SeekbarHelper();

        // Listeners
        songProgressBar.setOnSeekBarChangeListener(this); // Important

        ////start new service if not running
        if (GlobalVariables.isMyServiceRunning) {
            Log.v(LOG_TAG, "Service Is Already Running. Do Nothing!");
            //NOP
        } else {
            Log.v(LOG_TAG, "StartNewService!");
            GlobalVariables.isMyServiceRunning = true;
            startService(playbackServiceIntent);
        }


        //     create BroadcastReceiver
        br = new BroadcastReceiver() {
            // actions when getting massages
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(PARAM_TASK, 0);
                totalDuration = intent.getLongExtra(PARAM_TOTAL_DURATION, 0);
                long currentDuration = intent.getLongExtra(PARAM_CORENT_DURATION, 0);
                // catch  massage's about starting tasks
                switch (task) {
                    case TASK1_CODE:
                        songTitle = intent.getStringExtra(PARAM_TITLE);
                        songTitleLabel.setText(songTitle);
                        break;
                    case TASK2_CODE:
                        // Displaying Total Duration time
                        songTotalDurationLabel.setText("" + seekbarHelper.milliSecondsToTimer(totalDuration));
                        // Displaying time completed playing
                        songCurrentDurationLabel.setText("" + seekbarHelper.milliSecondsToTimer(currentDuration));
                        //Updating progress bar
                        int progress = (int) (seekbarHelper.getProgressPercentage(currentDuration, totalDuration));
                        songProgressBar.setProgress(progress);
                        break;
                }
            }
        };

        // create filter for BroadcastReceiver
        IntentFilter intFilt = new IntentFilter(BROADCAST_ACTION);
        // register BroadcastReceiver
        registerReceiver(br, intFilt);
        /**
         * Play button click event
         * plays a song and changes button to pause image
         * pauses a song and changes button to play image
         * */
        btnPlay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                // check for already playing
                Log.v(LOG_TAG, "btnPlayPressed");
                isPlaying = backgroundAudioService.btnPlay();
                if (isPlaying) {
                    // Changing button image to pause button
                    btnPlay.setImageResource(R.drawable.btn_pause);
                } else {
                    // Changing button image to play button
                    btnPlay.setImageResource(R.drawable.btn_play);
                }
            }
        });

        /**
         * Forward button click event
         * Forwards song specified seconds
         * */
        btnForward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnForward");
                backgroundAudioService.btnForward();
            }
        });

        /**
         * Backward button click event
         * Backward song to specified seconds
         * */
        btnBackward.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnBackward");
                backgroundAudioService.btnBackWard();

            }
        });

        /**
         * Next button click event
         * Plays next song by taking currentSongIndex + 1
         * */
        btnNext.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnNext");
                backgroundAudioService.btnNext();
            }
        });

        /**
         * Back button click event
         * Plays previous song by currentSongIndex - 1
         * */
        btnPrevious.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "Previous");
                backgroundAudioService.btnPrevious();
            }
        });

        /**
         * Button Click event for Repeat button
         * Enables repeat flag to true
         * */
        btnRepeat.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnRepeat");
                isRepeat = backgroundAudioService.btnRepeat();
                if (!isRepeat) {
                    Toast.makeText(getApplicationContext(), "Repeat is OFF", Toast.LENGTH_SHORT).show();
                    btnRepeat.setImageResource(R.drawable.btn_repeat);
                } else {
                    Toast.makeText(getApplicationContext(), "Repeat is ON", Toast.LENGTH_SHORT).show();
                    btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
                    btnShuffle.setImageResource(R.drawable.btn_shuffle);
                }
            }
        });

        /**
         * Button Click event for Shuffle button
         * Enables shuffle flag to true
         * */
        btnShuffle.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (!bound) return;
                Log.v(LOG_TAG, "btnShuffle");
                isShuffle = backgroundAudioService.btnShuffle();
                if (!isShuffle) {
                    Toast.makeText(getApplicationContext(), "Shuffle is OFF", Toast.LENGTH_SHORT).show();
                    btnShuffle.setImageResource(R.drawable.btn_shuffle);
                } else {
                    Toast.makeText(getApplicationContext(), "Shuffle is ON", Toast.LENGTH_SHORT).show();
                    // make shuffle to false
                    btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
                    btnRepeat.setImageResource(R.drawable.btn_repeat);
                }
            }
        });

        /**
         * Button Click event for Play list click event
         * Launches list activity which displays list of songs
         * */
        btnPlaylist.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.v(LOG_TAG, "btnPlaylist");
                Intent i = new Intent(getApplicationContext(), PlayListActivity.class);
                startActivityForResult(i, 100);
            }
        });

    }

    /**
     * Receiving song index from playlist view
     * and play the song
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100) {
            currentSongIndex = data.getExtras().getInt("songIndex");
            // play selected song
            Log.v(LOG_TAG, "btnPlaylistOnActivityResult.CurrentSongIndex = " + currentSongIndex);
            backgroundAudioService.playSong(currentSongIndex);
        }

    }


    /**
     * Update timer on seekbar
     */
    public void updateProgressBar() {
        backgroundAudioService.updateProgressBar();
    }


    /**
     *
     * */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }

    /**
     * When user starts moving the progress handler
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // remove message Handler from updating progress bar
        backgroundAudioService.mHandlerRemoveCallbacks();
    }

    /**
     * When user stops moving the progress hanlder
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        backgroundAudioService.mHandlerRemoveCallbacks();
        int currentPosition = seekbarHelper.progressToTimer(seekBar.getProgress(), (int) totalDuration);
        Log.v(LOG_TAG, "currentPosition = " + currentPosition);
        // forward or backward to certain seconds
        backgroundAudioService.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(br);
    }


    private void restoreState () {
        preferences = getPreferences(MODE_PRIVATE);
        // restore saved state


        currentSongIndex = preferences.getInt("currentSongIndex",0);
        songTitle = preferences.getString("songTitle","Song Title");
        isShuffle = preferences.getBoolean("isShuffle", false);

        songTitleLabel.setText(songTitle);
        isRepeat = preferences.getBoolean("isRepeat", false);
        //restore  shuffle/repeat image state
        if (!isShuffle) {
            btnShuffle.setImageResource(R.drawable.btn_shuffle);
        } else {
            btnShuffle.setImageResource(R.drawable.btn_shuffle_focused);
            btnRepeat.setImageResource(R.drawable.btn_repeat);
        }

        if (!isRepeat) {
            btnRepeat.setImageResource(R.drawable.btn_repeat);
        } else {
            btnRepeat.setImageResource(R.drawable.btn_repeat_focused);
            btnShuffle.setImageResource(R.drawable.btn_shuffle);
        }


    }
}

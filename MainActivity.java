package eu.tomaka.radiored7;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import eu.tomaka.radiored7.helpers.ShoutcastParser;
import eu.tomaka.radiored7.helpers.ModifiedWebView;

public class MainActivity extends Activity implements View.OnClickListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, OnBufferingUpdateListener {

    private MediaPlayer mp = new MediaPlayer();
    private Button buttonPlay;
    private Button buttonStop;
    private Button buttonSendGreetings;
    private Spinner spinnerRaioChannel;
    private HashMap<String, String> channelSCAddressHash = new HashMap<String, String>();
    private String chosenChannel = "Główny";
    private TextView streamGenre;
    private TextView streamTitle;
    private String genereLabel;
    private String titleLabel;
    private Integer playerState = 0; // 0 = Stopped; 1 = Playing; 2 = Paused (currently not used)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinnerRaioChannel = (Spinner) findViewById(R.id.spinnerRaioChannel);
        buttonPlay = (Button) findViewById(R.id.buttonPlay);
        buttonStop = (Button) findViewById(R.id.buttonStop);
        buttonSendGreetings = (Button) findViewById(R.id.buttonSendGreetings);
        streamGenre = (TextView) findViewById(R.id.textViewStreamGenere);
        streamTitle = (TextView) findViewById(R.id.textViewStreamTitle);
        buttonPlay.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        buttonSendGreetings.setOnClickListener(this);
        setButtonsState();
        setupShoutcastAddresses();
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.d("Red7", "update");
            }
        });
        //new thread to update stream title and genre every 30 seconds
        Thread t = new Thread() {

            public void run() {
                try {
                    while (!isInterrupted()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("Red7", "update genre");
                                reloadSCInfo();
                            }
                        });
                        Thread.sleep(30000);
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        t.start();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onClick(View v) {
        if (v == buttonPlay) {
            startRadio();
        } else if (v == buttonStop) {
            stopRadio();
        } else if (v == buttonSendGreetings) {
            Log.d("Red7", "tutaj");

            startGreetingsWebView();
        }
    }

    private void initializeMP(){
        mp = new MediaPlayer();
        try {
            mp.setDataSource(channelSCAddressHash.get(chosenChannel));
        } catch (IOException e) {
            Log.e("Red7", "Unable initialize player with address " + channelSCAddressHash.get(chosenChannel));
            e.printStackTrace();
        }
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                Log.e("Red7", "tuuuuuuu " );

            }
        });
    }


    private void startRadio() {
        Log.d("Red7", "Radio started");

        initializeMP();
        mp.prepareAsync();
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            public void onPrepared(MediaPlayer mp) {
                MainActivity.this.mp.start();
            }
        });
        playerState = 1;
        Log.d("PlayerState", playerState.toString());
        mp.setOnBufferingUpdateListener(this);
        setButtonsState();
    }

    private void stopRadio() {
        Log.d("Red7", "Stop pressed");

        try {
            if (mp.isPlaying()) {
                mp.stop();
                mp.release();
            }
        }
        catch(Exception e) {
            Log.d("Red7", "Media player has alredy been released - do nothing" );
        }
        playerState = 0;
        Log.d("PlayerState", playerState.toString());
        setButtonsState();
    }

    private void setupShoutcastAddresses() {
        channelSCAddressHash.put("Główny", "http://sluchaj.radiors.pl:19182");
        channelSCAddressHash.put("Fly", "http://sluchaj.radiors.pl:19204");
        channelSCAddressHash.put("Disco-Polo", "http://sluchaj.radiors.pl:19206");
        List<String> channelList = new ArrayList(channelSCAddressHash.keySet());
        Collections.sort(channelList,Collections.reverseOrder());
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>
                (this, R.layout.custom_spinner ,channelList);
        spinnerRaioChannel.setAdapter(dataAdapter);
        addListenerOnSpinnerItemSelection();

    }
    public void addListenerOnSpinnerItemSelection(){

        spinnerRaioChannel = (Spinner) findViewById(R.id.spinnerRaioChannel);
        spinnerRaioChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                Toast.makeText(parent.getContext(), "Wybrano kanał: " + parent.getItemAtPosition(position).toString() + "playerState = " + playerState.toString(), Toast.LENGTH_SHORT).show();
                chosenChannel = parent.getItemAtPosition(position).toString();
                //stop radio if playing
                stopRadio();
                initializeMP();
                reloadSCInfo();
                setButtonsState();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void reloadSCInfo(){

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {

                try {

                    genereLabel = new ShoutcastParser().getStreamGenere(channelSCAddressHash.get(chosenChannel));
                    titleLabel = new ShoutcastParser().getStreamTitle(channelSCAddressHash.get(chosenChannel));
                    Log.d("KtoGRA?", genereLabel);

                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            thread.join();
            streamGenre.setText(getString(R.string.streamGenere)+" "+genereLabel);
            streamTitle.setText(getString(R.string.streamTitle)+" "+titleLabel);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void setButtonsState(){

        if ( playerState == 0 ) {
            buttonPlay.setEnabled(true);
            buttonStop.setEnabled(false);
        } else if ( playerState == 1 ) {
            buttonPlay.setEnabled(false);
            buttonStop.setEnabled(true);
        }

    }

    private void startGreetingsWebView(){
        Log.d("Red7", "tutaj");

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.sendGreetings) + " " + getString(R.string.channel) + " " + chosenChannel);

        ModifiedWebView wv = new ModifiedWebView(this);
        WebSettings webSettings = wv.getSettings();
        webSettings.setJavaScriptEnabled(true);

        if(chosenChannel.equals("Główny")) {
            wv.loadUrl("http://panel.radiors.pl/pozdro.php?kanal=glowny");
        } else if (chosenChannel.equals("Fly")) {
            wv.loadUrl("http://panel.radiors.pl/pozdro.php?kanal=fly");
        } else if (chosenChannel.equals("Disco-Polo")) {
            wv.loadUrl("http://panel.radiors.pl/pozdro.php?kanal=disco");
        }
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);

                return true;
            }
        });

        alert.setView(wv);
        alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.show();
    }


    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.d("Red7", "onBufferingUpdate" + percent);

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d("Red7", "onCompletion" );

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.d("Red7", "onError" );

        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d("Red7", "onPrepared" );

    }
}

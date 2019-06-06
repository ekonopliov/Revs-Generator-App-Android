package com.example.myapplication;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Bluetooth components
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;
    String deviceName = "HC-05";
    boolean BTConnected = false;

    //Mediaplayers
    MediaPlayer mediaPlayerExhaustFirst;
    MediaPlayer mediaPlayerExhaustSecond;
    MediaPlayer mediaPlayerRev;

    //Switches for mediaplayers
    Switch switchExhaustSystemFirst;
    Switch switchExhaustSystemSecond;

    //Volume controls
    AudioManager audioManager;
    SeekBar seekBarLoudness;

    //System status variable
    TextView systemStatus;

    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) && device.getName().equals(deviceName)) {
                //if device with name of deviceName has connected
                Toast.makeText(MainActivity.this, "Controller connected", Toast.LENGTH_SHORT).show();
                systemStatus.setText("Connected");
                BTConnected = true;
            }

            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action) && device.getName().equals(deviceName)) {
                //if device with name of deviceName is about to disconnect
                Toast.makeText(MainActivity.this, "Controller disconnection request", Toast.LENGTH_SHORT).show();
                systemStatus.setText("Disonnected");
                BTConnected = false;
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action) && device.getName().equals(deviceName)) {
                //if device with name of deviceName has disconnected
                Toast.makeText(MainActivity.this, "Controller disconnected", Toast.LENGTH_SHORT).show();
                systemStatus.setText("Disconnected");
                BTConnected = false;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //default

        //BT connection/disconnection trackers
        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(BTReceiver, filter1);
        this.registerReceiver(BTReceiver, filter2);
        this.registerReceiver(BTReceiver, filter3);

        //Connect button
        final Button btnConnect = findViewById(R.id.btnConnect);

        //Layout
        final ConstraintLayout layout = findViewById(R.id.background);

        //System satus
        systemStatus = findViewById(R.id.textViewSystemStatus);

        //Exhaust switches
        switchExhaustSystemFirst = findViewById(R.id.switchExhaustSystemFirst);
        switchExhaustSystemSecond = findViewById(R.id.switchExhaustSystemSecond);

        //Volume controls setup
        seekBarLoudness = findViewById(R.id.seekBarBottom);
        seekBarLoudness.setVisibility(View.VISIBLE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initControls();

        //Design stuff
        btnConnect.setBackgroundColor(Color.argb( 255,100,0,0));  //red
        layout.setBackgroundColor(Color.argb( 255,16,16,16));  //dark gray
        seekBarLoudness.getProgressDrawable().setColorFilter(Color.argb( 255,100,0,0), PorterDuff.Mode.SRC_IN); // red
        seekBarLoudness.getThumb().setColorFilter(Color.argb( 255,100,0,0), PorterDuff.Mode.SRC_IN); //red

        //Connect button listener
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                try {
                    if (findBT()) {
                        if (!BTConnected) {
                            openBT();
                        } else
                            Toast.makeText(MainActivity.this, "Already connected to controller!", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(MainActivity.this, "Connection to controller failed", Toast.LENGTH_SHORT).show();
                }
            }

        });

        //First exhaust switch listener
        switchExhaustSystemFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    if(switchExhaustSystemFirst.isChecked()) {
                        switchExhaustSystemFirst.getTrackDrawable().setColorFilter(Color.argb(255, 100, 0, 0), PorterDuff.Mode.SRC_IN);
                        mediaPlayerExhaustFirst = MediaPlayer.create(MainActivity.this, R.raw.soundecho);
                        mediaPlayerExhaustFirst.start();
                        mediaPlayerExhaustFirst.setLooping(true);
                    }
                    else {
                        if(mediaPlayerExhaustFirst != null){
                            mediaPlayerExhaustFirst.reset();
                        }
                        switchExhaustSystemFirst.getTrackDrawable().setColorFilter(Color.argb( 255,60,60,60), PorterDuff.Mode.SRC_IN);
                    }

                }

        });

        //Second exhaust switch listener
        switchExhaustSystemSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(switchExhaustSystemSecond.isChecked()) {
                    switchExhaustSystemSecond.getTrackDrawable().setColorFilter(Color.argb(255, 100, 0, 0), PorterDuff.Mode.SRC_IN);
                    mediaPlayerExhaustSecond = MediaPlayer.create(MainActivity.this, R.raw.echosound2);
                    mediaPlayerExhaustSecond.start();
                    mediaPlayerExhaustSecond.setLooping(true);
                }
                else {
                    if(mediaPlayerExhaustSecond != null){
                        mediaPlayerExhaustSecond.reset();
                    }
                    switchExhaustSystemSecond.getTrackDrawable().setColorFilter(Color.argb( 255,60,60,60), PorterDuff.Mode.SRC_IN);
                }
            }

        });

    }

    private void initControls() {
        try {
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            seekBarLoudness.setMax(audioManager
                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 15, 0);
            seekBarLoudness.setProgress(audioManager
                    .getStreamVolume(AudioManager.STREAM_MUSIC));

            seekBarLoudness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0) {
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            progress, 0);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //returns int gandom in range min-max
    private int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    //Analyzes received data
    void analyzeReceivedData(String data) {

        switch (data){
            case "1":
                switchExhaustSystemFirst.getTrackDrawable().setColorFilter(Color.argb(255, 100, 0, 0), PorterDuff.Mode.SRC_IN);
                switchExhaustSystemFirst.setChecked(true);
                mediaPlayerExhaustFirst = MediaPlayer.create(this, R.raw.soundecho);
                mediaPlayerExhaustFirst.start();
                mediaPlayerExhaustFirst.setLooping(true);
                break;

            case "2":
                if(mediaPlayerExhaustFirst!=null) {
                    switchExhaustSystemFirst.getTrackDrawable().setColorFilter(Color.argb(255, 60, 60, 60), PorterDuff.Mode.SRC_IN);
                    switchExhaustSystemFirst.setChecked(false);
                    mediaPlayerExhaustFirst.reset();
                }
                break;

            case "3":
                switchExhaustSystemSecond.getTrackDrawable().setColorFilter(Color.argb(255, 100, 0, 0), PorterDuff.Mode.SRC_IN);
                switchExhaustSystemSecond.setChecked(true);
                mediaPlayerExhaustSecond = MediaPlayer.create(this, R.raw.echosound2);
                mediaPlayerExhaustSecond.start();
                mediaPlayerExhaustSecond.setLooping(true);
                break;
            case "4":
                if(mediaPlayerExhaustSecond!=null) {
                    switchExhaustSystemSecond.getTrackDrawable().setColorFilter(Color.argb( 255,60,60,60), PorterDuff.Mode.SRC_IN);
                    switchExhaustSystemSecond.setChecked(false);
                    mediaPlayerExhaustSecond.reset();
                }
                break;

            case "5":
                if(data.equals("5")) {
                    mediaPlayerRev = MediaPlayer.create(this, R.raw.soundfire);
                    mediaPlayerRev.start();
                }
                break;
        }
        Toast.makeText(this,"Received data: " + data ,Toast.LENGTH_SHORT).show();
    }


    //finds and initializes BT device with name deviceName
    boolean findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            Toast.makeText(this,"No bluetooth adapter available",Toast.LENGTH_SHORT).show();
            return false;
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        boolean found = false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals(deviceName))
                {
                    mmDevice = device;
                    found = true;
                    break;
                }
            }
        }
        if(found) {
            //Toast.makeText(this, "Bluetooth device found", Toast.LENGTH_SHORT).show();
            return true;
        }
        else {
            Toast.makeText(this, "No device with name '" + deviceName + "' was found.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    //Opens connection with device and starts listening for data
    boolean openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        //Toast.makeText(this,"Bluetooth connected" ,Toast.LENGTH_SHORT).show();
        return true;
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            analyzeReceivedData(data);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    //Sends data
    void sendData(String msg) throws IOException
    {
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        Toast.makeText(this,"Sent",Toast.LENGTH_SHORT).show();
    }

    //Closes BT connection
    void closeBT() throws IOException
    {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        Toast.makeText(this,"Bluetooth disconnected",Toast.LENGTH_SHORT).show();
    }
}


package com.masakiauto.loaderremocon;

import static com.masakiauto.loaderremocon.R.color.back_ground;
import static com.masakiauto.loaderremocon.R.drawable.button_custom;
import static com.masakiauto.loaderremocon.R.drawable.button_deny;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    static final String TAG = "BTTEST1";

    private static final int LOADER_NONE = 10;
    private static final int LOADER_UP = 11;
    private static final int LOADER_DOWN = 12;
    private static final int WINCH_NONE = 20;
    private static final int WINCH_UP = 21;
    private static final int WINCH_DOWN = 22;

    //全部OFF
    private static final int R_STATUS_00 = 1;
    //荷台UP ウィンチOFF
    private static final int R_STATUS_U0 = 2;
    //荷台DOWN ウィンチOFF
    private static final int R_STATUS_D0 = 3;
    //荷台OFF ウィンチUP
    private static final int R_STATUS_0U = 4;
    //荷台UP ウィンチUP
    private static final int R_STATUS_UU = 5;
    //荷台DOWN ウィンチUP
    private static final int R_STATUS_DU = 6;
    //荷台OFF ウィンチDOWN
    private static final int R_STATUS_0D = 7;
    //荷台UP ウィンチDOWN
    private static final int R_STATUS_UD = 8;
    //荷台DOWN ウィンチDOWN
    private static final int R_STATUS_DD = 9;

    private static int mLoaderStatus = LOADER_NONE;
    private static int mWinchStatus = WINCH_NONE;
    private static int mLoaderRemote = LOADER_NONE;
    private static int mWinchRemote = WINCH_NONE;

    TextView btStatusTextView;
    TextView messageTextView;
    TextView tvCommand;
    Button btUp, btDown, btRollUp, btRollDown;

    BluetoothAdapter bluetoothAdapter;
    BTClientThread btClientThread;

    final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String s;

            switch (msg.what) {
                case Constants.MESSAGE_BT:
                    s = (String) msg.obj;
                    if (s != null) {
                        btStatusTextView.setText(s);
                    }
                    break;

                case Constants.MESSAGE_TEMP:
                    s = (String) msg.obj;
                    if (s != null) {
                        messageTextView.setText(s);
                    }
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find Views
        btStatusTextView = findViewById(R.id.btStatusTextView);
        messageTextView = findViewById(R.id.messageTextView);
        tvCommand = findViewById(R.id.tvCommand);
        btUp = findViewById(R.id.btUp);
        btDown = findViewById(R.id.btDown);
        btRollUp = findViewById(R.id.btRollUp);
        btRollDown = findViewById(R.id.btRollDown);

        findViewById(R.id.btUp).setOnTouchListener(this);
        findViewById(R.id.btDown).setOnTouchListener(this);
        findViewById(R.id.btRollUp).setOnTouchListener(this);
        findViewById(R.id.btRollDown).setOnTouchListener(this);
        /*
         findViewById(R.id.btUp).setOnClickListener(this);
         findViewById(R.id.btDown).setOnClickListener(this);
         findViewById(R.id.btRollUp).setOnClickListener(this);
         findViewById(R.id.btRollDown).setOnClickListener(this);
        */

        if(savedInstanceState != null){
            String temp = savedInstanceState.getString(Constants.BT_MESS);
            messageTextView.setText(temp);
        }

        // Initialize Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if( bluetoothAdapter == null ){
            Log.d(TAG, "This device doesn't support Bluetooth.");
        }

    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        btClientThread = new BTClientThread();
        btClientThread.start();
    }

    @Override
    protected void onPause(){
        Log.d(TAG, "onPause");
        super.onPause();
        if(btClientThread != null){
            btClientThread.interrupt();
            btClientThread = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        outState.putString(Constants.BT_MESS, messageTextView.getText().toString());
    }


    public class BTClientThread extends Thread {
        InputStream inputStream;
        OutputStream outputStream;
        BluetoothSocket bluetoothSocket;
        BluetoothDevice bluetoothDevice;
        private static final int COMMAND_0 = 48; //ASCII code "0"
        private static final int COMMAND_1 = 49; //ASCII code "1"
        private static final int COMMAND_2 = 50; //ASCII code "2"
        private static final int COMMAND_3 = 51; //ASCII code "3"
        private static final int COMMAND_4 = 52; //ASCII code "4"
        private static final int COMMAND_5 = 53; //ASCII code "5"
        private static final int COMMAND_6 = 54; //ASCII code "6"
        private static final int COMMAND_7 = 55; //ASCII code "7"
        private static final int COMMAND_8 = 56; //ASCII code "8"


        public void run(){
            byte[] incomingBuff = new byte[64];
            int i = 0;

            bluetoothDevice = null;
            Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
            for(BluetoothDevice device : devices){
                if(device.getName().equals(Constants.BT_DEVICE)){
                    Log.d(TAG, "Detect device.");
                    bluetoothDevice = device;
                    break;
                }
            }
            if(bluetoothDevice == null){
                Log.d(TAG, "No Device found.");
                return;
            }

            try {

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(Constants.BT_UUID);
                while (true) {
                    if (Thread.interrupted()) {
                        Log.d(TAG, "Thread interrupted before connect.");
                        break;
                    }
                    try {
                        bluetoothSocket.connect();
                        handler.obtainMessage(
                                Constants.MESSAGE_BT,
                                "CONNECTED " + bluetoothDevice.getName())
                                .sendToTarget();
                        inputStream = bluetoothSocket.getInputStream();
                        outputStream = bluetoothSocket.getOutputStream();

                        while (true) {
                            if (Thread.interrupted()) {
                                Log.d(TAG, "Thread interrupted before I/O.");
                                break;
                            }

                            // Send Command
                            if(mLoaderStatus != mLoaderRemote) {
                                sendCommand(mLoaderStatus);
                                mLoaderRemote = mLoaderStatus;
                            } else if(mWinchStatus != mWinchRemote){
                                sendCommand(mWinchStatus);
                                mWinchRemote = mWinchStatus;
                            } else {
                                sendCommand(0);
                            }

                            // Read Response
                            int incomingBytes = inputStream.read(incomingBuff);
                            byte[] buff = new byte[incomingBytes];
                            System.arraycopy(incomingBuff, 0, buff, 0,
                                    incomingBytes);
                            String s = new String(buff, StandardCharsets.UTF_8);
                            String t = new String(incomingBuff, StandardCharsets.UTF_8);

                            // Show Result to UI
                            handler.obtainMessage(
                                    Constants.MESSAGE_TEMP,
                                    t
                            ).sendToTarget();

                            // Update again in a few seconds
                            Thread.sleep(20);
                        }
                    } catch (IOException e) {
                        // connect will throw IOException immediately
                        // when it's disconnected.
                        Log.d(TAG, e.getMessage());
                    }

                    handler.obtainMessage(
                            Constants.MESSAGE_BT,
                            "DISCONNECTED")
                            .sendToTarget();

                    // Re-try after 3 sec
                    Thread.sleep(3 * 1000);

                }
            } catch (InterruptedException | IOException e){
                e.printStackTrace();
            }

            if(bluetoothSocket != null){
                try{
                    bluetoothSocket.close();
                } catch (IOException e){
                    bluetoothSocket = null;
                }
            }

            handler.obtainMessage(
                    Constants.MESSAGE_BT,
                    "DISCONNECTED - Exit BTClientThread"
            ).sendToTarget();
        }

        private void sendCommand(int com) throws IOException {
            try{
                switch(com){
                    case LOADER_NONE:
                        Log.d(TAG, "56");
                        outputStream.write(COMMAND_5);
                        outputStream.write(COMMAND_6);
                        break;
                    case LOADER_UP:
                        Log.d(TAG, "16");
                        outputStream.write(COMMAND_1);
                        outputStream.write(COMMAND_6);
                        break;
                    case LOADER_DOWN:
                        Log.d(TAG, "52");
                        outputStream.write(COMMAND_5);
                        outputStream.write(COMMAND_2);
                        break;
                    case WINCH_NONE:
                        Log.d(TAG, "78");
                        outputStream.write(COMMAND_7);
                        outputStream.write(COMMAND_8);
                        break;
                    case WINCH_UP:
                        Log.d(TAG, "38");
                        outputStream.write(COMMAND_3);
                        outputStream.write(COMMAND_8);
                        break;
                    case WINCH_DOWN:
                        Log.d(TAG, "74");
                        outputStream.write(COMMAND_7);
                        outputStream.write(COMMAND_4);
                        break;
                    default:
                        Log.d(TAG, "0");
                        outputStream.write(COMMAND_0);
                }
            }catch(IOException e){
                Log.d(TAG, e.getMessage());
            }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int act = event.getActionMasked();
        if(act == MotionEvent.ACTION_DOWN
                || act == MotionEvent.ACTION_UP){
            if(v.isClickable()) {
                buttonUpDown((Button) v, act);
            }
        } else {
            //Log.d(TAG, event.toString());
        }
        return false;
    }
    private void buttonUpDown(Button b, int act){
        //Log.d(TAG, "buttonUpDown");
        switch(b.getId()){
            case R.id.btUp:
                //Log.d(TAG, "btUp");
                if(act == MotionEvent.ACTION_DOWN){
                    btDown.setEnabled(false);
                    if(mLoaderRemote != LOADER_UP){
                        mLoaderStatus = LOADER_UP;
                    }
                    setBtCommand(R_STATUS_U0);
                } else if(act == MotionEvent.ACTION_UP){
                    btDown.setEnabled(true);
                    if(mLoaderRemote != LOADER_NONE){
                        mLoaderStatus = LOADER_NONE;
                    }
                    setBtCommand(R_STATUS_00);
                }
                break;
            case R.id.btDown:
                //Log.d(TAG, "btDown");
                if(act == MotionEvent.ACTION_DOWN){
                    btUp.setEnabled(false);
                    if(mLoaderRemote != LOADER_DOWN){
                        mLoaderStatus = LOADER_DOWN;
                    }
                    setBtCommand(R_STATUS_D0);
                } else if (act == MotionEvent.ACTION_UP){
                    btUp.setEnabled(true);
                    if(mLoaderRemote != LOADER_NONE){
                        mLoaderStatus = LOADER_NONE;
                    }
                    setBtCommand(R_STATUS_00);
                }
                break;
            case R.id.btRollUp:
                //Log.d(TAG, "btRollUp");
                if(act == MotionEvent.ACTION_DOWN){
                    btRollDown.setEnabled(false);
                    if(mWinchRemote != WINCH_UP){
                        mWinchStatus = WINCH_UP;
                    }
                    setBtCommand(R_STATUS_0U);
                } else if (act == MotionEvent.ACTION_UP){
                    btRollDown.setEnabled(true);
                    if(mWinchRemote != WINCH_NONE){
                        mWinchStatus = WINCH_NONE;
                    }
                    setBtCommand(R_STATUS_00);
                }
                break;
            case R.id.btRollDown:
                if(act == MotionEvent.ACTION_DOWN){
                    btRollUp.setEnabled(false);
                    if(mWinchRemote != WINCH_DOWN){
                        mWinchStatus = WINCH_DOWN;
                    }
                    setBtCommand(R_STATUS_0D);
                } else if (act == MotionEvent.ACTION_UP){
                    btRollUp.setEnabled(true);
                    if(mWinchRemote != WINCH_NONE){
                        mWinchStatus = WINCH_NONE;
                    }
                    setBtCommand(R_STATUS_00);
                }
                break;
        }
    }

    private void setBtCommand(int i){
        switch(i){
            case R_STATUS_00:
                tvCommand.setText("5678");
                break;
            case R_STATUS_U0:
                tvCommand.setText("1678");
                break;
            case R_STATUS_D0:
                tvCommand.setText("5278");
                break;
            case R_STATUS_0U:
                tvCommand.setText("5638");
                break;
            case R_STATUS_UU:
                tvCommand.setText("1638");
                break;
            case R_STATUS_DU:
                tvCommand.setText("5238");
                break;
            case R_STATUS_0D:
                tvCommand.setText("5674");
                break;
            case R_STATUS_UD:
                tvCommand.setText("1674");
                break;
            case R_STATUS_DD:
                tvCommand.setText("5274");
                break;
        }
    }

}
package com.example.elegoosmartcarcontroller;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends Activity {
    private TextView angleTextView;
    private TextView powerTextView;
    private Toolbar bluetooth;

    // TODO: Replace with the actual MAC address
    public final static String MODULE_MAC = "98:D3:34:90:6F:A1";
    public final static int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    BluetoothAdapter bta;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    ConnectedThread btt = null;
    TextView response;
    public Handler mHandler;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetooth = findViewById(R.id.toolbar);
        angleTextView = findViewById(R.id.angleTextViewLab);
        powerTextView = findViewById(R.id.powerTextViewLab);
        JoystickView joystick = findViewById(R.id.joystickView);

        // Initialize bluetooth, angle, and power
        bluetooth.setTitle(getResources().getString(R.string.bluetooth_connection_lab, "Disconnected"));
        angleTextView.setText(getResources().getString(R.string.angle_lab, 0));
        powerTextView.setText(getResources().getString(R.string.power_lab, 0));

        // Event listener that always returns the variation of the angle in degrees, motion power in percentage and direction of movement
        joystick.setOnJoystickMoveListener((angle, power, direction) -> {
            // Set text for the joystick values
            angleTextView.setText(getResources().getString(R.string.angle_lab, angle));
            powerTextView.setText(getResources().getString(R.string.power_lab, power));

            Log.i("[BLUETOOTH]", "Attempting to send data");
            if (mmSocket != null && mmSocket.isConnected() && btt != null) { //if we have connection to the bluetoothmodule
                // TODO: Send the data here??
                String sendText = String.valueOf(angle);
                btt.write(sendText.getBytes());
                Log.i("[BLUETOOTH]", "Data sent");
            } else {
                Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_LONG).show();
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);

        Log.i("[BLUETOOTH]", "Creating listeners");
        response = findViewById(R.id.response);

        bta = BluetoothAdapter.getDefaultAdapter();

        //if bluetooth is not enabled then create Intent for user to turn it on
        if (!bta.isEnabled()) {
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
        } else {
            initiateBluetoothProcess();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT) {
            initiateBluetoothProcess();
        }
    }

    public void initiateBluetoothProcess() {
        if (bta.isEnabled()) {
            //attempt to connect to bluetooth module
            BluetoothSocket tmp;
            mmDevice = bta.getRemoteDevice(MODULE_MAC);

            //create socket
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID);
                mmSocket = tmp;
                mmSocket.connect();
                Log.i("[BLUETOOTH]","Connected to: " + mmDevice.getName());
                bluetooth = findViewById(R.id.toolbar);
                bluetooth.setTitle(getResources().getString(R.string.bluetooth_connection_lab, "Connected"));
            } catch(IOException e) {
                try{mmSocket.close();}catch(IOException c){return;}
            }

            Log.i("[BLUETOOTH]", "Creating handler");
            mHandler = new Handler(Looper.getMainLooper()){
                @Override
                public void handleMessage(Message msg) {
                    //super.handleMessage(msg);
                    if(msg.what == ConnectedThread.RESPONSE_MESSAGE){
                        String txt = (String)msg.obj;
                        if(response.getText().toString().length() >= 30){
                            response.setText("");
                            response.append(txt);
                        }else{
                            response.append("\n" + txt);
                        }
                    }
                }
            };

            Log.i("[BLUETOOTH]", "Creating and running Thread");
            btt = new ConnectedThread(mmSocket,mHandler);
            btt.start();
        }
    }
}
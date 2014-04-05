/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.BluetoothArduino;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.BluetoothArduino.R;

/**
 * This is the main Activity that displays the controls
 */
public class BluetoothArduino extends Activity implements OnClickListener {
    // Debugging
    private static final String TAG = "BluetoothArduino";
    private static final boolean D = true;

    // Message types sent from the BluetoothArduinoService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothArduinoService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mStatus;
    private ListView mActionsSendedView;
    private Button mButtonConncect;
    private Button mButtonAction1;
    private Button mButtonAction2;
    private Button mButtonAction3;    
    private Button mButtonAction4;
    private Button mButtonAction5;
    private Button mButtonAction6;    
    
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the service
    private BluetoothArduinoService mArduinoService = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        // Set up the custom title
        mStatus = (TextView) findViewById(R.id.status_text);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mArduinoService == null) setupControls();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mArduinoService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mArduinoService.getState() == BluetoothArduinoService.STATE_NONE) {
              // Start the Bluetooth Arduino services
              mArduinoService.start();
            }
        }
    }

    private void setupControls() {
        Log.d(TAG, "setupControls()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mActionsSendedView = (ListView) findViewById(R.id.in);
        mActionsSendedView.setAdapter(mConversationArrayAdapter);

        // Initialize the buttons with a listener that for click events        
        mButtonConncect = (Button) findViewById(R.id.button_connect_device);
        mButtonConncect.setOnClickListener(this);
        
        mButtonAction1 = (Button) findViewById(R.id.button_action_1);
        mButtonAction1.setOnClickListener(this);
        
        mButtonAction2 = (Button) findViewById(R.id.button_action_2);
        mButtonAction2.setOnClickListener(this);
        
        mButtonAction3 = (Button) findViewById(R.id.button_action_3);
        mButtonAction3.setOnClickListener(this);
        
        mButtonAction4 = (Button) findViewById(R.id.button_action_4);
        mButtonAction4.setOnClickListener(this);
        
        mButtonAction5 = (Button) findViewById(R.id.button_action_5);
        mButtonAction5.setOnClickListener(this);
        
        mButtonAction6 = (Button) findViewById(R.id.button_action_6);
        mButtonAction6.setOnClickListener(this);        

        // Initialize the BluetoothArduinoService to perform bluetooth connections
        mArduinoService = new BluetoothArduinoService(this, mHandler);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth Arduino services
        if (mArduinoService != null) mArduinoService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mArduinoService.getState() != BluetoothArduinoService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothArduinoService to write
            byte[] send = message.getBytes();
            mArduinoService.write(send);
        }
    }

    // The Handler that gets information back from the BluetoothArduinoService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothArduinoService.STATE_CONNECTED:
                    mStatus.setText(R.string.title_connected_to);
                    mStatus.append(mConnectedDeviceName);
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothArduinoService.STATE_CONNECTING:
                    mStatus.setText(R.string.title_connecting);
                    break;
                case BluetoothArduinoService.STATE_LISTEN:
                case BluetoothArduinoService.STATE_NONE:
                    mStatus.setText(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                mConversationArrayAdapter.add("Action:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mArduinoService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupControls();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.button_connect_device:
	            // Launch the DeviceListActivity to see devices and do scan
	            Intent serverIntent = new Intent(this, DeviceListActivity.class);
	            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);				
	            break;
		
			case R.id.button_action_1: 
				sendMessage("1"); 
				break;

			case R.id.button_action_2: 
				sendMessage("2"); 
				break;
				
			case R.id.button_action_3: 
				sendMessage("3"); 
				break;
				
			case R.id.button_action_4: 
				sendMessage("4"); 
				break;

			case R.id.button_action_5: 
				sendMessage("5"); 
				break;
				
			case R.id.button_action_6: 
				sendMessage("6"); 
				break;				
		}		
	}
}
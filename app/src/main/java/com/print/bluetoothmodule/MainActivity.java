package com.print.bluetoothmodule;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements
            View.OnClickListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 1;
    public BluetoothAdapter mBluetoothAdapter;
    public BluetoothDevice mDevice;

    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ListView pairedListView;

    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBluetoothAdapter();
        initWindow();

    }

    private void initBluetoothAdapter() {
        // 初始化配置
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check the device support the Bluetooth Device or not
        // Device is not support Bluetooth
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "Device does not support Bluetooth");
            return;
        }

        // Device support Bluetooth
        // popup the notice to open the Bluetooth
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivity(intent);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    private void initWindow() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Button btn_scan = findViewById(R.id.btn_scan);

        btn_scan.setOnClickListener(v -> {
            //蓝牙发现代码
            discoverDevices();
            v.setEnabled(false);
        });

        initPairedListView();
    }

    private void initPairedListView() {
        pairedListView = findViewById(R.id.paired_deivces);
        mPairedDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_item);

        pairedListView.setAdapter(mPairedDevicesArrayAdapter);


        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }
    }
    private void discoverDevices() {
        if (mBluetoothAdapter == null) {
            initBluetoothAdapter();
        }

        if (mBluetoothAdapter.startDiscovery()) {
            Log.d(TAG,"启动蓝牙扫描设备···");
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_scan:
                initBluetoothAdapter();
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != Activity.RESULT_OK) {
               new ConnectThread(mDevice).start();
                Log.d(TAG,"打开蓝牙成功!");
            }

            if (resultCode == RESULT_CANCELED) {
                Log.d(TAG, "放弃打开蓝牙！");
            }

//            IntentFilter filter = new IntentFilter();
//            filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//            mContext.registerReceiver(mReceiver, filter);

        } else {
            Log.d(TAG, "打开蓝牙异常！");
            return;
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                mDevice = device;
                mPairedDevicesArrayAdapter.add(device.getName() + "\n"
                        + device.getAddress());
                mPairedDevicesArrayAdapter.notifyDataSetChanged();
            }
        }
    };


    @Override
    protected void onStop() {
        super.onStop();
        this.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 注册广播监听
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
    private class ConnectThread extends Thread {
        private final BluetoothSocket mSocket;
        private final BluetoothDevice mDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSocket = tmp;
        }

        @Override
        public void run() {
            //建立连接前取消发现设备
            mBluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();
            } catch (IOException connectException) {
                try {
                    mSocket.close();
                } catch (IOException closeException) {

                }
                return;
            }
            //doSomething(mSocket);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
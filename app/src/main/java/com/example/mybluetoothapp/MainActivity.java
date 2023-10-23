package com.example.mybluetoothapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    public String TAG = "-----------";
    private String macAddress = "";
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Button BtnConnect, BtnOn, BtnOff, BtnReadInfo;
    TextView ConnectionText;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice;
    BluetoothSocket bluetoothSocket;
    OutputStream outputStream;
    InputStream inputStream;
    int REQUEST_ENABLE_BT;
    Set<BluetoothDevice> pairedDevices;

    private byte[] Buffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ConnectionText = (TextView) findViewById(R.id.dispositivos);

        BtnConnect = (Button) findViewById(R.id.btnConnect);
        BtnOn = (Button) findViewById(R.id.BtnOn);
        BtnOff = (Button) findViewById(R.id.BtnOff);
        BtnReadInfo = (Button) findViewById(R.id.BtnReadInfo);

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        REQUEST_ENABLE_BT = 1;

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Dispositivo não suporta bluetooth!!", Toast.LENGTH_LONG).show();
        } else {
            turnOnMethods();
            turnOffMethods();
            connectMethods();
            readBluetoothDeviceInfo();
        }
    }

    private void connectMethods(){
        BtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED) {
                    if (Build.VERSION.SDK_INT > 31) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 100);
                    }
                }

                pairedDevices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals("HC-06")) {
                        macAddress = device.getAddress();
                        Toast.makeText(getApplicationContext(),"O dispositivo foi encontrado no historico",Toast.LENGTH_SHORT).show();
                        break;
                    }
                }

                bluetoothDevice = bluetoothAdapter.getRemoteDevice(macAddress);

                new Thread(new Runnable() {
                    @Override
                    public void run() {


                        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_DENIED) {
                            if (Build.VERSION.SDK_INT > 31) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, 100);
                            }
                        }

                        try {
                            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                            bluetoothSocket.connect();
                            outputStream = bluetoothSocket.getOutputStream();
                            inputStream = bluetoothSocket.getInputStream();
                            Log.d("Message", "Connected to HC-06");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Bluetooth successfully connected", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } catch (IOException e) {
                            Log.d("Message", "Turn on bluetooth and restart the app");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Turn on bluetooth and restart the app", Toast.LENGTH_SHORT).show();
                                }
                            });
                            throw new RuntimeException(e);
                        }
                    }
                }).start();
            }
        });
    }

    private void turnOnMethods() {
        BtnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled()) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(getApplicationContext(), "Não possui permissão necessaria", Toast.LENGTH_LONG).show();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "Iniciando Bluetooth :)", Toast.LENGTH_LONG).show();
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    } catch (Error err) {
                        System.out.println(err);
                    }
                }
            }
        });
    }

    private void turnOffMethods() {
        BtnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothAdapter.isEnabled()) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(), "Não tem permissão para desligar", Toast.LENGTH_LONG).show();
                        return;
                    }
                    bluetoothAdapter.disable();
                    Toast.makeText(getApplicationContext(), "Desligando Bluetooth :)", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void showConnectionsMethods() {
        BtnReadInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(), "Habilite o Bluetooth", Toast.LENGTH_LONG).show();
                    return;
                }
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                pairedDevices = bluetoothAdapter.getBondedDevices();
                ConnectionText.setText("");
                for (BluetoothDevice device : pairedDevices) {
                    String deviceName = device.getName();
                    String deviceHardwareAddress = device.getAddress(); // MAC address
                    String deviceInfo = deviceName + " - " + deviceHardwareAddress;
                    String currentText = ConnectionText.getText().toString();
                    String textToShow = currentText + '\n' + deviceInfo;
                    ConnectionText.setText(textToShow);
                }
            }
        });

    }

    private void readBluetoothDeviceInfo(){
        BtnReadInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Buffer = new byte[1024];
                        int numBytes;
                        try {
                            numBytes = inputStream.read(Buffer);
                            String data = new String(Buffer, 0, numBytes);
                            ConnectionText.setText(data);
                        } catch (Exception e) {
                            Log.d(TAG, "Input stream was disconnected", e);
                        }
                    }
                }).start();
            }
        });
    }
}
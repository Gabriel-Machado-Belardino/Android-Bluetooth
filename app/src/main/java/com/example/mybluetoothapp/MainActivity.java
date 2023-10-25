package com.example.mybluetoothapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Objects;
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
    Gson gson;

    private byte[] Buffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gson = new Gson();

        ConnectionText = (TextView) findViewById(R.id.dispositivos);

        BtnConnect = (Button) findViewById(R.id.btnConnect);
        BtnOn = (Button) findViewById(R.id.BtnOn);
        BtnOff = (Button) findViewById(R.id.BtnOff);
        BtnReadInfo = (Button) findViewById(R.id.BtnReadInfo);

        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
        REQUEST_ENABLE_BT = 100;

        requestBluetoothPermission();

        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Dispositivo não suporta bluetooth!!", Toast.LENGTH_LONG).show();
        } else {
            tryToConnect();
            turnOnMethods();
            turnOffMethods();
            connectMethods();
            readBluetoothDeviceInfo();
        }
    }

    private void requestPermission(String permission){
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, REQUEST_ENABLE_BT);
        }
    }

    private void requestBluetoothPermission(){
        requestPermission(Manifest.permission.BLUETOOTH);
        requestPermission(Manifest.permission.BLUETOOTH_CONNECT);
    }

    ActivityResultLauncher<Intent> enableBtLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Bluetooth ativado com sucesso
                }
            }
    );

    private void turnOnBluetooth(){
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtLauncher.launch(enableBtIntent);
            }
        }
    }

    private void connectMethods() {
        BtnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryToConnect();
            }
        });
    }

    private void tryToConnect(){
        if(bluetoothSocket != null && bluetoothSocket.isConnected()){
            Toast.makeText(getApplicationContext(),"O dispostivo ja esta conectado",Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBluetoothPermission();
            return;
        }

        if(!bluetoothAdapter.isEnabled()){
            turnOnBluetooth();
            return;

        }

        pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getName().equals("HC-06")) {
                macAddress = device.getAddress();
                Toast.makeText(getApplicationContext(), "O dispositivo foi encontrado no historico", Toast.LENGTH_SHORT).show();
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

                int counter = 0;
                do{
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                        bluetoothSocket.connect();
                    }catch(IOException err){
                        err.printStackTrace();
                    }
                    counter++;
                }while (!bluetoothSocket.isConnected() && counter < 0);
                if(!bluetoothSocket.isConnected()){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Não foi possivel se conectar ao dispositivo, tente novamente :(", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                try {
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

    private void turnOnMethods() {
        BtnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothAdapter.isEnabled()) {
                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            requestBluetoothPermission();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "Iniciando Bluetooth :)", Toast.LENGTH_LONG).show();
                        turnOnBluetooth();
                    } catch (Error err) {
                        System.out.println(err);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth já esta ativo :)", Toast.LENGTH_LONG).show();
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

    private void readBluetoothDeviceInfo() {
        BtnReadInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                                    String line = reader.readLine();
                                    Log.e(TAG,line);
                                    while (line != null) {
                                        // Processar a linha de dados
                                        if (!line.isEmpty()) {
                                            try {
                                                DadosClass dadosClass = gson.fromJson(line, DadosClass.class);
                                                int potenciomerterValue = dadosClass.getValue();
                                                ConnectionText.setText(dadosClass.toString());
                                            }catch (JsonSyntaxException err){
                                                Log.e(TAG,"Recebeu uma informação que não é JSON",err);
                                            }
                                        }
                                        line = reader.readLine();
                                    }
                            } catch (IOException e) {
                                Log.d(TAG, "Input stream was disconnected", e);
                            }
                        }
                    }).start();
            }
        });
    }


    public class DadosClass{
        @SerializedName("sensor")
        private String sensor;
        @SerializedName("value")
        private int value;

        public DadosClass(String sensor, int value){
            this.setValue(value);
            this.setSensor(sensor);
        }

        public DadosClass(){}

        @NonNull
        @Override
        public String toString() {
            return "Sensor : " + this.getSensor() + "; Valor : " + (this.getValue() * 4) ;
        }

        public void setSensor(String sensor){
            this.sensor = sensor;
        }
        public String getSensor(){
            return this.sensor;
        }
        public void setValue(int value){
            this.value = value;
        }
        public int getValue(){
            return this.value;
        }
    }
}


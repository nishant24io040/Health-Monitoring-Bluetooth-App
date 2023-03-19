package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.aflak.bluetooth.Bluetooth;
import me.aflak.bluetooth.interfaces.BluetoothCallback;
import me.aflak.bluetooth.interfaces.DeviceCallback;
import me.aflak.bluetooth.interfaces.DiscoveryCallback;

public class MainActivity extends AppCompatActivity {

    Bluetooth bluetooth;
    BluetoothAdapter btadapter;
    SwitchCompat btON;
    Adapter adapter;
    RecyclerView rcview;
    List<BluetoothDevice> deviceslist = new ArrayList<>();
    Set<BluetoothDevice> pairedDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        btadapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = btadapter.getBondedDevices();


        bluetooth = new Bluetooth(this);
        bluetooth.setBluetoothCallback(bluetoothCallback);
        btON = findViewById(R.id.switch1);
        bluetooth.onStart();


        if (bluetooth.isEnabled()){
            deviceslist.addAll(pairedDevices);
//            deviceslist = bluetooth.getPairedDevices();
            rcview = findViewById(R.id.rcview);
            rcview.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            adapter = new Adapter(MainActivity.this,deviceslist,btadapter);
            rcview.setAdapter(adapter);
            btON.setChecked(true);
        }

        btON.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if(bluetooth.isEnabled()){
                } else {
                    bluetooth.enable();
                }
            } else {
                bluetooth.disable();
            }
        });



    }

    private final BluetoothCallback bluetoothCallback = new BluetoothCallback() {
        @Override public void onBluetoothTurningOn() {}
        @Override public void onBluetoothTurningOff() {}
        @Override public void onBluetoothOff() {
            deviceslist.clear();
            rcview = findViewById(R.id.rcview);
            rcview.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            adapter = new Adapter(MainActivity.this,deviceslist,btadapter);
            rcview.setAdapter(adapter);
            Toast.makeText(MainActivity.this, "Bluetooth is Off", Toast.LENGTH_SHORT).show();
            btON.setChecked(false);
        }
        @Override public void onUserDeniedActivation() {}

        @SuppressLint("MissingPermission")
        @Override
        public void onBluetoothOn() {
//            deviceslist = bluetooth.getPairedDevices();
//            BluetoothDevice de;
            pairedDevices = btadapter.getBondedDevices();
            deviceslist.addAll(pairedDevices);
            ArrayList<BluetoothDevice> dlist= new ArrayList<>();
//            de=deviceslist.get(12);
//            dlist.add(de);
            rcview = findViewById(R.id.rcview);
            rcview.setLayoutManager(new LinearLayoutManager(MainActivity.this));
            adapter = new Adapter(MainActivity.this,deviceslist,btadapter);
            rcview.setAdapter(adapter);
            Toast.makeText(MainActivity.this, "Bluetooth is On", Toast.LENGTH_SHORT).show();
            btON.setChecked(true);
        }
    };


}
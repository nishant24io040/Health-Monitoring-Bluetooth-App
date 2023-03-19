package com.example.myapplication;

import static com.example.myapplication.Adapter.mUUID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DataActivity extends AppCompatActivity {
    final String TAG = "MY_APP_DEBUG_TAG";
    TextView tv1,tv2,tv3,pulse,temp;
    Button btn,btnsend;
    EditText etv;
    ;
    BluetoothDevice device;
    int counter = 0;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;
    private static final String APP_NAME = "My_Application";

    String state;

    List<BluetoothDevice> deviceslist = new ArrayList<>();
    Set<BluetoothDevice> pairedDevices;
    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket btSocket = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        if (ContextCompat.checkSelfPermission(DataActivity.this,
                Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                ActivityCompat.requestPermissions(DataActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                return;
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = bluetoothAdapter.getBondedDevices();
        deviceslist.addAll(pairedDevices);

        Bundle bundle = getIntent().getExtras();
        int i = bundle.getInt("position");
        String s=String.valueOf(i);
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        device = deviceslist.get(i);

        tv1 = findViewById(R.id.tv1);
        tv2 = findViewById(R.id.tv);
        tv3 = findViewById(R.id.oxygen);
        pulse = findViewById(R.id.pulse);
        temp = findViewById(R.id.temp);
        btn = findViewById(R.id.btn);
        tv1.setText(device.getName());
        etv = findViewById(R.id.etv);

        btnsend =findViewById(R.id.btnsend);
        try {
            btSocket = device.createRfcommSocketToServiceRecord(mUUID);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        btn.setOnClickListener(v -> {
            final Receive[] r = new Receive[1];
            ServerClass serverClass = new ServerClass();
            serverClass.start();
                do {
                    try {

                        btSocket.connect();
                        if (btSocket.isConnected()){
                            tv2.setText("Connected");
                            Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
                            r[0] = new Receive(btSocket);
                            r[0].start();
                            btn.setVisibility(View.GONE);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }counter++;
                } while (!btSocket.isConnected() && counter==3);

                if (!btSocket.isConnected()){
                    Toast.makeText(this, "unable to connect", Toast.LENGTH_SHORT).show();
                }
        });
        btnsend.setOnClickListener(v -> {
            try {
                btSocket.getOutputStream().write(etv.getText().toString().getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });



    }
    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            if (ContextCompat.checkSelfPermission(DataActivity.this,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions((Activity) DataActivity.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return;
                }
            }
            if (ContextCompat.checkSelfPermission(DataActivity.this,
                    Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions((Activity) DataActivity.this, new String[]{Manifest.permission.BLUETOOTH}, 2);
                    return;
                }
            }
            try {
                serverSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,mUUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run()
        {
            BluetoothSocket socket=null;

            while (socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);

                    socket=serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);



                    break;
                }
            }
        }
    }

    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            switch (msg.what)
            {
                case STATE_LISTENING:
                    state="Listening";
                    break;
                case STATE_CONNECTING:
                    state="Connection";
                    break;
                case STATE_CONNECTED:
                    state="Connected";
                    break;
                case STATE_CONNECTION_FAILED:
                    state="Connection failed";
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuff= (byte[]) msg.obj;
                    String tempMsg=new String(readBuff,0,msg.arg1);
                    String[] splited = tempMsg.split("k");
                    if (splited.length>2) {
                        splited[0] = "Temperature " + splited[0] + "F";
                        splited[1] = "Pulse Rate " + splited[1];
                        splited[2] = "SPO2 " + splited[2]  ;
                        temp.setText(splited[0]);
                        pulse.setText(splited[1]);
                        tv3.setText(splited[2]);
                    }
                    break;
            }
            return true;
        }
    });

    private class Receive extends Thread{
        private final InputStream inputStream1;
        public Receive(BluetoothSocket bluetoothSocket){
            try {
                inputStream1 = bluetoothSocket.getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void run() {
            byte[] buffer=new byte[1024];
            int bytes;

            while (true)
            {
                try {
                    bytes=inputStream1.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}

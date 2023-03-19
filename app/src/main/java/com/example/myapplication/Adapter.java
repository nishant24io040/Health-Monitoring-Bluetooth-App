package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Adapter extends RecyclerView.Adapter<Adapter.Holder> {


    Context context;
    List<BluetoothDevice> list;
    BluetoothAdapter bluetoothAdapter;
    static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    int counter = 0;
    String masgrecieved;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSAGE_RECEIVED=5;
    private static final String APP_NAME = "My_Application";
    String state;
    BluetoothSocket btSocket = null;
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
                    masgrecieved = tempMsg;

                    break;
            }
            return true;
        }
    });

    public Adapter(Context context, List<BluetoothDevice> list, BluetoothAdapter btAdapter) {
        this.context = context;
        this.list = list;
        this.bluetoothAdapter = btAdapter;
    }

    @NonNull
    @Override
    public Adapter.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_item, parent, false);
        return new Holder(view);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BluetoothDevice d = list.get(position);
        final Receive[] r = new Receive[1];


//        Toast.makeText(context, d.getBluetoothClass().toString() , Toast.LENGTH_SHORT).show();
        holder.Bluetoothdevice.setText(d.getName());
        ServerClass serverClass = new ServerClass();
        holder.state.setText("Paired");
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context,DataActivity.class);
            intent.putExtra("position",position);
            context.startActivity(intent);
//            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
//            dialog.setTitle(d.getName());
//            dialog.setMessage(d.getAddress());
//            dialog.setPositiveButton("Connect with arduino", (dialogInterface, i) -> {
//                serverClass.start();
//                if (Objects.equals(state, "Connection failed")){
//                    Toast.makeText(context, "trying clientclass", Toast.LENGTH_SHORT).show();
//                }
//                do {
//                    try {
//                        btSocket = d.createRfcommSocketToServiceRecord(mUUID);
//                        btSocket.connect();
//                        if (btSocket.isConnected()){
//                            Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
//                            holder.state.setText("Connected");
//                            r[0] = new Receive(btSocket);
//                            r[0].start();
//                        }
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }counter++;
//                } while (!btSocket.isConnected() && counter==3);
//
//                if (!btSocket.isConnected()){
//                    Toast.makeText(context, "unable to connect", Toast.LENGTH_SHORT).show();
//                }
//
//            });
//
//            dialog.setNegativeButton("unpair", new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialogInterface, int i) {
//                    Toast.makeText(context, "unpaired", Toast.LENGTH_SHORT).show();
//                    unpairDevice(d);
//                }
//            });
//            dialog.show();
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }



    public static class Holder extends RecyclerView.ViewHolder{
        TextView Bluetoothdevice,state,rmasg;
        EditText sendmasg;
        ImageView btnsend;

        public Holder(@NonNull View itemView) {
            super(itemView);
            Bluetoothdevice = itemView.findViewById(R.id.device);
            state = itemView.findViewById(R.id.pair);

        }
    }



    private class ServerClass extends Thread {
        private BluetoothServerSocket serverSocket;

        public ServerClass(){
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 2);
                    return;
                }
            }
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                {
                    ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.BLUETOOTH}, 2);
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

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass()
                    .getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }


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


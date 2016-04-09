package edu.umich.vehicleapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;

import com.github.pires.obd.commands.SpeedCommand;
import com.github.pires.obd.commands.engine.RPMCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.enums.ObdProtocols;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String deviceAddress;

    private BluetoothSocket socket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createDialog();
    }

    protected void createDialog() {
        ArrayList deviceStrs = new ArrayList();
        final ArrayList devices = new ArrayList();
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        if (pairedDevices.size() > 0)
        {
            for (BluetoothDevice device : pairedDevices)
            {
                deviceStrs.add(device.getName() + "\n" + device.getAddress());
                devices.add(device.getAddress());
            }
        }

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.select_dialog_singlechoice,
                deviceStrs.toArray(new String[deviceStrs.size()]));

        alertDialog.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                deviceAddress = devices.get(position).toString();
                new ConnectDeviceTask().execute();
            }
        });

        alertDialog.setTitle("Choose Bluetooth device");
        alertDialog.show();
    }

    private class ConnectDeviceTask extends AsyncTask<Void, Integer, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

            BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

            try {
                socket = device.createInsecureRfcommSocketToServiceRecord(uuid);
                socket.connect();
            } catch (IOException exp) {
                Log.d("SPEED_INFO", exp.getMessage());
            }

            initConnection();
            getTestInfo();
            return null;
        }

        private void initConnection() {
            try {
                new EchoOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                new LineFeedOffCommand().run(socket.getInputStream(), socket.getOutputStream());
                new TimeoutCommand(125).run(socket.getInputStream(), socket.getOutputStream());
                new SelectProtocolCommand(ObdProtocols.AUTO).run(socket.getInputStream(), socket.getOutputStream());
            } catch (Exception e) {
                Log.v("SPEED_INFO", e.getMessage());
            }
        }

        private void getTestInfo() {
            RPMCommand rpmCommand = new RPMCommand();
            SpeedCommand speedCommand = new SpeedCommand();

            try {
                rpmCommand.run(socket.getInputStream(), socket.getOutputStream());
                speedCommand.run(socket.getInputStream(), socket.getOutputStream());
            } catch (Exception e) {
                Log.v("SPEED_INFO", e.getMessage());
            }
            Log.d("SPEED_INFO", speedCommand.getFormattedResult());
            Log.d("SPEED_INFO", rpmCommand.getFormattedResult());
        }
    }
}



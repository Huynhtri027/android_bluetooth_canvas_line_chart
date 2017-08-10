package com.example.administrator.canvas_draw_in_time;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FindBlueTooth extends AppCompatActivity {
    private BluetoothAdapter  mBluetoothAdapter;
    private List<BluetoothDevice> devices = new ArrayList<>();
    private List<String> devices_name = new ArrayList<>();
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_blue_tooth);
        listView = (ListView) findViewById(R.id.bt_list);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            //弹出对话框提示用户是后打开
            //Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivity(intent);
            //startActivityForResult(intent, REQUEST_ENABLE);
            //不做提示，强行打开
            mBluetoothAdapter.enable();
        }
        Set<BluetoothDevice> bonded_devices = mBluetoothAdapter.getBondedDevices();
        Iterator<BluetoothDevice> iter = bonded_devices.iterator();
        for(int i=0; i<bonded_devices.size(); i++) {
            BluetoothDevice device = (BluetoothDevice) iter.next();
            devices.add(device);
            devices_name.add(device.getName()+"\n"+device.toString());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 , devices_name);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice device = devices.get(position);
                Intent intent =new Intent(FindBlueTooth.this, MainActivity.class);
                //用Bundle携带数据
                Bundle bundle=new Bundle();
                //传递device
                bundle.putParcelable("device", device);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }




}

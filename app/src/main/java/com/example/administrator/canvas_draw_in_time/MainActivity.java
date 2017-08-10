package com.example.administrator.canvas_draw_in_time;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Bitmap bufferBitmap;
    Canvas bufferCanvas;
    SeekBar seekbar;
    private ImageView iv_canvas;
    int width = 300;
    int height = 300;
    List<Float> list_y = new ArrayList<>();
    Float max_y = 30.0f;
    int point_num = 20;
    int point_max = 100;
    BluetoothDevice device;
    Thread connect;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv_canvas = (ImageView) findViewById(R.id.iv_canvas);
        seekbar = (SeekBar) findViewById(R.id.seekBar);
        seekbar.setMax(point_max);
        seekbar.setProgress(point_num);
        seekbar.setOnSeekBarChangeListener(onListener);
        Toast.makeText(this, "Main start", Toast.LENGTH_SHORT).show();
        bufferBitmap = Bitmap.createBitmap(width, height,Bitmap.Config.ARGB_8888);//创建内存位图
        iv_canvas.setImageBitmap(bufferBitmap);
        bufferCanvas = new Canvas(bufferBitmap);//创建绘图画布
        drawOnBuffer();
        bufferCanvas.drawBitmap(bufferBitmap, 0, 0,new Paint());
        try{
            device = getIntent().getParcelableExtra("device");
        }catch (Exception e)
        {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
        connect = new ConnectThread(device, BluetoothAdapter.getDefaultAdapter());
        connect.start();

    }

    SeekBar.OnSeekBarChangeListener onListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            point_num = progress+1;
            drawOnBuffer();
            bufferCanvas.drawBitmap(bufferBitmap, 0, 0,new Paint());
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            Float result = msg.getData().getFloat("result");
            list_y.add(result);
            max_y = Math.max(Math.abs(result*1.1f), max_y);
            drawOnBuffer();
            bufferCanvas.drawBitmap(bufferBitmap, 0, 0,new Paint());
            iv_canvas.setImageBitmap(bufferBitmap);
            super.handleMessage(msg);
        }
    };
    final Handler errorhandler = new Handler() {
        public void handleMessage(Message msg) {
            String e = msg.getData().getString("e");
            Toast.makeText(MainActivity.this, e, Toast.LENGTH_SHORT).show();
            super.handleMessage(msg);
        }
    };

    private void drawOnBuffer() {
        Paint paint=new Paint();
        paint.setAntiAlias(true);
        bufferCanvas.drawColor(Color.WHITE);
        paint.setColor(Color.rgb(0,0,0));
        paint.setTextAlign(Paint.Align.CENTER);
        int right = list_y.size();
        int left = Math.max(right-point_num, 0);
        float[] pts = new float[4*(right-left)+4];
        try {
            for (int n = left; n < right; n++) {
                float y = list_y.get(n);
                float fy = y;
                fy = ( fy + max_y) / (max_y * 2) * height * 0.89f;
                y = (fy + 0.1f * height);
                float x = ((n - left) * width / point_num * 0.89f) + (0.1f * width);
                bufferCanvas.drawCircle((int) x, (int) (height - y), 2, paint);   //随机画点
                pts[4 * (n - left)] = x;
                pts[4 * (n - left) + 1] = (height - y);
                pts[4 * (n - left) + 2] = x;
                pts[4 * (n - left) + 3] = (height - y);
                Log.d("Circle", "第n个" + n);
            }
            if (4 * (right - left - 1) > 2)
                bufferCanvas.drawLines(pts, 2, 4 * (right - left - 1), paint);
        }catch (Exception e)
        {
            Log.e("error", "drawOnBuffer: "+ e.toString());
        }
        int x_gap_num = Math.max(1, point_num/6);
        //画竖线
        for(int n=left; n<=left+point_num; n++){
            double x =((n-left)*width/point_num*0.89) + (0.1*width);
            if((n-left)%x_gap_num == 0) {
                bufferCanvas.drawText("" + n, (int)x, height-(int) (0.05 * height), paint);
                paint.setAlpha(100);
                bufferCanvas.drawLine((int)x, height-(int) (0.1 * height), (int)x, (float)0.01*height, paint);
                paint.setAlpha(255);
            }
        }

        int y_line = 6;    //画10条横线
        Paint.FontMetrics font = paint.getFontMetrics();
        for(int n=0;n<=y_line;n++)
        {
            int y = (int)((float)n/(float)y_line*(0.89*height)) + (int)(0.1*height);
            int x = (int)(0.1*width);
            paint.setAlpha(100);
            bufferCanvas.drawLine(x, height-y, width, height-y, paint);
            paint.setAlpha(255);
            bufferCanvas.drawText(""+(int)((1.0f*n/y_line-0.5)*2.0f*max_y), x/2, height-(y+(int)(font.top-font.bottom)/2), paint);
        }
    }

    private class ConnectThread extends Thread {
        BluetoothAdapter mAdapter;
        BluetoothSocket mySocket;
        private String uuid = "00001101-0000-1000-8000-00805F9B34FB";
        private UUID MY_UUID = UUID.fromString(uuid);
        private ConnectThread(BluetoothDevice device, BluetoothAdapter mAdapter) {
            this.mAdapter = mAdapter;
            int sdk = Build.VERSION.SDK_INT;

            if (sdk >= 10) {
                try {
                    mySocket = device
                            .createInsecureRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                    MyToast(e);
                    e.printStackTrace();
                    System.out.println(e.toString());
                }
            } else {
                try {
                    mySocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    MyToast(e);
                    e.printStackTrace();
                    System.out.println(e.toString());
                }
            }
        }
        public void MyToast(Exception e)
        {
            Message m = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("e", e.toString());
            m.setData(bundle);
            errorhandler.sendMessage(m);
        }
        public void MySToast(String e)
        {
            Message m = new Message();
            Bundle bundle = new Bundle();
            bundle.putString("e", e);
            m.setData(bundle);
            errorhandler.sendMessage(m);
        }
        Thread connectBluetooth;
        OutputStream mmOutStream;
        public void run() {
            //
            //mAdapter.cancelDiscovery();
            try {
                mySocket.connect();
                MySToast("connect success");
                // 启动接收远程设备发送过来的数据
                connectBluetooth = new ReceiveData(mySocket, handler);
                connectBluetooth.start();
                //输出流
                MySToast("outStream");
                mmOutStream = mySocket.getOutputStream();
                mmOutStream.write(11111);
            } catch (IOException e) {
                MyToast(e);
                try {
                    mySocket.close();
                } catch (IOException ee) {
                    MyToast(ee);
                }
            }

        }


        public class ReceiveData extends Thread {
            // 变量 略过
            BluetoothSocket mmSocket;
            InputStream mmInStream;
            Handler handler;
            // 构造方法
            public ReceiveData(BluetoothSocket socket, Handler handler) {
                this.mmSocket = socket;
                this.handler = handler;
                InputStream tempIn = null;
                // 获取输入流
                try {
                    tempIn = socket.getInputStream();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    MyToast(e);
                }
                mmInStream = tempIn;
            }

            @Override
            public void run() {
                // 监听输入流
                byte bytes[] = new byte[1024];
                while (true) {
                    try {
                        mmInStream.read(bytes);
                        String num = new String(bytes,"utf-8");
                        MySToast(num);
                        MySToast("Try convert to float");
                        Float result = Float.valueOf(num);
                        Message m = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putFloat("result", result);
                        m.setData(bundle);
                        handler.sendMessage(m);
                        MySToast("send float finish");
                         // 此处处理数据……
                    } catch (Exception e) {
                        MyToast(e);
                        try {
                            if (mmInStream != null) {
                                mmInStream.close();
                            }
                            break;
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            MyToast(e1);
                        }
                    }
                    try {
                        Thread.sleep(50);// 延迟
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        MyToast(e);
                    }
                }
            }
        }
    }
}


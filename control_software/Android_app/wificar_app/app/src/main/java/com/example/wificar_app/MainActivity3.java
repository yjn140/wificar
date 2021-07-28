package com.example.wificar_app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity3 extends AppCompatActivity {

    TargetDetection mytergetor;

    Socket_client my_ConSocket=new Socket_client();
    packet_deal my_packetD=new packet_deal();
    static long time_ago=0;
    int S_leve=0,S_ang=0;
    int Z_leve=0,Z_angle=0,Z_plat;
    int last_leve=0,last_ang=0,last_Z_plat=0;
    int img_index;
    int tic_index=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.activity_main3);
        getSupportActionBar().hide();//隐藏标题栏
        MyRockerView  mRockerViewXY = (MyRockerView) findViewById(R.id.rockerXY_View);//8方向
        MyRockerView  mRockerViewZ = (MyRockerView) findViewById(R.id.rockerz_View);//8方向
        TextView mytextview1=(TextView)findViewById(R.id.textviewdis1);
        TextView mytextview2=(TextView)findViewById(R.id.textviewdis2);
        TextView mytextview3=(TextView)findViewById(R.id.textviewdis3);
        TextView mytextview4=(TextView)findViewById(R.id.textviewdis4);
        Vibrator vibrator = (Vibrator)this.getSystemService(this.VIBRATOR_SERVICE);
        LinearLayout linearlayout1=findViewById(R.id.linearlayout1);
        Button button_con=findViewById(R.id.button2);
        initOpenCV();//初始化opencv
        mytergetor=new TargetDetection(this);

        {//小车运动控制
            mRockerViewXY.setOnShakeListener(MyRockerView.DirectionMode.DIRECTION_8, new MyRockerView.OnShakeListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void direction(MyRockerView.Direction direction) {
                    String directionXY = "";

                    //vibrator.vibrate(1000);
                    if (direction == MyRockerView.Direction.DIRECTION_CENTER) {
                        directionXY = ("当前方向：中");
                    } else if (direction == MyRockerView.Direction.DIRECTION_DOWN) {
                        directionXY = ("当前方向：下");
                    } else if (direction == MyRockerView.Direction.DIRECTION_LEFT) {
                        directionXY = ("当前方向：左");
                    } else if (direction == MyRockerView.Direction.DIRECTION_UP) {
                        directionXY = ("当前方向：上");
                    } else if (direction == MyRockerView.Direction.DIRECTION_RIGHT) {
                        directionXY = ("当前方向：右");
                    } else if (direction == MyRockerView.Direction.DIRECTION_DOWN_LEFT) {
                        directionXY = ("当前方向：左下");
                    } else if (direction == MyRockerView.Direction.DIRECTION_DOWN_RIGHT) {
                        directionXY = ("当前方向：右下");
                    } else if (direction == MyRockerView.Direction.DIRECTION_UP_LEFT) {
                        directionXY = ("当前方向：左上");
                    } else if (direction == MyRockerView.Direction.DIRECTION_UP_RIGHT) {
                        directionXY = ("当前方向：右上");
                    }
                    mytextview1.setText(directionXY);
                }

                @Override
                public void onFinish() {

                }
            });
            //角度
            mRockerViewXY.setOnAngleChangeListener(new MyRockerView.OnAngleChangeListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void angle(double angle, int level) {
                    String angleXY = "";
                    double angle_turn = (270 - angle);
                    if (angle_turn > 180) angle_turn -= 360;
                    S_ang = (int) angle_turn;
                    angleXY = ("当前角度：" + S_ang);
                    mytextview2.setText(angleXY);
                }

                @Override
                public void onFinish() {
                }
            });
            //级别
            mRockerViewXY.setOnDistanceLevelListener(new MyRockerView.OnDistanceLevelListener() {
                @Override
                public void onDistanceLevel(int level) {
                    String levelXY = "";
                    S_leve = level;
                    if (level != 0) {
                        long[] patter = {5, level / 3, 5, level / 3};
                        vibrator.vibrate(patter, 0);
                    } else {
                        vibrator.cancel();
                    }
                    levelXY = ("当前距离级别：" + S_leve);
                    mytextview3.setText(levelXY);
                }
            });
        }//小车运动控制

        {//小车云台控制
            mRockerViewZ.setOnShakeListener(MyRockerView.DirectionMode.DIRECTION_2_VERTICAL, new MyRockerView.OnShakeListener() {
                @Override
                public void onStart() {
                }

                @Override
                public void direction(MyRockerView.Direction direction) {
                    String directionXY = "";

                    //vibrator.vibrate(1000);
                    if (direction == MyRockerView.Direction.DIRECTION_CENTER) {
                        directionXY = ("当前方向：中");
                    } else if (direction == MyRockerView.Direction.DIRECTION_DOWN) {
                        directionXY = ("当前方向：下");
                    } else if (direction == MyRockerView.Direction.DIRECTION_UP) {
                        directionXY = ("当前方向：上");
                    }
                    mytextview1.setText(directionXY);
                }

                @Override
                public void onFinish() {

                }
            });
            //角度
            mRockerViewZ.setOnAngleChangeListener(new MyRockerView.OnAngleChangeListener() {
                @Override
                public void onStart() {
                }
                @Override
                public void angle(double angle, int level) {
                    double angle_turn = (270 - angle);
                    if (angle_turn > 180) angle_turn -= 360;
                    mytextview4.setText(Z_plat_control((int) angle_turn,0,0));
                    if (Z_plat != 0) {
                        long[] patter = {5, Math.abs(Z_plat / 3), 5, Math.abs(Z_plat / 3)};
                        vibrator.vibrate(patter, 0);
                    } else {
                        vibrator.cancel();
                    }
                }
                @Override
                public void onFinish() {
                }
            });
            //级别
            mRockerViewZ.setOnDistanceLevelListener(new MyRockerView.OnDistanceLevelListener() {
                @Override
                public void onDistanceLevel(int level) {
                    mytextview4.setText(Z_plat_control(0,level,1));
                    if (Z_plat != 0) {
                        long[] patter = {5, Math.abs(Z_plat / 3), 5, Math.abs(Z_plat / 3)};
                        vibrator.vibrate(patter, 0);
                    } else {
                        vibrator.cancel();
                    }
                }
            });
        }//小车云台控制

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                    if(S_ang!=last_ang||S_leve!=last_leve) {//避免频繁发送
                        my_ConSocket.sendstart(my_packetD.CMD_packet((byte)0x01,S_ang,S_leve));
                        last_leve=S_leve;
                        last_ang=S_ang;
                    }
                    if(true||tic_index++%4==0) {//避免频繁发送
                        my_ConSocket.sendstart(my_packetD.CMD_packet((byte)0x02,last_Z_plat,-1));
                        last_Z_plat=Z_plat;
                    }
            }
        };
        timer.schedule(task,0,20);//开启定时器


        my_ConSocket.mMainHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        //receive_message.setText("收到字节数："+img_buf.length);
                        try {
                            //imageview1.setImageBitmap(bitmap);   //imgview显示图片
                             long now = SystemClock.uptimeMillis();
                             Bitmap mmp= mytergetor.threadTergetor_star(my_ConSocket.bitmap);
                             String time = "时间：" + (SystemClock.uptimeMillis() - now) + "ms";
                             if(true||mmp!=null) {
                                // String mmm="img="+img_index++;
                                // mytextview4.setText(mmm);
                                 BitmapDrawable bd = new BitmapDrawable(getResources(), mmp);
                                 linearlayout1.setBackground(bd);
                             }
                        }catch (Exception es) {
                            //Toast.makeText(MainActivity2.this, "连接socket服务器失败!", Toast.LENGTH_LONG).show();
                            es.printStackTrace();
                        }
                    break;
                    case 1:
                        if(my_ConSocket.is_connect)
                        {
                            button_con.setText("断开设备：已连接");
                            Toast.makeText(getApplicationContext(), "连接成功!", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            button_con.setText("连接设备：未连接");
                            Toast.makeText(getApplicationContext(), "连接失败！请重连！！！", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2:
                        if(!my_ConSocket.is_connect)
                        {
                            button_con.setText("连接设备：未连接");
                            Toast.makeText(getApplicationContext(), "意外断开服务端连接!", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        };//回调

    }
    static int angle_now,leve_now;
    private  String Z_plat_control(int angle,int leve,int type)
    {
        if(type==0)
        {
            Z_angle=angle;
        }
        else if(type==1)
        {
            Z_leve=leve;
        }
        Z_plat=(int)(Z_leve*Math.cos(Z_angle*Math.PI/180))/2;

        String  levelXY= ("云台：" + Z_plat);
        return  levelXY;
    }

    private void initOpenCV() {//opencv初始化
        boolean success = OpenCVLoader.initDebug();
        if (success){
            Toast.makeText(getApplication(), "OpenCV库加载成功！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplication(), "OpenCV库加载失败！！！", Toast.LENGTH_SHORT).show();
            finish();//结束
        }
    }

    public void button1_click(View mm)
    {
        Button button_con=findViewById(R.id.button2);
        if(my_ConSocket.is_connect)//若已连接则断开
        {
            my_ConSocket.disconnect();
            button_con.setText("连接设备：未连接");
        }
        Intent intent = new Intent(this, MainActivity.class);
        String text ="";
        intent.putExtra(Intent_key,text);
        startActivityForResult(intent,0);//此处的requestCode应与下面结果处理函中调用的requestCode一致
    }
    public void toggleButton_click(View mm)
    {
        ToggleButton ToggleButton1=(ToggleButton)findViewById(R.id.toggleButton);
        if(ToggleButton1.isChecked())
        {

            my_ConSocket.sendstart(my_packetD.CMD_packet((byte)0x03,255,0));
        }
        else
        {
            my_ConSocket.sendstart(my_packetD.CMD_packet((byte)0x03,0,0));
        }
    }

    public void button2_click(View mm)
    {
        Button button_con=findViewById(R.id.button2);
        if(!my_ConSocket.is_connect)//若已连接则断开
        {
            my_ConSocket.connect_socket(my_ConSocket.host_ip_inital,my_ConSocket.host_point_initial,0);
        }
        else
        {
            my_ConSocket.disconnect();
            button_con.setText("连接设备：未连接");
        }
    }

    public void button3_click(View mm)
    {
        finish();
    }

    //发送Intent对应字符串内容的key
    public  static  final String Intent_key="MESSAGE";
    //发送消息，启动secondActivity!
    public void sendMessage(View view){
        Intent intent = new Intent(this, MainActivity.class);
        String text ="";
        intent.putExtra(Intent_key,text);
        startActivityForResult(intent,0);//此处的requestCode应与下面结果处理函中调用的requestCode一致
    }


    //结果处理函数，当从secondActivity中返回时调用此函数
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK) {
            switch (requestCode) {
                case 0:
                    Bundle bundle = data.getExtras();
                    //String IP_point = null;
                    if (bundle != null)
                    {
                        String host_ip = bundle.getString("host_ip");
                        int host_point=bundle.getInt("host_point");
                        my_ConSocket.connect_socket(host_ip,host_point,0);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
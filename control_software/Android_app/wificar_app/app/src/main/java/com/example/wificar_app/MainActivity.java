package com.example.wificar_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    //实例化socket客户端类
   Socket_client my_ConSocket=new Socket_client();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(1024, 1024);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();//隐藏标题栏
    }

    public void socket_disORconnect(View button_now)
    {
        EditText edi_text1=(EditText)findViewById(R.id.edittext1);
        EditText edi_text2=(EditText)findViewById(R.id.edittext2);
        String host_ip=my_ConSocket.host_ip_inital;
        int host_point=my_ConSocket.host_point_initial;
        if(edi_text1.getText().length()!=0)
        {
            if(my_ConSocket.isCorrectIp(edi_text1.getText().toString()))//检查是否是IP地址
            {
                host_ip=edi_text1.getText().toString();
            }
            else
            {
                Looper.prepare();//在子线程中使用要加上这个
                Toast toast= Toast.makeText(getApplicationContext(), "IP地址不符合规范,请重新输入！！！", Toast.LENGTH_SHORT);
                //显示toast信息
                toast.setGravity(Gravity.TOP|Gravity.CENTER, -50, 100);
                toast.show();
                Looper.loop();
                return;
            }
        }
        if(edi_text2.getText().length()!=0)
        {
            host_point = Integer.parseInt(edi_text2.getText().toString());
        }

        Intent intent =getIntent();
        //这里使用bundle绷带来传输数据
        Bundle bundle =new Bundle();
        //传输的内容仍然是键值对的形式
        bundle.putString("host_ip",host_ip);//回发的消息,hello world from secondActivity!
        bundle.putInt("host_point",host_point);
        intent.putExtras(bundle);
        setResult(RESULT_OK,intent);
        finish();
    }

    public void button4_click(View mm) throws IOException
    {
        finish();
        Toast.makeText(MainActivity.this,"退出连接页面!", Toast.LENGTH_LONG).show();
    }
}
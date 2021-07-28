package com.example.wificar_app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Socket_client {
    /**
     * 主 变量
     */

    // 主线程Handler
    // 用于将从服务器获取的消息显示出来
    public Handler mMainHandler;
    // Socket变量
    private Socket socket;
    // Socket连接指示变量
    public boolean is_connect=false;
    private boolean is_connecting=false;
    // 线程池
    // 为了方便展示,此处直接采用线程池进行线程管理,而没有一个个开线程
    private ExecutorService mThreadPool;
    /**
     * 接收服务器消息 变量
     */
    // 输入流对象
    InputStream is;

    // 输入流读取器对象
    InputStreamReader isr;
    DataInputStream socket_in ;//= new DataInputStream(is);
    // 接收服务器发送过来的消息
    String response;
    //rec_img my_recimg;



    private byte[] img_buf;
    public byte[] CMD_buf;
    public Bitmap bitmap;
    /**
     * 发送消息到服务器 变量
     */
    // 输出流对象
    OutputStream outputStream;
    public String host_ip_inital="192.168.4.1";
    public int host_point_initial=10000;
    /**
     * 按钮 变量
     */

    // 连接 断开连接 发送数据到服务器 的按钮变量
    private Button btnConnect, btnDisconnect, btnSend;

    // 显示接收服务器消息 按钮
    private TextView Receive,receive_message;

    // 输入需要发送的消息 输入框
    private EditText mEdit;

    Socket_client()
    {
        // 初始化线程池
        mThreadPool = Executors.newCachedThreadPool();

        //socket=new Socket();
    }

    public void sendstart(byte[] send_buf) {
        // 利用线程池直接开启一个线程 & 执行该线程
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if(socket!=null&&socket.isConnected()) {
                        // 步骤1：从Socket 获得输出流对象OutputStream
                        // 该对象作用：发送数据
                        outputStream = socket.getOutputStream();

                        // 步骤2：写入需要发送的数据到输出流对象中
                        outputStream.write(send_buf);
                        // 特别注意：数据的结尾加上换行符才可让服务器端的readline()停止阻塞

                        // 步骤3：发送数据到服务端
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //断开socket连接
    public void disconnect()
    {
        try {
            is_connect=false;
            // 断开 客户端发送到服务器 的连接，即关闭输出流对象OutputStream
            if(outputStream!=null)
                outputStream.close();
            // 断开 服务器发送到客户端 的连接，即关闭输入流读取器对象BufferedReader
            if(socket_in!=null)
                socket_in.close();
            // 最终关闭整个Socket连接
            if(socket!=null) {
                socket.close();
            }
            // 判断客户端和服务器是否已经断开连接
            System.out.println(socket.isConnected());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    //连接socket
    public void connect_socket(String host_ip,int host_point,int type) {
        // 利用线程池直接开启一个线程 & 执行该线程
        if(is_connecting) return;//正在连接的话不允许重复连接
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    is_connecting=true;
                    socket=new Socket();
                    socket.connect(new InetSocketAddress(host_ip, host_point),5000);//连接
                    // 判断客户端和服务器是否连接成功
                    if(socket.isConnected())
                    {
                        is_connect=true;
                        threadRec_star();//建立接受线程
                        //通知主线程,连接成功
                        Message msg = Message.obtain();
                        msg.what = 1;
                        mMainHandler.sendMessage(msg);

                        mThreadPool.execute(new Runnable() {
                            @Override
                            public void run() {
                                while(is_connect)//检测socket的状态
                                {
                                    try {
                                        Thread.sleep(1000);
                                        /*try {
                                            ;
                                            //socket.sendUrgentData(0xff);
                                        } catch (IOException e) {
                                            disconnect();    //如果抛出了异常，那么就是断开连接了  跳出无限循环
                                            Message msg = Message.obtain();
                                            msg.what = 2;
                                            mMainHandler.sendMessage(msg);
                                        }*/
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });

                    }
                    else
                    {
                        is_connect=false;
                        Message msg = Message.obtain();
                        msg.what = 1;
                        mMainHandler.sendMessage(msg);
                    }
                    System.out.println("socket连接"+is_connect);
                    is_connecting=false;
                } catch (IOException e) {
                    e.printStackTrace();
                    is_connecting=false;//连接失败
                    is_connect=false;
                    disconnect();
                    Message msg = Message.obtain();
                    msg.what = 1;
                    mMainHandler.sendMessage(msg);
                }
            }
        });
    }



    private void threadRec_star(){
        // 利用线程池直接开启一个线程 & 执行该线程
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // 步骤1：创建输入流对象InputStream
                    is = socket.getInputStream();

                    // 步骤2：创建输入流读取器对象 并传入输入流对象
                    // 该对象作用：获取服务器返回的数据
                    isr = new InputStreamReader(is);
                    //br = new BufferedReader(isr);

                    socket_in = new DataInputStream(is);


                    int resplen=0;//每次接受到的数据长度
                    //Ring_bufffer my_buf=new  Ring_bufffer(1024*1024*10);//创建一个1M的缓冲区（数据队列）

                    int buffer_size=0;
                    //int frame_index=0;//完整的包接受到的位置
                    //int frame_size=0;//正在接受的包的长度;
                    while (is_connect)
                    {
                        // 步骤3：通过输入流读取器对象 接收服务器发送过来的数据
                        //resplen=br.read(respbyte,0,1024*10);//获取数据流
                            byte[] find_head=new byte[6];
                            byte[] clearbuf=new byte[1];//包头的5个字节
                            while(!is_head(find_head))//没找到头就一直找
                            {
                                resplen=socket_in.available();
                                if(resplen>=1) {
                                    find_head[0]=find_head[1];
                                    find_head[1]=find_head[2];
                                    find_head[2]=find_head[3];
                                    find_head[3]=find_head[4];
                                    find_head[4]=find_head[5];
                                    socket_in.read(clearbuf,0,1);//找头
                                    find_head[5]=clearbuf[0];
                                }
                            }
                            //运行到这里说明找到头了
                            byte[] pag_head=new byte[4];//包头的5个字节
                            while(resplen<4) {
                                resplen = socket_in.available();
                            }
                                socket_in.read(pag_head,0,4);//将daxiaoxinxi取出
                                int a = (Integer)(pag_head[3] & 0xff) << 24;//低在前
                                int b = (Integer)(pag_head[2] & 0xff) << 16;
                                int c = (Integer)(pag_head[1] & 0xff) << 8;
                                int d = (Integer)(pag_head[0] & 0xff);
                                int  packet_size= (a | b | c | d);
                                while(resplen<packet_size)//直到缓冲区里面缓冲到了整个图片数据
                                {
                                    resplen=socket_in.available();
                                }
                                    byte[] pag_img=new byte[packet_size];//
                                    socket_in.read(pag_img,0,packet_size);//
                                    img_buf=pag_img;
                                    bitmap = BitmapFactory.decodeByteArray(img_buf, 0, packet_size);  //生成位图
                                    // 步骤4:通知主线程,将接收的消息显示到界面
                                    Message msg = Message.obtain();
                                    msg.what = 0;
                                    mMainHandler.sendMessage(msg);

                    }
                } catch (IOException  e)
                {
                    e.printStackTrace();
                }

            }
        });
    }


    public boolean is_head(byte[] is_in)
    {
        if((Integer)(0xff&is_in[0])==0xf0&&
                (Integer)(0xff&is_in[1])==0xf0&&
                (Integer)(0xff&is_in[2])==0x00&&
                (Integer)(0xff&is_in[3])==0x00&&
                (Integer)(0xff&is_in[4])==0xff&&
                (Integer)(0xff&is_in[5])==0xff
        ) {
            return true;
        }
        else return false;
    }


    //char[]转byte[]
    public static byte[] getBytes(char[] chars,int len) {
        byte[] ret=new byte[len];
        for (int i=0;i<len;i++) {
            ret[i]=(byte) chars[i];
        }

        return ret;
    }

    //判断字符是否是IP
    public boolean isCorrectIp(String ipString) {
        //1、判断是否是7-15位之间（0.0.0.0-255.255.255.255.255）
        if (ipString.length()<7||ipString.length()>15) {
            return false;
        }
        //2、判断是否能以小数点分成四段
        String[] ipArray = ipString.split("\\.");
        if (ipArray.length != 4) {
            return false;
        }
        for (int i = 0; i < ipArray.length; i++) {
            //3、判断每段是否都是数字
            try {
                int number = Integer.parseInt(ipArray[i]);
                //4.判断每段数字是否都在0-255之间
                if (number <0||number>255) {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}

class packet_deal
{
    //封包处理
    byte[] CMD_packet(byte cmd,int val1,int val2)
    {
        byte[] sendbuf=new byte[13];
        sendbuf[0]=(byte)0xf0;
        sendbuf[1]=(byte)0x0c;//
        sendbuf[2]=cmd;//命令
        sendbuf[3] = (byte) ((val1) & 0xff);
        sendbuf[4] = (byte) ((val1 >> 8) & 0xff);
        sendbuf[5] = (byte) ((val1 >> 16) & 0xff);
        sendbuf[6] = (byte) ((val1 >> 24) & 0xff);
        sendbuf[7] = (byte) ((val2) & 0xff);
        sendbuf[8] = (byte) ((val2 >> 8) & 0xff);
        sendbuf[9] = (byte) ((val2 >> 16) & 0xff);
        sendbuf[10] = (byte) ((val2 >> 24) & 0xff);
        sendbuf[11]=(byte)0x0d;
        sendbuf[12]=(byte)0x0a;
        return sendbuf;
    }

    byte[] CMD_packet(byte cmd,int val1)
    {
        byte[] sendbuf=new byte[8];
        sendbuf[0]=(byte)0xf0;
        sendbuf[1]=(byte)0x08;//长度
        sendbuf[2]=cmd;//命令
        sendbuf[3] = (byte) ((val1) & 0xff);
        sendbuf[4] = (byte) ((val1 >> 8) & 0xff);
        sendbuf[5] = (byte) ((val1 >> 16) & 0xff);
        sendbuf[6] = (byte) ((val1 >> 24) & 0xff);
        sendbuf[7]=(byte)0x0a;
        return sendbuf;
    }

    byte[] CMD_packet(byte cmd,byte[] bytes)
    {
        if(bytes.length>250)//包不能大于250
        {
            return null;
        }
        byte[] sendbuf=new byte[bytes.length+3];
        sendbuf[0]=(byte)0xf0;
        sendbuf[1]=(byte)(bytes.length+3);//长度
        sendbuf[2]=cmd;//命令
        for (int i=1;i<bytes.length;i++)
        {
            sendbuf[i+2]=bytes[i];//命令
        }
        sendbuf[bytes.length+2]=(byte)0x0a;
        return sendbuf;
    }

}


//自建链表(环形缓冲器),自建的环形缓冲器
class Ring_bufffer
{
    public byte[] Rbuf;
    private int size;//总的缓冲区大小
    private int sta;
    private int fin;
    public int len;
    Ring_bufffer(int len)
    {
        size=len;
        Rbuf=new byte[size];
        sta=0;
        fin=0;
    }


    //返回索引的位置值
    public Integer RetAs(int as_index)
    {
        if(sta+as_index<size)
            return 0xff & Rbuf[sta+as_index];
        else return 0xff & Rbuf[sta+as_index-size];
    }

    //返回有效数据的个数
    public int Retcount()
    {
        if(fin>=sta)
            return len=fin-sta;
        else return len=fin+size-sta;
    }

    //移除前面的数的个数
    public void remove(int count)
    {
        sta+=count;
        if(sta>size) sta-=size;
    }

    //往这个缓冲器中增加数据
    public void addbytes(byte[] bytes)
    {
        int newfin= fin+bytes.length;
        if(newfin<size) //若在环形缓冲器范围内
        {
            System.arraycopy(bytes, 0,Rbuf, fin, bytes.length);
            fin=newfin;
        }
        else
        {
            System.arraycopy(bytes, 0,Rbuf, fin, size-fin);
            newfin-=size;
            System.arraycopy(bytes, 0,Rbuf, 0, newfin);
            fin=newfin;
        }
    }

    //获取前面count个的数据
    public byte[] retbytes(int count)
    {
        if(Retcount()<count) return null;//判断此个数是否大于有效数据个数
        byte[] ret_buf=new byte[count];
        int midval=sta+count;
        if(midval<size) //若在环形缓冲器范围内
        {
            System.arraycopy(Rbuf, sta,ret_buf, 0, count);
        }
        else
        {
            System.arraycopy(Rbuf, sta,ret_buf, 0, size-sta);
            midval-=size;
            System.arraycopy(Rbuf, 0,ret_buf, size-sta, midval);
        }
        return ret_buf;
    }

    public void reclear()
    {
        sta=0;
        fin=0;
    }

}

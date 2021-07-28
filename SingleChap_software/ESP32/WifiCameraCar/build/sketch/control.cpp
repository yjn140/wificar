#include "control.h"


#define PI 3.14

uint8_t Servo_left = 12;
uint8_t Servo_right = 13;
uint8_t Servo_plat = 14;
uint8_t led = 4;

uint8_t Sleft_ch = 4;
uint8_t Sright_ch = 5;
uint8_t Splat_ch = 3;
uint8_t led_ch = 7;

hw_timer_t * timer = NULL;
int pwm_SL=0,pwm_SR=0,pwm_SP=0;

//输入值为-50~50,+为正传，-为反转
void S_left_run(int val)
{
    pwm_SL=(val+75)/5;
    if(val<-50)val=-50;
    if(val>50)val=50;
    int dutyCycle=(int)(val +77);
    //Serial.print(" left_run:");
    //Serial.print(val);
    pwm_SL=dutyCycle;
    
}

//输入值为-50~50,+为正传，-为反转
void S_right_run(int val)
{
    val=-val;//两个舵机对称安装，所以旋转方向相反
    if(val<-50)val=-50;
    if(val>50)val=50;
    int dutyCycle=(int)(val +77);
    //Serial.print(" right_run:");
    //Serial.print(val);
    pwm_SR=dutyCycle;
    //ledcWrite(Sright_ch, dutyCycle);  // 输出PWM
}

//输入值为0~40,对应0~80度
void S_form_run(int angle)
{
    if(angle<0)angle=0;
    if(angle>40)angle=40;
    int dutyCycle=(int)(angle*1 +27);
    pwm_SP=dutyCycle;
}

//输入值为0~255.对应led等亮度
void Led_control(int pwm)
{
    if(pwm<0)pwm=0;
    if(pwm>255)pwm=255;
    ledcWrite(led_ch, pwm);  // 输出PWM
}


//5~25
static void IRAM_ATTR Timer0(){
        ledcWrite(Sright_ch, pwm_SR);  // 输出PWM
        ledcWrite(Sleft_ch, pwm_SL);  // 输出PWM
        ledcWrite(Splat_ch, pwm_SP);  // 输出PWM
}


//电机初始化
void servo_GPIO_init()
{
    //pinMode(Servo_left, OUTPUT);
    timer = timerBegin(0, 80, true);
    timerAttachInterrupt(timer, &Timer0, true);//回调ontimer
    timerAlarmWrite(timer, 20000, true);//20000us
    timerAlarmEnable(timer);//开始计时

    ledcAttachPin(Servo_right, Sright_ch); // assign RGB led pins to channels
    ledcAttachPin(Servo_left, Sleft_ch);
    ledcAttachPin(Servo_plat, Splat_ch);
    ledcAttachPin(led, led_ch);
    ledcSetup(Sleft_ch, 50, 10); // 50Hz PWM, 10-bit  1024分辨率
    ledcSetup(Sright_ch, 50, 10);
    ledcSetup(Splat_ch, 50, 10);
    ledcSetup(led_ch, 20000, 8);//20Khz pwm, 8bit 256分辨率
    S_left_run(0);
    S_right_run(0);
    //S_form_run(100);//各状态复位
    Led_control(0);
}

//输入direction -180~180, 0为正前方，+为左方；grade为方向上的量的等级0~10级
void car_run(int direction,int grade)
{
    float radian=(float)(direction*PI)/180.0; //角度转弧度
    float front_speed=grade*cos(radian);//向前的速度分量，若为负则后退
    float dir_speed=grade*sin(radian);//+为向左的速度分量，-为右的速度分量
    //设左轮速度为x，右轮为y
    //X+Y=5*front_speed  
    //x-Y=5*dir_speed
    //求出的x和y就是左轮和右轮的速度了
    //x=2.5*front_speed+2.5*dir_speed
    //y=2.5*front_speed-2.5*dir_speed
    int S_L_speed=(int)(2.5*(front_speed+dir_speed));
    int S_R_speed=(int)(2.5*(front_speed-dir_speed));
    //Serial.print(" S_L_speed:");
    //Serial.print(S_L_speed);
    //Serial.print(" S_R_speed:");
   // Serial.print(S_R_speed);
    S_right_run(S_L_speed);
    S_left_run(S_R_speed);
    //Serial.print("\r\n");
}

//type=0为绝对量操作，操作范围0~100度；type为1则为相对量操作，+为加角度，-为减角度
void flatform(int val,int type)
{
    static int angle=0;
    if(type)//相对操作
    {
        angle+=val;
        
    }
    else//绝对操作
    {
        angle=val;
    }
    if(angle>100) angle=100;
    else if(angle<0) angle=0;
    S_form_run(angle);
    //Serial.print(" angle:");
   // Serial.print(angle);
}

char buffer[100];


void CMD_ACT(int len)
{
    switch (buffer[1])
    {
    case 0x01/* 控制小车行走 */:
        /* code */
        if(len>=10)
        {
            int angle = (int) ((buffer[2] & 0xFF)   
            | ((buffer[3] & 0xFF)<<8)   
            | ((buffer[4] & 0xFF)<<16)   
            | ((buffer[5] & 0xFF)<<24)); 

            int leve = (int) ((buffer[6] & 0xFF)   
            | ((buffer[7] & 0xFF)<<8)   
            | ((buffer[8] & 0xFF)<<16)   
            | ((buffer[9] & 0xFF)<<24)); 

            //Serial.print(" angle:");
            //Serial.print(angle);
            //Serial.print(" leve:");
            //Serial.print(leve);
            //Serial.print("\r\n");
            car_run(angle,leve);
        }
        break;
    case 0x02/* 控制小车平台 */:
        /* code */
        if(len>=10)
        {
            int Z_relative = (int) ((buffer[2] & 0xFF)   
            | ((buffer[3] & 0xFF)<<8)   
            | ((buffer[4] & 0xFF)<<16)   
            | ((buffer[5] & 0xFF)<<24)); 

            int Z_absolute = (int) ((buffer[6] & 0xFF)   
            | ((buffer[7] & 0xFF)<<8)   
            | ((buffer[8] & 0xFF)<<16)   
            | ((buffer[9] & 0xFF)<<24)); 

            //Serial.print(" Z_relative:");
            //Serial.print(Z_relative);
            //Serial.print(" Z_absolute:");
            //Serial.print(Z_absolute);
            if(Z_absolute==-1)//-1代表启用相对操作
            {

                flatform(Z_relative/2,1);
            }
            else if(Z_absolute>=0)
            {
                flatform(Z_absolute,0);
            }
            //Serial.print("\r\n");
        }
        break;
    case 0x03/* LED */:
        /* code */
        if(len>=10)
        {
            int Z_Led = (int) ((buffer[2] & 0xFF)   
            | ((buffer[3] & 0xFF)<<8)   
            | ((buffer[4] & 0xFF)<<16)   
            | ((buffer[5] & 0xFF)<<24)); 

            Led_control(Z_Led);
        }
        break;
    default:
        break;
    }
}


//接收到的指令处理
void CMD_res_deal(char read_byte)
{
    static char last_byte=0x00;
    static int index=0;
    static bool sta=false;
    if(last_byte==0xf0&&read_byte==0x0c)
    {
        index=0;
        sta=true;
    }
    if(last_byte==0x0d&&read_byte==0x0a)
    {
        CMD_ACT(index);//执行命令
        index=0;
        sta=false;
    }
    if(sta)
    {
        buffer[index++]=read_byte;
    }
    last_byte=read_byte;
}
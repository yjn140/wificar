#ifndef __CONTROL_H__
#define __CONTROL_H__
#include <math.h>
#include <WiFi.h>

//电机初始化
void servo_GPIO_init();
//输入值为0~255.对应led等亮度
void Led_control(int pwm);
//输入direction -180~180, 0为正前方，+为左方；grade为方向上的量的等级0~10级
void car_run(int direction,int grade);
//type=0为绝对量操作，操作范围0~100度；type为1则为相对量操作，+为加角度，-为减角度
void flatform(int val,int type);
//接收到的指令处理
void CMD_res_deal(char read_byte);

void CMD_ACT(int len);
#endif
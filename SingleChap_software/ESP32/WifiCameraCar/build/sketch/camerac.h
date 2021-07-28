#ifndef __CAMERAC_H__
#define __CAMERAC_H__
#include <WiFi.h>
#include "esp_camera.h"
#define CAMERA_MODEL_AI_THINKER // 有PSRAM
#include "camera_pins.h"



uint8_t camera_init();//摄像头初始化
void fb_send(camera_fb_t* fb_in,WiFiClient* now_client);//将摄像头的数据缓冲帧处理并发送
void Taskwifi(void *pvParameters);//数据发送任务

#endif
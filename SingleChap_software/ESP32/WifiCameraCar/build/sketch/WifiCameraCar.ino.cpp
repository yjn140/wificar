#include <Arduino.h>
#line 1 "f:\\myWorkSpace\\0_my_project\\3_wifi_car\\wifi-smart-car\\SingleChap_software\\ESP32\\WifiCameraCar\\WifiCameraCar.ino"
#include <soc/soc.h>
#include <soc/rtc_cntl_reg.h>
#include "control.h"
#include "camerac.h"

#define MAX_SRV_CLIENTS 1 //最多允许连接一个wifi
const char *ssid = "WIFI_CAR"; //wifi名和密码
const char *password = "12345678";

SemaphoreHandle_t frameMutex;

uint8_t x = 1;
IPAddress local_IP(192, 168, x, 1); //自定义的IP地址
IPAddress gateway(192, 168, x, 1);//网关地址（AP模式下和local_IP一致）
IPAddress subnet(255, 255, 255, 0);//子网掩码
WiFiServer server(10000);                  //端口10000
WiFiClient serverClients[MAX_SRV_CLIENTS]; //目前仅允许连接一个wifi设备

camera_fb_t *fb = NULL;                    //摄像头用于缓冲图片的指针

//利用双核最高效的执行
void Taskcontrol(void *pvParameters); //1核执行控制

#line 24 "f:\\myWorkSpace\\0_my_project\\3_wifi_car\\wifi-smart-car\\SingleChap_software\\ESP32\\WifiCameraCar\\WifiCameraCar.ino"
void setup();
#line 67 "f:\\myWorkSpace\\0_my_project\\3_wifi_car\\wifi-smart-car\\SingleChap_software\\ESP32\\WifiCameraCar\\WifiCameraCar.ino"
void loop();
#line 73 "f:\\myWorkSpace\\0_my_project\\3_wifi_car\\wifi-smart-car\\SingleChap_software\\ESP32\\WifiCameraCar\\WifiCameraCar.ino"
void wifi_client();
#line 24 "f:\\myWorkSpace\\0_my_project\\3_wifi_car\\wifi-smart-car\\SingleChap_software\\ESP32\\WifiCameraCar\\WifiCameraCar.ino"
void setup()
{
    WRITE_PERI_REG(RTC_CNTL_BROWN_OUT_REG, 0); //关闭低电压检测,避免无限重启
    Serial.begin(115200);
    Serial.setDebugOutput(true);
    Serial.println();
    camera_init();
    //wifi运行在AP模式下
    WiFi.softAP(ssid, password, 1, 0, 1);
    while (WiFi.softAPgetStationNum() < 1) //等待wifi连接
    {
        delay(500);
        Serial.print(".");
    }
    Serial.println("");
    Serial.println("WiFi is connected");
    delay(2000);
    //tcpip启动服务端
    server.begin();
    server.setNoDelay(true);
    Serial.print("Camera Ready! Use 'http://");
    Serial.print(WiFi.softAPIP());
    Serial.println("' to connect");
    servo_GPIO_init();

    xTaskCreatePinnedToCore(
        Taskwifi, "Taskwifi" // A name just for humans
        ,
        1024*10 // This stack size can be checked & adjusted by reading the Stack Highwater
        ,
        NULL, 1 // Priority, with 3 (configMAX_PRIORITIES - 1) being the highest, and 0 being the lowest.
        ,
        NULL, 0);//0核心运行

    xTaskCreatePinnedToCore(
        Taskcontrol, "Taskcontrol",
         1024 * 100 // Stack size
        ,
        NULL, 1 // Priority
        ,
        NULL, 1);//1核心运行
}

void loop()
{ 
    //void* mm;
    //Taskcontrol(mm);
}

void wifi_client()
{
    int angle_plat = 0;
    esp_err_t res = ESP_OK;
    int64_t fr_start = esp_timer_get_time(); //当前时间
    uint8_t i;
    while (1)
    {
        if (server.hasClient())
        {
            for (i = 0; i < MAX_SRV_CLIENTS; i++)
            { //查找空闲或者断开连接的客户端，并置为可用
                if (!serverClients[i] || !serverClients[i].connected())
                {
                    if (serverClients[i])
                        serverClients[i].stop();
                    serverClients[i] = server.available();
                    Serial.print("New client: ");
                    Serial.println(i);
                    continue;
                }
            } //若没有可用客户端，则停止连接
            WiFiClient serverClient = server.available();
            serverClient.stop();
        }
        //检查客户端的数据
        for (i = 0; i < MAX_SRV_CLIENTS; i++)
        {
            if (serverClients[i] && serverClients[i].connected())
            {
                fb = esp_camera_fb_get(); //获取摄像头的数据帧
                if (fb)
                {
                    fb_send(fb,&serverClients[i]); //发送图片
                }
                else
                {
                    Serial.println("Camera capture failed");
                }

                if (fb)
                    esp_camera_fb_return(fb); //释放抓取到的图片
                delay(1);
                if (serverClients[i].available())
                {
                    while (serverClients[i].available())
                    {
                        CMD_res_deal(serverClients[i].read()); //对命令进行处理
                    }
                }
            }
        }
    }
}

void Taskcontrol(void *pvParameters)
{
    wifi_client();
    vTaskDelete(NULL);
}


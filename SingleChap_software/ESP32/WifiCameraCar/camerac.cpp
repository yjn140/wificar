#include "camerac.h"

struct img_send
{
    uint8_t *img_buf;
    size_t img_size; //要发送的字节数
    uint8_t is_send;
    /* data */
};

WiFiClient *now_sendclient; //要发送的目标客户端

//交替发送图片数据，提升效率
struct img_send send_img[2];
uint8_t send_index = 0;

uint8_t camera_init()
{
    //摄像头配置初始化
    camera_config_t config;
    config.ledc_channel = LEDC_CHANNEL_0;
    config.ledc_timer = LEDC_TIMER_0;
    config.pin_d0 = Y2_GPIO_NUM;
    config.pin_d1 = Y3_GPIO_NUM;
    config.pin_d2 = Y4_GPIO_NUM;
    config.pin_d3 = Y5_GPIO_NUM;
    config.pin_d4 = Y6_GPIO_NUM;
    config.pin_d5 = Y7_GPIO_NUM;
    config.pin_d6 = Y8_GPIO_NUM;
    config.pin_d7 = Y9_GPIO_NUM;
    config.pin_xclk = XCLK_GPIO_NUM;
    config.pin_pclk = PCLK_GPIO_NUM;
    config.pin_vsync = VSYNC_GPIO_NUM;
    config.pin_href = HREF_GPIO_NUM;
    config.pin_sscb_sda = SIOD_GPIO_NUM;
    config.pin_sscb_scl = SIOC_GPIO_NUM;
    config.pin_pwdn = PWDN_GPIO_NUM;
    config.pin_reset = RESET_GPIO_NUM;
    config.xclk_freq_hz = 20000000;
    config.pixel_format = PIXFORMAT_JPEG;

    if (psramFound()) //esp32cam有psram
    {
        config.frame_size = FRAMESIZE_VGA;
        config.jpeg_quality = 12;
        config.fb_count = 1;
    }
    else
    {
        config.frame_size = FRAMESIZE_VGA;
        config.jpeg_quality = 12;
        config.fb_count = 1;
    }

#if defined(CAMERA_MODEL_ESP_EYE)
    pinMode(13, INPUT_PULLUP);
    pinMode(14, INPUT_PULLUP);
#endif

    // camera init
    esp_err_t err = esp_camera_init(&config);
    if (err != ESP_OK)
    {
        Serial.printf("Camera init failed with error 0x%x", err);
        return 1;
    }

    sensor_t *s = esp_camera_sensor_get();
    // initial sensors are flipped vertically and colors are a bit saturated
    if (s->id.PID == OV3660_PID)
    {
        s->set_vflip(s, 1);       // flip it back
        s->set_brightness(s, 1);  // up the brightness just a bit
        s->set_saturation(s, -2); // lower the saturation
    }
    // drop down frame size for higher initial frame rate
    s->set_framesize(s, FRAMESIZE_VGA);

#if defined(CAMERA_MODEL_M5STACK_WIDE) || defined(CAMERA_MODEL_M5STACK_ESP32CAM)
    s->set_vflip(s, 1);
    s->set_hmirror(s, 1);
#endif
    send_img[0].is_send=0;
    send_img[1].is_send=0;
    send_img[0].img_buf = (uint8_t *)ps_malloc(200 * 1024 + 10); //100K即可
    send_img[1].img_buf = (uint8_t *)ps_malloc(200 * 1024 + 10); //100K即可
    return 0;
}

void fb_send(camera_fb_t *fb_in, WiFiClient *now_client)
{
    now_sendclient = now_client;
    size_t jpegSize = fb_in->len;
    if (send_index == 0)
        send_index = 1;
    else
        send_index = 0;
    if (jpegSize <= 200 * 1024) //200k以内
    {
        send_img[send_index].img_buf[0] = 0xf0;
        send_img[send_index].img_buf[1] = 0xf0;
        send_img[send_index].img_buf[2] = 0x00;
        send_img[send_index].img_buf[3] = 0x00;
        send_img[send_index].img_buf[4] = 0xff;
        send_img[send_index].img_buf[5] = 0xff;

        send_img[send_index].img_buf[9] = (uint8_t)((jpegSize) >> 24);
        send_img[send_index].img_buf[8] = (uint8_t)((jpegSize) >> 16);
        send_img[send_index].img_buf[7] = (uint8_t)((jpegSize) >> 8);
        send_img[send_index].img_buf[6] = (uint8_t)((jpegSize));
        memcpy(send_img[send_index].img_buf + 10, fb_in->buf, jpegSize); //将fb中的数据复制到待发送的指针中
        send_img[send_index].img_size = jpegSize + 10;                   //要发送的数据长度
        send_img[send_index].is_send = 1;                               //1表示进入发送状态
        Serial.println("img ready ");
    }
    else
    {
        Serial.println("size lims");
        //报错
    }
}

void Taskwifi(void *pvParameters) // This is a task.
{
    while (1)
    {
        int i = 0;
        for (i = 0; i <= 1; i++)
        {
            if (send_img[i].is_send == 1)
            {
                //Serial.print("join!!");
                send_img[i].is_send = 0;
                //Serial.println(" 1");
                if (!now_sendclient->write(send_img[i].img_buf, send_img[i].img_size)) //在这里处理接受到的数据
                {
                    Serial.print("send_file!!");
                }
               // Serial.println(" 2");
                //vTaskDelay(100 / portTICK_RATE_MS);
                //vTaskDelay(1);
                delay(20);
                Serial.println("img send.");
            }
        }
        vTaskDelay(1);
    }
    vTaskDelete(NULL);
}
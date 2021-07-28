# WifiSmartCAR

#### 注意

第一版本是从原作者gitee仓库克隆过来的,我希望结合我的学习和理解,进行运用。

#### 介绍

原作者"Geek汪"B站链接：https://www.bilibili.com/video/BV1V44y1B7rH?from=search&seid=15136165417081287188

仓库链接[WiFi_smart_car: ESP32CAM-Android-opencv(yolo)-3d打印-图传 (gitee.com)](https://gitee.com/Geek_W/wifi-smart-car)

本开源项目是一台低成本的基于ESP32CAM的wifi图传小车，ESP32CAM-Android-opencv-Picture biography。


#### 软件架构
软件架构说明
ESP32CAM开发板+androidAPP+opencv（yolo）+3个G90舵机（2个持续旋转的+1个90度旋转）+3D打印件

#### 安装教程

1. 需要安装arduino程序进行编写和烧录，也可使用乐鑫烧录工具。
2.  需要一台Android手机安装apk
3.  需要一台3d打印机打印3D文件

#### 使用说明
包含的资源有：
1.wifi小车的3d模型的stl文件，需要源文件请联系（源文件为cero绘制）。
2.因为精力原因并未打板的pcb文件（使用stm32f411主控，包含两个闭环直流驱动接口，mpu6050芯片，一枚彩色led），
尺寸仅为2cm*3CM，每路驱动能力峰值2A，可用于制作平衡车。
3.安卓控制APP，该程序可连接小车WiFi实现图传并控制小车运动。其中sdk文件夹是opencv422的库。
4.esp32程序是使用vscode基于arduino框架编写，使用freeRTOS调用双核实现图传和小车的控制。
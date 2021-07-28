# WifiSmartCAR

#### 介绍
B站链接：https://www.bilibili.com/video/BV1V44y1B7rH?from=search&seid=15136165417081287188
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


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)

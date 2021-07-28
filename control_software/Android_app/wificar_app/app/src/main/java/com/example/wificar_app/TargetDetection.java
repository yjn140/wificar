package com.example.wificar_app;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TargetDetection {

    // 线程池
    // 为了方便展示,此处直接采用线程池进行线程管理,而没有一个个开线程
    private ExecutorService mThreadPool;

    private Net net = null;
    private  Activity this_app;
    private static final String[] classNames = {"background",
            "aeroplane", "bicycle", "bird", "boat",
            "bottle", "bus", "car", "cat", "chair",
            "cow", "diningtable", "dog", "horse",
            "motorbike", "person", "pottedplant",
            "sheep", "sofa", "train", "tvmonitor"};

    TargetDetection(Activity App)
    {
        this_app=App;
        mThreadPool = Executors.newCachedThreadPool();
        if(!loadModel())return;//加载模型
    }





    /**
     * 加载模型
     */
    private boolean loadModel() {
        if (net == null) {
            try {
                String proto = getPath("deploy.prototxt", this_app);//deploy_gender.prototxt
                String weights = getPath("mobilenet_iter_73000.caffemodel", this_app);//gender_net.caffemodel
                net = Dnn.readNetFromCaffe(proto, weights);
                Log.i("model", "load model successfully.");
            }
            catch (Exception e)
            {
                this_app.finish();
               return false;
            }
        }
        return true;
    }

    boolean is_dealfin=true;
    Mat detections;//目标的绘制区域


    //在线程中处理目标检测任务
    public Bitmap threadTergetor_star(Bitmap image_in) {
        //开启一个线程
        mThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (is_dealfin) {
                        is_dealfin = false;
                        Bitmap image = image_in.copy(Bitmap.Config.ARGB_8888, true);
                        // 确保加载完成
                        recognize(image);

                        is_dealfin = true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        return  draw_img(image_in);
    }


    private Bitmap draw_img(Bitmap drawimg)
    {
        final double THRESHOLD = 0.7;
        if(detections!=null) {
            Mat imageMat = new Mat();
            Utils.bitmapToMat(drawimg, imageMat);
            int cols = imageMat.cols();
            int rows = imageMat.rows();
            detections = detections.reshape(1, (int) detections.total() / 7);
            boolean detected = false;
            for (int i = 0; i < detections.rows(); ++i) {
                double confidenceTmp = detections.get(i, 2)[0];
                if (confidenceTmp > THRESHOLD) {
                    detected = true;
                    int classId = (int) detections.get(i, 1)[0];
                    if(classId==15) {//识别到是人
                        int left = (int) (detections.get(i, 3)[0] * cols);
                        int top = (int) (detections.get(i, 4)[0] * rows);
                        int right = (int) (detections.get(i, 5)[0] * cols);
                        int bottom = (int) (detections.get(i, 6)[0] * rows);
                        // Draw rectangle around detected object.
                        Imgproc.rectangle(imageMat, new Point(left, top), new Point(right, bottom),
                                new Scalar(0, 255, 0,255), 2);
                        String label = classNames[classId] + ": " + String.format("%.2f", confidenceTmp);
                        int[] baseLine = new int[1];
                        Size labelSize = Imgproc.getTextSize(label, 3, 0.5, 2, baseLine);
                        // Draw background for label.
                        Imgproc.rectangle(imageMat, new Point(left, top - labelSize.height),
                                new Point(left + labelSize.width, top + baseLine[0]),
                                new Scalar(0, 255, 0,255), Core.FILLED);
                        // Write class name and confidence.
                        Imgproc.putText(imageMat, label, new Point(left, top),
                                3, 0.5, new Scalar(0, 0, 0,255));
                    }
                }
            }
            if (!detected) {
                //Toast.makeText(this_app, "没有检测到目标！", Toast.LENGTH_LONG).show();
                return drawimg;
            }
            Utils.matToBitmap(imageMat, drawimg);
        }
        return drawimg;
    }

    /**
     * 识别,是最耗时间的操作， 放在线程池中进行    */
    private boolean recognize(Bitmap image) {
        // 该网络的输入层要求的图片尺寸为 300*300
        final int IN_WIDTH = 300;
        final int IN_HEIGHT = 300;
        final float WH_RATIO = (float) IN_WIDTH / IN_HEIGHT;
        final double IN_SCALE_FACTOR = 0.007843;
        final double MEAN_VAL = 127.5;
        if (net == null) {
            Toast.makeText(this_app, "正在加载模型，请稍后...", Toast.LENGTH_LONG).show();
            while (net == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        Mat imageMat = new Mat();
        Utils.bitmapToMat(image, imageMat);
        Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_RGBA2RGB);
        Mat blob = Dnn.blobFromImage(imageMat, IN_SCALE_FACTOR,
                new Size(IN_WIDTH, IN_HEIGHT),
                new Scalar(MEAN_VAL, MEAN_VAL, MEAN_VAL),
                false, false);
        net.setInput(blob);
        detections = net.forward();//绘制的区域信息放置到全局变量中
        return true;
    }

    // Upload file to storage and return a path.
    private static String getPath(String file, Context context) {
        Log.i("getPath", "start upload file " + file);
        AssetManager assetManager = context.getAssets();
        BufferedInputStream inputStream = null;
        try {
            // Read data from assets.
            inputStream = new BufferedInputStream(assetManager.open(file));
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            inputStream.close();
            // Create copy file in storage.
            File outFile = new File(context.getFilesDir(), file);
            FileOutputStream os = new FileOutputStream(outFile);
            os.write(data);
            os.close();
            Log.i("getPath", "upload file " + file + "done");
            // Return a path to file which may be read in common way.
            return outFile.getAbsolutePath();
        } catch (IOException ex) {
            Log.e("getPath", "Failed to upload a file");
        }
        return "";
    }

}

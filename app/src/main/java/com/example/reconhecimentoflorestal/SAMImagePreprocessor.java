package com.example.reconhecimentoflorestal;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.serenegiant.utils.FileUtils;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SAMImagePreprocessor {
    private static final float[] MEAN = {123.675f, 116.28f, 103.53f};
    private static final float[] STD = {58.395f, 57.12f, 57.375f};

    public static SAMPreprocessingResult preprocessImage(Context context, Bitmap bitmap) throws IOException {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        // mostra o mat
//        saveBitmap(context, mat);

//        if (mat.channels() == 4) {
//            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2RGB);
//        } else {
//            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB);
//        }
        if (mat.channels() == 4) {
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
        }

        Log.d("NUM CANAIS", "" + mat.channels());

        // motrar o mat
//        saveBitmap(context, mat);

        int origWidth = mat.cols();
        int origHeight = mat.rows();

        int resizedWidth, resizedHeight;
        if (origWidth > origHeight) {
            resizedWidth = 1024;
            resizedHeight = (int) (1024.0 / origWidth * origHeight);
        } else {
            resizedHeight = 1024;
            resizedWidth = (int) (1024.0 / origHeight * origWidth);
        }

        Mat resizedMat = new Mat();
        Imgproc.resize(mat, resizedMat, new Size(resizedWidth, resizedHeight), 0, 0, Imgproc.INTER_CUBIC);
        Log.d("PREPROCESS", "Resized Mat dimensions: " + resizedMat.size().toString());
        // mostrar o resized mat
//        saveBitmap(context, resizedMat);

        resizedMat.convertTo(resizedMat, CvType.CV_32FC3);

        float[] pixelValue = new float[3];
        resizedMat.get(100, 50, pixelValue);

        Log.d("VALOR PIXEL", "[" +
        pixelValue[0] + ", " + pixelValue[1] + ", " + pixelValue[2] + "]");

        Core.subtract(resizedMat, new Scalar(MEAN[0], MEAN[1], MEAN[2]), resizedMat);
        Core.divide(resizedMat, new Scalar(STD[0], STD[1], STD[2]), resizedMat);
//        resizedMat.convertTo(resizedMat, CvType.CV_32FC3, 1.0 / 255);
//
//        List<Mat> channels = new ArrayList<>(3);
//        Core.split(resizedMat, channels);
//
//        channels.get(0).convertTo(channels.get(0), -1, 1.0 / STD[0], -MEAN[0] / STD[0]);
//        channels.get(1).convertTo(channels.get(1), -1, 1.0 / STD[1], -MEAN[1] / STD[1]);
//        channels.get(2).convertTo(channels.get(2), -1, 1.0 / STD[2], -MEAN[2] / STD[2]);
//
//        // Merge the channels back into one image
//        Core.merge(channels, resizedMat);

        float[] data = new float[(int) (resizedMat.total() * resizedMat.channels())];
        resizedMat.get(0, 0, data);

        float[][][][] inputTensor = new float[1][3][1024][1024];
        for (int i = 0; i < resizedHeight; i++) {
            for (int j = 0; j < resizedWidth; j++) {
                for (int c = 0; c < 3; c++) {
                    inputTensor[0][c][i][j] = data[i * resizedWidth * 3 + j * 3 + c];
                }
            }
        }

        if (resizedHeight < 1024) {
            for (int c = 0; c < 3; c++) {
                for (int j = resizedHeight; j < 1024; j++) {
                    Arrays.fill(inputTensor[0][c][j], 0);
                }
            }
        } else if (resizedWidth < 1024) {
            for (int c = 0; c < 3; c++) {
                for (int h = 0; h < resizedHeight; h++) {
                    for (int w = resizedWidth; w < 1024; w++) {
                        inputTensor[0][c][h][w] = 0;
                    }
                }
            }
        }

        Bitmap tensorBitmap = convertTensorToBitmap(inputTensor);
        Mat tensorMat = new Mat();
        Utils.bitmapToMat(tensorBitmap, tensorMat);
//        saveBitmap(context, tensorMat);
        Log.d("PREPROCESS", "Resized Mat dimensions: " + resizedMat.size().toString());

        return new SAMPreprocessingResult(inputTensor, resizedWidth, resizedHeight);
    }


    public static void saveBitmap(Context context, Mat mat) {
        try {
            File file = FileUtils.getCaptureFile(
                    context,
                    Environment.DIRECTORY_DCIM,
                    ".jpg");

            FileOutputStream fos = new FileOutputStream(file);

            Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, bitmap);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            Log.d("IMAGE SAVED", "Image saved to: " + file.getAbsolutePath());

            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri contentUri = Uri.fromFile(file);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private static Bitmap convertTensorToBitmap(float[][][][] tensor) {
        int imageWidth = 1024;
        int imageHeight = 1024;
        Bitmap bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

        int[] pixels = new int[imageWidth * imageHeight];
        int index = 0;

        for (int h = 0; h < imageHeight; h++) {
            for (int w = 0; w < imageWidth; w++) {
                float r = tensor[0][0][h][w];
                float g = tensor[0][1][h][w];
                float b = tensor[0][2][h][w];

                // Ajustar os valores de volta para [0, 255]
                r = Math.min(255, Math.max(0, r * STD[0] + MEAN[0]));
                g = Math.min(255, Math.max(0, g * STD[1] + MEAN[1]));
                b = Math.min(255, Math.max(0, b * STD[2] + MEAN[2]));

                // Converter valores para ARGB
                pixels[index++] = Color.argb(255, (int) r, (int) g, (int) b);
            }
        }

        bitmap.setPixels(pixels, 0, imageWidth, 0, 0, imageWidth, imageHeight);
        return bitmap;
    }
}


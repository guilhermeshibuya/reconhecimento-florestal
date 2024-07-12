package com.example.reconhecimentoflorestal;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SAMImagePreprocessor {
    private static final float[] MEAN = {123.675f, 116.28f, 103.53f};
    private static final float[] STD = {58.395f, 57.12f, 57.375f};

    public static float[][][] preprocessImage(String imagePath) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);

        Bitmap rgbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        int origWidth = rgbBitmap.getWidth();
        int origHeight = rgbBitmap.getHeight();

        int resizedWidth, resizedHeight;
        if (origWidth > origHeight) {
            resizedWidth = 1024;
            resizedHeight = (int) (1024.0 / origWidth * origHeight);
        } else {
            resizedHeight = 1024;
            resizedWidth = (int) (1024.0 / origHeight * origWidth);
        }

        // Resize image
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(rgbBitmap, resizedWidth, resizedHeight, true);
        Log.d("PREPROCESS", "Resized Bitmap dimensions: " + resizedBitmap.getWidth() + "x" + resizedBitmap.getHeight());

        // Convert to OpenCV Mat
        Mat mat = new Mat(resizedHeight, resizedWidth, CvType.CV_8UC3);
        Utils.bitmapToMat(resizedBitmap, mat);
        Log.d("PREPROCESS", "Mat after bitmapToMat: " + mat.size().toString());

        // Convert to Float and normalize
        mat.convertTo(mat, CvType.CV_32FC4);

        // Extrair apenas os canais RGB (ignorando o canal Alpha)
        List<Mat> channelsList = new ArrayList<>();
        Core.split(mat, channelsList);

// Verificar se há quatro canais e descartar o Alpha se necessário
        if (channelsList.size() == 4) {
            channelsList.remove(3); // Remover o canal Alpha
        }

        Core.merge(channelsList, mat);

//        Mat meanMat = new Mat(mat.size(), mat.type());
//        Mat stdMat = new Mat(mat.size(), mat.type());

//        Core.add(meanMat, new Scalar(MEAN[0], MEAN[1], MEAN[2]), meanMat);
//        Core.add(stdMat, new Scalar(STD[0], STD[1], STD[2]), stdMat);
//

        Core.subtract(mat, new Scalar(MEAN[0], MEAN[1], MEAN[2]), mat);
        Core.divide(mat, new Scalar(STD[0], STD[1], STD[2]), mat);

        Log.d("PREPROCESS", "Mat após divisão pelo desvio padrão: " + Arrays.toString(mat.get(0, 0)));

        // Convert Mat to float array
        float[] data = new float[(int) (mat.total() * mat.channels())];
        mat.get(0, 0, data);

        // Convert to 1x3xHxW
        float[][][] inputTensor = new float[1024][1024][3];
        for (int i = 0; i < resizedHeight; i++) {
            for (int j = 0; j < resizedWidth; j++) {
                for (int c = 0; c < 3; c++) {
                    inputTensor[i][j][c] = data[i * resizedWidth * 3 + j * 3 + c];
                }
            }
        }

        // Pad with zeros
        if (resizedHeight < 1024) {
            for (int i = resizedHeight; i < 1024; i++) {
                for (int j = 0; j < 1024; j++) {
                    Arrays.fill(inputTensor[i][j], 0);
                }
            }
        } else if (resizedWidth < 1024) {
            for (int i = 0; i < 1024; i++) {
                for (int j = resizedWidth; j < 1024; j++) {
                    Arrays.fill(inputTensor[i][j], 0);
                }
            }
        }

        return inputTensor;
    }
}

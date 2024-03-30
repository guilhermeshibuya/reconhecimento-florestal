package com.example.reconhecimentoflorestal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Debug;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import kotlin.jvm.internal.FloatSpreadBuilder;


public class ModelUtilities {
    private Context mContext;

    public ModelUtilities(Context context) {
        mContext = context;
    }

    public float[][][][] preprocessImages(Bitmap bitmap) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        float[][][][] inputArray = new float[1][3][224][224]; // Batch size = 1

        // Converte a imagem em um array de float
        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                int pixel = resizedBitmap.getPixel(x, y);
                inputArray[0][0][y][x] = Color.red(pixel) / 255.0f;
                inputArray[0][1][y][x] = Color.green(pixel) / 255.0f;
                inputArray[0][2][y][x] = Color.blue(pixel) / 255.0f;
            }
        }
        return inputArray;
    }

    public String runInference(float[][][][] inputArray) {
        String results = "";

        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();

            // Obtém o identificador do recurso raw do modelo ONNX
            int resourceId = mContext.getResources().getIdentifier("model", "raw", mContext.getPackageName());

            InputStream inputStream = mContext.getResources().openRawResource(resourceId);

            File modelFile = new File(mContext.getCacheDir(), "model.onnx");
            FileOutputStream outputStream = new FileOutputStream(modelFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            OrtSession session = env.createSession(modelFile.getAbsolutePath(), options);

            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputArray);
            OrtSession.Result output = session.run(Collections.singletonMap("images", inputTensor));

            float[][] outputValues = (float[][]) output.get(0).getValue();

            inputTensor.close();

            results = formatResults(outputValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    private void quickSort(float[] arr, int start, int end) {
        if (start < end) {
            int pivot = partition(arr, start, end);

            quickSort(arr, start, pivot - 1);
            quickSort(arr ,pivot + 1, end);
        }
    }

    private int partition(float[] arr, int start, int end)
    {
        float pivot = arr[end];
        int i = (start - 1);

        for (int j = start; j < end; j++) {
            if (arr[j] >= pivot) {
                i++;

                float temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        float temp = arr[i + 1];
        arr[i + 1] = arr[end];
        arr[end] = temp;

        return i + 1;
    }




    private String formatResults(float[][] output) {
        float[] clone = output[0].clone();
        quickSort(clone, 0, clone.length - 1);

        StringBuilder strBuilder = new StringBuilder();

        for (int i = 0; i < 5; i++) {
            float prob = clone[i] * 100;
            strBuilder.append("Classe ").append(i).append(": ").append(String.format(Locale.getDefault(), "%.4f", prob)).append("%\n");
        }
        return strBuilder.toString();
    }
}

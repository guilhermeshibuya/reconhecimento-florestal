package com.example.reconhecimentoflorestal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Debug;
import android.util.Half;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import kotlin.jvm.internal.FloatSpreadBuilder;

class InferenceResult {
    public float[] results;
    public int[] indices;

    public InferenceResult(float[] results, int[] indices) {
        this.results = results;
        this.indices = indices;
    }
}

public class ModelUtilities {
    private Context mContext;

    public ModelUtilities(Context context) {
        mContext = context;
    }

    public float[][][][] preprocessImages(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.resize(mat, mat, new Size(224, 224), 0, 0, Imgproc.INTER_CUBIC);

        mat.convertTo(mat, CvType.CV_32FC3, 1.0 / 255.0);

        float[][][][] outputArray = new float[1][3][224][224];

        for (int y = 0; y < 224; y++) {
            for (int x = 0; x < 224; x++) {
                double[] pixel = mat.get(y, x);
                outputArray[0][0][y][x] = (float) pixel[2];
                outputArray[0][1][y][x] = (float) pixel[1];
                outputArray[0][2][y][x] = (float) pixel[0];
            }
        }
        return outputArray;
//        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
//        float[][][][] inputArray = new float[1][3][224][224]; // Batch size = 1
//
//        // Converte a imagem em um array de float
//        for (int y = 0; y < 224; y++) {
//            for (int x = 0; x < 224; x++) {
//                int pixel = resizedBitmap.getPixel(x, y);
//                inputArray[0][0][y][x] = Color.red(pixel) / 255.0f;
//                inputArray[0][1][y][x] = Color.green(pixel) / 255.0f;
//                inputArray[0][2][y][x] = Color.blue(pixel) / 255.0f;
//            }
//        }
//        return inputArray;
    }

    public InferenceResult runInference(float[][][][] inputArray) {
        float[] results = new float[5];
        int[] top5indices = new int[5];

        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();

            // Obtém o identificador do recurso raw do modelo ONNX
            int resourceId = mContext.getResources().getIdentifier("best_30_06_2024", "raw", mContext.getPackageName());

            InputStream inputStream = mContext.getResources().openRawResource(resourceId);

            File modelFile = new File(mContext.getCacheDir(), "best_30_06_2024.onnx");
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
            top5indices = getTop5Indices(outputValues[0]);

            inputTensor.close();

            for (int i = 0; i < 5; i++) {
                int index = top5indices[i];
                results[i] = outputValues[0][index];
//                Log.d("OUTPUTS", i + 1 +  ": " + classes[index] + " - " + results[i] * 100);
            }
//            Log.d("OUTPUT", Arrays.toString(outputValues[0]));
//            Log.d("INDICES OUTPUT", Arrays.toString(top5indices));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new InferenceResult(results, top5indices);
    }

    private void quickSort(float[] arr, int[] indices, int start, int end) {
        if (start < end) {
            int pivotIndex = partition(arr, indices, start, end);

            quickSort(arr, indices, start, pivotIndex - 1);
            quickSort(arr, indices, pivotIndex + 1, end);
        }
    }

    private int partition(float[] arr, int[] indices, int start, int end) {
        float pivot = arr[end];
        int i = start - 1;

        for (int j = start; j < end; j++) {
            if (arr[j] >= pivot) {
                i++;

                float temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;

                int tempIndex = indices[i];
                indices[i] = indices[j];
                indices[j] = tempIndex;
            }
        }

        float temp = arr[i + 1];
        arr[i + 1] = arr[end];
        arr[end] = temp;

        int tempIndex = indices[i + 1];
        indices[i + 1] = indices[end];
        indices[end] = tempIndex;

        return i + 1;
    }

    private int[] getTop5Indices(float[] result) {
        int[] indices = new int[result.length];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }

        float[] clone = result.clone();
        quickSort(clone, indices, 0, clone.length - 1);

        return Arrays.copyOfRange(indices, 0, 5);
    }

    private float[] formatResults(float[][] output) {
        int n = 5;
        float[] clone = output[0].clone();
        float[] results = new float[n];

//        quickSort(clone, 0, clone.length - 1);

//        StringBuilder strBuilder = new StringBuilder();

        for (int i = 0; i < n; i++) {
            float prob = clone[i] * 100;
            results[i] = prob;
//            strBuilder.append("Classe ").append(i).append(": ").append(String.format(Locale.getDefault(), "%.4f", prob)).append("%\n");
        }
//        return strBuilder.toString();
        return results;
    }
}

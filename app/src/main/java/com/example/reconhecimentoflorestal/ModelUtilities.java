package com.example.reconhecimentoflorestal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Debug;
import android.util.Half;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.IntStream;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
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

    public InferenceResult runInference(float[][][][] inputArray) {
        float[] results = new float[5];
        int[] top5indices = new int[5];

        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();

            InputStream inputStream = mContext.getResources().openRawResource(R.raw.best_30_06_2024);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);

            OrtSession session = env.createSession(buffer, options);

            OnnxTensor inputTensor = OnnxTensor.createTensor(env, inputArray);

            long startTime = System.nanoTime();

            OrtSession.Result output = session.run(Collections.singletonMap("images", inputTensor));

            long endTime = System.nanoTime();
            long inferenceTime = endTime - startTime;

            double inferenceTimeMs = inferenceTime / 1_000_000.0;
            System.out.println("Tempo de inferência: " + inferenceTimeMs + " ms");

            float[][] outputValues = (float[][]) output.get(0).getValue();
//            top5indices = getTop5Indices(outputValues[0]);
            top5indices = getTop5Indices(outputValues[0]);
            inputTensor.close();
            session.close();

            for (int i = 0; i < 5; i++) {
                int index = top5indices[i];
                results[i] = outputValues[0][index];
            }
        } catch (OrtException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new InferenceResult(results, top5indices);
    }

    private int[] getTop5Indices(float[] result) {
        return IntStream.range(0, result.length)
                .boxed()
                .sorted((i, j) -> Float.compare(result[j], result[i]))
                .mapToInt(element -> element)
                .limit(5)
                .toArray();
    }
}

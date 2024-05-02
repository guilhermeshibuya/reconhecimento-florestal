package com.example.reconhecimentoflorestal;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Debug;
import android.util.Half;
import android.util.Log;

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

public class ModelUtilities {
    private Context mContext;

    public ModelUtilities(Context context) {
        mContext = context;
    }

    String[] classes = {
            "Acrocarpus fraxinifolius_ACROCARPUS",
            "Apuleia leiocarpa_GARAPEIRA",
            "Araucaria angustifolia_ARAUCARIA",
            "Aspidosperma polyneuron_PEROBA ROSA",
            "Aspidosperma sp_PAU CETIM",
            "Bagassa guianensis_TATAJUBA",
            "Balfourodendron riedelianum_PAU MARFIM",
            "Bertholletia excelsa_CASTANHEIRA",
            "Bertolethia excelsa_CASTANHEIRA",
            "Bowdichia sp_SUCUPIRA",
            "Brosimum paraensis_MUIRAPIRANGA",
            "Carapa guianensis_ANDIROBA",
            "Cariniana estrellensis_JEQUITIBA",
            "Cedrela fissilis_CEDRO",
            "Cedrelinga catenaeformis_CEDRORANA",
            "Clarisia racemosa_GUARIUBA",
            "Cordia Goeldiana_FREIJO",
            "Cordia alliodora_LOURO-AMARELO",
            "Couratari sp_TAUARI",
            "Dipteryx sp_CUMARU",
            "Erisma uncinatum_CEDRINHO",
            "Eucalyptus sp_EUCALIPTO",
            "Euxylophora paraensis_PAU AMARELO",
            "Goupia glabra_CUPIUBA",
            "Grevilea robusta_GREVILEA",
            "Manilkara huberi_MASSARANDUBA",
            "Hymenaea sp_JATOBA",
            "Hymenolobium petraeum_ANGELIM PEDRA",
            "Laurus nobilis_LOURO",
            "Machaerium sp_MACHAERIUM",
            "Melia azedarach_CINAMOMO",
            "Mezilaurus itauba_ITAUBA",
            "Micropholis venulosa_CURUPIXA",
            "Myroxylon balsamum_CABREUVA VERMELHA",
            "Mimosa scabrella_BRACATINGA",
            "Ocotea porosa_IMBUIA",
            "Peltagyne sp_ROXINHO",
            "Pinus sp_PINUS",
            "Podocarpus lambertii_PODOCARPUS",
            "Pouteria pachycarpa_GOIABAO",
            "Simarouba amara_MARUPA",
            "Swietenia macrophylla_MOGNO",
            "Virola surinamensis_VIROLA",
            "Vochysia sp_QUARUBA CEDRO"
    };

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

    public float[] runInference(float[][][][] inputArray) {
        float[] results = new float[5];
        try {
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();

            // Obtém o identificador do recurso raw do modelo ONNX
            int resourceId = mContext.getResources().getIdentifier("best3", "raw", mContext.getPackageName());

            InputStream inputStream = mContext.getResources().openRawResource(resourceId);

            File modelFile = new File(mContext.getCacheDir(), "best3.onnx");
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
            int[] top5ind = getTop5Indices(outputValues[0]);

            inputTensor.close();

            for (int i = 0; i < 5; i++) {
                int index = top5ind[i];
                results[i] = outputValues[0][index];
                Log.d("OUTPUTS", i + 1 +  ": " + classes[index] + " - " + results[i] * 100);
            }

//            results = formatResults(outputValues);

            Log.d("OUTPUT", Arrays.toString(results));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
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

//    private void quickSort(float[] arr, int start, int end) {
//        if (start < end) {
//            int pivot = partition(arr, start, end);
//
//            quickSort(arr, start, pivot - 1);
//            quickSort(arr ,pivot + 1, end);
//        }
//    }
//
//    private int partition(float[] arr, int start, int end)
//    {
//        float pivot = arr[end];
//        int i = (start - 1);
//
//        for (int j = start; j < end; j++) {
//            if (arr[j] >= pivot) {
//                i++;
//
//                float temp = arr[i];
//                arr[i] = arr[j];
//                arr[j] = temp;
//            }
//        }
//        float temp = arr[i + 1];
//        arr[i + 1] = arr[end];
//        arr[end] = temp;
//
//        return i + 1;
//    }

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

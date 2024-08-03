package com.example.reconhecimentoflorestal;

import android.content.Context;
import android.util.Log;

import ai.onnxruntime.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class SAMModelRunner {
    private OrtEnvironment env;
    private OrtSession encoderSession;
    private OrtSession decoderSession;

    public SAMModelRunner(Context context) throws OrtException, IOException {
        env = OrtEnvironment.getEnvironment();
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();

        InputStream encoderStream = context.getResources().openRawResource(R.raw.mobile_sam_tuned_encoder);
        byte[] encoderBytes = new byte[encoderStream.available()];
        encoderStream.read(encoderBytes);
        encoderSession = env.createSession(encoderBytes, options);

        InputStream decoderStream = context.getResources().openRawResource(R.raw.mobile_sam_tuned_decoder);
        byte[] decoderBytes = new byte[decoderStream.available()];
        decoderStream.read(decoderBytes);
        decoderSession = env.createSession(decoderBytes, options);
    }

    public float[][][][] generateEmbeddings(float[][][][] inputTensor) throws OrtException {
        OnnxTensor inputOnnxTensor = OnnxTensor.createTensor(env, inputTensor);
        OrtSession.Result encoderResult = encoderSession.run(Collections.singletonMap("images", inputOnnxTensor));
        float[][][][] embeddings = (float[][][][]) encoderResult.get(0).getValue();
        /*
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < embeddings.length; i++) {
            sb.append("[");
            for (int j = 0; j < embeddings[i].length; j++) {
                sb.append("[");
                for (int k = 0; k < embeddings[i][j].length; k++) {
                    sb.append("[");
                    for (int l = 0; l < embeddings[i][j][k].length; l++) {
                        sb.append(embeddings[i][j][k][l]);
                        if (l < embeddings[i][j][k].length - 1) {
                            sb.append(", ");
                        }
                    }
                    sb.append("]");
                    if (k < embeddings[i][j].length - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("]");
                if (j < embeddings[i].length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            if (i < embeddings.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");

        // Save the embeddings string to a file
        try (FileWriter writer = new FileWriter("/storage/emulated/0/Download/embeddings.txt")) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
         */

        return embeddings;
    }

    public float[][][] runDecoder(float[][][][] embeddings, float[] boundingBox, int[] inputLabels, int origWidth, int origHeight, int resizedWidth, int resizedHeight) throws OrtException {
        float[][][] onnxCoord = new float[1][2][2];
        float[][] onnxLabel = new float[1][inputLabels.length];

        onnxCoord[0][0][0] = boundingBox[0] * (resizedWidth / (float)origWidth);
        onnxCoord[0][0][1] = boundingBox[1] * (resizedHeight / (float)origHeight);
        onnxCoord[0][1][0] = boundingBox[2] * (resizedWidth / (float)origWidth);
        onnxCoord[0][1][1] = boundingBox[3] * (resizedHeight / (float)origHeight);
        Log.d("BBOX CONVERTIDO", Arrays.deepToString(onnxCoord));

        for (int i = 0; i < onnxLabel.length; i++) {
            onnxLabel[0][i] = inputLabels[i];
        }

        float[][][][] onnxMaskInput = new float[1][1][256][256];
        float[] onnxHasMaskInput = new float[1];

        Map<String, OnnxTensor> inputs = new HashMap<>();
        inputs.put("image_embeddings", OnnxTensor.createTensor(env, embeddings));
        inputs.put("point_coords", OnnxTensor.createTensor(env, onnxCoord));
        inputs.put("point_labels", OnnxTensor.createTensor(env, onnxLabel));
        inputs.put("mask_input", OnnxTensor.createTensor(env, onnxMaskInput));
        inputs.put("has_mask_input", OnnxTensor.createTensor(env, onnxHasMaskInput));
        inputs.put("orig_im_size", OnnxTensor.createTensor(env, new float[]{origHeight, origWidth}));

        OrtSession.Result decoderResult = decoderSession.run(inputs);
        float[][][][] masks = (float[][][][]) decoderResult.get(0).getValue();

        int channels = masks[0].length;
        int height = masks[0][0].length;
        int width = masks[0][0][0].length;

        float[][][] mask = new float[channels][height][width];

        for (int c = 0; c < channels; c++) {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    mask[c][i][j] = masks[0][c][i][j];
                }
            }
        }

        return mask;
    }
}

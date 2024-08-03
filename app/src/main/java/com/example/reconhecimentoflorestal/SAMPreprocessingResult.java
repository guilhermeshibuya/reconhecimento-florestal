package com.example.reconhecimentoflorestal;

public class SAMPreprocessingResult {
    private float[][][][] tensor;
    private int resizedWidth;
    private int resizedHeight;

    public SAMPreprocessingResult(float[][][][] tensor, int resizedWidth, int resizedHeight) {
        this.tensor = tensor;
        this.resizedWidth = resizedWidth;
        this.resizedHeight = resizedHeight;
    }

    public float[][][][] getTensor() {
        return tensor;
    }

    public int getResizedWidth() {
        return resizedWidth;
    }

    public int getResizedHeight() {
        return resizedHeight;
    }
}

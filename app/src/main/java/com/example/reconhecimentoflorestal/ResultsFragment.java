package com.example.reconhecimentoflorestal;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class ResultsFragment extends Fragment {
    private SharedViewModel viewModel;
    private HorizontalBarChart hBarChart;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.results_fragment, container, false);

        viewModel.getImage().observe(getViewLifecycleOwner(), image -> {
            ModelUtilities modelUtilities = new ModelUtilities(getContext());

            float[][][][] inputArray = modelUtilities.preprocessImages(image);

            float[] results = modelUtilities.runInference(inputArray);

            hBarChart = view.findViewById(R.id.resultsBarChart);

            ArrayList<BarEntry> barEntries = new ArrayList<>();

            for (int i = 0; i < results.length; i++) {
                float value = results[i];

                BarEntry entry = new BarEntry(i, value);
                barEntries.add(entry);
            }

            BarDataSet barDataSet = new BarDataSet(barEntries, "Resultados");
            barDataSet.setColors(Color.parseColor("#5AC279"));

            BarData barData = new BarData(barDataSet);
            barData.setBarWidth(0.5f);

            hBarChart.setData(barData);

            hBarChart.setTouchEnabled(false);

            hBarChart.getDescription().setEnabled(false);

            hBarChart.getXAxis().setDrawGridLines(false);
            hBarChart.getXAxis().setDrawAxisLine(false);
            hBarChart.getXAxis().setDrawLabels(false);

            hBarChart.getAxisLeft().setDrawGridLines(false);
            hBarChart.getAxisLeft().setDrawAxisLine(false);
            hBarChart.getAxisLeft().setDrawLabels(false);

            hBarChart.getAxisRight().setDrawGridLines(false);
            hBarChart.getAxisRight().setDrawAxisLine(false);
            hBarChart.getAxisRight().setDrawLabels(false);
        });

        return  view;
    }
}

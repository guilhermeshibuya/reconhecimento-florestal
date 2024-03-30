package com.example.reconhecimentoflorestal;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

public class ResultsFragment extends Fragment {
    private SharedViewModel viewModel;
    private TextView textResult;

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

            String results = modelUtilities.runInference(inputArray);

            textResult = view.findViewById(R.id.results_text);

            textResult.setText(results);
        });

        return  view;
    }
}

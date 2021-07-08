package com.mobiledgex.tritonclient.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.mobiledgex.tritonclient.R;
import com.mobiledgex.tritonlib.InceptionProcessorActivity;
import com.mobiledgex.tritonlib.Yolov4ProcessorActivity;

public class HomeFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        Button buttonYolov4 = root.findViewById(R.id.button_yolov4);
        Button buttonInception = root.findViewById(R.id.button_inception);

        buttonYolov4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), Yolov4ProcessorActivity.class);
                startActivity(intent);
            }
        });

        buttonInception.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), InceptionProcessorActivity.class);
                startActivity(intent);
            }
        });

        return root;
    }
}
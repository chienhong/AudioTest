package com.example.audiotest.ui;

import com.example.audiotest.R;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragAudioTestAllPage extends Fragment {

	private static final String LOG_TAG = "FragAudioTestAllPage";
	
    public FragAudioTestAllPage() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_audiotest_all, container, false);
        
        initView(rootView);
        
        return rootView;
    }
    
    private void initView(View rootView){
    	
    	
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
}

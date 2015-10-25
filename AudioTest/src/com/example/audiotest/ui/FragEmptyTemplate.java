package com.example.audiotest.ui;

import com.example.audiotest.R;
import com.example.audiotest.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FragEmptyTemplate extends Fragment {

	private static final String LOG_TAG = "FragEmptyTemplate";
	
    public FragEmptyTemplate() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_audio_test_page, container, false);
        
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

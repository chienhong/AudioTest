package com.example.audiotest.ui;

import com.example.audiotest.module.AudioConfig;
import com.example.audiotest.R;
import com.example.audiotest.R.id;
import com.example.audiotest.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

public class FragAudioBasicSettingPage extends Fragment {

	private static final String LOG_TAG = "FragAudioBasicSettingPage";
	
	//UI Elements
	private EditText etDynPlayRateRatio = null;
	private Spinner spnSampleRate = null;
	private Spinner spnAudioFrameSize = null;
	private EditText etComposedFrameCount = null;
	
	private ArrayAdapter<String> listSampleRateAdapter;
	private ArrayAdapter<String> listAudioFrameSizeAdapter;
	private String[] listSampleRate = new String[] { "8000", "12000", "16000", "24000", "48000" };
	private String[] listAudioFrameSize = new String[] { "20", "40", "60" };
	
	private AudioConfig mAudioConfig;
	
    public FragAudioBasicSettingPage() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_audio_basic_setting_page, container, false);
        
        mAudioConfig = AudioConfig.GetInstance();
        
        initView(rootView);
        
        return rootView;
    }
    
    private void initView(View rootView){
    	
    	spnSampleRate = (Spinner) rootView.findViewById(R.id.spnSampleRate);
    	
    	listSampleRateAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listSampleRate);
    	spnSampleRate.setAdapter(listSampleRateAdapter);
    	spnSampleRate.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int sampleRate = Integer.parseInt(listSampleRate[position]);
				Log.d(LOG_TAG, String.format("SampleRate: %d selected", sampleRate));
				mAudioConfig.setAudioSampleRate(sampleRate);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
    		
    	});
    	
    	//init default value of sample rate
    	String strSampleRate = String.valueOf(AudioConfig.RECORDER_BASE_SAMPLERATE);
    	int positionSampleRate = listSampleRateAdapter.getPosition(strSampleRate);
    	spnSampleRate.setSelection(positionSampleRate);
    	
    	spnAudioFrameSize = (Spinner) rootView.findViewById(R.id.spnAudioFrameSize);
    	listAudioFrameSizeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listAudioFrameSize);
    	spnAudioFrameSize.setAdapter(listAudioFrameSizeAdapter);
    	
    	spnAudioFrameSize.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int audioFrameSize = Integer.parseInt(listAudioFrameSize[position]);
				Log.d(LOG_TAG, String.format("Audio Frame Size: %d ms selected", audioFrameSize));
				mAudioConfig.setAudioFrameSize(audioFrameSize);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	
    	//init default value of audio frame size
    	String strAudioFrameSize = String.valueOf(AudioConfig.mAudioFrameSize);
    	int positionAudioFrameSize = listAudioFrameSizeAdapter.getPosition(strAudioFrameSize);
    	spnAudioFrameSize.setSelection(positionAudioFrameSize);
    	
    	etDynPlayRateRatio = (EditText) rootView.findViewById(R.id.etDynPlayRateRatio);
    	String strPlayRateSpeedupRatio = String.valueOf(mAudioConfig.getPlaybackSpeedupRatio());
    	etDynPlayRateRatio.setText(strPlayRateSpeedupRatio);
    	etDynPlayRateRatio.addTextChangedListener(new TextWatcher(){

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				if((null != s) && (s.length() > 0)){
					int speedupRatio = Integer.parseInt(s.toString());
					mAudioConfig.setPlaybackSpeedupRatio(speedupRatio);
					Log.d(LOG_TAG, String.format("new playback speed up ratio: %d", speedupRatio));
				}
			}
    		
    	});
    	
    	etComposedFrameCount = (EditText) rootView.findViewById(R.id.etComposedFrameCount);
    	etComposedFrameCount.addTextChangedListener(new TextWatcher(){

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void afterTextChanged(Editable s) {
				// TODO Auto-generated method stub
				if((null != s) && (s.length() > 0)){
					int composedCount = Integer.parseInt(s.toString());
					if((composedCount > 0) && (composedCount < 21)){
						mAudioConfig.setComposedFrameCount(composedCount);
					}
					else{
						String strComposedCount = String.valueOf(mAudioConfig.getComposedFrameCount());
						etComposedFrameCount.setText(strComposedCount);
					}
				}
			}
    		
    	});
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
}

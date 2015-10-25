package com.example.audiotest.ui;

import com.example.audiotest.R;
import com.example.audiotest.R.id;
import com.example.audiotest.R.layout;
import com.example.audiotest.module.OpusConfig;
import com.example.audiotest.module.OpusWrapper;

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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;

public class FragOpusSettingPage extends Fragment {
	
	private static final String LOG_TAG = "FragOpusSettingPage";
	
	private OpusConfig mOpusConfig = null;
	
	//UI Elements
	private RadioGroup rgOpusApplication = null;
	private RadioButton rbAppVoIP = null;
	private RadioButton rbAppAudio = null;
	private RadioButton rbAppRistrictLowDelay = null;
	private RadioGroup rgOpusSignal = null;
	private RadioButton rbSignalAuto = null;
	private RadioButton rbSignalVoice = null;
	private RadioButton rbSignalMusic = null;
	private RadioGroup rgOpusBandwidth = null;
	private RadioButton rbBandwidthNarrow = null;
	private RadioButton rbBandwidthMedium = null;
	private RadioButton rbBandwidthWide = null;
	private RadioButton rbBandwidthSuper = null;
	private RadioButton rbBandwidthFull = null;
	private Spinner spnOpusBitrate = null;
	private EditText etOpusGainValue = null;
	private ArrayAdapter<String> listOpusBitrateAdapter = null;
	private String[] listOpusBitrate = new String[] { "8000", "12000", "16000", "24000", "48000", "64000", "96000" };
	
	private int mOpusApplication = OpusWrapper.OPUS_APPLICATION_VOIP;
	private int mOpusSignal = OpusWrapper.OPUS_SIGNAL_TYPE_VOICE;
	private int mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_MEDIUMBAND;
	
    public FragOpusSettingPage() {
    	mOpusConfig = OpusConfig.GetInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_opus_setting_page, container, false);
        
        mOpusConfig = OpusConfig.GetInstance();
        mOpusApplication = mOpusConfig.getOAValue();
        mOpusSignal = mOpusConfig.getOSTValue();
        mOpusBandwidth = mOpusConfig.getOBValue();
        
        initView(rootView);
        
        return rootView;
    }
    
    private void initView(View rootView){
    	
    	rgOpusApplication = (RadioGroup) rootView.findViewById(R.id.rgOpusApplication);
    	rgOpusApplication.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG, String.format("opus application checked id=%d", checkedId));
				setOpusApplication(checkedId);
			}
    	});
    	
    	rbAppVoIP = (RadioButton) rootView.findViewById(R.id.rbAppVoIP);
    	rbAppAudio = (RadioButton) rootView.findViewById(R.id.rbAppAudio);
    	rbAppRistrictLowDelay = (RadioButton) rootView.findViewById(R.id.rbAppRistrictLowDelay);
    	initApplicationUIValue();
    	
    	rgOpusSignal = (RadioGroup) rootView.findViewById(R.id.rgOpusSignal);
    	rgOpusSignal.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG, String.format("opus signal checked id=%d", checkedId));
				setOpusSignal(checkedId);
			}
    		
    	});
    	rbSignalAuto = (RadioButton) rootView.findViewById(R.id.rbSignalAuto);
    	rbSignalVoice = (RadioButton) rootView.findViewById(R.id.rbSignalVoice);
    	rbSignalMusic = (RadioButton) rootView.findViewById(R.id.rbSignalMusic);
    	initSignalUIValue();
    	
    	rgOpusBandwidth = (RadioGroup) rootView.findViewById(R.id.rgOpusBandWidth);
    	rgOpusBandwidth.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG, String.format("opus bandwidth checked id=%d", checkedId));
				setOpusBandwidth(checkedId);
			}
    		
    	});
    	rbBandwidthNarrow = (RadioButton) rootView.findViewById(R.id.rbBandWidthNarrow);
    	rbBandwidthMedium = (RadioButton) rootView.findViewById(R.id.rbBandWidthMedium);
    	rbBandwidthWide = (RadioButton) rootView.findViewById(R.id.rbBandWidthWide);
    	rbBandwidthSuper = (RadioButton) rootView.findViewById(R.id.rbBandWidthSuperWide);
    	rbBandwidthFull = (RadioButton) rootView.findViewById(R.id.rbBandWidthFull);
    	initBandwidthValue();
    	
    	spnOpusBitrate = (Spinner) rootView.findViewById(R.id.spnOpusBitrate);
    	listOpusBitrateAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listOpusBitrate);
    	spnOpusBitrate.setAdapter(listOpusBitrateAdapter);
    	
    	spnOpusBitrate.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int bitrate = Integer.parseInt(listOpusBitrate[position]);
				Log.d(LOG_TAG, String.format("Opus Bitrate: %d selected", bitrate));
				mOpusConfig.setBitrate(bitrate);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	initBitrateUIValue();
    	
    	
    	etOpusGainValue = (EditText) rootView.findViewById(R.id.etOpusGainValue);
    	etOpusGainValue.addTextChangedListener(new TextWatcher(){

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
					int gainValue = Integer.parseInt(s.toString());
					if((gainValue>=0) && (gainValue<32768)){
						mOpusConfig.setGainValue(gainValue);
					}
				}
			}
    		
    	});
    	
    }
    
    private void initBitrateUIValue(){
    	int bitrate = mOpusConfig.getBitrate();
    	String strBitrate = String.valueOf(bitrate);
    	int index = 0;
    	for(index = 0; index<listOpusBitrate.length; index++){
    		if(strBitrate.equalsIgnoreCase(listOpusBitrate[index])){
    			break;
    		}
    	}
    	
    	if(index < listOpusBitrate.length){
    		spnOpusBitrate.setSelection(index);
    	}
    	
    }
    
    private void initApplicationUIValue(){
    	int opusApplication = mOpusConfig.getOAValue();
    	
    	switch(opusApplication){
    		case OpusWrapper.OPUS_APPLICATION_AUDIO:
    			rbAppAudio.setChecked(true);
    			break;
    		case OpusWrapper.OPUS_APPLICATION_VOIP:
    			rbAppVoIP.setChecked(true);
    			break;
    		case OpusWrapper.OPUS_APPLICATION_RESTRICTED_LOWDELAY:
    			rbAppRistrictLowDelay.setChecked(true);
    			break;
    	}
    }
    
    private void initSignalUIValue(){
    	int opusSignal = mOpusConfig.getOSTValue();
    	switch(opusSignal){
    		case OpusWrapper.OPUS_SIGNAL_TYPE_AUTO:
    			rbSignalAuto.setChecked(true);
    			break;
    		case OpusWrapper.OPUS_SIGNAL_TYPE_MUSIC:
    			rbSignalMusic.setChecked(true);
    			break;
    		case OpusWrapper.OPUS_SIGNAL_TYPE_VOICE:
    			rbSignalVoice.setChecked(true);
    			break;
    	}
    }
    
    private void initBandwidthValue(){
    	int opusBandwidth = mOpusConfig.getOBValue();
    	switch(opusBandwidth){
    		case OpusWrapper.OPUS_BANDWIDTH_FULLBAND:
    			rbBandwidthFull.setChecked(true);
    			break;
    		case OpusWrapper.OPUS_BANDWIDTH_MEDIUMBAND:
    			rbBandwidthMedium.setChecked(true);
    			break;
    		case OpusWrapper.OPUS_BANDWIDTH_NARROWBAND:
    			rbBandwidthNarrow.setChecked(true);
    			break;
    		case OpusWrapper.OPUS_BANDWIDTH_SUPERWIDEBAND:
    			rbBandwidthSuper.setChecked(true);
    			break;
    		case OpusWrapper.OPUS_BANDWIDTH_WIDEBAND:
    			rbBandwidthWide.setChecked(true);
    			break;
    	}
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
    private void setOpusBandwidth(int checkedId){
    	switch(checkedId){
			case R.id.rbBandWidthNarrow:
				mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_NARROWBAND;
				break;
			case R.id.rbBandWidthMedium:
				mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_MEDIUMBAND;
				break;
			case R.id.rbBandWidthWide:
				mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_WIDEBAND;
				break;
			case R.id.rbBandWidthSuperWide:
				mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_SUPERWIDEBAND;
				break;
			case R.id.rbBandWidthFull:
				mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_FULLBAND;
				break;
		}
		mOpusConfig.setOBValue(mOpusBandwidth);
    }
    
    private void setOpusSignal(int checkedId){
    	switch(checkedId){
			case R.id.rbSignalAuto:
				mOpusSignal = OpusWrapper.OPUS_SIGNAL_TYPE_AUTO;
				break;
			case R.id.rbSignalVoice:
				mOpusSignal = OpusWrapper.OPUS_SIGNAL_TYPE_VOICE;
				break;
			case R.id.rbSignalMusic:
				mOpusSignal = OpusWrapper.OPUS_SIGNAL_TYPE_MUSIC;
				break;
    	}
    	mOpusConfig.setOSTValue(mOpusSignal);
    }
    
    private void setOpusApplication(int checkedId){
    	switch(checkedId){
    		case R.id.rbAppAudio:
    			mOpusApplication = OpusWrapper.OPUS_APPLICATION_AUDIO;
    			break;
    		case R.id.rbAppVoIP:
    			mOpusApplication = OpusWrapper.OPUS_APPLICATION_VOIP;
    			break;
    		case R.id.rbAppRistrictLowDelay:
    			mOpusApplication = OpusWrapper.OPUS_APPLICATION_RESTRICTED_LOWDELAY;
    			break;
    	}
    	mOpusConfig.setOAValue(mOpusApplication);
    }

}

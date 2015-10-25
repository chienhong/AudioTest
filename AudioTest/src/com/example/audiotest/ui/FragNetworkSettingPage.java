package com.example.audiotest.ui;

import com.example.audiotest.config.UDPTestConfig;
import com.example.audiotest.R;
import com.example.audiotest.R.layout;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class FragNetworkSettingPage extends Fragment {

	private static final String LOG_TAG = "FragNetworkSettingPage";
	
	private EditText etTargetIPText = null;
	private EditText etUDPListenPort = null;
	private EditText etUDPTargetPort = null;
	
	//private 
	private UDPTestConfig mUDPTestConfig = null;
	
    public FragNetworkSettingPage() {
    	mUDPTestConfig = UDPTestConfig.GetInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_network_setting_page, container, false);
        
        initView(rootView);
        
        return rootView;
    }
    
    private void initView(View rootView){
    	
    	etTargetIPText = (EditText) rootView.findViewById(R.id.etTargetIPText);
    	etTargetIPText.addTextChangedListener(new TextWatcher(){

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
					String strIPAddr = s.toString();
					mUDPTestConfig.setTargetIPAddress(strIPAddr);
				}
				
			}
    	});
    	String strTargetIP = mUDPTestConfig.getTargetIPAddress();
    	etTargetIPText.setText(strTargetIP);
    	
    	etUDPListenPort = (EditText) rootView.findViewById(R.id.etUDPListenPort);
    	etUDPListenPort.addTextChangedListener(new TextWatcher(){

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
				
			}
    		
    	});
    	String strUDPListenPort = String.valueOf(mUDPTestConfig.getUDPListenPort());
    	etUDPListenPort.setText(strUDPListenPort);
    	
    	etUDPTargetPort = (EditText) rootView.findViewById(R.id.etUDPTargetPort);
    	etUDPTargetPort.addTextChangedListener(new TextWatcher(){

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
				
			}
    		
    	});
    	String strUDPTargetPort = String.valueOf(mUDPTestConfig.getUDPTargetPort());
    	etUDPTargetPort.setText(strUDPTargetPort);
    	
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
}

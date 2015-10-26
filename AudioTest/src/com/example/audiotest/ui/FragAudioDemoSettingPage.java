package com.example.audiotest.ui;

import java.util.concurrent.atomic.AtomicBoolean;

import com.example.audiotest.R;
import com.example.audiotest.config.AudioTestConfig;
import com.example.audiotest.config.UDPTestConfig;
import com.example.audiotest.module.AudioConfig;
import com.example.audiotest.module.AudioProcessor;
import com.example.audiotest.module.OpusConfig;
import com.example.audiotest.module.WaveReader;
import com.example.audiotest.mp3.MP3Reader;
import com.example.audiotest.network.IUDPReceiveInterface;
import com.example.audiotest.network.UDPReceiver;
import com.example.audiotest.network.UDPSender;
import com.example.audiotest.module.*;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class FragAudioDemoSettingPage extends Fragment implements IUDPReceiveInterface, AudioProcessor.IAudioProcessorCallback {

	private static final String LOG_TAG = "FragAudioBasicSettingPage";
	
	//UI Elements
	private Button btnAudioStart = null;
	private Button btnAudioStop = null;
	private CheckBox cbMP3Source = null;
	private static AtomicBoolean mIsRunning = new AtomicBoolean();
	private AudioProcessor mAudioProcessor = null;
	private RadioGroup rgAudioTestMode = null;
	private RadioButton rbLocalLoop = null;
	private RadioButton rbLocalUDP = null;
	private CheckBox cbEnableAECM = null;
	private CheckBox cbEnableVAD = null;
	private CheckBox cbEnableNS = null;
	private CheckBox cbEnableAGC = null;
	
	//UI Elements for Network Settings
	private EditText etTargetIPText = null;
	private EditText etUDPListenPort = null;
	private EditText etUDPTargetPort = null;
	
	//UDP Test
	private UDPSender mUDPSender = null;
	private UDPReceiver mUDPReceiver = null;
	
	//MP3 Source Reader
	private MP3Reader mMP3Reader = null;
	
	//Audio Config
	private AudioConfig mAudioConfig = null;
	private OpusConfig mOpusConfig = null;
	private AudioTestConfig mAudioTestConfig = null;
	
	private int mTestMode = AudioTestConfig.AUDIO_TEST_LOCAL_LOOP;
	private UDPTestConfig mUDPTestConfig = null;
	
//	private Spinner spnSampleRate = null;
//	private Spinner spnAudioFrameSize = null;	
//	private ArrayAdapter<String> listSampleRateAdapter;
//	private ArrayAdapter<String> listAudioFrameSizeAdapter;
//	private String[] listSampleRate = new String[] { "8000", "12000", "16000", "24000", "48000" };
//	private String[] listAudioFrameSize = new String[] { "20", "40", "60" };
	
	public static Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) { 
        	switch(msg.what) {
	        	default:
	        		break;
        	}
        }
    };
    public FragAudioDemoSettingPage() {
    	mAudioConfig = AudioConfig.GetInstance();
    	mAudioConfig.setEnableTestMode(false);
    	mAudioConfig.setDropModeConfig(false);
    	mAudioConfig.setNSAutoDB(false);
    	mAudioConfig.setInitAGCConfig(false);
    	
    	mAudioTestConfig = AudioTestConfig.GetInstance();
    	mUDPTestConfig = UDPTestConfig.GetInstance();
    	
    	mOpusConfig = OpusConfig.GetInstance();
        mOpusConfig.setAutoComplexity(false);	//disable automatically opus complexity by CPU speed.

    	mAudioConfig.setAGCTargetLvDbfs(3);	//AGC Default: 3
    	mAudioConfig.setAGCCompressionGaindB(9);	//AGC Default: 9
        mIsRunning.set(false);
    	
    	//init opus test settings:
    	mAudioConfig.setAECMConfig(false);
    	mAudioConfig.setEnableAGC(true);
    	mAudioConfig.setEnableNS(true);
    	mAudioConfig.setEnableVAD(false);
    	mAudioConfig.setCalculateDB(true);
    	
//    	mUDPListen = false;	//UDP Listen default value    	
    	AudioConfig.RECORDER_BASE_SAMPLERATE = 16000;
    	AudioConfig.RECORDER_CUR_SAMPLERATE = 16000;
    	AudioConfig.AUDIOTRACK_BASE_SAMPLERATE = 16000;
    	AudioConfig.AUDIOTRACK_CUR_SAMPLERATE = 16000;
    	
    	mOpusConfig.setBitrate(96000);
    	mOpusConfig.setOCValue(5);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_audio_demo, container, false);
        
        mAudioConfig = AudioConfig.GetInstance();
        
        initView(rootView);
        initAudioTestModeUI(rootView);
        initNetworkSettingView(rootView);
        initAudioControlUI(rootView);
        
        return rootView;
    }
    
    private void initAudioControlUI(View rootView)
    {
    	cbEnableAECM = (CheckBox) rootView.findViewById(R.id.cbEnableAEC);
    	cbEnableAECM.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean bEnable) {
				// TODO Auto-generated method stub
				mAudioConfig.setAECMConfig(bEnable);
			}
    		
    	});
    	cbEnableAECM.setChecked(mAudioConfig.getAECMConfig());
    	
    	cbEnableVAD = (CheckBox) rootView.findViewById(R.id.cbEnableVAD);
    	cbEnableVAD.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableVAD(isChecked);
			}
    		
    	});
    	cbEnableVAD.setChecked(mAudioConfig.getEnableVAD());
    	
    	cbEnableAGC = (CheckBox) rootView.findViewById(R.id.cbEnableAGC);
    	cbEnableAGC.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableAGC(isChecked);
			}
    		
    	});
    	//cbEnableAGC.setChecked(mAudioConfig.getAdjustGain());
    	cbEnableAGC.setChecked(mAudioConfig.getEnableAGC());
    	
    	cbEnableNS = (CheckBox) rootView.findViewById(R.id.cbEnableNS);
    	cbEnableNS.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableNS(isChecked);
			}
    		
    	});
    	cbEnableNS.setChecked(mAudioConfig.getEnableNS());
    	
    }
    
	public String intToIp(int i) {
			
			return (i & 0xFF) + "." +
					((i >> 8) & 0xFF) + "." +
					((i >> 16) & 0xFF) + "." + 
					((i >> 24) & 0xFF);
	 	}
	
    private void initNetworkSettingView(View rootView){
        
    	WifiManager wifiManager = (WifiManager) getActivity().getSystemService(Context.WIFI_SERVICE);
    	WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    	int ipAddress = wifiInfo.getIpAddress();
    	String ip = intToIp(ipAddress);
    	Log.d(LOG_TAG, String.format("Local ip=%s", ip));
    	mUDPTestConfig.setTargetIPAddress(ip);
    	
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
    
    private void initAudioTestModeUI(View rootView){
    	
    	rgAudioTestMode = (RadioGroup) rootView.findViewById(R.id.rgAudioTestMode);
    	rgAudioTestMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				switch(checkedId){
					case R.id.rbLocalLoop:
						mAudioTestConfig.setAudioTestMode(AudioTestConfig.AUDIO_TEST_LOCAL_LOOP);
						break;
					case R.id.rbLocalUDP:
						mAudioTestConfig.setAudioTestMode(AudioTestConfig.AUDIO_TEST_LOCAL_UDP);
						break;
				}
			}
    		
    	});
    	
    	rbLocalLoop = (RadioButton) rootView.findViewById(R.id.rbLocalLoop);
    	
    	rbLocalUDP = (RadioButton) rootView.findViewById(R.id.rbLocalUDP);
    	
    	switch(mAudioTestConfig.getAudioTestMode()){
    		case AudioTestConfig.AUDIO_TEST_LOCAL_LOOP:
    			rbLocalLoop.setChecked(true);
    			break;
    		case AudioTestConfig.AUDIO_TEST_LOCAL_UDP:
    			rbLocalUDP.setChecked(true);
    			break;
    	}
    }
    private void initView(View rootView){
    	
    	btnAudioStart = (Button) rootView.findViewById(R.id.btnAudioStart);
    	btnAudioStart.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG, "Start audio streamer.");

				if(mIsRunning.get()){
					return;
				}
				
				if(AudioTestConfig.AUDIO_TEST_LOCAL_UDP == mAudioTestConfig.getAudioTestMode()){
					if(null != mUDPReceiver){
						mUDPReceiver.stop();
						mUDPReceiver = null;
					}
					startUDPReceiver();
					
					if(null != mUDPSender){
						mUDPSender.stop();
						mUDPSender = null;
					}
					
		    		mUDPSender = new UDPSender(mUDPTestConfig.getTargetIPAddress(), mUDPTestConfig.getUDPTargetPort());
		    		Log.d(LOG_TAG, String.format("Create new UDP Sender success: %s", mUDPTestConfig.getTargetIPAddress()));
				}
				
				mAudioConfig.setAdjustGainLevel(1);
				if(mAudioConfig.getCalculateDB()){
					mAudioConfig.setAGCCompressionGaindB(mAudioConfig.getAGCDefaultCompressionGaindB());
				}
				
				mIsRunning.set(true);
				if(mAudioConfig.getEnableMP3() || mAudioConfig.getEnableVoice()){
					
					String mp3Source = "/Music/sample16k.mp3";
					String fileAtPath = Environment.getExternalStorageDirectory().getAbsolutePath() + mp3Source;
					int audioBufferSize = (mAudioConfig.getAudioSampleRate()*mAudioConfig.getAudioFrameSize()*2)/1000;
					mMP3Reader = new MP3Reader(fileAtPath, audioBufferSize);
					mMP3Reader.startMP3Reader();
				}
				
				initAudioProcessor();
				
				if(null != mAudioProcessor){
					mAudioConfig.setAECMConfig(cbEnableAECM.isChecked());
					mAudioProcessor.startAudioProcessor();

				}
	    		btnAudioStart.setEnabled(false);
	    		btnAudioStop.setEnabled(true);
			}
    		
    	});
    	
    	btnAudioStop = (Button) rootView.findViewById(R.id.btnAudioStop);
    	btnAudioStop.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.d(LOG_TAG, "Stop audio streamer.");
				mIsRunning.set(false);
				//if(null != mUDPReceiver){
				//	mUDPReceiver.stop();
				//	mUDPReceiver = null;
				//}
				
				if(null != mUDPSender){
					mUDPSender.stop();
					mUDPSender = null;
				}
				
				if(null != mAudioProcessor){
					mAudioProcessor.stopAudioProcessor();
					mAudioProcessor = null;
				}
				btnAudioStart.setEnabled(true);
				btnAudioStop.setEnabled(false);
			}
    		
    	});
    	
    	if(mIsRunning.get()){
    		btnAudioStart.setEnabled(false);
    		btnAudioStop.setEnabled(true);
    	}
    	else{
    		btnAudioStart.setEnabled(true);
    		btnAudioStop.setEnabled(false);
    	}
    	
    }

    public void startUDPReceiver(){
		mUDPReceiver = new UDPReceiver(mUDPTestConfig.getUDPListenPort(), UDPTestConfig.mTestMode, this);
		mUDPReceiver.start(mUIHandler);
	}
    
    private void initAudioProcessor(){
    	if(null != mAudioProcessor){
    		mAudioProcessor.stopAudioProcessor();
    		mAudioProcessor = null;
    	}
    	mTestMode = mAudioTestConfig.getAudioTestMode();
    	//AudioManager audioMgr = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
    	
    	if(mAudioConfig.getEnableMP3() || mAudioConfig.getEnableVoice() || mAudioConfig.getEnableWave()){
    		mAudioProcessor = new AudioProcessor("AudioTest", this, AudioConfig.AUDIO_MODE_MP3, getActivity());
    	}
    	else if(AudioTestConfig.AUDIO_TEST_LOCAL_LOOP == mTestMode){
    		mAudioProcessor = new AudioProcessor("AudioTest", this, AudioConfig.AUDIO_MODE_LOCAL, getActivity());
    	}
    	else if(AudioTestConfig.AUDIO_TEST_LOCAL_UDP == mTestMode){
    		mAudioProcessor = new AudioProcessor("AudioTest", this, AudioConfig.AUDIO_MODE_VOIP, getActivity());
    	}
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }
    
    /**
     * IUDPReceiveInterface implements start
     */
	@Override
	public void RecvData(byte[] pData) {
		// TODO Auto-generated method stub
		//Log.d(LOG_TAG, String.format("RecvData length=%d", pData.length));
		
		if(null != mAudioProcessor){
			mAudioProcessor.processData(pData);
		}
	}
	/**
	 * IUDPReceiverInterface implements end
	 */
	
	/**
	 * AudioProcessor.IAudioProcessorCallback implements start
	 */
	@Override
	public void SendData(byte[] pData) {
		// TODO Auto-generated method stub
		if(AudioTestConfig.AUDIO_TEST_LOCAL_LOOP == mTestMode){
			//mAudioProcessor.processData(pData);
			RecvData(pData);
		}
		else if(null != mUDPSender){
			//mUDPSender.sendData(pData);
			mUDPSender.sendAudioData(pData);
		}
	}
	
	@Override
	public short[] ObtainData(){
		short[] data = null;
		if(mAudioConfig.getEnableMP3() || mAudioConfig.getEnableVoice()){
			if(null != mMP3Reader){
				//AVPacket packet = new AVPacket('A', seqNo, 0, pData);

				data = mMP3Reader.getMP3Data();
				if(null != data){
					//Log.d(LOG_TAG, String.format("Obtain Data from MP3: data length=%d", data.length));
				}
				else{
					Log.d(LOG_TAG, String.format("Obtain Data from MP3: data is null"));
				}
			}

		}
//		else if(mAudioConfig.getEnableWave()){
//			if(null != mWaveReader){
//				data = mWaveReader.getWaveData();
//				if(null != data){
//					//Log.d(LOG_TAG, String.format("Obtain Data from MP3: data length=%d", data.length));
//				}
//				else{
//					Log.d(LOG_TAG, String.format("Obtain Data from Wave: data is null"));
//				}
//			}
//		}
		return data;
	}
	/**
	 * AudioProcessor.IAudioProcessorCallback implements end
	 */
	
}

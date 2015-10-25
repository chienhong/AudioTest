package com.example.audiotest.ui;


import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.audiofx.Equalizer;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.audiotest.config.AudioTestConfig;
import com.example.audiotest.config.UDPTestConfig;
import com.example.audiotest.module.AGCWrapper;
import com.example.audiotest.module.AudioConfig;
import com.example.audiotest.module.AudioProcessor;
import com.example.audiotest.module.NSWrapper;
import com.example.audiotest.module.OpusConfig;
import com.example.audiotest.module.OpusWrapper;
import com.example.audiotest.module.WaveReader;
import com.example.audiotest.mp3.MP3Reader;
import com.example.audiotest.network.IUDPReceiveInterface;
import com.example.audiotest.network.UDPReceiver;
import com.example.audiotest.network.UDPReceiverDelayTime;
import com.example.audiotest.network.UDPSender;
import com.example.audiotest.R;
import com.example.audiotest.R.id;
import com.example.audiotest.R.layout;



public class FragAudioTestPage extends Fragment implements IUDPReceiveInterface, AudioProcessor.IAudioProcessorCallback {

	private static final String LOG_TAG = "FragAudioTestPage";
	
	private AudioManager mAudioManager = null;
	
	//UI Elements
	private CheckBox cbAudioTestMode = null;
	private Button btnAudioStart = null;
	private Button btnAudioStop = null;
	private RadioGroup rgAudioTestMode = null;
	private RadioButton rbLocalLoop = null;
	private RadioButton rbLocalUDP = null;
	private CheckBox cbUDPListen = null;
	private boolean mUDPListen = false;
	private CheckBox cbMP3Source = null;
	private CheckBox cbMP3Voice = null;
	private TextView tvVersion = null;
	private CheckBox cbAutoRename = null;
	private CheckBox cbOpusRename = null;
	private CheckBox cbAudioEffectRename = null;
	private CheckBox cbWaveFile = null;
	private CheckBox cbAGCRename = null;
	private CheckBox cbAECRename = null;
	private CheckBox cbAECTest = null;
	private CheckBox cbNSRename = null;
	private CheckBox cbPacketLostRename = null;
//	private CheckBox cbSendDelayProfile = null;
	private CheckBox cbRecvDelayProfile = null;
	private CheckBox cbRecvSeqNoProfile = null;
	private CheckBox cbSaveRecvPacketTime = null;
	private CheckBox cbSaveRecvSeqNo = null;
	private Spinner spnWaveSource = null;
	private ArrayAdapter<String> listWaveSourceAdapter;
	private String[] listWaveSource = new String[] { "/Music/sample8k.wav", "/Music/sample12k.wav" , "/Music/sample16k.wav", "/Music/sample24k.wav", "/Music/sample48k.wav" };
	private Spinner spnMP3Source = null;
	private ArrayAdapter<String> listMP3SourceAdapter;
	private String[] listMP3Source = new String[] { "/Music/sample8k.mp3", "/Music/sample12k.mp3" , "/Music/sample16k.mp3", "/Music/sample24k.mp3", "/Music/sample48k.mp3" };
	
	//UI Elements for Performance Counter
	private TextView tvRecvQueueValue = null;
	private TextView tvEstimateDelayTimeValue = null;
	
	//UI Elements for Audio Control
	private CheckBox cbEnableAECM = null;
	private CheckBox cbEnableDropMode = null;
	private CheckBox cbEnableDynPlayRate = null;
	private CheckBox cbEnableAudioSave = null;
	private EditText etAudioDropRate = null;
	private CheckBox cbSaveVBRDataSize = null;
	private CheckBox cbAudioTrackPlayDelay = null;
	private EditText etAudioTrackDelayTime = null;
	private CheckBox cbEnableQueueVoice = null;
	private EditText etVoiceQueueSize = null;
	private CheckBox cbDropNonVoiceData = null;
	private CheckBox cbRecorderFadeIn = null;
	private CheckBox cbWriteSilentData = null;
	private CheckBox cbPauseAudioTrack = null;
	private CheckBox cbOpusSilentData = null;
	private CheckBox cbDuplicateData = null;
	private CheckBox cbEnableGenData = null;
	private CheckBox cbGenPairData = null;
	private CheckBox cbGenerateSin = null;
	private CheckBox cbGenFadeInOut = null;
	private Spinner spnSinTone = null;
	private ArrayAdapter<String> listSinToneAdapter;
	private String[] listSinTone = new String[] {  "10", "20", "40", "110", "220", "440", "660", "880", "1000", "2000", "5000", "10000", "20000" };
	private CheckBox cbCountAmplitudeDiff = null;
	
	private Button btnStrongRate = null;
	private CheckBox cbEnableNS = null;
	private CheckBox cbEnableEqualizer = null;
	private CheckBox cbEnableAGC = null;
	private CheckBox cbAdjustGain = null;
	private Button btnAdjustGain = null;
	private EditText etAGCMaxCompressiondB = null;
	private EditText etDBMaxThreshold = null;
	private EditText etDBMinThreshold = null;
	private CheckBox cbCalculateDB = null;
	private CheckBox cbSaveDB = null;
	private CheckBox cbDuplicateAudioData = null;
	private CheckBox cbEnableVAD = null;
	private CheckBox cbSendIfVAD = null;
	private CheckBox cbDisorderPlay = null;
	
	//Packet Lost Rename
	private CheckBox cbPacketLostTCP = null;
	private CheckBox cbPacketLostUDPAck = null;
	private CheckBox cbPacketLostUDPNoAck = null;
	private Spinner spnPacketLostRate = null;
	private ArrayAdapter<String> listPacketLostRateAdapter;
	private String[] listPacketLostRate = new String[] { "0", "2", "5", "10", "15", "20" };
	private CheckBox cbPacketLostRetransmit = null;
	private Spinner spnPacketLostRetransmit = null;
	private ArrayAdapter<String> listPacketLostRetransmitAdapter;
	private String[] listPacketLostRetransmit = new String[] { "1", "2" };
	private Spinner spnPacketLostRetransmitInterval = null;
	private ArrayAdapter<String> listPacketLostRetransmitIntervalAdapter;
	private String[] listPacketLostRetransmitInterval = new String[] { "10", "20", "40", "80", "120" };
	
	//UI Elements for Audio Basic Settings
	//private EditText etDynPlayRateRatio = null;
	private Spinner spnSampleRate = null;
	private Spinner spnAudioFrameSize = null;
	private EditText etComposedFrameCount = null;	
	private ArrayAdapter<String> listSampleRateAdapter;
	private ArrayAdapter<String> listAudioFrameSizeAdapter;
	private String[] listSampleRate = new String[] { "8000", "12000", "16000", "24000", "48000" };
	private String[] listAudioFrameSize = new String[] { "5", "10", "20", "40", "60" };
	private Spinner spnDynSpeedUp = null;
	private ArrayAdapter<String> listDynSpeedUpAdapter;
	private String[] listDynSpeedUp = new String[] { "3", "5", "10", "20", "30", "40", "50", "60", "70", "80", "90", "100" };
	
	//UI Elements for Audio Control Settings
	private RadioGroup rgAGCMode = null;
	private RadioButton rbAGCModeUnchanged = null;
	private RadioButton rbAGCModeAdaptiveAnalog = null;
	private RadioButton rbAGCModeAdaptiveDigital = null;
	private RadioButton rbAGCModeFixedDigital = null;
	private EditText etAGCTargetLvDbfs = null;
	private EditText etAGCCompressionGaindB = null;
	private CheckBox cbAGCAddFarEnd = null;
	private CheckBox cbAGCHasEcho = null;
	private Spinner spnAGCProcSampleTime = null;
	private ArrayAdapter<String> listAGCProcSampleTimeAdapter;
	private String[] listAGCProcSampleTime = new String[] { "10", "20" };
	private RadioGroup rgNSMode = null;
	private RadioButton rbNSModeMild = null;
	private RadioButton rbNSModeMedium = null;
	private RadioButton rbNSModeAggressive = null;
	private RadioGroup rgNSSampleType = null;
	private RadioButton rbNSSampleWhite = null;
	private RadioButton rbNSSamplePink = null;
	private RadioButton rbNSSampleBrownian = null;
	//AEC Control
	private Spinner spnAECDelayTimeBias = null;
	private ArrayAdapter<String> listAECDelayTimeAdapter;
	private String[] listAECDelayTimeBias = new String[] { "-40", "-30", "-20", "-10", "0", "10", "20", "30", "40" };
	private Spinner spnAECProcessSampleCount = null;
	private ArrayAdapter<String> listAECProcessSampleAdapter;
	private String[] listAECProcessSampleCount = new String[] { "80", "160" };
	//Equalizer Config
	private TextView tvEquBand0 = null;
	private TextView tvEquBand1 = null;
	private TextView tvEquBand2 = null;
	private TextView tvEquBand3 = null;
	private TextView tvEquBand4 = null;
	private SeekBar sbEquBand0 = null;
	private SeekBar sbEquBand1 = null;
	private SeekBar sbEquBand2 = null;
	private SeekBar sbEquBand3 = null;
	private SeekBar sbEquBand4 = null;
	private short mEquMaxGainLevel = 0;
	private short mEquMinGainLevel = 0;
	private short mEquGainLevelRange = 0;
	
	//UI Elements for Opus Basic Settings
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
	private String[] listOpusBitrate = new String[] { "8000", "12000", "16000", "24000", "48000", "64000", "96000", "128000", "256000", "510000" };
	private EditText etOpusComplexity = null;
	private CheckBox cbOpusEnableDTX = null;
	private CheckBox cbOpusInbandFEC = null;
	private CheckBox cbOpusEnablePLC = null;
	private CheckBox cbOpusAddSilent = null;
	private Spinner spnOpusPacketLostPerc = null;
	private ArrayAdapter<String> listOpusPacketLostPercAdapter = null;
	private String[] listOpusPacketLostPerc = new String[] { "0", "1", "2", "3", "5", "8", "10", "12", "15", "20" };
	
	//UI Elements for Network Control
	private EditText etUDPSendDelayRate = null;
	private EditText etUDPSendDelayTime = null;
	private EditText etUDPRecvDelayRate = null;
	private EditText etUDPRecvDelayTime = null;
	private EditText etSendDelayBias = null;
	private EditText etRecvDelayBias = null;
	private EditText etInterruptTime = null;
	private Button btnSendInterrupt = null;
	private Button btnRecvInterrupt = null;
	private AtomicBoolean mIsSendInterrupt = new AtomicBoolean();
	private AtomicBoolean mIsRecvInterrupt = new AtomicBoolean();
	
	//UI Elements for Network Settings
	private EditText etTargetIPText = null;
	private EditText etUDPListenPort = null;
	private EditText etUDPTargetPort = null;
	
	//Opus Config
	private int mOpusApplication = OpusWrapper.OPUS_APPLICATION_VOIP;
	private int mOpusSignal = OpusWrapper.OPUS_SIGNAL_TYPE_VOICE;
	private int mOpusBandwidth = OpusWrapper.OPUS_BANDWIDTH_MEDIUMBAND;
	
	private Thread perfCounterUpdateThread = null;
	
	private static AtomicBoolean mIsRunning = new AtomicBoolean();
	private AtomicInteger mPacketSeqNo = new AtomicInteger();
	private AudioProcessor mAudioProcessor = null;
	//private boolean mEnableMP3 = false;
	
	//UDP Test
	private UDPSender mUDPSender = null;
	private UDPReceiver mUDPReceiver = null;
	
	//Audio Config
	private AudioConfig mAudioConfig = null;
	private OpusConfig mOpusConfig = null;
	
	//MP3 Source Reader
	private MP3Reader mMP3Reader = null;
	
	//Wave Source Reader
	private WaveReader mWaveReader = null;

	//Send/Recv delay control
	private Random rSendDelayControl = new Random(123456);
	private Random rRecvDelayControl = new Random(456789);
	
	private int mTestMode = AudioTestConfig.AUDIO_TEST_LOCAL_LOOP;
	private UDPTestConfig mUDPTestConfig = null;
	private UDPReceiverDelayTime mUDPRecvDelayTime = null;
	
	private AudioTestConfig mAudioTestConfig = null;
	
	public static Handler mUIHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) { 
        	switch(msg.what) {
	        	default:
	        		break;
        	}
        }
    };
	
    public FragAudioTestPage() {

    	mAudioConfig = AudioConfig.GetInstance();
    	mAudioConfig.setEnableTestMode(false);
    	mAudioConfig.setDropModeConfig(false);
    	mAudioConfig.setNSAutoDB(false);
    	mAudioConfig.setInitAGCConfig(false);
    	
    	mAudioTestConfig = AudioTestConfig.GetInstance();
    	mUDPTestConfig = UDPTestConfig.GetInstance();
    	
    	mOpusConfig = OpusConfig.GetInstance();
    	mOpusApplication = mOpusConfig.getOAValue();
        mOpusSignal = mOpusConfig.getOSTValue();
        mOpusBandwidth = mOpusConfig.getOBValue();
        mOpusConfig.setAutoComplexity(false);	//disable automatically opus complexity by CPU speed.
//      mOpusConfig.setInbandFEC(true);

//    	mAudioConfig.setAGCTargetLvDbfs(3);	//AGC Default: 3
//    	mAudioConfig.setAGCCompressionGaindB(12);	//AGC Default: 9
        mIsRunning.set(false);
    	
    	//init opus test settings:
//    	mAudioConfig.setAECMConfig(false);
//    	mAudioConfig.setEnableQueueVoice(true);
//    	mAudioConfig.setDropNonVoiceData(false);
//    	mAudioConfig.setEnableAGC(false);
//    	mAudioConfig.setCalculateDB(false);
//    	mAudioConfig.setAdjustGain(false);
//    	mAudioConfig.setOpusRename(true);
//    	mAudioConfig.setAutoRename(true);
//    	mAudioConfig.setAudioEffectRename(true);
//    	mAudioConfig.setAGCRename(false);
//    	mAudioConfig.setAECRename(true);
//    	mAudioConfig.setNSRename(true);
//    	mAudioConfig.setPacketLostRename(true);
//    	mAudioConfig.setEnableAECTestMode(false);
//    	mAudioConfig.setEnableLogEncData(true);
//    	mAudioConfig.setEnableMP3(true);
//    	mAudioConfig.setEnableVoice(true);
//    	mAudioConfig.setEnableWave(true);
//    	mAudioConfig.setEnableWaveSaver(true);
//    	mAudioConfig.setEnableNS(false);
//    	mAudioConfig.setDropModeConfig(true);
//    	mAudioConfig.setEnableVAD(false);
//    	mAudioConfig.setSendIfVAD(true);
//    	mAudioConfig.setEnableEqualizer(false);
//    	mAudioConfig.setGenSinData(true);
//    	mAudioConfig.setGenFadeInOut(true);
//    	mAudioConfig.setDropModeConfig(true);
//    	mAudioConfig.setEnableGenData(true);
//    	mAudioConfig.setRecvDelayProfile(true);
//    	mAudioConfig.setRecvSeqNoProfile(true);
//    	mAudioConfig.setEnableRecvPacketTimeLog(true);
    	
    	mUDPListen = false;	//UDP Listen default value
    	
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
        View rootView = inflater.inflate(R.layout.frag_audio_test_page, container, false);
        
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        
        initView(rootView);

        initAudioControlUI(rootView);
        initGenVoiceControlUI(rootView);
        initNetworkControlUI(rootView);
        initBasicSettingView(rootView);
        initOpusSettingView(rootView);
        initNetworkSettingView(rootView);
        initAudioControlSettings(rootView);
        initPacketLostSettings(rootView);
        
        if(mUDPListen){
			if(null != mUDPReceiver){
				mUDPReceiver.stop();
				mUDPReceiver = null;
			}
			startUDPReceiver();
    	}
        
        return rootView;
    }
    
    private void initPacketLostSettings(View rootView){
    	
    	cbPacketLostTCP = (CheckBox) rootView.findViewById(R.id.cbPacketLostTCP);
    	cbPacketLostTCP.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setPacketLostTCP(isChecked);
			}
    		
    	});
    	cbPacketLostTCP.setChecked(mAudioConfig.getPacketLostTCP());
    	
    	cbPacketLostUDPAck = (CheckBox) rootView.findViewById(R.id.cbPacketLostUDPAck);
    	cbPacketLostUDPAck.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setPacketLostUDPAck(isChecked);
			}
    		
    	});
    	cbPacketLostUDPAck.setChecked(mAudioConfig.getPacketLostUDPAck());
    	
    	cbPacketLostUDPNoAck = (CheckBox) rootView.findViewById(R.id.cbPacketLostUDPNoAck);
    	cbPacketLostUDPNoAck.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setPacketLostUDPNoAck(isChecked);
			}
    		
    	});
    	cbPacketLostUDPNoAck.setChecked(mAudioConfig.getPacketLostUDPNoAck());
    	
    	spnPacketLostRate = (Spinner) rootView.findViewById(R.id.spnPacketLostRate);
    	listPacketLostRateAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listPacketLostRate);
    	spnPacketLostRate.setAdapter(listPacketLostRateAdapter);
    	spnPacketLostRate.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int lostRate = Integer.parseInt(listPacketLostRate[position]);
				mAudioConfig.setPacketLostRate(lostRate);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	String strPacketLostRate = String.valueOf(mAudioConfig.getPacketLostRate());
    	int posPacketLostRate = listPacketLostRateAdapter.getPosition(strPacketLostRate);
    	spnPacketLostRate.setSelection(posPacketLostRate);
    	
    	cbPacketLostRetransmit = (CheckBox) rootView.findViewById(R.id.cbPacketLostRetransmit);
    	cbPacketLostRetransmit.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setPacketLostDuplicate(isChecked);
			}
    		
    	});
    	cbPacketLostRetransmit.setChecked(mAudioConfig.getPacketLostDuplicate());
    	
    	spnPacketLostRetransmit = (Spinner) rootView.findViewById(R.id.spnPacketLostRetransmit);
    	listPacketLostRetransmitAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listPacketLostRetransmit);
    	spnPacketLostRetransmit.setAdapter(listPacketLostRetransmitAdapter);
    	spnPacketLostRetransmit.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int retransmitCount = Integer.parseInt(listPacketLostRetransmit[position]);
				Log.d(LOG_TAG, String.format("PacketLostDuplicate Count=%d", retransmitCount));
				mAudioConfig.setRetransmitCount(retransmitCount);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	String strPacketLostDuplicate = String.valueOf(mAudioConfig.getRetransmitCount());
    	int posPacketLostDuplicateCount = listPacketLostRetransmitAdapter.getPosition(strPacketLostDuplicate);
    	spnPacketLostRetransmit.setSelection(posPacketLostDuplicateCount);
    	
    	spnPacketLostRetransmitInterval = (Spinner) rootView.findViewById(R.id.spnPacketLostRetransmitInterval);
    	listPacketLostRetransmitIntervalAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listPacketLostRetransmitInterval);
    	spnPacketLostRetransmitInterval.setAdapter(listPacketLostRetransmitIntervalAdapter);
    	spnPacketLostRetransmitInterval.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int interval = Integer.parseInt(listPacketLostRetransmitInterval[position]);
				mAudioConfig.setRetransmitInterval(interval);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	String strPacketLostRetransmitInterval = String.valueOf(mAudioConfig.getRetransmitInterval());
    	int posRetransmitInterval = listPacketLostRetransmitIntervalAdapter.getPosition(strPacketLostRetransmitInterval);
    	spnPacketLostRetransmitInterval.setSelection(posRetransmitInterval);
    	
    }
    
    private void initAudioControlSettings(View rootView){
    	spnAECDelayTimeBias = (Spinner) rootView.findViewById(R.id.spnAECDelayTimeBias);
    	listAECDelayTimeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listAECDelayTimeBias);
    	spnAECDelayTimeBias.setAdapter(listAECDelayTimeAdapter);
    	spnAECDelayTimeBias.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int delayBias = Integer.parseInt(listAECDelayTimeBias[position]);
				Log.d(LOG_TAG, String.format("AEC Delay Time Bias: %d selected", delayBias));
				mAudioConfig.setAECDelayTimeBias(delayBias);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
    		
    	});
    	
    	//init default value of sample rate
    	String strAECDelayTimeBias = String.valueOf(mAudioConfig.getAECDelayTimeBias());
    	int posAECDelayBias = listAECDelayTimeAdapter.getPosition(strAECDelayTimeBias);
    	spnAECDelayTimeBias.setSelection(posAECDelayBias);
    	
    	spnAECProcessSampleCount = (Spinner) rootView.findViewById(R.id.spnAECProcessSampleCount);
    	listAECProcessSampleAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listAECProcessSampleCount);
    	spnAECProcessSampleCount.setAdapter(listAECProcessSampleAdapter);
    	spnAECProcessSampleCount.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int sampleCount = Integer.parseInt(listAECProcessSampleCount[position]);
				Log.d(LOG_TAG, String.format("AEC Delay Time Bias: %d selected", sampleCount));
				mAudioConfig.setAECProcessSampleCount(sampleCount);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
    		
    	});
    	
    	//init default value of sample rate
    	String strAECProcessSampleCount = String.valueOf(mAudioConfig.getAECProcessSampleCount());
    	int posAECProcessSampleCount = listAECProcessSampleAdapter.getPosition(strAECProcessSampleCount);
    	spnAECProcessSampleCount.setSelection(posAECProcessSampleCount);
    	
    	rgAGCMode = (RadioGroup) rootView.findViewById(R.id.rgAGCMode);
    	rgAGCMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				setAGCMode(checkedId);
			}

    	});
    	
    	rbAGCModeUnchanged = (RadioButton) rootView.findViewById(R.id.rbAGCModeUnchanged);
    	rbAGCModeAdaptiveAnalog = (RadioButton) rootView.findViewById(R.id.rbAGCModeAdaptiveAnalog);
    	rbAGCModeAdaptiveDigital = (RadioButton) rootView.findViewById(R.id.rbAGCModeAdaptiveDigital);
    	rbAGCModeFixedDigital = (RadioButton) rootView.findViewById(R.id.rbAGCModeFixedDigital);
    	initAGCModeUI(mAudioConfig.getAGCMode());
    	
    	etAGCTargetLvDbfs = (EditText) rootView.findViewById(R.id.etAGCTargetLvDbfs);
    	etAGCTargetLvDbfs.addTextChangedListener(new TextWatcher(){

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
					int value = Integer.parseInt(s.toString());
					if((value > 0) && (value < 100)){
						//mAudioConfig.setAGCMicMinLevel(value);
						mAudioConfig.setAGCTargetLvDbfs(value);
						Log.d(LOG_TAG, String.format("setAGCTargetLvDbfs = %d", value));
					}
				}
			}
    		
    	});
    	String strTargetLvDbfs = String.valueOf(mAudioConfig.getAGCTargetLvDbfs());
    	etAGCTargetLvDbfs.setText(strTargetLvDbfs);
    	
    	etAGCCompressionGaindB = (EditText) rootView.findViewById(R.id.etAGCCompressionGaindB);
    	etAGCCompressionGaindB.addTextChangedListener(new TextWatcher(){

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
					int value = Integer.parseInt(s.toString());
					if((value > 0) && (value < 99)){
						//mAudioConfig.setAGCMicMaxLevel(value);
						Log.d(LOG_TAG, String.format("setAGCCompressionGaindB=%d", value));
						mAudioConfig.setAGCCompressionGaindB(value);
					}
				}
			}
    		
    	});
    	String strCompressionGaindB = String.valueOf(mAudioConfig.getAGCCompressionGaindB());
    	etAGCCompressionGaindB.setText(strCompressionGaindB);
    	
    	cbAGCAddFarEnd = (CheckBox) rootView.findViewById(R.id.cbAGCAddFarEnd);
    	cbAGCAddFarEnd.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setAGCAddFarEnd(isChecked);
			}
    		
    	});
    	cbAGCAddFarEnd.setChecked(mAudioConfig.getAGCAddFarEnd());
    	
    	cbAGCHasEcho = (CheckBox) rootView.findViewById(R.id.cbAGCHasEcho);
    	cbAGCHasEcho.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setAGCHasEcho(isChecked);
			}
    		
    	});
    	cbAGCHasEcho.setChecked(mAudioConfig.getAGCHasEcho());
    	

    	spnAGCProcSampleTime = (Spinner) rootView.findViewById(R.id.spnAGCProcSampleTime);
    	listAGCProcSampleTimeAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listAGCProcSampleTime);
    	spnAGCProcSampleTime.setAdapter(listAGCProcSampleTimeAdapter);
    	spnAGCProcSampleTime.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int procSampleTime = Integer.parseInt(listAGCProcSampleTime[position]);
				Log.d(LOG_TAG, String.format("AGC Proc Sample Time: %d", procSampleTime));
				mAudioConfig.setAGCProcSampleTime(procSampleTime);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
			}
    		
    	});
    	
    	//init default value of sample rate
    	String strAGCProcSampleTime = String.valueOf(mAudioConfig.getAGCProcSampleTime());
    	int posAGCProcessSampleTime = listAGCProcSampleTimeAdapter.getPosition(strAGCProcSampleTime);
    	spnAGCProcSampleTime.setSelection(posAGCProcessSampleTime);
    	
    	
    	rgNSMode = (RadioGroup) rootView.findViewById(R.id.rgNSMode);
    	rgNSMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				setNSMode(checkedId);
			}
    		
    	});
    	
    	rbNSModeMild = (RadioButton) rootView.findViewById(R.id.rbNSModeMild);
    	rbNSModeMedium = (RadioButton) rootView.findViewById(R.id.rbNSModeMedium);
    	rbNSModeAggressive = (RadioButton) rootView.findViewById(R.id.rbNSModeAggressive);
    	
    	initNSModeUI(mAudioConfig.getNSMode());
    	
    	rgNSSampleType = (RadioGroup) rootView.findViewById(R.id.rgNSSampleType);
    	rgNSSampleType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				setNSSampleType(checkedId);
			}
		});
    	rbNSSampleWhite = (RadioButton) rootView.findViewById(R.id.rbNSSampleWhite);
    	rbNSSamplePink = (RadioButton) rootView.findViewById(R.id.rbNSSamplePink);
    	rbNSSampleBrownian = (RadioButton) rootView.findViewById(R.id.rbNSSampleBrownian);
    	
    	tvEquBand0 = (TextView) rootView.findViewById(R.id.tvEquBand0);
    	sbEquBand0 = (SeekBar) rootView.findViewById(R.id.sbEquBand0);
    	sbEquBand0.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				int gainValue = (progress * mEquGainLevelRange / 100) + mEquMinGainLevel;
				mAudioConfig.setEquBand0GainValue((short)gainValue);
				if(null != mAudioProcessor){
					mAudioProcessor.adjustEquBandLevel((short) 0, (short)gainValue);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	
    	tvEquBand1 = (TextView) rootView.findViewById(R.id.tvEquBand1);
    	sbEquBand1 = (SeekBar) rootView.findViewById(R.id.sbEquBand1);
    	sbEquBand1.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				//Log.d(LOG_TAG, String.format("progress=%d", progress));
				int gainValue = (progress * mEquGainLevelRange / 100) + mEquMinGainLevel;
				mAudioConfig.setEquBand1GainValue((short)gainValue);
				if(null != mAudioProcessor){
					mAudioProcessor.adjustEquBandLevel((short) 1, (short)gainValue);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	
    	tvEquBand2 = (TextView) rootView.findViewById(R.id.tvEquBand2);
    	sbEquBand2 = (SeekBar) rootView.findViewById(R.id.sbEquBand2);
    	sbEquBand2.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				int gainValue = (progress * mEquGainLevelRange / 100) + mEquMinGainLevel;
				mAudioConfig.setEquBand2GainValue((short)gainValue);
				if(null != mAudioProcessor){
					mAudioProcessor.adjustEquBandLevel((short) 2, (short)gainValue);
				}
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	
    	tvEquBand3 = (TextView) rootView.findViewById(R.id.tvEquBand3);
    	sbEquBand3 = (SeekBar) rootView.findViewById(R.id.sbEquBand3);
    	sbEquBand3.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				int gainValue = (progress * mEquGainLevelRange / 100) + mEquMinGainLevel;
				mAudioConfig.setEquBand3GainValue((short)gainValue);
				if(null != mAudioProcessor){
					mAudioProcessor.adjustEquBandLevel((short) 3, (short)gainValue);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	
    	tvEquBand4 = (TextView) rootView.findViewById(R.id.tvEquBand4);
    	sbEquBand4 = (SeekBar) rootView.findViewById(R.id.sbEquBand4);
    	sbEquBand4.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				int gainValue = (progress * mEquGainLevelRange / 100) + mEquMinGainLevel;
				mAudioConfig.setEquBand4GainValue((short)gainValue);
				if(null != mAudioProcessor){
					mAudioProcessor.adjustEquBandLevel((short) 4, (short)gainValue);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	
    	initEqualizerUI();
    }
    
    private void initEqualizerUI(){
    	int mPlayBufferSize = AudioTrack.getMinBufferSize(16000, 2, AudioFormat.ENCODING_PCM_16BIT);
    	AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, 2,
    			AudioFormat.ENCODING_PCM_16BIT, mPlayBufferSize, AudioTrack.MODE_STREAM);
    	Equalizer equalizer = new Equalizer(0, audioTrack.getAudioSessionId());
    	
    	short numBands = equalizer.getNumberOfBands();
		short[] levelRange = equalizer.getBandLevelRange();
		mEquMaxGainLevel = levelRange[1];
		mEquMinGainLevel = levelRange[0];
		mEquGainLevelRange = (short) (mEquMaxGainLevel - mEquMinGainLevel);
		for(int index=0; index < numBands; index++){
			int[] freqRange = equalizer.getBandFreqRange((short) index);
			short gainLevel = equalizer.getBandLevel((short) index);
			initEqualizerTextView(index, freqRange[0], freqRange[1], gainLevel);
		}
		
		equalizer.release();
		audioTrack.release();
    }
    
    private void initEqualizerTextView(int index, int minFreq, int maxFreq, short gainLevel){
    	if(0 == index){
    		tvEquBand0.setText(String.format("Band 0 (%d~%d) (milliHertz):", minFreq, maxFreq));
    		mAudioConfig.setEquBand0GainValue(gainLevel);
    		int percentage = 100 * (gainLevel - mEquMinGainLevel) / mEquGainLevelRange;
    		sbEquBand0.setProgress(percentage);
    	}
    	else if(1 == index){
    		tvEquBand1.setText(String.format("Band 1 (%d~%d) (milliHertz):", minFreq, maxFreq));
    		mAudioConfig.setEquBand1GainValue(gainLevel);
    		int percentage = 100 * (gainLevel - mEquMinGainLevel) / mEquGainLevelRange;
    		sbEquBand1.setProgress(percentage);
    	}
    	else if(2 == index){
    		tvEquBand2.setText(String.format("Band 2 (%d~%d) (milliHertz):", minFreq, maxFreq));
    		mAudioConfig.setEquBand2GainValue(gainLevel);
    		int percentage = 100 * (gainLevel - mEquMinGainLevel) / mEquGainLevelRange;
    		sbEquBand2.setProgress(percentage);
    	}
    	else if(3 == index){
    		tvEquBand3.setText(String.format("Band 3 (%d~%d) (milliHertz):", minFreq, maxFreq));
    		mAudioConfig.setEquBand3GainValue(gainLevel);
    		int percentage = (100 * (gainLevel - mEquMinGainLevel)) / mEquGainLevelRange;
    		sbEquBand3.setProgress(percentage);
    	}
    	else if(4 == index){
    		tvEquBand4.setText(String.format("Band 4 (%d~%d) (milliHertz):", minFreq, maxFreq));
    		mAudioConfig.setEquBand4GainValue(gainLevel);
    		int percentage = (100 * (gainLevel - mEquMinGainLevel)) / mEquGainLevelRange;
    		sbEquBand4.setProgress(percentage);
    	}
    }
    
    private void initEqualizerBandLevel(){
    	if(null != mAudioProcessor){
    		mAudioProcessor.adjustEquBandLevel((short) 0, mAudioConfig.getEquBand0GainValue());
    		mAudioProcessor.adjustEquBandLevel((short) 1, mAudioConfig.getEquBand1GainValue());
    		mAudioProcessor.adjustEquBandLevel((short) 2, mAudioConfig.getEquBand2GainValue());
    		mAudioProcessor.adjustEquBandLevel((short) 3, mAudioConfig.getEquBand3GainValue());
    		mAudioProcessor.adjustEquBandLevel((short) 4, mAudioConfig.getEquBand4GainValue());
    	}
    }
    
    private void initNSModeUI(int nsMode){
    	switch(nsMode){
    		case NSWrapper.NS_MODE_MILD:
    			rbNSModeMild.setChecked(true);
    			break;
    		case NSWrapper.NS_MODE_MEDIUM:
    			rbNSModeMedium.setChecked(true);
    			break;
    		case NSWrapper.NS_MODE_AGGRESSIVE:
    			rbNSModeAggressive.setChecked(true);
    			break;
    	}
    }
    
    private void setNSSampleType(int checkedId){
    	switch(checkedId){
    		case R.id.rbNSSampleWhite:
    			mAudioConfig.setNSSampleType("white");
    			break;
    		case R.id.rbNSSamplePink:
    			mAudioConfig.setNSSampleType("pink");
    			break;
    		case R.id.rbNSSampleBrownian:
    			mAudioConfig.setNSSampleType("brownian");
    			break;
    	}
    }
    
    private void setNSMode(int checkedId){
    	switch(checkedId){
    		case R.id.rbNSModeMild:
    			mAudioConfig.setNSMode(NSWrapper.NS_MODE_MILD);
    			break;
    		case R.id.rbNSModeMedium:
    			mAudioConfig.setNSMode(NSWrapper.NS_MODE_MEDIUM);
    			break;
    		case R.id.rbNSModeAggressive:
    			mAudioConfig.setNSMode(NSWrapper.NS_MODE_AGGRESSIVE);
    			break;
    	}
    	
    }
    
    private void initAGCModeUI(int agcMode){
    	switch(agcMode){
    		case AGCWrapper.AGC_MODE_UNCHANGED:
    			rbAGCModeUnchanged.setChecked(true);
    			break;
    		case AGCWrapper.AGC_MODE_ADAPTIVE_ANALOG:
    			rbAGCModeAdaptiveAnalog.setChecked(true);
    			break;
    		case AGCWrapper.AGC_MODE_ADAPTIVE_DIGITAL:
    			rbAGCModeAdaptiveDigital.setChecked(true);
    			break;
    		case AGCWrapper.AGC_MODE_FIXED_DIGITAL:
    			rbAGCModeFixedDigital.setChecked(true);
    			break;
    	}
    }
    
    private void setAGCMode(int checkedId){
    	switch(checkedId){
    		case R.id.rbAGCModeUnchanged:
    			rbAGCModeUnchanged.setChecked(true);
    			mAudioConfig.setAGCMode(AGCWrapper.AGC_MODE_UNCHANGED);
    			break;
    		case R.id.rbAGCModeAdaptiveAnalog:
    			rbAGCModeAdaptiveAnalog.setChecked(true);
    			mAudioConfig.setAGCMode(AGCWrapper.AGC_MODE_ADAPTIVE_ANALOG);
    			break;
    		case R.id.rbAGCModeAdaptiveDigital:
    			rbAGCModeAdaptiveDigital.setChecked(true);
    			mAudioConfig.setAGCMode(AGCWrapper.AGC_MODE_ADAPTIVE_DIGITAL);
    			break;
    		case R.id.rbAGCModeFixedDigital:
    			rbAGCModeFixedDigital.setChecked(true);
    			mAudioConfig.setAGCMode(AGCWrapper.AGC_MODE_FIXED_DIGITAL);
    			break;
    	}
    }
    
    private void initNetworkControlUI(View rootView){
    	etUDPSendDelayRate = (EditText) rootView.findViewById(R.id.etUDPSenDelayRate);
    	etUDPSendDelayRate.addTextChangedListener(new TextWatcher(){

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
					int delayRate = Integer.parseInt(s.toString());
					mUDPTestConfig.setUDPSendDelayRate(delayRate);
				}
			}
    	});
    	String strSendDelayRate = String.valueOf(mUDPTestConfig.getUDPSendDelayRate());
    	etUDPSendDelayRate.setText(strSendDelayRate);
    	
    	etUDPSendDelayTime = (EditText) rootView.findViewById(R.id.etUDPSendDelayTime);
    	etUDPSendDelayTime.addTextChangedListener(new TextWatcher(){

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
					int delayTime = Integer.parseInt(s.toString());
					mUDPTestConfig.setUDPSendDelayTime(delayTime);
				}
			}
    		
    	});
    	String strSendDelayTime = String.valueOf(mUDPTestConfig.getUDPSendDelayTime());
    	etUDPSendDelayTime.setText(strSendDelayTime);
    	
    	etUDPRecvDelayRate = (EditText) rootView.findViewById(R.id.etUDPRecvDelayRate);
    	etUDPRecvDelayRate.addTextChangedListener(new TextWatcher(){

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
					int delayRate = Integer.parseInt(s.toString());
					mUDPTestConfig.setUDPRecvDelayRate(delayRate);
				}
			}
    		
    	});
    	String strRecvDelayRate = String.valueOf(mUDPTestConfig.getUDPRecvDelayRate());
    	etUDPRecvDelayRate.setText(strRecvDelayRate);
    	
    	etUDPRecvDelayTime = (EditText) rootView.findViewById(R.id.etUDPRecvDelayTime);
    	etUDPRecvDelayTime.addTextChangedListener(new TextWatcher(){

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
					int delayTime = Integer.parseInt(s.toString());
					mUDPTestConfig.setUDPRecvDelayTime(delayTime);
				}
			}
    		
    	});
    	String strRecvDelayTime = String.valueOf(mUDPTestConfig.getUDPRecvDelayTime());
    	etUDPRecvDelayTime.setText(strRecvDelayTime);
    	
    	etSendDelayBias = (EditText) rootView.findViewById(R.id.etSendDelayTimeBiasAdjust);
    	etSendDelayBias.addTextChangedListener(new TextWatcher(){

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
					int delayBias = Integer.parseInt(s.toString());
					if((delayBias >= 0) && (delayBias <= mUDPTestConfig.getUDPSendDelayTime())){
						mUDPTestConfig.setSendDelayBias(delayBias);
					}
				}
			}
    		
    	});
    	String strSendDelayBias = String.valueOf(mUDPTestConfig.getSendDelayBias());
    	etSendDelayBias.setText(strSendDelayBias);
    	
    	etRecvDelayBias = (EditText) rootView.findViewById(R.id.etRecvDelayTimeBiasAdjust);
    	etRecvDelayBias.addTextChangedListener(new TextWatcher(){

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
					int delayBias = Integer.parseInt(s.toString());
					if((delayBias >= 0) && (delayBias <= mUDPTestConfig.getUDPRecvDelayTime())){
						mUDPTestConfig.setRecvDelayBias(delayBias);
					}
				}
			}
    		
    	});
    	String strRecvDelayBias = String.valueOf(mUDPTestConfig.getRecvDelayBias());
    	etRecvDelayBias.setText(strRecvDelayBias);
    	
    	etInterruptTime = (EditText) rootView.findViewById(R.id.etAudioDelayCtlSleepTime);
    	etInterruptTime.addTextChangedListener(new TextWatcher(){

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
					int sleepTime = Integer.parseInt(s.toString());
					if(sleepTime >= 0){
						mUDPTestConfig.setInterruptDelayTime(sleepTime);
					}
				}
			}
    		
    	});
    	String strInterruptTime = String.valueOf(mUDPTestConfig.getInterruptDelayTime());
    	etInterruptTime.setText(strInterruptTime);
    	
    	btnSendInterrupt = (Button) rootView.findViewById(R.id.btnSendInterrupt);
    	btnSendInterrupt.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mIsSendInterrupt.set(true);
			}
    		
    	});
    	
    	btnRecvInterrupt = (Button) rootView.findViewById(R.id.btnRecvInterrupt);
    	btnRecvInterrupt.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mIsRecvInterrupt.set(true);
			}
    		
    	});
    	
    }
    
    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(LOG_TAG, String.format("getLocalIpAddress:",ex.toString()));
        }
        return "";
    }
    
	public String intToIp(int i) {

// 	   return ((i >> 24 ) & 0xFF ) + "." +
// 	               ((i >> 16 ) & 0xFF) + "." +
// 	               ((i >> 8 ) & 0xFF) + "." +
// 	               ( i & 0xFF) ;
		
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
    
    private void initOpusSettingView(View rootView){
    	rgOpusApplication = (RadioGroup) rootView.findViewById(R.id.rgOpusApplication);
    	rgOpusApplication.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

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
    	rgOpusSignal.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

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
    	rgOpusBandwidth.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){

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
    	
    	etOpusComplexity = (EditText) rootView.findViewById(R.id.etOpusComplexity);
    	etOpusComplexity.addTextChangedListener(new TextWatcher(){

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
					int complexity = Integer.parseInt(s.toString());
					if((complexity>=0) && (complexity < 10)){
						mOpusConfig.setOCValue(complexity);
					}
				}
			}
    		
    	});
    	
    	int complexity = mOpusConfig.getOCValue();
    	String strComplexity = String.valueOf(complexity);
    	etOpusComplexity.setText(strComplexity);
    	    	
    	cbOpusEnableDTX = (CheckBox) rootView.findViewById(R.id.cbOpusEnableDTX);
    	cbOpusEnableDTX.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mOpusConfig.setDTXConfig(isChecked);
			}
    		
    	});
    	cbOpusEnableDTX.setChecked(mOpusConfig.getDTXConfig());
    	
    	cbOpusInbandFEC = (CheckBox) rootView.findViewById(R.id.cbOpusInbandFEC);
    	cbOpusInbandFEC.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mOpusConfig.setInbandFEC(isChecked);
			}
    		
    	});
    	cbOpusInbandFEC.setChecked(mOpusConfig.getInbandFEC());
    	
    	cbOpusEnablePLC = (CheckBox) rootView.findViewById(R.id.cbOpusEnablePLC);
    	cbOpusEnablePLC.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mOpusConfig.setEnablePLC(isChecked);
			}
    		
    	});
    	cbOpusEnablePLC.setChecked(mOpusConfig.getEnablePLC());
    	
    	cbOpusAddSilent = (CheckBox) rootView.findViewById(R.id.cbOpusAddSilent);
    	cbOpusAddSilent.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mOpusConfig.setAddSilent(isChecked);
			}
    		
    	});
    	cbOpusAddSilent.setChecked(mOpusConfig.getAddSilent());
    	
    	spnOpusPacketLostPerc = (Spinner) rootView.findViewById(R.id.spnOpusPacketLostPerc);
    	listOpusPacketLostPercAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listOpusPacketLostPerc);
    	spnOpusPacketLostPerc.setAdapter(listOpusPacketLostPercAdapter);
    	
    	spnOpusPacketLostPerc.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
//				int bitrate = Integer.parseInt(listOpusBitrate[position]);
//				Log.d(LOG_TAG, String.format("Opus Bitrate: %d selected", bitrate));
//				mOpusConfig.setBitrate(bitrate);
				int percentage = Integer.parseInt(listOpusPacketLostPerc[position]);
				mOpusConfig.setPacketLostPerc(percentage);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
//    	initBitrateUIValue();
    	initOpusPacketLostPercView();
    	
    }
    
    private void initOpusPacketLostPercView(){
    	String strPacketLostPerc = String.valueOf(mOpusConfig.getPacketLostPerc());
    	int posPacketLostPerc = listOpusPacketLostPercAdapter.getPosition(strPacketLostPerc);
    	spnOpusPacketLostPerc.setSelection(posPacketLostPerc);
    }
    
    private void initBasicSettingView(View rootView){
    	
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
    	
    	spnMP3Source = (Spinner) rootView.findViewById(R.id.spnMP3Source);
    	listMP3SourceAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listMP3Source);
    	spnMP3Source.setAdapter(listMP3SourceAdapter);
    	spnMP3Source.setSelection(2);
//    	etDynPlayRateRatio = (EditText) rootView.findViewById(R.id.etDynPlayRateRatio);
//    	String strPlayRateSpeedupRatio = String.valueOf(mAudioConfig.getPlaybackSpeedupRatio());
//    	etDynPlayRateRatio.setText(strPlayRateSpeedupRatio);
//    	etDynPlayRateRatio.addTextChangedListener(new TextWatcher(){
//
//			@Override
//			public void beforeTextChanged(CharSequence s, int start, int count,
//					int after) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void onTextChanged(CharSequence s, int start, int before,
//					int count) {
//				// TODO Auto-generated method stub
//				
//			}
//
//			@Override
//			public void afterTextChanged(Editable s) {
//				// TODO Auto-generated method stub
//				if((null != s) && (s.length() > 0)){
//					int speedupRatio = Integer.parseInt(s.toString());
//					if(speedupRatio > 1){
//						mAudioConfig.setPlaybackSpeedupRatio(speedupRatio);
//						Log.d(LOG_TAG, String.format("new playback speed up ratio: %d", speedupRatio));
//					}
//				}
//			}
//    		
//    	});
    	
    	spnWaveSource = (Spinner) rootView.findViewById(R.id.spnWaveSource);
    	listWaveSourceAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listWaveSource);
    	spnWaveSource.setAdapter(listWaveSourceAdapter);
    	spnWaveSource.setSelection(2);
    	
    	spnDynSpeedUp = (Spinner) rootView.findViewById(R.id.spnDynSpeedUp);
    	listDynSpeedUpAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listDynSpeedUp);
    	spnDynSpeedUp.setAdapter(listDynSpeedUpAdapter);
    	
    	spnDynSpeedUp.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int speedUpRate = Integer.parseInt(listDynSpeedUp[position]);
				mAudioConfig.setPlaybackSpeedupRatio(speedUpRate);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
     	String strSpeedUp = String.valueOf(mAudioConfig.getPlaybackSpeedupRatio());
    	int posSpeedUpRate = listDynSpeedUpAdapter.getPosition(strSpeedUp);
    	spnDynSpeedUp.setSelection(posSpeedUpRate);
    	
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

    private void startPerfUpdateUIThread(){
    	
    	perfCounterUpdateThread = new Thread(){
    		@Override
    		public void run(){
    			while(mIsRunning.get()){
    		    	getActivity().runOnUiThread(new Runnable() {
    					@Override
    					public void run() {
    						if(null != mAudioProcessor){
    							int curQueueSize = mAudioProcessor.getAudioQueueSize();
    							String strRecvQueue = String.valueOf(curQueueSize);
    							tvRecvQueueValue.setText(strRecvQueue);
    							
    							int estimateDelayTime = (curQueueSize+1) * mAudioConfig.getAudioFrameSize();
    							String strEstimateDelayTime = String.valueOf(estimateDelayTime);
    							tvEstimateDelayTimeValue.setText(strEstimateDelayTime);
    						}
    					}
    		    	});
    		    	
    		    	try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
    			}
    		}
    	};
    	perfCounterUpdateThread.start();
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
					if(null == mUDPReceiver){
						Toast.makeText(getActivity(), "Please start UDP Listen first...", 
				                Toast.LENGTH_SHORT).show();
						return;
					}
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
					int mp3Index = spnMP3Source.getSelectedItemPosition();
					String mp3Source = listMP3Source[mp3Index];
					String fileAtPath = Environment.getExternalStorageDirectory().getAbsolutePath() + mp3Source;
					int audioBufferSize = (mAudioConfig.getAudioSampleRate()*mAudioConfig.getAudioFrameSize()*2)/1000;
					mMP3Reader = new MP3Reader(fileAtPath, audioBufferSize);
					mMP3Reader.startMP3Reader();
				}
				else if(mAudioConfig.getEnableWave()){
					int waveIndex = spnWaveSource.getSelectedItemPosition();
					String waveSource = listWaveSource[waveIndex];
					String fileAtPath = Environment.getExternalStorageDirectory().getAbsolutePath() + waveSource;
					int audioBufferSize = (mAudioConfig.getAudioSampleRate()*mAudioConfig.getAudioFrameSize()*2)/1000;;
					mWaveReader = new WaveReader(fileAtPath, audioBufferSize);
					mWaveReader.startWaveReader();
				}
				
				initAudioProcessor();
				
				if(null != mAudioProcessor){
					mAudioConfig.setAECMConfig(cbEnableAECM.isChecked());
					mAudioProcessor.startAudioProcessor();
					startPerfUpdateUIThread();
					initEqualizerBandLevel();
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
				if(null != mMP3Reader){
					mMP3Reader.stopMP3Reader();
					mMP3Reader = null;
				}
				
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
    	
    	
    	initPerformanceCounterUI(rootView);
    	
    	cbUDPListen = (CheckBox) rootView.findViewById(R.id.cbUDPListen);
    	cbUDPListen.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				if(isChecked){
					if(null != mUDPReceiver){
						mUDPReceiver.stop();
						mUDPReceiver = null;
					}
					startUDPReceiver();
				}
				else{
					if(null != mUDPReceiver){
						mUDPReceiver.stop();
						mUDPReceiver = null;
					}
				}
			}
    		
    	});
    	cbUDPListen.setChecked(mUDPListen);
    	
    	cbMP3Source = (CheckBox) rootView.findViewById(R.id.cbMP3Source);
    	cbMP3Source.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableMP3(isChecked);
			}
    		
    	});
    	cbMP3Source.setChecked(mAudioConfig.getEnableMP3());
    	
    	cbMP3Voice = (CheckBox) rootView.findViewById(R.id.cbMP3Voice);
    	cbMP3Voice.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableVoice(isChecked);
			}
    		
    	});
    	cbMP3Voice.setChecked(mAudioConfig.getEnableVoice());
    	
    	tvVersion = (TextView) rootView.findViewById(R.id.tvVersion);
    	PackageInfo pInfo;
		try {
			pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
			String strVersion = "Version:" + pInfo.versionName;
			tvVersion.setText(strVersion);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		cbAutoRename = (CheckBox) rootView.findViewById(R.id.cbAutoRename);
		cbAutoRename.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setAutoRename(isChecked);
			}
			
		});
		cbAutoRename.setChecked(mAudioConfig.getAutoRename());
		
		cbOpusRename = (CheckBox) rootView.findViewById(R.id.cbOpusRename);
		cbOpusRename.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setOpusRename(isChecked);
			}
			
		});
		cbOpusRename.setChecked(mAudioConfig.getOpusRename());
		
		cbAudioEffectRename = (CheckBox) rootView.findViewById(R.id.cbAudioEffectRename);
		cbAudioEffectRename.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setAudioEffectRename(isChecked);
			}
			
		});
		cbAudioEffectRename.setChecked(mAudioConfig.getAudioEffectRename());
		
		cbAGCRename = (CheckBox) rootView.findViewById(R.id.cbAGCRename);
		cbAGCRename.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setAGCRename(isChecked);
			}
			
		});
		cbAGCRename.setChecked(mAudioConfig.getAGCRename());
		
		cbAECRename = (CheckBox) rootView.findViewById(R.id.cbAECRename);
		cbAECRename.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setAECRename(isChecked);
			}
			
		});
		cbAECRename.setChecked(mAudioConfig.getAECRename());
		
		cbAECTest = (CheckBox) rootView.findViewById(R.id.cbAECTest);
		cbAECTest.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableAECTestMode(isChecked);
			}
			
		});
		cbAECTest.setChecked(mAudioConfig.getEnableAECTestMode());
		
		cbNSRename = (CheckBox) rootView.findViewById(R.id.cbNSRename);
		cbNSRename.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setNSRename(isChecked);
			}
			
		});
		cbNSRename.setChecked(mAudioConfig.getNSRename());
		
		cbPacketLostRename = (CheckBox) rootView.findViewById(R.id.cbPacketLostRename);
		cbPacketLostRename.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setPacketLostRename(isChecked);
			}
			
		});
		cbPacketLostRename.setChecked(mAudioConfig.getPacketLostRename());
		
//		cbSendDelayProfile = (CheckBox) rootView.findViewById(R.id.cbSendDelayProfile);
//		cbSendDelayProfile.setOnCheckedChangeListener(new OnCheckedChangeListener(){
//
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView,
//					boolean isChecked) {
//				// TODO Auto-generated method stub
//				mAudioConfig.setSendDelayProfile(isChecked);
//			}
//			
//		});
//		cbSendDelayProfile.setChecked(mAudioConfig.getSendDelayProfile());
		
		cbRecvDelayProfile = (CheckBox) rootView.findViewById(R.id.cbRecvDelayProfile);
		cbRecvDelayProfile.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setRecvDelayProfile(isChecked);
			}
			
		});
		cbRecvDelayProfile.setChecked(mAudioConfig.getRecvDelayProfile());
		
		cbRecvSeqNoProfile = (CheckBox) rootView.findViewById(R.id.cbRecvSeqNoProfile);
		cbRecvSeqNoProfile.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setRecvSeqNoProfile(isChecked);
			}
			
		});
		cbRecvSeqNoProfile.setChecked(mAudioConfig.getRecvSeqNoProfile());
		
		
		cbSaveRecvPacketTime = (CheckBox) rootView.findViewById(R.id.cbSaveRecvPacketTime);
		cbSaveRecvPacketTime.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableRecvPacketTimeLog(isChecked);
			}
			
		});
		cbSaveRecvPacketTime.setChecked(mAudioConfig.getEnableRecvPacketTimeLog());
		
		cbSaveRecvSeqNo = (CheckBox) rootView.findViewById(R.id.cbSaveRecvSeqNo);
		cbSaveRecvSeqNo.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setSaveRecvSeqNo(isChecked);
			}
			
		});
		cbSaveRecvSeqNo.setChecked(mAudioConfig.getSaveRecvSeqNo());
		
		cbWaveFile = (CheckBox) rootView.findViewById(R.id.cbWaveFile);
		cbWaveFile.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableWave(isChecked);
			}
			
		});
		cbWaveFile.setChecked(mAudioConfig.getEnableWave());
		
    	initAudioTestModeUI(rootView);
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
    	
    	cbEnableDropMode = (CheckBox) rootView.findViewById(R.id.cbEnableDrop);
    	cbEnableDropMode.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setDropModeConfig(isChecked);
			}
    		
    	});
    	cbEnableDropMode.setChecked(mAudioConfig.getDropModeConfig());
    	
    	cbEnableDynPlayRate = (CheckBox) rootView.findViewById(R.id.cbDynPlayRate);
    	cbEnableDynPlayRate.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setDynPlaybackRateConfig(isChecked);
			}
    		
    	});
    	cbEnableDynPlayRate.setChecked(mAudioConfig.getDynPlaybackRateConfig());
    	
    	cbEnableAudioSave = (CheckBox) rootView.findViewById(R.id.cbSaveAudioWave);
    	cbEnableAudioSave.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableWaveSaver(isChecked);
			}
    		
    	});
    	cbEnableAudioSave.setChecked(mAudioConfig.getEnableWaveSaver());
    	
    	etAudioDropRate = (EditText) rootView.findViewById(R.id.etAudioDropRate);
    	etAudioDropRate.addTextChangedListener(new TextWatcher(){

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
					int droprate = Integer.parseInt(s.toString());
					if(droprate > 1){
						mAudioConfig.setAudioDropRate(droprate);
					}
				}
			}
    	});
    	String strAudioDropRate = String.valueOf(mAudioConfig.getAudioDropRate());
    	etAudioDropRate.setText(strAudioDropRate);
    	
    	cbSaveVBRDataSize = (CheckBox) rootView.findViewById(R.id.cbSaveVBRDataSize);
    	cbSaveVBRDataSize.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableLogEncData(isChecked);
			}
    		
    	});
    	cbSaveVBRDataSize.setChecked(mAudioConfig.getEnableLogEncData());
    	
    	cbAudioTrackPlayDelay = (CheckBox) rootView.findViewById(R.id.cbAudioTrackPlayDelay);
    	cbAudioTrackPlayDelay.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableAudioTrackDelay(isChecked);
			}
    		
    	});
    	cbAudioTrackPlayDelay.setChecked(mAudioConfig.getEnableAudioTrackDelay());
    	
    	etAudioTrackDelayTime = (EditText) rootView.findViewById(R.id.etAudioTrackDelayTime);
    	etAudioTrackDelayTime.addTextChangedListener(new TextWatcher(){

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
					int delayTime = Integer.parseInt(s.toString());
					if(delayTime >= 0){
						mAudioConfig.setAudioTrackDelayTime(delayTime);
					}
				}
			}
    		
    	});
    	String strAudioTrackDelayTime = String.valueOf(mAudioConfig.getAudioTrackDelayTime());
    	etAudioTrackDelayTime.setText(strAudioTrackDelayTime);
    	
    	cbEnableQueueVoice = (CheckBox) rootView.findViewById(R.id.cbEnableQueueVoice);
    	cbEnableQueueVoice.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableQueueVoice(isChecked);
			}
    		
    	});
    	cbEnableQueueVoice.setChecked(mAudioConfig.getEnableQueueVoice());
    	
    	etVoiceQueueSize = (EditText) rootView.findViewById(R.id.etVoiceQueueSize);
    	etVoiceQueueSize.addTextChangedListener(new TextWatcher(){

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
					int queueSize = Integer.parseInt(s.toString());
					if(queueSize > 0){
						mAudioConfig.setMinimumVoicePlayQueueSize(queueSize);
					}
				}
			}
    		
    	});
    	String minVoiceQueueSize = String.valueOf(mAudioConfig.getMinimumVoicePlayQueueSize());
    	etVoiceQueueSize.setText(minVoiceQueueSize);
    	
    	cbDropNonVoiceData = (CheckBox) rootView.findViewById(R.id.cbDropNonVoiceData);
    	cbDropNonVoiceData.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setDropNonVoiceData(isChecked);
			}
    		
    	});
    	cbDropNonVoiceData.setChecked(mAudioConfig.getDropNonVoiceData());
    	
    	cbRecorderFadeIn = (CheckBox) rootView.findViewById(R.id.cbRecorderFadeIn);
    	cbRecorderFadeIn.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableRecorderFadeIn(isChecked);
			}
    		
    	});
    	cbRecorderFadeIn.setChecked(mAudioConfig.getEnableRecorderFadeIn());
    	
    	btnStrongRate = (Button) rootView.findViewById(R.id.btnStrongRate);
    	btnStrongRate.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mAudioConfig.addDynSpeedUpLevel(1);
			}
    		
    	});
    	
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
    	
    	cbEnableEqualizer = (CheckBox) rootView.findViewById(R.id.cbEnableEqualizer);
    	cbEnableEqualizer.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableEqualizer(isChecked);
			}
    		
    	});
    	cbEnableEqualizer.setChecked(mAudioConfig.getEnableEqualizer());
    	
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
    	
    	cbSendIfVAD = (CheckBox) rootView.findViewById(R.id.cbSendIfVAD);
    	cbSendIfVAD.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setSendIfVAD(isChecked);
			}
    		
    	});
    	cbSendIfVAD.setChecked(mAudioConfig.getSendIfVAD());
    	
    	cbDisorderPlay = (CheckBox) rootView.findViewById(R.id.cbDisorderPlay);
    	cbDisorderPlay.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setDisorderPlay(isChecked);
			}
    		
    	});
    	cbDisorderPlay.setChecked(mAudioConfig.getDisorderPlay());
    	
    	cbAdjustGain = (CheckBox) rootView.findViewById(R.id.cbAdjustGain);
    	cbAdjustGain.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setAdjustGain(isChecked);
			}
    		
    	});
    	cbAdjustGain.setChecked(mAudioConfig.getAdjustGain());
    	
    	btnAdjustGain = (Button) rootView.findViewById(R.id.btnAdjustGain);
    	btnAdjustGain.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				mAudioConfig.addAdjustGainLevel(1);
			}
    		
    	});
    	
    	etDBMaxThreshold = (EditText) rootView.findViewById(R.id.etDBMaxThreshold);
    	etDBMaxThreshold.addTextChangedListener(new TextWatcher(){

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
				if((null != s) && s.length() > 1){
					int dbMax = Integer.parseInt(s.toString());
					if(dbMax < 0){
						mAudioConfig.setDBMaxThreshold(dbMax);
						Log.d(LOG_TAG, String.format("DBMaxThreshold=%d", dbMax));
					}
				}
			}
    		
    	});
    	String strDBMax = String.valueOf(mAudioConfig.getDBMaxThreshold());
    	etDBMaxThreshold.setText(strDBMax);
    	
    	etDBMinThreshold = (EditText) rootView.findViewById(R.id.etDBMinThreshold);
    	etDBMinThreshold.addTextChangedListener(new TextWatcher(){

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
				if((null != s) && s.length() > 1){
					int dbMin = Integer.parseInt(s.toString());
					if( (-90 < dbMin) && (dbMin < 0)){
						mAudioConfig.setDBMinThreshold(dbMin);
						Log.d(LOG_TAG, String.format("etDBMinThreshold=%d", dbMin));
					}
				}
			}
    		
    	});
    	String strDBMin = String.valueOf(mAudioConfig.getDBMinThreshold());
    	etDBMinThreshold.setText(strDBMin);
    	
    	etAGCMaxCompressiondB = (EditText) rootView.findViewById(R.id.etAGCMaxCompressiondB);
    	etAGCMaxCompressiondB.addTextChangedListener(new TextWatcher(){

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
					int value = Integer.parseInt(s.toString());
					if((value > 0) && (value < 100)){
						mAudioConfig.setAGCMaxCompressiondB(value);
					}
				}
			}
    	});
    	String strAGCMaxCompressiondB = String.valueOf(mAudioConfig.getAGCMaxCompressiondB());
    	etAGCMaxCompressiondB.setText(strAGCMaxCompressiondB);
    	
    	cbCalculateDB = (CheckBox) rootView.findViewById(R.id.cbCalculateDB);
    	cbCalculateDB.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setCalculateDB(isChecked);
			}
    		
    	});
    	cbCalculateDB.setChecked(mAudioConfig.getCalculateDB());
    	
    	cbSaveDB = (CheckBox) rootView.findViewById(R.id.cbSaveDB);
    	cbSaveDB.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setSaveDB(isChecked);
			}
    		
    	});
    	cbSaveDB.setChecked(mAudioConfig.getSaveDB());
    	
    	cbDuplicateAudioData = (CheckBox) rootView.findViewById(R.id.cbDuplicateAudioData);
    	cbDuplicateAudioData.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setDuplicateAudioData(isChecked);
			}
    		
    	});
    	cbDuplicateAudioData.setChecked(mAudioConfig.getDuplicateAudioData());
    	
    }
    
    private void initGenVoiceControlUI(View rootView){
    	cbWriteSilentData = (CheckBox) rootView.findViewById(R.id.cbWriteSilentData);
    	cbWriteSilentData.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setWriteSilentData(isChecked);
			}
    		
    	});
    	cbWriteSilentData.setChecked(mAudioConfig.getWriteSilentData());
    	
    	cbPauseAudioTrack = (CheckBox) rootView.findViewById(R.id.cbPauseAudioTrack);
    	cbPauseAudioTrack.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setPauseAudioTrack(isChecked);
			}
    		
    	});
    	cbPauseAudioTrack.setChecked(mAudioConfig.getPauseAudioTrack());
    	
    	cbOpusSilentData = (CheckBox) rootView.findViewById(R.id.cbOpusSilentData);
    	cbOpusSilentData.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setOpusSilentData(isChecked);
			}
    		
    	});
    	cbOpusSilentData.setChecked(mAudioConfig.getOpusSilentData());
    	
    	cbDuplicateData = (CheckBox) rootView.findViewById(R.id.cbDuplicateData);
    	cbDuplicateData.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setDuplicateData(isChecked);
			}
    		
    	});
    	cbDuplicateData.setChecked(mAudioConfig.getDuplicateData());
    	
    	cbEnableGenData = (CheckBox) rootView.findViewById(R.id.cbEnableGenData);
    	cbEnableGenData.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableGenData(isChecked);
			}
    		
    	});
    	cbEnableGenData.setChecked(mAudioConfig.getEnableGenData());
    	
    	cbGenPairData = (CheckBox) rootView.findViewById(R.id.cbGenPairData);
    	cbGenPairData.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setGenPairData(isChecked);
			}
    		
    	});
    	cbGenPairData.setChecked(mAudioConfig.getGenPairData());
    	
    	cbGenerateSin = (CheckBox) rootView.findViewById(R.id.cbGenerateSin);
    	cbGenerateSin.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setGenSinData(isChecked);
			}
    		
    	});
    	cbGenerateSin.setChecked(mAudioConfig.getGenSinData());
    	
    	cbGenFadeInOut = (CheckBox) rootView.findViewById(R.id.cbGenFadeInOut);
    	cbGenFadeInOut.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setGenFadeInOut(isChecked);
			}
    		
    	});
    	cbGenFadeInOut.setChecked(mAudioConfig.getGenFadeInOut());
    	
    	spnSinTone = (Spinner) rootView.findViewById(R.id.spnSinTone);
    	listSinToneAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, listSinTone);
    	spnSinTone.setAdapter(listSinToneAdapter);
    	
    	spnSinTone.setOnItemSelectedListener(new OnItemSelectedListener(){

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				int sinTone = Integer.parseInt(listSinTone[position]);
				mAudioConfig.setSinTone(sinTone);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
     	String strSinTone = String.valueOf(mAudioConfig.getSinTone());
    	int posSinTone = listSinToneAdapter.getPosition(strSinTone);
    	spnSinTone.setSelection(posSinTone);
    	
    	cbCountAmplitudeDiff = (CheckBox) rootView.findViewById(R.id.cbCountAmplitudeDiff);
    	cbCountAmplitudeDiff.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setCountAmplitudeDiff(isChecked);
			}
    		
    	});
    	cbCountAmplitudeDiff.setChecked(mAudioConfig.getCountAmplitudeDiff());
    	
    }
    
    private void initAudioTestModeUI(View rootView){
    	
    	cbAudioTestMode = (CheckBox) rootView.findViewById(R.id.cbAudioTestMode);
    	cbAudioTestMode.setOnCheckedChangeListener(new OnCheckedChangeListener(){

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				// TODO Auto-generated method stub
				mAudioConfig.setEnableTestMode(isChecked);
			}
    		
    	});
    	cbAudioTestMode.setChecked(mAudioConfig.getEnableTestMode());
    	
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
    
    public void startUDPReceiver(){
		mUDPReceiver = new UDPReceiver(mUDPTestConfig.getUDPListenPort(), UDPTestConfig.mTestMode, this);
		mUDPReceiver.start(mUIHandler);
	}
    
    private void initPerformanceCounterUI(View rootView){
    	tvRecvQueueValue = (TextView) rootView.findViewById(R.id.tvRecvQueueValue);
    	tvEstimateDelayTimeValue = (TextView) rootView.findViewById(R.id.tvEstimateDelayTimeValue);
    	
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //((AudioTestActivity) activity).onSectionAttached(
        //        getArguments().getInt(ARG_SECTION_NUMBER));
    }

    /**
     * IUDPReceiveInterface implements start
     */
	@Override
	public void RecvData(byte[] pData) {
		// TODO Auto-generated method stub
		//Log.d(LOG_TAG, String.format("RecvData length=%d", pData.length));
		int delayRate = mUDPTestConfig.getUDPRecvDelayRate();
		if(delayRate > 0){
			int i1 = rRecvDelayControl.nextInt(100);
			if(i1 < delayRate){
				int delayTime = mUDPTestConfig.getUDPRecvDelayTime();
				//delayTime = delayTime*i1/100;
				int delayBias = mUDPTestConfig.getRecvDelayBias();
				if(delayBias > 0){
					int adjustValue = rRecvDelayControl.nextInt(delayBias*2) - delayBias;
					delayTime = delayTime + adjustValue;
				}
				if(delayTime > 0){
					try {
						//Log.d(LOG_TAG, String.format("RecvData Sleep time: %d with random int=%d, bias=%d", delayTime, i1, delayBias));
						Thread.sleep(delayTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		if(mIsRecvInterrupt.get()){
			mIsRecvInterrupt.set(false);
			int sleepTime = mUDPTestConfig.getInterruptDelayTime();
			if(sleepTime > 0){
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
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
		int delayRate = mUDPTestConfig.getUDPSendDelayRate();
		if(delayRate > 0){
			int i1 = rSendDelayControl.nextInt(100);
			if(i1 < delayRate){
				int delayTime = mUDPTestConfig.getUDPSendDelayTime();
				int delayBias = mUDPTestConfig.getSendDelayBias();
				if(delayBias > 0){
					int adjustValue = rSendDelayControl.nextInt(delayBias*2) - delayBias;
					delayTime = delayTime + adjustValue;
				}
				//delayTime = delayTime*i1/100;
				if(delayTime > 0){
					try {
						//Log.d(LOG_TAG, String.format("SendData Sleep time: %d with random int=%d, bias=%d", delayTime, i1, delayBias));
						Thread.sleep(delayTime);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		if(mIsSendInterrupt.get()){
			mIsSendInterrupt.set(false);
			int sleepTime = mUDPTestConfig.getInterruptDelayTime();
			if(sleepTime > 0){
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
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
		else if(mAudioConfig.getEnableWave()){
			if(null != mWaveReader){
				data = mWaveReader.getWaveData();
				if(null != data){
					//Log.d(LOG_TAG, String.format("Obtain Data from MP3: data length=%d", data.length));
				}
				else{
					Log.d(LOG_TAG, String.format("Obtain Data from Wave: data is null"));
				}
			}
		}
		return data;
	}
	/**
	 * AudioProcessor.IAudioProcessorCallback implements end
	 */
}

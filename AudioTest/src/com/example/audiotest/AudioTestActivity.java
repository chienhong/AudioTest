package com.example.audiotest;

import com.example.audiotest.ui.FragAudioBasicSettingPage;
import com.example.audiotest.ui.FragAudioDemoSettingPage;
import com.example.audiotest.ui.FragAudioTestAllPage;
import com.example.audiotest.ui.FragAudioTestPage;
import com.example.audiotest.ui.FragNetworkSettingPage;
import com.example.audiotest.ui.FragOpusSettingPage;
import com.example.audiotest.R;

import android.app.Activity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
//import android.support.v7.media.MediaControlIntent;
//import android.support.v7.media.MediaRouteSelector;
//import android.support.v7.media.MediaRouteSelector;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;


public class AudioTestActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

	private static final String LOG_TAG = "AudioTestActivity";
//	private MediaRouter mRouter;
//    private MediaRouter.Callback mCallback;
    //private MediaRouteSelector mSelector;
    
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    
    private static FragAudioTestPage mFragAudioTestPage = null;
    private static FragAudioBasicSettingPage mFragAudioBasicSetting = null;
    private static FragOpusSettingPage mFragOpusSetting = null;
    private static FragNetworkSettingPage mFragNetworkSetting = null;
    private static FragAudioTestAllPage mFragAudioTestAllPage = null;
    private static FragAudioDemoSettingPage mFragAudioDemoSettingPage = null;
    
    private AudioManager mAudioManager = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_test);
        
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        /*
        mRouter = MediaRouter.getInstance(this);
        mCallback = new MyCallback();
        mSelector = new MediaRouteSelector.Builder()
                .addControlCategory(MediaControlIntent.CATEGORY_LIVE_AUDIO)
                .addControlCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK)
                .build();
                */
        //mRouter = MediaRouter.
        //MediaRouter
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(0 == position){
        	if(null == mFragAudioDemoSettingPage){
        		mFragAudioDemoSettingPage = new FragAudioDemoSettingPage();
        	}
        	fragmentManager.beginTransaction()
				.replace(R.id.container, mFragAudioDemoSettingPage)
				.commit();
        }
        else if(1 == position){
//        	if(null == mFragAudioBasicSetting){
//        		mFragAudioBasicSetting = new FragAudioBasicSettingPage();
//        	}
//        	fragmentManager.beginTransaction()
//				.replace(R.id.container, mFragAudioBasicSetting)
//				.commit();
        	if(null == mFragAudioTestPage){
        		mFragAudioTestPage = new FragAudioTestPage();
        	}
        	fragmentManager.beginTransaction()
        			.replace(R.id.container, mFragAudioTestPage)
        			.commit();
        }
        else if(2 == position){
        	if(null == mFragOpusSetting){
        		mFragOpusSetting = new FragOpusSettingPage();
        	}
        	fragmentManager.beginTransaction()
        			.replace(R.id.container, mFragOpusSetting)
        			.commit();
        }
        else if(3 == position){
        	if(null == mFragNetworkSetting){
        		mFragNetworkSetting = new FragNetworkSettingPage();
        	}
        	fragmentManager.beginTransaction()
        			.replace(R.id.container, mFragNetworkSetting)
        			.commit();
        }
        else if(4 == position){
        	if(null == mFragAudioTestAllPage){
        		mFragAudioTestAllPage = new FragAudioTestAllPage();
        	}
        	fragmentManager.beginTransaction()
					.replace(R.id.container, mFragAudioTestAllPage)
					.commit();
        	
        }
        else{
        	fragmentManager.beginTransaction()
                	.replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                	.commit();
        }
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        if (!mNavigationDrawerFragment.isDrawerOpen()) {
//            // Only show items in the action bar relevant to this screen
//            // if the drawer is not showing. Otherwise, let the drawer
//            // decide what to show in the action bar.
//            getMenuInflater().inflate(R.menu.audio_test, menu);
//            restoreActionBar();
//            return true;
//        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

    	if ((keyCode != KeyEvent.KEYCODE_VOLUME_UP) && (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN))
			return false;

		String szOSBrand = android.os.Build.BRAND;

		int curVolume = 0;
		int newVolume = 0;
		int maxVolume = 0;

		if (szOSBrand.equalsIgnoreCase("Xiaomi")){
			curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		}
		else{
			curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
			maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
		}
		android.util.Log.d(LOG_TAG, String.format("curVolume=%d, maxVolume=%d", curVolume, maxVolume));
		switch (keyCode)
		{
			case KeyEvent.KEYCODE_VOLUME_UP:
				Log.d(LOG_TAG, "KEYCODE_VOLUME_UP");
				mAudioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
				newVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
				if (curVolume == newVolume)
					return false;
				Log.d(LOG_TAG, "Raise curVolume to value:" + newVolume);
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				if (curVolume <= 1)
					return false;
				Log.d(LOG_TAG, "KEYCODE_VOLUME_DOWN");

				mAudioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
				newVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
				Log.d(LOG_TAG, "Lower curVolume to value:" + newVolume);
				if (curVolume == newVolume)
					return false;
				break;
			default:
				return false;
		}
		return true;
	}
    
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.frag_template, container, false);
            return rootView;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((AudioTestActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }
    }

}

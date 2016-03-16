package edu.temple.cis.funfsens;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.json.IJsonObject;
import edu.mit.media.funf.pipeline.BasicPipeline;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.probe.Probe.DataListener;
import edu.mit.media.funf.probe.builtin.AudioFeaturesProbe;
import edu.mit.media.funf.probe.builtin.BluetoothProbe;
import edu.mit.media.funf.probe.builtin.LightSensorProbe;
import edu.mit.media.funf.probe.builtin.WifiProbe;
import edu.mit.media.funf.storage.HttpArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;



/**
 * A placeholder fragment containing a simple view.
 */
public class TestFragment extends Fragment implements DataListener, 
	View.OnClickListener, OnCheckedChangeListener {
	private static final String LOG_TAG = "funfsens.testfrag";
	private View rootView;
	// Widgets
	private Spinner spinScenes;
	private CheckBox ckbAudio, ckbLight,ckbBluetooth, ckbWifi;
	private Button butRecord;
	private Button butStop;
	
	private EditText etAudioEntries, etLightEntries, etBTEntries, etWifiEntries;
	private EditText etFeatureNum;
	private CheckBox ckbAutoUpload;
	
	private Button butReset;
	private Button butArchive;
	private Button butUpload;
	
	private ArrayAdapter<CharSequence> sceneArrayAdapter;
	// AudioFeaturesProbe pipeline
	private static final String PIPELINE_TEST_AUDIO = "test_audio";
	private static final String PIPELINE_TEST_LIGHT = "test_light";
	private static final String PIPELINE_TEST_BLUETOOTH = "test_Bluetooth";
	private static final String PIPELINE_TEST_WIFI = "test_Wifi";

	// Pipelines and probes
	private BasicPipeline audioPipeline = null;
	private BasicPipeline lightPipeline = null;
	private BasicPipeline bluetoothPipeline = null;
	private BasicPipeline wifiPipeline = null;
	
	private FunfManager funfManager = null;
	
	private AudioFeaturesProbe audioFeaturesProbe = null;
	private LightSensorProbe lightSensorProbe = null;
	private WifiProbe wifiProbe = null;
	private BluetoothProbe bluetoothProbe = null;
	private Handler handler = null;
	
	private String curSelectedScene = null;
	
	boolean audioProbeDisabled = true, audioProbeFinished = false;
	boolean lightProbeDisabled = true, lightProbeFinished = false;
	boolean bluetoothProbeDisabled = true, bluetoothProbeFinished = false;
	boolean wifiProbeDisabled = true, wifiProbeFinished = false;
	
	boolean allFinished = false;
	public TestFragment() {
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_test,
				container, false);
		
		handler = new Handler();
		/* Initialize view widgets */
		// Select scene spinner
		spinScenes = (Spinner)rootView.findViewById(R.id.spinScenes);
		sceneArrayAdapter = ArrayAdapter.createFromResource(
				rootView.getContext(), R.array.scenes_array, 
				android.R.layout.simple_spinner_item);
		sceneArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinScenes.setAdapter(sceneArrayAdapter);
		// Pipeline Check box
		ckbAudio = (CheckBox)rootView.findViewById(R.id.ckbAudio); 
		ckbLight = (CheckBox)rootView.findViewById(R.id.ckbLight);
		ckbBluetooth = (CheckBox)rootView.findViewById(R.id.ckbBluetooth);
		ckbWifi = (CheckBox)rootView.findViewById(R.id.ckbWifi);
		
		// Number of training features
		etFeatureNum = (EditText)rootView.findViewById(R.id.etFeatureNum);
		etFeatureNum.setText("30");
		// Auto upload check box
		ckbAutoUpload = (CheckBox)rootView.findViewById(R.id.ckbAutoUpload);
		ckbAutoUpload.setChecked(true);
		
		// Start record button
		butRecord = (Button)rootView.findViewById(R.id.butRecord);
		butRecord.setEnabled(false);
		butRecord.setOnClickListener(this);
		// Stop button
		butStop = (Button)rootView.findViewById(R.id.butStop);
		butStop.setEnabled(false);
		butStop.setOnClickListener(this);
		// Reset button
		butReset = (Button)rootView.findViewById(R.id.butReset);
		butReset.setEnabled(false);
		butReset.setOnClickListener(this);
		// Archive button
		butArchive = (Button)rootView.findViewById(R.id.butArchive);
		butArchive.setEnabled(false); 
		butArchive.setOnClickListener(this);
		// Upload
		butUpload = (Button)rootView.findViewById(R.id.butUpload);
		butUpload.setEnabled(false);
		butUpload.setOnClickListener(this);
		// # of database entries
		etAudioEntries = (EditText)rootView.findViewById(R.id.etAudioEntries);
		etLightEntries = (EditText)rootView.findViewById(R.id.etLightEntries);
		etBTEntries = (EditText)rootView.findViewById(R.id.etBTEntries);
		etWifiEntries = (EditText)rootView.findViewById(R.id.etWifiEntries);
		// Funf audio sensing service 
		rootView.getContext().bindService(new Intent(rootView.getContext(), FunfManager.class), 
				funfManagerConn, Context.BIND_AUTO_CREATE);

		return rootView;
	}
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.butRecord:
			audioProbeFinished = false; lightProbeFinished = false;
			bluetoothProbeFinished = false; wifiProbeFinished = false;
			allFinished = false;
			
			if(audioPipeline.isEnabled()) {
				audioFeaturesProbe.registerPassiveListener(this);
				audioFeaturesProbe.registerListener(audioPipeline);
			}
			if(lightPipeline.isEnabled()) {
				lightSensorProbe.registerPassiveListener(this);
				lightSensorProbe.registerListener(lightPipeline);
			}
			if(bluetoothPipeline.isEnabled()) {
				bluetoothProbe.registerPassiveListener(this);
				bluetoothProbe.registerListener(bluetoothPipeline);

			}
			if(wifiPipeline.isEnabled()) {
				wifiProbe.registerPassiveListener(this);
				wifiProbe.registerListener(wifiPipeline);
			}
			if(audioPipeline.isEnabled() || lightPipeline.isEnabled() || 
					bluetoothPipeline.isEnabled()  || wifiPipeline.isEnabled()) {
				// Flip buttons
				spinScenes.setEnabled(false);
				
				butRecord.setEnabled(false);
				butStop.setEnabled(true);
				butReset.setEnabled(false);
				butArchive.setEnabled(false); butUpload.setEnabled(false);
				
				ckbAudio.setEnabled(false); ckbLight.setEnabled(false);
				ckbBluetooth.setEnabled(false); ckbWifi.setEnabled(false);
				
				etFeatureNum.setEnabled(false);
			} else {
				Toast.makeText(v.getContext(), "No pipeline is enabled", 
						Toast.LENGTH_SHORT).show();
			}
			break;
		case R.id.butStop:
			
/*			if(audioPipeline.isEnabled()) {
				audioFeaturesProbe.unregisterPassiveListener(this);
				audioFeaturesProbe.unregisterListener(audioPipeline);
			}
			if(lightPipeline.isEnabled()) {
				lightSensorProbe.unregisterPassiveListener(this);
				lightSensorProbe.unregisterListener(lightPipeline);
			}
			if(bluetoothPipeline.isEnabled()) {
				bluetoothProbe.unregisterPassiveListener(this);
				bluetoothProbe.unregisterListener(bluetoothPipeline);

			}
			if(wifiPipeline.isEnabled()) {
				wifiProbe.unregisterPassiveListener(this);
				wifiProbe.unregisterListener(wifiPipeline);
			}*/

			// Flip buttons
			spinScenes.setEnabled(true);
			
			butRecord.setEnabled(true);
			butStop.setEnabled(false);
			butReset.setEnabled(true);
			butArchive.setEnabled(true); butUpload.setEnabled(true);

			ckbAudio.setEnabled(true); ckbLight.setEnabled(true);
			ckbBluetooth.setEnabled(true); ckbWifi.setEnabled(true);			

			etFeatureNum.setEnabled(true);
			
			break;
			
		case R.id.butReset:
			
			if(!audioPipeline.isEnabled() && !lightPipeline.isEnabled() && 
					!bluetoothPipeline.isEnabled()  && !wifiPipeline.isEnabled()) {
				Toast.makeText(v.getContext(), "No pipeline is enabled", 
						Toast.LENGTH_SHORT).show();
				return;
			}
			if(audioPipeline.isEnabled()) {
				clearDBEntries(audioPipeline);
				updateDBEntriesDisplay(audioPipeline, etAudioEntries);
			}
			if(lightPipeline.isEnabled()) {
				clearDBEntries(lightPipeline);
				updateDBEntriesDisplay(lightPipeline, etLightEntries);
			}
			if(bluetoothPipeline.isEnabled()) {
				clearDBEntries(bluetoothPipeline);
				updateDBEntriesDisplay(bluetoothPipeline, etBTEntries);
			}
			if(wifiPipeline.isEnabled()) {
				clearDBEntries(wifiPipeline);
				updateDBEntriesDisplay(wifiPipeline, etWifiEntries);
			}
			break;
			
		case R.id.butArchive:
			if(!audioPipeline.isEnabled() && !lightPipeline.isEnabled() && 
					!bluetoothPipeline.isEnabled()  && !wifiPipeline.isEnabled()) {
				Toast.makeText(v.getContext(), "No pipeline is enabled", 
						Toast.LENGTH_SHORT).show();
				return;
			} else {
				runArchive(audioPipeline); runArchive(wifiPipeline);
				runArchive(lightPipeline); runArchive(bluetoothPipeline);
			}
			butArchive.setEnabled(false);
			break;
		case R.id.butUpload:
			// Upload
			if(!audioPipeline.isEnabled() && !lightPipeline.isEnabled() && 
					!bluetoothPipeline.isEnabled()  && !wifiPipeline.isEnabled()) {
				Toast.makeText(v.getContext(), "No pipeline is enabled", 
						Toast.LENGTH_SHORT).show();
			} else {
				runUpload(audioPipeline); runUpload(lightPipeline);
				runUpload(wifiPipeline); runUpload(bluetoothPipeline);
				
			}
			butUpload.setEnabled(false);
			break;
		default:
			;
		}
		
	}
	private boolean runArchive(final BasicPipeline pipeline) {
		if(pipeline.isEnabled()) {
			pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);
			// Wait 3 seconds before upload finish
			handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	if(pipeline.getName().equals(PIPELINE_TEST_AUDIO)) {
                		updateDBEntriesDisplay(audioPipeline, etAudioEntries);
                	} else if(pipeline.getName().equals(PIPELINE_TEST_LIGHT)) {
                		updateDBEntriesDisplay(lightPipeline, etLightEntries);
                	} else if(pipeline.getName().equals(PIPELINE_TEST_BLUETOOTH)) {
                		updateDBEntriesDisplay(bluetoothPipeline, etBTEntries);
                	} else if(pipeline.getName().equals(PIPELINE_TEST_WIFI)) {
                		updateDBEntriesDisplay(wifiPipeline, etWifiEntries);
                	}
                }
            }, 3000L);
			
			return true;
		} else return false;
	}
	private boolean runUpload(final BasicPipeline pipeline) {
		if(pipeline.isEnabled()) {	
			// 
			runArchive(pipeline);
			// Wait 3 seconds for archiving
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {	
					uploadByScene(pipeline);

					// Wait 3 seconds for uploading data
					handler.postDelayed(new Runnable() {
		                @Override
		                public void run() {
		                	// Disable pipeline to prevent from executing 'upload' action
		                	if(pipeline.getName().equals(PIPELINE_TEST_AUDIO)) {
		                		audioProbeFinished = false;
		                		ckbAudio.setChecked(false);
		                	} else if(pipeline.getName().equals(PIPELINE_TEST_LIGHT)) {
		                		lightProbeFinished = false;
		                		ckbLight.setChecked(false);
		                	} else if(pipeline.getName().equals(PIPELINE_TEST_BLUETOOTH)) {
		                		bluetoothProbeFinished = false;
		                		ckbBluetooth.setChecked(false);
		                	} else if(pipeline.getName().equals(PIPELINE_TEST_WIFI)) {
		                		wifiProbeFinished = false;
		                		ckbWifi.setChecked(false);
		                	}
		                }
		            }, 3000L);
				}
				
			}, 3000L);
			
			
			
			return true;
		}else {
			return false;
			
		}
	}
	private void clearDBEntries(BasicPipeline pipeline) {
		SQLiteDatabase db = pipeline.getDb();
		int count = db.delete(NameValueDatabaseHelper.DATA_TABLE.name, "1", null);
		Toast.makeText(rootView.getContext(), count + " row deleted.", Toast.LENGTH_SHORT).show();
	}
	private void uploadByScene(BasicPipeline pipeline) {
		// replace upload url with scene
		HttpArchive upload = (HttpArchive) pipeline.getUpload();
		List<String> urlSplits = Arrays.asList(upload.getId().split("/"));
		urlSplits.set(urlSplits.size()-1, curSelectedScene.toLowerCase());
		//urlSplits.add();
		String newUrl = Joiner.on('/').join(urlSplits);
		upload.setUrl(newUrl);
		Log.d(LOG_TAG, upload.getId() + ": " + newUrl);
		pipeline.onRun(BasicPipeline.ACTION_UPLOAD, null);
	}
	private int getDBEntries(BasicPipeline pipeline)
	{
		String TOTAL_ENTRIES_SQL = "SELECT COUNT(*) FROM " 
				+ NameValueDatabaseHelper.DATA_TABLE.name;
				
		int entries = 0;
		if(pipeline == null) {
			return 0;
		}
		SQLiteDatabase db = pipeline.getDb();
		Cursor cur = db.rawQuery(TOTAL_ENTRIES_SQL, null);
		cur.moveToFirst();
		entries = cur.getInt(0);
		Log.d(LOG_TAG, "Current entries in " 
				+ NameValueDatabaseHelper.DATA_TABLE.name + ": " + entries);
		//db.close();
		return entries;
	}
	
	private int updateDBEntriesDisplay(BasicPipeline pipeline, final EditText etEntries)
	{
		if(pipeline.isEnabled()) {
			final int entries = getDBEntries(pipeline);
			getActivity().runOnUiThread(new Runnable() {
	
				@Override
				public void run() {
					etEntries.setText("" + entries);
					
				}
				
			});
			return entries;
		} else {
			return 0;
		}
	}
	/*
	 * Start audio features pipeline
	 */
	private ServiceConnection funfManagerConn = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			funfManager = ((FunfManager.LocalBinder)arg1).getManager();

			// Initialize probes
			Gson gson = funfManager.getGson();
			
			audioFeaturesProbe = gson.fromJson(new JsonObject(), AudioFeaturesProbe.class);
			lightSensorProbe = gson.fromJson(new JsonObject(), LightSensorProbe.class);
			bluetoothProbe = gson.fromJson(new JsonObject(), BluetoothProbe.class);
			wifiProbe = gson.fromJson(new JsonObject(), WifiProbe.class);
			
			// Initialize pipelines
			funfManager.disablePipeline(PIPELINE_TEST_AUDIO);
			funfManager.disablePipeline(PIPELINE_TEST_LIGHT);
			funfManager.disablePipeline(PIPELINE_TEST_BLUETOOTH);
			funfManager.disablePipeline(PIPELINE_TEST_WIFI);

			audioPipeline = (BasicPipeline)funfManager.getRegisteredPipeline(PIPELINE_TEST_AUDIO);
			lightPipeline = (BasicPipeline)funfManager.getRegisteredPipeline(PIPELINE_TEST_LIGHT);
			bluetoothPipeline = (BasicPipeline)funfManager.getRegisteredPipeline(PIPELINE_TEST_BLUETOOTH);
			wifiPipeline = (BasicPipeline)funfManager.getRegisteredPipeline(PIPELINE_TEST_WIFI);
			
			// Setup probe check boxes
			ckbAudio.setOnCheckedChangeListener(TestFragment.this);
			ckbLight.setOnCheckedChangeListener(TestFragment.this);
			ckbBluetooth.setOnCheckedChangeListener(TestFragment.this);
			ckbWifi.setOnCheckedChangeListener(TestFragment.this);
			
			// Scene spinner
			curSelectedScene = "Office";
			spinScenes.setSelection(sceneArrayAdapter.getPosition(curSelectedScene));
			
			spinScenes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
						int position, long id) {
					String scene = parentView.getItemAtPosition(position).toString();
					curSelectedScene = scene;
					
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}
			});
			// Enable check box and buttons
			ckbAudio.setEnabled(true); ckbLight.setEnabled(true);
			ckbBluetooth.setEnabled(true); ckbWifi.setEnabled(true);
			
			butRecord.setEnabled(true);
		}
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			
		}

		
	};
	
	@Override 
	public void onDestroy() {
		super.onDestroy();
		if(funfManager != null) {
			
			if(audioPipeline.isEnabled()) {
				funfManager.disablePipeline(audioPipeline.getName());
			}
			if(lightPipeline.isEnabled()) {
				funfManager.disablePipeline(lightPipeline.getName());
			}
			if(bluetoothPipeline.isEnabled()) {
				funfManager.disablePipeline(bluetoothPipeline.getName());
			}
			if(wifiPipeline.isEnabled()) {
				funfManager.disablePipeline(wifiPipeline.getName());
			}
			
			funfManager.stopSelf();
			
		}
		rootView.getContext().unbindService(funfManagerConn);
	}
	private synchronized boolean checkFinished() {
		if( (!audioPipeline.isEnabled() || audioProbeFinished) && 
			(!lightPipeline.isEnabled() || lightProbeFinished) && 
			(!bluetoothPipeline.isEnabled() || bluetoothProbeFinished) && 
			(!wifiPipeline.isEnabled() || wifiProbeFinished)) {
			Log.d(LOG_TAG, "Check Finished ");
			Toast.makeText(rootView.getContext(), "Finished collecting features.", Toast.LENGTH_LONG).show();
			allFinished = true;
			return true;
		} else {
			return false;
		}
	}
	@Override
	public void onDataCompleted(IJsonObject arg0, JsonElement arg1) {
		// Obtain probe type
		synchronized(this) {
		Iterator<Entry<String, JsonElement>> it = arg0.entrySet().iterator();
		String probeType = null;
		if(it.hasNext()) {
			probeType = it.next().getValue().toString();
		}
		Log.d(LOG_TAG, probeType + " completed." );
		
		if(allFinished) {
			return;
		}
		
		// Re-register to keeping listening 
		BasicPipeline pipeline = null;
		if(probeType.equals("\"edu.mit.media.funf.probe.builtin.AudioFeaturesProbe\"")) {
			pipeline = audioPipeline;
			
			//audioFeaturesProbe.registerPassiveListener(TrainFragment.this);
			audioProbeFinished = true;
			
		} else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.LightSensorProbe\"")) {
			pipeline = lightPipeline;
			
			//lightSensorProbe.registerPassiveListener(TrainFragment.this);
			lightProbeFinished = true;
		}
		else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.BluetoothProbe\"")) {
			pipeline = bluetoothPipeline;
			
			//bluetoothProbe.registerPassiveListener(TrainFragment.this);
			bluetoothProbeFinished = true;
			
		}
		else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.WifiProbe\"")) {
			pipeline = wifiPipeline;
			
			//wifiProbe.registerPassiveListener(TrainFragment.this);
			wifiProbeFinished = true;
			updateDBEntriesDisplay(wifiPipeline, etWifiEntries);
			
		} else  {
			Log.e(LOG_TAG, "Unsupported probe: " + probeType);
		}
		}
		if(!allFinished && checkFinished()) {
			Log.d(LOG_TAG, "Finished in data completed");
			rootView.post(new Runnable() {
	
				@Override
				public void run() {
					runUpload(audioPipeline); runUpload(lightPipeline);
					runUpload(bluetoothPipeline); runUpload(wifiPipeline);
					butStop.callOnClick();
				}
				
			});
		}
	//Log.d(LOG_TAG, "" + checkFinish());
		/*
		Log.d(LOG_TAG, "Data completed");
		Log.d(LOG_TAG, "arg0");
		Set<Entry<String, JsonElement>> jsonSet0 = arg0.entrySet();
		Iterator<Entry<String, JsonElement>> iterator0 = jsonSet0.iterator();
		while(iterator0.hasNext()) {
			Entry<String, JsonElement> e = iterator0.next();
			Log.d(LOG_TAG, e.getKey() + " " + e.getValue());
			
		}//*/
		//Log.d(LOG_TAG, "arg1: " + arg1.toString());
	}

	@Override
	public void onDataReceived(IJsonObject arg0, IJsonObject arg1) {
		// Obtain probe type
		synchronized(this) {
		Iterator<Entry<String, JsonElement>> it = arg0.entrySet().iterator();
		String probeType = null;
		if(it.hasNext()) {
			probeType = it.next().getValue().toString();
		}
		Log.d(LOG_TAG, probeType + " received." );
		
		if(allFinished) {
			return;
		}
		
		// Check corresponding entries
		if(probeType == null) {
			Log.e(LOG_TAG, "Can not detect probe type!");
			return;
		}
		
		int featureRequired = Integer.parseInt(etFeatureNum.getText().toString());
		BasicPipeline pipeline = null;
		if(probeType.equals("\"edu.mit.media.funf.probe.builtin.AudioFeaturesProbe\"")) {
			pipeline = audioPipeline;
			int entries = updateDBEntriesDisplay(audioPipeline, etAudioEntries);
			if(entries >= featureRequired) {
				//audioFeaturesProbe.unregisterListener(audioPipeline);
				//audioFeaturesProbe.unregisterPassiveListener(TrainFragment.this);
				audioFeaturesProbe.destroy();
				
				audioProbeFinished = true;
				
			}
		} else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.LightSensorProbe\"")) {
			pipeline = lightPipeline;
			int entries = updateDBEntriesDisplay(lightPipeline, etLightEntries);
			if(entries >= featureRequired) {
				//lightSensorProbe.unregisterListener(lightPipeline);
				//lightSensorProbe.unregisterPassiveListener(TrainFragment.this);
				lightSensorProbe.destroy();
				
				lightProbeFinished = true;
				
			}
		}
		else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.BluetoothProbe\"")) {
			pipeline = bluetoothPipeline;
			int entries = updateDBEntriesDisplay(bluetoothPipeline, etBTEntries);
			if(entries >= featureRequired) {
				//bluetoothProbe.unregisterPassiveListener(TrainFragment.this);
				//bluetoothProbe.unregisterListener(bluetoothPipeline);
				
				bluetoothProbe.destroy();
				
				bluetoothProbeFinished = true;
			}
		}
		else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.WifiProbe\"")) {
			pipeline = wifiPipeline;
			int entries = updateDBEntriesDisplay(wifiPipeline, etWifiEntries);
			if(entries >= featureRequired) {
				//wifiProbe.unregisterPassiveListener(TrainFragment.this);
				//wifiProbe.unregisterListener(wifiPipeline);
				
				wifiProbe.destroy();
				
				wifiProbeFinished = true;
			}
		} else  {
			Log.e(LOG_TAG, "Unsupported probe: " + probeType);
		}
		}
		if( !allFinished && checkFinished() ) {
			Log.d(LOG_TAG, "Finished in data received");
			rootView.post(new Runnable() {
				
				@Override
				public void run() {
					runUpload(audioPipeline); runUpload(lightPipeline);
					runUpload(bluetoothPipeline); runUpload(wifiPipeline);
					butStop.callOnClick();
				}
				
			});
		}
	
		/*
		Log.d(LOG_TAG, "Data recevied");
		Set<Entry<String, JsonElement>> jsonSet0 = arg0.entrySet();
		Iterator<Entry<String, JsonElement>> iterator0 = jsonSet0.iterator();
		while(iterator0.hasNext()) {
			Entry<String, JsonElement> e = iterator0.next();
			Log.d(LOG_TAG, e.getKey() + " " + e.getValue());
			
		}
		Log.d(LOG_TAG, "arg1");
		Set<Entry<String, JsonElement>> jsonSet1 = arg1.entrySet();
		Iterator<Entry<String, JsonElement>> iterator1 = jsonSet1.iterator();
		while(iterator1.hasNext()) {
			Entry<String, JsonElement> e = iterator1.next();
			Log.d(LOG_TAG, e.getKey() + " " + e.getValue());
			
		}//*/
		
	}
	private BasicPipeline enablePipeline(BasicPipeline pipeline, boolean isEnabled) {
		if(funfManager != null) {
			if(isEnabled) {
				funfManager.enablePipeline(pipeline.getName());
				pipeline = (BasicPipeline) funfManager.getRegisteredPipeline(pipeline.getName());
			} else {
				funfManager.disablePipeline(pipeline.getName());
			}
		}
		return pipeline;
	}
	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		switch(buttonView.getId()) {
		case R.id.ckbAudio:
			audioPipeline = enablePipeline(audioPipeline, isChecked);
			break;
		case R.id.ckbLight:
			lightPipeline = enablePipeline(lightPipeline, isChecked);
			break;
		case R.id.ckbBluetooth:
			bluetoothPipeline = enablePipeline(bluetoothPipeline, isChecked);
			break;
		case R.id.ckbWifi:
			wifiPipeline = enablePipeline(wifiPipeline, isChecked);
			break;
		}
		
	}

	
}


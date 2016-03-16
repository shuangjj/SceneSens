package edu.temple.cis.funfsens;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
import edu.mit.media.funf.storage.DefaultArchive;
import edu.mit.media.funf.storage.HttpArchive;
import edu.mit.media.funf.storage.NameValueDatabaseHelper;
import edu.mit.media.funf.storage.RemoteFileArchive;
import edu.mit.media.funf.storage.UploadService;
import edu.mit.media.funf.util.FileUtil;



/**
 * A placeholder fragment containing a simple view.
 */
public class TrainFragment extends Fragment implements DataListener, 
	View.OnClickListener, OnCheckedChangeListener {
	private static final String LOG_TAG = "funfsens.trainfrag";
	private View rootView;
	// usage
	private RadioButton radioTrain;
	private RadioGroup radioGroupUsage;
	// Scene
	private Spinner spinScenes;
	private EditText etLocName;
	// Pipeline enable checkbox
	private CheckBox ckbAudio, ckbLight,ckbBluetooth, ckbWifi;

	// Sensor feature counts
	private EditText etAudioEntries, etLightEntries, etBTEntries, etWifiEntries;
	private EditText etFeatureNum;
	private CheckBox ckbAutoUpload;
	// Notification setup
	private CheckBox ckbVibrate;
	private CheckBox ckbSound;
	// Control buttons
	private Button butRecord;
	private Button butCancel;
	private Button butUpload;
	
	private ArrayAdapter<CharSequence> sceneArrayAdapter;
	// AudioFeaturesProbe pipeline
	private static final String PIPELINE_AUDIO = "pipeline_audio";
	private static final String PIPELINE_LIGHT = "pipeline_light";
	private static final String PIPELINE_BLUETOOTH = "pipeline_Bluetooth";
	private static final String PIPELINE_WIFI = "pipeline_Wifi";
	
	private long audio_start, audio_stop;
	private long light_start, light_stop;
	private long bluetooth_start, bluetooth_stop;
	private long wifi_start, wifi_stop;
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
	
	
	private int notifyId = 0;
	
	public TrainFragment() {
	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_train,
				container, false);
		
		handler = new Handler();
		/* Initialize view widgets */
		radioGroupUsage = (RadioGroup)rootView.findViewById(R.id.radioGroupUsage);
		// Select scene spinner
		spinScenes = (Spinner)rootView.findViewById(R.id.spinScenes);
		sceneArrayAdapter = ArrayAdapter.createFromResource(
				rootView.getContext(), R.array.scenes_array, 
				android.R.layout.simple_spinner_item);
		sceneArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinScenes.setAdapter(sceneArrayAdapter); 
		// Location name
		etLocName = (EditText)rootView.findViewById(R.id.etLocName);
		etLocName.setText("Office");
		// Pipeline Check box
		ckbAudio = (CheckBox)rootView.findViewById(R.id.ckbAudio); 
		ckbLight = (CheckBox)rootView.findViewById(R.id.ckbLight);
		ckbBluetooth = (CheckBox)rootView.findViewById(R.id.ckbBluetooth);
		ckbWifi = (CheckBox)rootView.findViewById(R.id.ckbWifi);
		
		// Number of training features
		etFeatureNum = (EditText)rootView.findViewById(R.id.etFeatureNum);
		etFeatureNum.setText("15");
		// Auto upload check box
		ckbAutoUpload = (CheckBox)rootView.findViewById(R.id.ckbAutoUpload);
		ckbAutoUpload.setChecked(true);
		// Notification
		ckbVibrate = (CheckBox)rootView.findViewById(R.id.ckbVibrate);
		ckbSound = (CheckBox)rootView.findViewById(R.id.ckbSound);
		ckbVibrate.setChecked(true);
		ckbSound.setChecked(false);
		// Start record button
		butRecord = (Button)rootView.findViewById(R.id.butRecord);
		butRecord.setEnabled(false);
		butRecord.setOnClickListener(this);
		// Stop button
		butCancel = (Button)rootView.findViewById(R.id.butCancel);
		butCancel.setEnabled(false);
		butCancel.setOnClickListener(this);

		// Upload
		butUpload = (Button)rootView.findViewById(R.id.butUpload);
		butUpload.setEnabled(true);
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
				cleanArchives(audioPipeline);
				audioFeaturesProbe.registerPassiveListener(this);
				audioFeaturesProbe.registerListener(audioPipeline);
				audio_start = System.currentTimeMillis();
			}
			if(lightPipeline.isEnabled()) {
				cleanArchives(lightPipeline);
				lightSensorProbe.registerPassiveListener(this);
				lightSensorProbe.registerListener(lightPipeline);
				light_start = System.currentTimeMillis();
			}
			if(bluetoothPipeline.isEnabled()) {
				cleanArchives(bluetoothPipeline);
				bluetoothProbe.registerPassiveListener(this);
				bluetoothProbe.registerListener(bluetoothPipeline);
				bluetooth_start = System.currentTimeMillis();
			}
			if(wifiPipeline.isEnabled()) {
				cleanArchives(wifiPipeline);
				wifiProbe.registerPassiveListener(this);
				wifiProbe.registerListener(wifiPipeline);
				wifi_start = System.currentTimeMillis();
			}
			if(audioPipeline.isEnabled() || lightPipeline.isEnabled() || 
					bluetoothPipeline.isEnabled()  || wifiPipeline.isEnabled()) {
				// Flip buttons
				spinScenes.setEnabled(false);
				
				butRecord.setEnabled(false);
				butCancel.setEnabled(true);

				butUpload.setEnabled(false);
				
				ckbAudio.setEnabled(false); ckbLight.setEnabled(false);
				ckbBluetooth.setEnabled(false); ckbWifi.setEnabled(false);
				
				etFeatureNum.setEnabled(false);
			} else {
				Toast.makeText(v.getContext(), "No pipeline is enabled", 
						Toast.LENGTH_SHORT).show();
			}
			break;
			
		case R.id.butCancel:
			allFinished = true;
			clearFeatureCount();
			if(audioPipeline.isEnabled()) {
				audioFeaturesProbe.unregisterPassiveListener(this);
				audioFeaturesProbe.unregisterListener(audioPipeline);
				ckbAudio.setChecked(false);
			}
			if(lightPipeline.isEnabled()) {
				lightSensorProbe.unregisterPassiveListener(this);
				lightSensorProbe.unregisterListener(lightPipeline);
				ckbLight.setChecked(false);
			}
			if(bluetoothPipeline.isEnabled()) {
				bluetoothProbe.unregisterPassiveListener(this);
				bluetoothProbe.unregisterListener(bluetoothPipeline);
				ckbBluetooth.setChecked(false);

			}
			if(wifiPipeline.isEnabled()) {
				wifiProbe.unregisterPassiveListener(this);
				wifiProbe.unregisterListener(wifiPipeline);
				ckbWifi.setChecked(false);
			}
			// Flip buttons
			spinScenes.setEnabled(true);
			butRecord.setEnabled(true);
			butCancel.setEnabled(false);
			butUpload.setEnabled(true);

			ckbAudio.setEnabled(true); ckbLight.setEnabled(true);
			ckbBluetooth.setEnabled(true); ckbWifi.setEnabled(true);			
			
			
			etFeatureNum.setEnabled(true);
			break;


		case R.id.butUpload:
			// Obtain files to upload
			String uploadtxt = FileUtil.getSdCardPath(rootView.getContext()) + "uploads.txt";
			ArrayList<String> lines = new ArrayList<String>();
			if(new File(uploadtxt).exists()) {
				try {
					BufferedReader buf = new BufferedReader(new FileReader(uploadtxt));
					String line = null;
					while((line = buf.readLine()) != null) {
						lines.add(line);
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					Toast.makeText(rootView.getContext(), "Can't open upload profile", Toast.LENGTH_LONG).show();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if(lines.size() > 0) {
					// Upload using Async Task
					new UploadTask().execute(lines.toArray(new String[lines.size()]));
					butUpload.setEnabled(false);
				}else {
					Toast.makeText(rootView.getContext(), "No upload tasks", Toast.LENGTH_LONG).show();
				}
				
			} else {
				Toast.makeText(rootView.getContext(), "Can not find upload profile file!", Toast.LENGTH_LONG).show();
			}
			
			break;
		default:
			;
		}
		
	}
	private void clearFeatureCount() {
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
	}
	private class UploadTask extends AsyncTask<String, Integer, Integer> {
		ArrayList<String> UploadList = null;
		@Override
		protected Integer doInBackground(String... lines) {
			int count = lines.length;
			UploadList = new ArrayList<String>();
			int result = 0;
			for(int i=0; i < lines.length; i++) {
				String[] rc = lines[i].split(" ");
				String url = rc[0]; String dbPath = rc[1];
				Log.d(LOG_TAG, url + ":" + dbPath);
				
				HttpArchive http = new HttpArchive(funfManager, url);
				File archive = new File(dbPath);
				if(archive.isFile()) {
					if(http.add(archive)) {
						Log.d(LOG_TAG, dbPath + " uploaded");
						//Toast.makeText(rootView.getContext(), dbPath + " uploaded", Toast.LENGTH_SHORT).show();
						result++;
					
						// Remove archive from list
						UploadList.remove(lines[i]);
						Log.d(LOG_TAG, dbPath + " removed from list");
						archive.delete();
						Log.d(LOG_TAG, dbPath + " removed from disk");
					}else {
						UploadList.add(lines[i] + System.getProperty("line.separator"));
					}
					
				} else {
					
					Log.d(LOG_TAG, dbPath + " not exist");
					//Toast.makeText(rootView.getContext(), dbPath + " not exist", Toast.LENGTH_SHORT).show();
				}
				publishProgress((int) ((i / (float) count) * 100));
			
			}
			return result;
		}
		protected void onProgressUpdate(Integer... progress) {
		     
		}
		
		protected void onPostExecute(Integer result) {
		    // Write back upload profile
		    String root = FileUtil.getSdCardPath(rootView.getContext())	;     
		    File uploadstxt = new File(root + "uploads.txt");
		    try {
				FileOutputStream fos = new FileOutputStream(uploadstxt);
				for(String line : UploadList.toArray(new String[UploadList.size()])) {
					fos.write(line.getBytes());
				}
				fos.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    // Re-enable upload button
		    butUpload.setEnabled(true);
		    // Notification
 			notifyUser("Finished uploading", "Completed uploading " + result + " records", R.drawable.transfer_left_right32x32);
		     
		}
		
	}
	private void notifyUser(String title, String content, int icon) {
		NotificationManager notificationManager = (NotificationManager) 
					rootView.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
		//Define sound URI
		Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		Uri notifyUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(rootView.getContext())
		        .setContentTitle(title)
		        .setContentText(content)
		        .setSmallIcon(icon);
		if(ckbVibrate.isChecked()) {
			mBuilder.setVibrate(new long[]{1000, 1000, 1000});
		}
		if(ckbSound.isChecked()) {
			mBuilder.setSound(notifyUri);
		}
		//Display notification
		notificationManager.notify(notifyId++, mBuilder.build());
	}
	private boolean cleanArchives(BasicPipeline pipeline) {
		// Remove App data folders on sdcard
		String file_path = "/sdcard/" + rootView.getContext().getPackageName() 
				+ "/" + pipeline.getName();
		boolean deleted = deleteDirectory(new File(file_path));
		Log.d(LOG_TAG, file_path);
		//Toast.makeText(rootView.getContext(), file_path + " is deleted", Toast.LENGTH_SHORT).show();
		return deleted;
	}
	private static boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      if (files == null) {
	          return true;
	      }
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }
	private boolean runArchive(final BasicPipeline pipeline) {
		if(pipeline.isEnabled()) {
			pipeline.onRun(BasicPipeline.ACTION_ARCHIVE, null);
			// Wait 3 seconds before upload finish
			handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                	if(pipeline.getName().equals(PIPELINE_AUDIO)) {
                		updateDBEntriesDisplay(audioPipeline, etAudioEntries);
                	} else if(pipeline.getName().equals(PIPELINE_LIGHT)) {
                		updateDBEntriesDisplay(lightPipeline, etLightEntries);
                	} else if(pipeline.getName().equals(PIPELINE_BLUETOOTH)) {
                		updateDBEntriesDisplay(bluetoothPipeline, etBTEntries);
                	} else if(pipeline.getName().equals(PIPELINE_WIFI)) {
                		updateDBEntriesDisplay(wifiPipeline, etWifiEntries);
                	}
                }
            }, 3000L);
			
			return true;
		} else return false;
	}
	private int saveForUpload(BasicPipeline pipeline) {
		// Restored archived files
		int archived_num = 0;
		String rootPathOnSDCard = ((DefaultArchive)pipeline.getArchive()).getPathOnSDCard();
		File archivePath = new File( rootPathOnSDCard + "archive");
		File[] archives = archivePath.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir,
					String name) {
				int extIdx = name.lastIndexOf('.');
				if(extIdx > 0) {
					String ext = name.substring(extIdx);
					if(ext.equals(".db")) {
						return true;
					}
				}
				return false;
			}
			
		});
		// 
		String uploadsPath = FileUtil.getSdCardPath(rootView.getContext()) + "uploads/";
		File uploadsDir = new File(uploadsPath);
		if(!uploadsDir.exists()) {
			uploadsDir.mkdirs();
		}
		StringBuilder builder = new StringBuilder();
		for(File archive : archives) {
			
			String newPath = uploadsPath + archive.getName();
			archive.renameTo(new File(newPath));
			
			String log = String.format("%s %s\n", getUploadUrl(pipeline), newPath);
			builder.append(log);			
			
		}
		// Create upload profile
		FileOutputStream out = null;
		File f = new File(FileUtil.getSdCardPath(rootView.getContext()) + "uploads.txt");
		if(!f.isFile()) {
			try {
				f.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
		
		try {
			out = new FileOutputStream(f, true);
			if(out != null) {
				out.write(builder.toString().getBytes());
				//Log.d(LOG_TAG, "Writing to " + f.getAbsolutePath() + " " + builder.toString());
				out.close();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return archived_num;
		
	}
	
	private boolean runUpload(final BasicPipeline pipelines[], boolean upload) {
		final boolean autoUpload = upload;
			
			// Archive
			for(BasicPipeline pipeline : pipelines) {
				runArchive(pipeline);
			}
			// Wait 3 seconds for archiving
			handler.postDelayed(new Runnable() {

				@Override
				public void run() {	
					// Upload or restore
					for(BasicPipeline pipeline : pipelines) {
						if(autoUpload) {
							uploadByScene(pipeline);
						} else {
							
							saveForUpload(pipeline);
						}
					}
					// Wait 3 seconds for uploading data
					handler.postDelayed(new Runnable() {
		                @Override
		                public void run() {
		                	// Disable pipeline to prevent from executing 'upload' action
		                	for(BasicPipeline pipeline : pipelines) {
			                	if(pipeline.getName().equals(PIPELINE_AUDIO)) {
			                		audioProbeFinished = false;
			                		ckbAudio.setChecked(false);
			                	} else if(pipeline.getName().equals(PIPELINE_LIGHT)) {
			                		lightProbeFinished = false;
			                		ckbLight.setChecked(false);
			                	} else if(pipeline.getName().equals(PIPELINE_BLUETOOTH)) {
			                		bluetoothProbeFinished = false;
			                		ckbBluetooth.setChecked(false);
			                	} else if(pipeline.getName().equals(PIPELINE_WIFI)) {
			                		wifiProbeFinished = false;
			                		ckbWifi.setChecked(false);
			                	}
		                	}
		        			spinScenes.setEnabled(true);
		        			
		        			butRecord.setEnabled(true);
		        			butCancel.setEnabled(false);

		        			butUpload.setEnabled(true);

		        			ckbAudio.setEnabled(true); ckbLight.setEnabled(true);
		        			ckbBluetooth.setEnabled(true); ckbWifi.setEnabled(true);			

		        			etFeatureNum.setEnabled(true);
		        			
		        			clearFeatureCount();
		        			// Notify user 
		        			notifyUser(autoUpload ? "Finished uploading" : "Finished restoring to uploads",
		        					"Completed", 
		        					autoUpload ? R.drawable.transfer_left_right32x32 : R.drawable.notification_done32x32);
		        
		        		
		                }
		            }, 3000L);
				}
				
			}, 3000L);
			
			
			
			return true;

	}
	private void clearDBEntries(BasicPipeline pipeline) {
		SQLiteDatabase db = pipeline.getDb();
		int count = db.delete(NameValueDatabaseHelper.DATA_TABLE.name, "1", null);
		//Toast.makeText(rootView.getContext(), count + " row deleted.", Toast.LENGTH_SHORT).show();
	}
	private String getUploadUrl(BasicPipeline pipeline) {
		int id = radioGroupUsage.getCheckedRadioButtonId();
		RadioButton radio = (RadioButton) rootView.findViewById(id);
		Log.d(LOG_TAG, "Checked radio button: " + radio.getText().toString());
		// replace upload url with scene
		HttpArchive upload = (HttpArchive) pipeline.getUpload();
		//Log.d(LOG_TAG, upload.getId());
		List<String> urlSplits = new ArrayList(Arrays.asList(upload.getId().split("/")));
		// Replace usage
		urlSplits.set(4, radio.getText().toString().toLowerCase());
		urlSplits.set(urlSplits.size()-1, curSelectedScene.toLowerCase());
		/*for(String comp: urlSplits) {
			Log.d(LOG_TAG, comp);
		}*/
		// Append location name
		urlSplits.add(encodeURIComponent(etLocName.getText().toString()));
		// Append time stamp
		/*
		SQLiteDatabase db = pipeline.getDb();
		Cursor c = db.rawQuery("SELECT * FROM " + NameValueDatabaseHelper.DATA_TABLE.name, null);
		if(c.getCount() == 0) return;
		String ts = c.getString(c.getColumnIndex(NameValueDatabaseHelper.COLUMN_TIMESTAMP));
		urlSplits.add(encodeURIComponent(ts));
		for(String comp: urlSplits) {
		Log.d(LOG_TAG, comp);
		}
		
		String ts_start = null, ts_stop = null;
		if(pipeline.getName().equals(PIPELINE_AUDIO)) {
			ts_start = String.valueOf(audio_start);
			ts_stop = String.valueOf(audio_stop);
    	} else if(pipeline.getName().equals(PIPELINE_LIGHT)) {
    		ts_start = String.valueOf(light_start);
    		ts_stop = String.valueOf(light_stop);
    	} else if(pipeline.getName().equals(PIPELINE_BLUETOOTH)) {
    		ts_start = String.valueOf(bluetooth_start);
    		ts_stop = String.valueOf(bluetooth_stop);
    	} else if(pipeline.getName().equals(PIPELINE_WIFI)) {
    		ts_start = String.valueOf(wifi_start);
    		ts_stop = String.valueOf(wifi_stop);
    	}
		urlSplits.add(encodeURIComponent(ts_start));
		urlSplits.add(encodeURIComponent(ts_stop));
		*/
		// Reconstruct URL
		String newUrl = Joiner.on('/').join(urlSplits);
		return newUrl;
	}
	private void uploadByScene(BasicPipeline pipeline) {
		HttpArchive upload = (HttpArchive) pipeline.getUpload();
		// Reconstruct URL
		String newUrl = getUploadUrl(pipeline);
		upload.setUrl(newUrl);
		Log.d(LOG_TAG, upload.getId() + ": " + newUrl);
		pipeline.onRun(BasicPipeline.ACTION_UPLOAD, null);
	}
	private String encodeURIComponent(String s) {
	    String result;

	    try {
	        result = URLEncoder.encode(s, "UTF-8")
	                .replaceAll("\\+", "%20")
	                .replaceAll("\\%21", "!")
	                .replaceAll("\\%27", "'")
	                .replaceAll("\\%28", "(")
	                .replaceAll("\\%29", ")")
	                .replaceAll("\\%7E", "~");
	    } catch (UnsupportedEncodingException e) {
	        result = s;
	    }

	    return result;
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
			funfManager.disablePipeline(PIPELINE_AUDIO);
			funfManager.disablePipeline(PIPELINE_LIGHT);
			funfManager.disablePipeline(PIPELINE_BLUETOOTH);
			funfManager.disablePipeline(PIPELINE_WIFI);

			audioPipeline = (BasicPipeline)funfManager.getRegisteredPipeline(PIPELINE_AUDIO);
			lightPipeline = (BasicPipeline)funfManager.getRegisteredPipeline(PIPELINE_LIGHT);
			bluetoothPipeline = (BasicPipeline)funfManager.getRegisteredPipeline(PIPELINE_BLUETOOTH);
			wifiPipeline = (BasicPipeline)funfManager.getRegisteredPipeline(PIPELINE_WIFI);
			
			// Setup probe check boxes
			ckbAudio.setOnCheckedChangeListener(TrainFragment.this);
			ckbLight.setOnCheckedChangeListener(TrainFragment.this);
			ckbBluetooth.setOnCheckedChangeListener(TrainFragment.this);
			ckbWifi.setOnCheckedChangeListener(TrainFragment.this);
			
			// Scene spinner
			curSelectedScene = "Office";
			spinScenes.setSelection(sceneArrayAdapter.getPosition(curSelectedScene));
			
			spinScenes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parentView, View selectedItemView,
						int position, long id) {
					String scene = parentView.getItemAtPosition(position).toString();
					curSelectedScene = scene;
					etLocName.setText(curSelectedScene);
					
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
			audio_stop = System.currentTimeMillis();
			
		} else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.LightSensorProbe\"")) {
			pipeline = lightPipeline;
			
			//lightSensorProbe.registerPassiveListener(TrainFragment.this);
			lightProbeFinished = true;
			light_stop = System.currentTimeMillis();
		}
		else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.BluetoothProbe\"")) {
			pipeline = bluetoothPipeline;
			
			//bluetoothProbe.registerPassiveListener(TrainFragment.this);
			bluetoothProbeFinished = true;
			bluetooth_stop = System.currentTimeMillis();
			
		}
		else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.WifiProbe\"")) {
			pipeline = wifiPipeline;
			
			//wifiProbe.registerPassiveListener(TrainFragment.this);
			wifiProbeFinished = true;
			updateDBEntriesDisplay(wifiPipeline, etWifiEntries);
			wifi_stop = System.currentTimeMillis();
			
		} else  {
			Log.e(LOG_TAG, "Unsupported probe: " + probeType);
		}
		}
		if(!allFinished && checkFinished()) {
			Log.d(LOG_TAG, "Finished in data completed");
			rootView.post(new Runnable() {
	
				@Override
				public void run() {
					ArrayList<BasicPipeline> enabledPipelines = new ArrayList<BasicPipeline>();
					if(audioPipeline.isEnabled()) {
						enabledPipelines.add(audioPipeline);
					}
					if(lightPipeline.isEnabled()) {
						enabledPipelines.add(lightPipeline);
					}
					if(bluetoothPipeline.isEnabled()) {
						enabledPipelines.add(bluetoothPipeline);
					}
					if(wifiPipeline.isEnabled()) {
						enabledPipelines.add(wifiPipeline);
					}
					
					runUpload(enabledPipelines.toArray(new BasicPipeline[enabledPipelines.size()]), ckbAutoUpload.isChecked()); 

			
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
				audio_stop = System.currentTimeMillis();
			}
		} else if(probeType.equals("\"edu.mit.media.funf.probe.builtin.LightSensorProbe\"")) {
			pipeline = lightPipeline;
			int entries = updateDBEntriesDisplay(lightPipeline, etLightEntries);
			if(entries >= featureRequired) {
				//lightSensorProbe.unregisterListener(lightPipeline);
				//lightSensorProbe.unregisterPassiveListener(TrainFragment.this);
				lightSensorProbe.destroy();
				
				lightProbeFinished = true;
				light_stop = System.currentTimeMillis();
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
				bluetooth_stop = System.currentTimeMillis();
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
				wifi_stop = System.currentTimeMillis();
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

					ArrayList<BasicPipeline> enabledPipelines = new ArrayList<BasicPipeline>();
					if(audioPipeline.isEnabled()) {
						enabledPipelines.add(audioPipeline);
					}
					if(lightPipeline.isEnabled()) {
						enabledPipelines.add(lightPipeline);
					}
					if(bluetoothPipeline.isEnabled()) {
						enabledPipelines.add(bluetoothPipeline);
					}
					if(wifiPipeline.isEnabled()) {
						enabledPipelines.add(wifiPipeline);
					}
					
					runUpload(enabledPipelines.toArray(new BasicPipeline[enabledPipelines.size()]), ckbAutoUpload.isChecked()); 
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


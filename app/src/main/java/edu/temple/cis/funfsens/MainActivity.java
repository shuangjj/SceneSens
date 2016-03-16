package edu.temple.cis.funfsens;


import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;



public class MainActivity extends ActionBarActivity implements ActionBar.TabListener   {
	private static final String LOG_TAG = "funfsens.mainactivity";
	private Fragment trainFrag = null;
	private Fragment testFrag = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		// Test available sensors
		/*
		SensorManager sensorMgr = (SensorManager)getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors = sensorMgr.getSensorList(Sensor.TYPE_ALL);

	    for (Sensor sensor : sensors) {
	    	//sensor.get
	        Log.d("Sensor: ", "" + sensor.getName());
	    }
	    */
	    //
		ActionBar actionBar = getSupportActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowTitleEnabled(false);
		// Training tab
		Tab tab = actionBar.newTab().setText("Train").setTag("train")
				.setTabListener(this);
		actionBar.addTab(tab);
		// Test tab
		tab = actionBar.newTab().setText("App").setTag("test")
				.setTabListener(this);
		actionBar.addTab(tab);
		
		
		
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

	@Override
	public void onTabReselected(Tab arg0, FragmentTransaction ft) {

			
		
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		if(tab.getTag().equals("train")) {
			
			if(trainFrag == null) {
				trainFrag = Fragment.instantiate(this, TrainFragment.class.getName());
				ft.add(android.R.id.content, trainFrag);
				
			}else {
				//ft.attach(trainFrag);
				ft.show(trainFrag);
			}
		}else if(tab.getTag().equals("test")) {
			if(testFrag == null) {
				testFrag = Fragment.instantiate(this, TestFragment.class.getName());
				ft.add(android.R.id.content, testFrag);
				
			}else {
				//ft.attach(recognizeFrag);
				ft.show(testFrag);
			}
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
		if(tab.getTag().equals("train")) {
			if(trainFrag != null) {
				ft.hide(trainFrag);
			}
		} else if(tab.getTag().equals("test")) {
			ft.hide(testFrag);
		}
	}



}

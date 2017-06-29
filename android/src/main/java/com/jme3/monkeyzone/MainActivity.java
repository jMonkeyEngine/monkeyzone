package com.jme3.monkeyzone;

import com.jme3.app.AndroidHarness;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.view.Gravity;
import android.view.ViewGroup;
import android.os.Bundle;
import android.view.Display;
import android.util.DisplayMetrics;
import android.app.Activity;
import com.jme3.app.AndroidHarnessFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/*
		MobileAds.initialize(getApplicationContext(), getString(R.string.banner_ad_unit_id));
		AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
		*/
		
		// Calculate display in dip for better bitmap scaling.
		WindowManager windowManager = getWindowManager();
		Display display = windowManager.getDefaultDisplay();
		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		
		int widthDip = Math.round(display.getWidth() / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
		int heightDip = Math.round(display.getHeight() / (displayMetrics.ydpi / DisplayMetrics.DENSITY_DEFAULT));
		
		if (findViewById(R.id.fragment_container) != null) {
			if (savedInstanceState != null) {
                return;
            }

			AndroidHarnessFragment fragment = new AndroidHarnessFragment() {

				public void onCreate(Bundle savedInstanceState) {
					appClass = TestApplication.class.getName();
					super.onCreate(savedInstanceState);
				}

				public void createLayout() {
					super.createLayout();
					frameLayout.setVisibility(View.INVISIBLE);
				}

				public void removeSplashScreen() {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							frameLayout.setVisibility(View.VISIBLE);
						}
					});
				}
			};

			fragment.setArguments(getIntent().getExtras());
			getFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();
		}
	}
	
}

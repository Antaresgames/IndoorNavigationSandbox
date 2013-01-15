package cz.muni.fi.sandbox;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import cz.muni.fi.sandbox.R;

public class Preferences extends PreferenceActivity {

	
	private final String TAG = "Preferences";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate");
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}

package za.co.twinc.a9letterjumble;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.widget.Toolbar;
import android.view.View;

/**
 * Created by wilco on 2018/03/22.
 * 9LetterJumble
 */

public class SettingsActivity extends Activity {

    private static final String KEY_PREF_ABOUT      = "simple_text_about";
    public static final String KEY_PREF_REWARD      = "offer_rewarded_ads";
    public static final String KEY_PREF_CHALLENGE   = "challenge_reminders";
    public static final String KEY_PREF_SMART       = "smart_preference";
    public static final String KEY_PREF_DARK        = "dark_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Check for dark mode in settings
        SharedPreferences settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (settingsPref.getBoolean(SettingsActivity.KEY_PREF_DARK, false))
            setTheme(R.style.AppThemeDark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar bar = findViewById(R.id.toolbar);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }


    public static class SettingsFragment extends PreferenceFragment {

        final SharedPreferences.OnSharedPreferenceChangeListener listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                        if (s.equals(KEY_PREF_DARK)){
                            if (((SwitchPreference)findPreference(s)).isChecked())
                                getActivity().setTheme(R.style.AppThemeDark);
                            else
                                getActivity().setTheme(R.style.AppTheme);
                            getActivity().recreate();
                        }
                        else if (s.equals(KEY_PREF_CHALLENGE)){
                            if (((SwitchPreference)findPreference(s)).isChecked())
                                MainActivity.setNotification(getActivity());
                            else
                                MainActivity.cancelNotification(getActivity());
                        }
                    }
                };


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            Preference p;
            PreferenceManager preferenceManager = getPreferenceManager();

            // Set about string
            p = preferenceManager.findPreference(KEY_PREF_ABOUT);
            String versionName;
            try {
                versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            }catch (PackageManager.NameNotFoundException e){
                versionName = "Not Found";
            }
            if (p != null)
                p.setSummary(versionName);
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(listener);
        }

    }
}

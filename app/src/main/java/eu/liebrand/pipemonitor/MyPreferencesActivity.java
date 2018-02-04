package eu.liebrand.pipemonitor;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class MyPreferencesActivity extends AppCompatActivity implements OnSharedPreferenceChangeListener {

    MyPreferenceFragment prefFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefFragment=new MyPreferenceFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, prefFragment).commit();

    }

    public static class MyPreferenceFragment extends PreferenceFragment
    {
        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            PreferenceManager.setDefaultValues(getContext(), R.xml.preferences,
                        false);
            initSummary(getPreferenceScreen());
        }


        private void initSummary(Preference p) {
            if (p instanceof PreferenceGroup) {
                PreferenceGroup pGrp = (PreferenceGroup) p;
                for (int i = 0; i < pGrp.getPreferenceCount(); i++) {
                    initSummary(pGrp.getPreference(i));
                }
            } else {
                updatePrefSummary(p);
            }
        }

        public void updatePrefSummary(Preference p) {
            if (p instanceof ListPreference) {
                ListPreference listPref = (ListPreference) p;
                p.setSummary(listPref.getEntry());
            }
            if (p instanceof EditTextPreference) {
                EditTextPreference editTextPref = (EditTextPreference) p;
                if (p.getTitle().toString().toLowerCase().contains("password"))
                {
                    p.setSummary("******");
                } else {
                    p.setSummary(editTextPref.getText());
                }
            }
            if (p instanceof MultiSelectListPreference) {
                EditTextPreference editTextPref = (EditTextPreference) p;
                p.setSummary(editTextPref.getText());
            }
        }


    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        prefFragment.updatePrefSummary(prefFragment.findPreference(key));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        prefFragment.getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        prefFragment.getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

}

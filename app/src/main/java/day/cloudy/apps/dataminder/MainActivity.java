package day.cloudy.apps.dataminder;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public static class SettingsFragment extends PreferenceFragment {

        private SwitchPreference mMasterSwitch;

        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            addPreferencesFromResource(R.xml.prefs);

            mMasterSwitch = (SwitchPreference) findPreference("master");
            mMasterSwitch.setChecked(TrafficMonitorService.isRunning);
            mMasterSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean checked = (boolean) newValue;
                    TrafficMonitorService.toggle(getActivity().getApplicationContext(), checked);
                    return true;
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            EventBus.getDefault().register(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            EventBus.getDefault().unregister(this);
        }

        @Subscribe
        public void onTrafficMonitorServiceEvent(TrafficMonitorService.Event event) {
            if (null != mMasterSwitch)
                mMasterSwitch.setChecked(event.isRunning);
        }

    }

}

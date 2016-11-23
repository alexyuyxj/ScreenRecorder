package m.screenrecorder;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private static Recorder recorder;

	private ListPreference lpMaxFrameSize;
	private ListPreference lpVideoQuality;
	private EditTextPreference etpCacheFolder;
	private ListPreference lpFrameRate;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent i = getIntent();
		if (i != null && i.getBooleanExtra("STOP_RECORDER", false)) {
			finish();
			NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nm.cancel(0);
			stopRecorder();
		} else {
			createPreferences();
		}
	}

	@SuppressWarnings("deprecation")
	private void createPreferences() {
		PreferenceScreen ps = getPreferenceManager().createPreferenceScreen(this);
		setPreferenceScreen(ps);

		lpMaxFrameSize = new ListPreference(this);
		lpMaxFrameSize.setKey("srec_key_maxFrameSize");
		lpMaxFrameSize.setTitle("Max Frame Size");
		lpMaxFrameSize.setEntries(new String[]{
				"LEVEL_480_360",
				"LEVEL_1280_720",
				"LEVEL_1920_1080"
		});
		lpMaxFrameSize.setEntryValues(lpMaxFrameSize.getEntries());
		lpMaxFrameSize.setDialogTitle(lpMaxFrameSize.getTitle());
		ps.addPreference(lpMaxFrameSize);

		lpVideoQuality = new ListPreference(this);
		lpVideoQuality.setKey("srec_key_videoQuality");
		lpVideoQuality.setTitle("Video Quality");
		lpVideoQuality.setEntries(new String[]{
				"LEVEL_SUPER_LOW",
				"LEVEL_VERY_LOW",
				"LEVEL_LOW",
				"LEVEL_MEDIUN",
				"LEVEL_HIGH",
				"LEVEL_VERY_HIGH",
				"LEVEL_SUPER_HIGH"
		});
		lpVideoQuality.setEntryValues(lpVideoQuality.getEntries());
		lpVideoQuality.setDialogTitle(lpVideoQuality.getTitle());
		ps.addPreference(lpVideoQuality);

		etpCacheFolder = new EditTextPreference(this);
		etpCacheFolder.setKey("srec_key_cacheFolder");
		etpCacheFolder.setTitle("Cache Folder");
		etpCacheFolder.setDialogTitle(etpCacheFolder.getTitle());
		ps.addPreference(etpCacheFolder);

		lpFrameRate = new ListPreference(this);
		lpFrameRate.setKey("srec_key_frameRate");
		lpFrameRate.setTitle("Frame Rate");
		lpFrameRate.setEntries(new String[]{
				"15",
				"24",
				"30"
		});
		lpFrameRate.setEntryValues(lpFrameRate.getEntries());
		lpFrameRate.setDialogTitle(lpFrameRate.getTitle());
		ps.addPreference(lpFrameRate);
	}

	@SuppressWarnings("deprecation")
	protected void onResume() {
		super.onResume();
		SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
		refreshValues(sp);
		sp.registerOnSharedPreferenceChangeListener(this);
	}

	private void refreshValues(SharedPreferences sp) {
		String maxFrameSize = sp.getString(lpMaxFrameSize.getKey(), null);
		if (TextUtils.isEmpty(maxFrameSize)) {
			lpMaxFrameSize.setValueIndex(1);
		} else {
			lpMaxFrameSize.setValue(maxFrameSize);
		}
		lpMaxFrameSize.setSummary(lpMaxFrameSize.getValue());

		String videoQuality = sp.getString(lpVideoQuality.getKey(), null);
		if (TextUtils.isEmpty(videoQuality)) {
			lpVideoQuality.setValueIndex(4);
		} else {
			lpVideoQuality.setValue(videoQuality);
		}
		lpVideoQuality.setSummary(lpVideoQuality.getValue());

		String cacheFolder = sp.getString(etpCacheFolder.getKey(), null);
		etpCacheFolder.setText(TextUtils.isEmpty(cacheFolder) ? "/sdcard" : cacheFolder);
		etpCacheFolder.setSummary(etpCacheFolder.getText());

		String frameRate = sp.getString(lpFrameRate.getKey(), null);
		if (TextUtils.isEmpty(frameRate)) {
			lpFrameRate.setValueIndex(2);
		} else {
			lpFrameRate.setValue(frameRate);
		}
		lpFrameRate.setSummary(lpFrameRate.getValue());
	}

	@SuppressWarnings("deprecation")
	protected void onPause() {
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
		super.onPause();
	}

	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		refreshValues(sp);
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem mi = menu.add("Start");
		mi.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return super.onCreateOptionsMenu(menu);
	}

	@SuppressWarnings("deprecation")
	public boolean onOptionsItemSelected(MenuItem item) {
		if (recorder == null) {
			recorder = new Recorder(this);
			recorder.askPermission(1);
		} else {
			Toast.makeText(this, "Already Started", Toast.LENGTH_SHORT).show();
		}
		return super.onOptionsItemSelected(item);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			String maxFrameSize = lpMaxFrameSize.getValue();
			String videoQuality = lpVideoQuality.getValue();
			String cacheFolder = etpCacheFolder.getText();
			String frameRate = lpFrameRate.getValue();
			if (recorder.start(maxFrameSize, videoQuality, cacheFolder, frameRate, resultCode, data)) {
				showNotification();
				finish();
			} else {
				recorder = null;
			}
		} else {
			Toast.makeText(this, "User Cancelled", Toast.LENGTH_SHORT).show();
		}
	}

	private void showNotification() {
		Notification.Builder builder = new Notification.Builder(this);
		builder.setContentTitle(getString(R.string.app_name));
		builder.setContentText("Click to Stop");
		builder.setSmallIcon(R.drawable.ic_launcher);
		Intent i = new Intent();
		i.setComponent(new ComponentName(getPackageName(), getClass().getName()));
		i.putExtra("STOP_RECORDER", true);
		builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, i, 0));
		builder.setWhen(System.currentTimeMillis());
		Notification not = builder.build();
		not.flags = Notification.FLAG_NO_CLEAR;
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.notify(0, not);
	}

	private void stopRecorder() {
		if (recorder != null) {
			recorder.stop();
			recorder = null;
		}
		Toast.makeText(this, "Recorder Stopped", Toast.LENGTH_SHORT).show();
	}

}
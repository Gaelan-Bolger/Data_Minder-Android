package day.cloudy.apps.dataminder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;

import jp.co.recruit_lifestyle.android.floatingview.FloatingView;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewListener;
import jp.co.recruit_lifestyle.android.floatingview.FloatingViewManager;

public class TrafficMonitorService extends Service implements TrafficMonitor.Listener {

    private static final String TAG = TrafficMonitorService.class.getSimpleName();
    private static final String EXTRA_START = "start";
    private static final String ACTION_RESET_TOTALS = "reset_totals";
    private static final int NOTIFICATION_ID = 1014;

    public static boolean isRunning = false;
    private TrafficMonitor mTrafficMonitor;
    private FloatingViewManager mFloatingViewManager;

    private FloatingView mFloatingView;
    private TextView mTxText;
    private TextView mRxText;

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case "fullscreen":
                    updateDisplayMode();
                    break;
            }
        }
    };

    public static void toggle(Context context, boolean start) {
        Intent intent = new Intent(context, TrafficMonitorService.class);
        intent.putExtra(EXTRA_START, start);
        context.startService(intent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mPreferenceListener);
        mFloatingViewManager = new FloatingViewManager(this, new FloatingViewListener() {
            @Override
            public void onFinishFloatingView() {
                stopService();
            }
        });
        mFloatingViewManager.setFixedTrashIconImage(R.drawable.ic_delete_white_24dp);
        mFloatingViewManager.setActionTrashIconImage(R.drawable.ic_trash_action);
        updateDisplayMode();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mPreferenceListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null == intent) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (!TextUtils.isEmpty(action)) {
            switch (action) {
                case ACTION_RESET_TOTALS:
                    if (null != mTrafficMonitor)
                        mTrafficMonitor.resetTotals();
                    return START_STICKY;
            }
        }

        if (intent.hasExtra(EXTRA_START) && intent.getBooleanExtra(EXTRA_START, false)) {
            showNotification();
            addFloatingView();
            initTrafficMonitor();
            isRunning = true;
            return START_STICKY;
        }

        stopService();
        return START_NOT_STICKY;
    }

    @Override
    public void onTrafficUpdate(final long tx, final long rx) {
        mFloatingView.post(new Runnable() {
            @Override
            public void run() {
                String txMsg = Formatter.formatFileSize(TrafficMonitorService.this, tx);
                mTxText.setText(String.format(getString(R.string.floating_text_tx), txMsg));
                String rxMsg = Formatter.formatFileSize(TrafficMonitorService.this, rx);
                mRxText.setText(String.format(getString(R.string.floating_text_rx), rxMsg));

//                mFloatingView.moveToEdgeIfNotMoveAccept();
            }
        });
    }

    private void initTrafficMonitor() {
        if (null == mTrafficMonitor)
            mTrafficMonitor = TrafficMonitor.startMonitoring(this, this);
        Log.d(TAG, "Traffic Monitor initiated");
    }

    private void killTrafficMonitor() {
        if (null != mTrafficMonitor)
            mTrafficMonitor.stopMonitoring();
        mTrafficMonitor = null;
        Log.d(TAG, "Traffic Monitor killed");
    }

    private void addFloatingView() {
        if (null == mFloatingView) {
            View view = View.inflate(this, R.layout.floating_text, null);
            mTxText = (TextView) view.findViewById(R.id.tx_text);
            mRxText = (TextView) view.findViewById(R.id.rx_text);
            FloatingViewManager.Options options = new FloatingViewManager.Options();
            options.shape = FloatingViewManager.SHAPE_RECTANGLE;
            mFloatingView = mFloatingViewManager.addViewToWindow(view, options);
        }
    }

    private void removeFloatingView() {
        if (null != mFloatingView) {
            mFloatingViewManager.removeAllViewToWindow();
            mFloatingView = null;
        }
    }

    private void updateDisplayMode() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean fullscreen = sharedPreferences.getBoolean("fullscreen", false);
        if (null != mFloatingViewManager)
            mFloatingViewManager.setDisplayMode(fullscreen ? FloatingViewManager.DISPLAY_MODE_SHOW_ALWAYS : FloatingViewManager.DISPLAY_MODE_HIDE_FULLSCREEN);
    }

    private void stopService() {
        killTrafficMonitor();
        removeFloatingView();
        removeNotification();
        stopSelf();
        isRunning = false;
        EventBus.getDefault().post(new Event(isRunning));
    }

    private void showNotification() {
        Notification notification = new NotificationCompat.Builder(this)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_running))
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0))
                .addAction(android.R.drawable.ic_menu_revert, getString(R.string.reset), getResetTotalsPendingIntent())
                .addAction(android.R.drawable.ic_delete, getString(R.string.stop), getStopServicePendingIntent())
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private PendingIntent getResetTotalsPendingIntent() {
        Intent resetTotalsIntent = new Intent(this, TrafficMonitorService.class);
        resetTotalsIntent.setAction(ACTION_RESET_TOTALS);
        return PendingIntent.getService(this, 0, resetTotalsIntent, 0);
    }

    private PendingIntent getStopServicePendingIntent() {
        Intent stopServiceIntent = new Intent(this, TrafficMonitorService.class);
        stopServiceIntent.putExtra(EXTRA_START, false);
        return PendingIntent.getService(this, 0, stopServiceIntent, 0);
    }

    private void removeNotification() {
        getNotificationManager().cancel(NOTIFICATION_ID);
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public class Event {

        final boolean isRunning;

        public Event(boolean isRunning) {
            this.isRunning = isRunning;
        }

    }

}

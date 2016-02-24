package day.cloudy.apps.dataminder;

import android.content.Context;
import android.net.TrafficStats;
import android.preference.PreferenceManager;

import java.util.concurrent.atomic.AtomicBoolean;

public class TrafficMonitor {

    public interface Listener {
        void onTrafficUpdate(long tx, long rx);
    }

    private static TrafficMonitor mInstance;
    private static Listener mListener;

    public static TrafficMonitor startMonitoring(Context context, Listener listener) {
        if (null == mInstance)
            mInstance = new TrafficMonitor(context);
        mListener = listener;
        return mInstance;
    }

    private final AtomicBoolean mMonitoring = new AtomicBoolean(false);
    private final Context mContext;

    private long mStartingRxBytes;
    private long mStartingTxBytes;

    private long mTotalTxBytes;
    private long mTotalRxBytes;

    private TrafficMonitor(Context context) {
        mContext = context;
        resetTotals();

        mMonitoring.set(true);
        new Thread(new Runnable() {
            public void run() {
                while (mMonitoring.get()) {
                    long newTotalTxBytes = TrafficStats.getTotalTxBytes();
                    long newTotalRxBytes = TrafficStats.getTotalRxBytes();

                    String mode = PreferenceManager.getDefaultSharedPreferences(mContext).getString("mode", "totals");
                    if (mode.equals("totals")) {
                        notifyListener(newTotalTxBytes - mStartingTxBytes, newTotalRxBytes - mStartingRxBytes);
                    } else if (mode.equals("speeds")) {
                        notifyListener(newTotalTxBytes - mTotalTxBytes, newTotalRxBytes - mTotalRxBytes);
                    }

                    mTotalTxBytes = newTotalTxBytes;
                    mTotalRxBytes = newTotalRxBytes;

                    try {
                        Thread.sleep(900);
                    } catch (InterruptedException ignore) {
                    }
                }
            }

            private void notifyListener(long tx, long rx) {
                if (null != mListener)
                    mListener.onTrafficUpdate(tx, rx);
            }
        }).start();
    }

    public void resetTotals() {
        mStartingRxBytes = TrafficStats.getTotalRxBytes();
        mStartingTxBytes = TrafficStats.getTotalTxBytes();
    }

    public void stopMonitoring() {
        mMonitoring.set(false);
        mInstance = null;
    }
}

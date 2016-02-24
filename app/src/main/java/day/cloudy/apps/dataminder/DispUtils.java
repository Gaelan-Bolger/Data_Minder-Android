package day.cloudy.apps.dataminder;

import android.content.res.Resources;

public class DispUtils {

    public static int dp(int val) {
        return (int) (Resources.getSystem().getDisplayMetrics().density * val);
    }

}

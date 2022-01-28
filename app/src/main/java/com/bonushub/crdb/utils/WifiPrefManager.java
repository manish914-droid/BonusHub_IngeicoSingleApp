package com.bonushub.crdb.utils;
import android.content.Context;
import android.content.SharedPreferences;

public class WifiPrefManager {

    Context context;

    public WifiPrefManager(Context context) {
        this.context = context;
    }

    public void saveFreshApps(String freshApp) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("WifiDetails", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("freshApp", freshApp);
        editor.commit();
    }

    public String getAppStatus() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("WifiDetails", Context.MODE_PRIVATE);
        return sharedPreferences.getString("freshApp", "");
    }

    public boolean isWifiStatus() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("WifiDetails", Context.MODE_PRIVATE);
        boolean isWifiEmpty = sharedPreferences.getString("freshApp", "").isEmpty();

        return isWifiEmpty;
    }
}
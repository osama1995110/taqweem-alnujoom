package com.nujoom.calendar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int REQ_LOCATION = 2;
    private static final int REQ_NOTIF = 3;
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0b1622"));
        getWindow().setNavigationBarColor(Color.parseColor("#0e1a26"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0b1622"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);

        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) return false;
                Intent i = new Intent(MainActivity.this, QuranActivity.class);
                i.putExtra("url", u.toString());
                startActivity(i);
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("(window.handleBack && handleBack()) ? '1' : '0'", value -> {
            if (value == null || !value.contains("1")) finish();
        });
    }

    /* ---------------- Location ---------------- */

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocation() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        fetchLocation();
    }

    private void fetchLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) { sendLocationError("خدمة الموقع غير متاحة"); return; }
        Location best = null;
        try {
            for (String p : lm.getProviders(true)) {
                Location l = lm.getLastKnownLocation(p);
                if (l != null && (best == null || l.getTime() > best.getTime())) best = l;
            }
        } catch (SecurityException ignored) { }

        if (best != null && System.currentTimeMillis() - best.getTime() < 6L * 3600000L) {
            sendLocation(best);
            return;
        }
        String provider = null;
        if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) provider = LocationManager.NETWORK_PROVIDER;
        else if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) provider = LocationManager.GPS_PROVIDER;
        if (provider == null) {
            if (best != null) sendLocation(best);
            else sendLocationError("فعّل خدمة الموقع (GPS) ثم حاول مرة أخرى");
            return;
        }
        final Location fallback = best;
        try {
            lm.requestSingleUpdate(provider, new LocationListener() {
                @Override public void onLocationChanged(Location location) { sendLocation(location); }
                @Override public void onStatusChanged(String p, int status, Bundle extras) { }
                @Override public void onProviderEnabled(String p) { }
                @Override public void onProviderDisabled(String p) {
                    if (fallback != null) sendLocation(fallback);
                    else sendLocationError("فعّل خدمة الموقع (GPS) ثم حاول مرة أخرى");
                }
            }, Looper.getMainLooper());
        } catch (SecurityException e) {
            sendLocationError("لم يُمنح إذن الموقع");
        }
    }

    private void sendLocation(final Location l) {
        final double lat = l.getLatitude();
        final double lng = l.getLongitude();
        new Thread(() -> {
            String name = "";
            try {
                if (Geocoder.isPresent()) {
                    List<Address> list = new Geocoder(MainActivity.this, new Locale("ar"))
                            .getFromLocation(lat, lng, 1);
                    if (list != null && !list.isEmpty()) {
                        Address a = list.get(0);
                        if (a.getLocality() != null) name = a.getLocality();
                        else if (a.getSubAdminArea() != null) name = a.getSubAdminArea();
                        else if (a.getAdminArea() != null) name = a.getAdminArea();
                    }
                }
            } catch (Exception ignored) { }
            final String js = "onNativeLocation(" + lat + "," + lng + "," + JSONObject.quote(name) + ")";
            runOnUiThread(() -> web.evaluateJavascript(js, null));
        }).start();
    }

    private void sendLocationError(String msg) {
        final String js = "onNativeLocationError('" + msg.replace("'", "") + "')";
        runOnUiThread(() -> web.evaluateJavascript(js, null));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (hasLocationPermission()) fetchLocation();
            else sendLocationError("لم يُمنح إذن الموقع، اختر مدينتك من القائمة");
        }
    }

    /* ---------------- JS bridge ---------------- */

    private class Bridge {
        @JavascriptInterface
        public void scheduleAlarms(String json) {
            AlarmScheduler.save(getApplicationContext(), json);
        }

        @JavascriptInterface
        public void requestLocation() {
            runOnUiThread(MainActivity.this::startLocation);
        }

        @JavascriptInterface
        public void openQuran(int surah) {
            Intent i = new Intent(MainActivity.this, QuranActivity.class);
            i.putExtra("url", "https://quran.com/" + surah);
            startActivity(i);
        }

        @JavascriptInterface
        public void vibrate(int ms) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        }
    }
}

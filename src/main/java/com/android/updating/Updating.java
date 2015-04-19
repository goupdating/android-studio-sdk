package com.android.updating;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by sayagodshala on 3/3/2015.
 */
public class Updating {

    public interface UpdatingEventListener {
        public void onUpdatingLoaded(JSONObject item);
    }

    private TelephonyManager telephonyManager;
    private PackageManager packageManager;
    private int hasPhoneStatePerm = -1;
    private static final String ERROR_COMMON = "Unexpected Error occcured! [Most common Error: Device might not be connected to Internet or remote server is not up and running]";
    private static final String ERROR_404 = "Requested resource not found : 404";
    private static final String ERROR_500 = "Something went wrong at server end : 500";
    private static final String ERROR_INVALID = "Error Occured [Server's JSON response might be invalid]!";
    private static final String BASE_URL = "http://updating.herokuapp.com/";
    private static final String GET_UPDATE = BASE_URL
            + "mobileapp/updateavailable";

    private Context mContext;
    private String mApiKey;
    private RequestParams params;
    private static Updating instance = null;
    private UpdatingSDKAlertDialog updatingAlertDialog;

    public Updating(Context context, String apiKey) {
        mContext = context;
        mApiKey = apiKey;

        try {
            getALLNecessaryObjects();
            execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getALLNecessaryObjects() {
        packageManager = mContext.getPackageManager();
        hasPhoneStatePerm = packageManager.checkPermission(
                Manifest.permission.READ_PHONE_STATE,
                mContext.getPackageName());
        if (hasPhoneStatePerm == PackageManager.PERMISSION_GRANTED) {
            telephonyManager = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
        }
    }

    public static Updating init(Context context, String apiKey) {
        if (instance == null) {
            instance = new Updating(context, apiKey);
        }
        return instance;
    }

    private void execute() {
        params = new RequestParams();
        try {
            params.put("appKey", mApiKey);
            params.put("appVersion", packageInfo().getString("version_code"));
            params.put("deviceId", deviceInfo().getString("deviceId"));
            params.put("country", deviceInfo().getString("country"));
            params.put("operator", deviceInfo().getString("operator"));
            params.put("manufacture", deviceInfo().getString("manufacture"));
            params.put("model", deviceInfo().getString("model"));
            params.put("sdk", deviceInfo().getString("sdk"));
            params.put("os", deviceInfo().getString("os"));
            params.put("release", deviceInfo().getString("release"));
            invokeUpdateService(params);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void invokeUpdateService(RequestParams params) {
        Log.d("Updating Params : ", params.toString());
        AsyncHttpClient client = new AsyncHttpClient();
        client.post(GET_UPDATE, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  byte[] response) {
                try {

                    JSONObject responseObj = new JSONObject(
                            new String(response));
                    Log.d("Updating Response", responseObj.toString());

                    if (!responseObj.isNull("error")) {
                        if (!responseObj.getBoolean("error")) {
                            if (responseObj.getBoolean("updateAvailable")) {
                                showUpdateDialog(responseObj);
                            } else {
                                if (responseObj.has("developerMessage")) {
                                    if (!responseObj.getString("developerMessage").equalsIgnoreCase("")) {
                                        showMessageDialog(responseObj);
                                    }
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {
                if (statusCode == 404) {
                    Log.d("Updating Status Code", ERROR_404);
                } else if (statusCode == 500) {
                    Log.d("Updating Status Code", ERROR_500);
                } else {
                    Log.d("Updating Status Code", ERROR_COMMON);
                }
            }
        });
    }

    private JSONObject packageInfo() {
        JSONObject packageDetail = new JSONObject();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    mContext.getPackageName(), 0);
            packageDetail.put("package_name", mContext.getPackageName());
            packageDetail.put("version_code", packageInfo.versionCode);
            packageDetail.put("version_name", packageInfo.versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packageDetail;
    }

    private JSONObject deviceInfo() {

        Locale locale = Locale.getDefault();
        JSONObject device = new JSONObject();
        try {

            device.put("deviceId", uniqueDeviceID(mContext));
            device.put("release", Build.VERSION.RELEASE + "");
            device.put("sdk", Build.VERSION.SDK_INT + "");
            device.put("manufacture", Build.MANUFACTURER);
            device.put("model", Build.MODEL);

            if (!getOsName().equalsIgnoreCase("")) {
                device.put("os", getOsName());
            } else {
                device.put("os", "");
            }

            if (telephonyManager != null) {
                String operator = "";
                if (!telephonyManager.getSimOperatorName().equalsIgnoreCase("")) {
                    operator = telephonyManager.getSimOperatorName();
                }

                if (operator.toLowerCase().contains("idea") || operator.toLowerCase().contains("!dea"))
                    operator = "idea";

                device.put("operator", operator);

                String countryCode = locale.getCountry();

                if (!telephonyManager.getSimCountryIso().equalsIgnoreCase("")) {
                    locale = new Locale("", telephonyManager.getSimCountryIso().toUpperCase());
                    countryCode = locale.getCountry();
                }

                if (!telephonyManager.getNetworkCountryIso().equalsIgnoreCase("")) {
                    if (countryCode.equalsIgnoreCase("")) {
                        locale = new Locale("", telephonyManager.getNetworkCountryIso()
                                .toUpperCase());
                        countryCode = locale.getCountry();
                    }
                }
                device.put("country", countryCode);
            } else {
                device.put("operator", "");
                device.put("country", locale.getCountry());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return device;
    }

    private String uniqueDeviceID(Context context) {

        UUID deviceUuid;
        String tmDevice = "", tmSerial = "", androidId = "";
        String deviceIdStr = "";

        try {
            if (telephonyManager != null) {
                tmDevice = "" + telephonyManager.getDeviceId();
                tmSerial = "" + telephonyManager.getSimSerialNumber();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        androidId = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        if (androidId == null) {
            androidId = "";
        }


        String build = "";
        if (android.os.Build.SERIAL != null) {
            build = android.os.Build.SERIAL;
        }

        try {
            if (!tmDevice.equalsIgnoreCase("") && !tmSerial.equalsIgnoreCase("")) {
                deviceUuid = new UUID(androidId.hashCode(),
                        ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
                deviceIdStr = deviceUuid.toString();
            } else {
                deviceUuid = new UUID(androidId.hashCode(), build.hashCode());
                deviceIdStr = deviceUuid.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d("ANDROID_UUID", deviceIdStr + "");
        return deviceIdStr;
    }

    private String getOsName() {

        String os = "";

        try {

            Field[] fields = Build.VERSION_CODES.class.getFields();
            for (Field field : fields) {
                os = field.getName();
                int fieldValue = -1;

                try {
                    fieldValue = field.getInt(new Object());
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return os;

    }

    private static void openPlayStore(Context context) {
        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            context.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("http://play.google.com/store/apps/details?id="
                            + context.getPackageName())));
        }
    }

    private void showUpdateDialog(JSONObject responseObj) {
        updatingAlertDialog = new UpdatingSDKAlertDialog(mContext, responseObj,
                "Update", "",
                new UpdatingSDKAlertDialog.UpdatingSDKAlertDialogListener() {
                    @Override
                    public void onButton1Action() {
                        openPlayStore(mContext);
                    }

                    @Override
                    public void onButton2Action() {

                    }
                });
        updatingAlertDialog.show();
    }

    private void showMessageDialog(JSONObject responseObj) {
        updatingAlertDialog = new UpdatingSDKAlertDialog(mContext, responseObj,
                "Ok", "",
                new UpdatingSDKAlertDialog.UpdatingSDKAlertDialogListener() {
                    @Override
                    public void onButton1Action() {
                    }

                    @Override
                    public void onButton2Action() {
                    }
                });
        updatingAlertDialog.show();
    }
}

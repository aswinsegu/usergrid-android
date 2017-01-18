package org.apache.usergrid.push;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.firebase.iid.FirebaseInstanceId;

import org.apache.usergrid.android.UsergridAsync;
import org.apache.usergrid.android.UsergridSharedDevice;
import org.apache.usergrid.android.callbacks.UsergridResponseCallback;
import org.apache.usergrid.java.client.Usergrid;
import org.apache.usergrid.java.client.UsergridClient;
import org.apache.usergrid.java.client.UsergridClientConfig;
import org.apache.usergrid.java.client.UsergridEnums;
import org.apache.usergrid.java.client.UsergridRequest;
import org.apache.usergrid.java.client.response.UsergridResponse;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    public static String FCM_TOKEN;

    // App level Properties
    private static final String TAG = "MainActivity";
    private static final String NOTIFICATION_MESSAGE = "Hello Test Notification";
    private static final String NOTIFICATION_GROUP_MESSAGE = "Hello Test Group Notification";
    private static final String USERGRID_PREFS_FILE_NAME = "usergrid_prefs.xml";

    public static boolean USERGRID_PREFS_NEEDS_REFRESH = false;
    public static int BALANCE = 10;

    // Usergrid Properties
    public static String BASE_URL = "http://ec2-52-90-148-67.compute-1.amazonaws.com:8080";
    public static String ORG_ID = "usergrid";
    public static String APP_ID = "sandbox";
    public static String NOTIFIER_ID = "firebasePN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUsergridInstance();
        retrieveSavedPrefs();

        final ImageButton infoButton = (ImageButton) findViewById(R.id.infoButton);
        if( infoButton != null ) {
            final Intent settingsActivity = new Intent(this, SettingsActivity.class);
            infoButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.startActivity(settingsActivity);
                }
            });
        }

        final Button pushToThisDeviceButton = (Button) findViewById(R.id.pushToThisDevice);
        if( pushToThisDeviceButton != null ) {
            pushToThisDeviceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.sendPush(UsergridSharedDevice.getSharedDeviceUUID(MainActivity.this),NOTIFICATION_MESSAGE);
                }
            });
        }

        final Button pushToAllDevicesButton = (Button) findViewById(R.id.pushToAllDevices);
        if( pushToAllDevicesButton != null ) {
            pushToAllDevicesButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.sendPush("*",NOTIFICATION_GROUP_MESSAGE);
                }
            });
        }

        // Balance Usecase
        final EditText balanceAmtText = (EditText) findViewById(R.id.balanceAmt);
        balanceAmtText.setInputType(InputType.TYPE_CLASS_NUMBER);

        final Button updateBalanceButton = (Button) findViewById(R.id.updateBalance);
        if( updateBalanceButton != null ) {
            updateBalanceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if( balanceAmtText != null ) {
                        MainActivity.BALANCE = Integer.parseInt(balanceAmtText.getText().toString());
                        updateBalance(UsergridSharedDevice.getSharedDeviceUUID(MainActivity.this), MainActivity.BALANCE);
                    }
                }
            });
        }

        // Wireless Usecase
        final Button activateWirelessButton = (Button) findViewById(R.id.activateWireless);
        if( activateWirelessButton != null ) {
            activateWirelessButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateWirelessStatus(UsergridSharedDevice.getSharedDeviceUUID(MainActivity.this), "Active");
                }
            });
        }
        final Button deActivateWirelessButton = (Button) findViewById(R.id.deactivateWireless);
        if( deActivateWirelessButton != null ) {
            deActivateWirelessButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateWirelessStatus(UsergridSharedDevice.getSharedDeviceUUID(MainActivity.this), "Inactive");
                }
            });
        }
    }

    public void updateBalance(@NonNull final String deviceId, @NonNull final int balance) {
        Log.i(TAG, "Description : Updating Balance");

        HashMap<String,Integer> updateMap = new HashMap<>();
        updateMap.put("balance", balance);

        UsergridRequest notificationRequest = new UsergridRequest(UsergridEnums.UsergridHttpMethod.PUT,UsergridRequest.APPLICATION_JSON_MEDIA_TYPE,Usergrid.clientAppUrl(),null,updateMap,Usergrid.authForRequests(),"devices", deviceId);
        UsergridAsync.sendRequest(notificationRequest, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NonNull UsergridResponse response) {
                Log.i(TAG, "Updated Balance successfully :" + response.getResponseJson());
                if(!response.ok() && response.getResponseError() != null) {
                    Log.i(TAG, "Error Description :" + response.getResponseJson());
                }
            }
        });
    }

    public void updateWirelessStatus(@NonNull final String deviceId, @NonNull final String status) {
        Log.i(TAG, "Description : Updating Wireless Status");

        HashMap<String,String> updateMap = new HashMap<>();
        updateMap.put("wireless", status);

        UsergridRequest notificationRequest = new UsergridRequest(UsergridEnums.UsergridHttpMethod.PUT,UsergridRequest.APPLICATION_JSON_MEDIA_TYPE,Usergrid.clientAppUrl(),null,updateMap,Usergrid.authForRequests(),"devices", deviceId);
        UsergridAsync.sendRequest(notificationRequest, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NonNull UsergridResponse response) {
                Log.i(TAG, "Updated Wireless Status successfully :" + response.getResponseJson());
                if(!response.ok() && response.getResponseError() != null) {
                    Log.i(TAG, "Error Description :" + response.getResponseJson());
                }
            }
        });
    }

    @Override
    protected void onResume() {
        if( USERGRID_PREFS_NEEDS_REFRESH ) {
            Usergrid.setConfig(new UsergridClientConfig(ORG_ID,APP_ID,BASE_URL));
            if( FCM_TOKEN != null && !FCM_TOKEN.isEmpty() ) {
                UsergridAsync.applyPushToken(this, FCM_TOKEN, MainActivity.NOTIFIER_ID, new UsergridResponseCallback() {
                    @Override
                    public void onResponse(@NonNull UsergridResponse response) { }
                });
            }
            this.savePrefs();
            USERGRID_PREFS_NEEDS_REFRESH = false;
        } else {
            try {
                FCM_TOKEN = FirebaseInstanceId.getInstance().getToken();
                if( FCM_TOKEN != null ) {
                    UsergridAsync.applyPushToken(this, FCM_TOKEN, MainActivity.NOTIFIER_ID, new UsergridResponseCallback() {
                        @Override
                        public void onResponse(@NotNull UsergridResponse response) { }
                    });
                }
            } catch (Exception ignored) { }
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        this.savePrefs();
        super.onDestroy();
    }

    // Utility Methods
    public static UsergridClient initUsergridInstance() {
        return Usergrid.initSharedInstance(ORG_ID,APP_ID,BASE_URL);
    }

    public static void registerPush(@NonNull final Context context, @NonNull final String registrationId) {
        initUsergridInstance();
        MainActivity.FCM_TOKEN = registrationId;
        UsergridAsync.applyPushToken(context, registrationId, MainActivity.NOTIFIER_ID, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NonNull UsergridResponse response) {
                Log.i(TAG, "Response :" + response.getResponseJson());
                if( !response.ok() ) {
                    Log.i(TAG, "Error Description :" + response.getResponseJson());
                }
            }
        });
    }

    public void sendPush(@NonNull final String deviceId, @NonNull final String message) {
        Log.i(TAG, "Description : Sending PN");

        HashMap<String,String> notificationMap = new HashMap<>();
        notificationMap.put(MainActivity.NOTIFIER_ID,message);

        HashMap<String,HashMap<String,String>> payloadMap = new HashMap<>();
        payloadMap.put("payloads",notificationMap);

        UsergridRequest notificationRequest = new UsergridRequest(UsergridEnums.UsergridHttpMethod.POST,UsergridRequest.APPLICATION_JSON_MEDIA_TYPE,Usergrid.clientAppUrl(),null,payloadMap,Usergrid.authForRequests(),"devices", deviceId, "notifications");
        UsergridAsync.sendRequest(notificationRequest, new UsergridResponseCallback() {
            @Override
            public void onResponse(@NonNull UsergridResponse response) {
                Log.i(TAG, "Push request completed successfully :" + response.getResponseJson());
                if(!response.ok() && response.getResponseError() != null) {
                    Log.i(TAG, "Error Description :" + response.getResponseJson());
                }
            }
        });
    }

    public void savePrefs() {
        SharedPreferences prefs = this.getSharedPreferences(USERGRID_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("ORG_ID", ORG_ID);
        editor.putString("APP_ID", APP_ID);
        editor.putString("BASE_URL", BASE_URL);
        editor.putString("NOTIFIER_ID", NOTIFIER_ID);
        editor.apply();
    }

    public void retrieveSavedPrefs() {
        SharedPreferences prefs = this.getSharedPreferences(USERGRID_PREFS_FILE_NAME, Context.MODE_PRIVATE);
        ORG_ID = prefs.getString("ORG_ID", ORG_ID);
        APP_ID = prefs.getString("APP_ID", APP_ID);
        BASE_URL = prefs.getString("BASE_URL",BASE_URL);
        NOTIFIER_ID = prefs.getString("NOTIFIER_ID",NOTIFIER_ID);
    }
}
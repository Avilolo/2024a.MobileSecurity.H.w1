package com.example.a2024ahw1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;

public class LoginActivity extends AppCompatActivity {
    private AppCompatButton loginBtn;
    private TextInputEditText  battery_etv;
    private MaterialTextView direction_txt;
    private float batteryLevel;
    private int contactSize;
    private float latestAzimuth = 0.0f;
    private SensorManager sensorManager;
    private Sensor magnetometerSensor;


    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctxt, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            batteryLevel = level * 100 / (float)scale;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        findViews();
        permissionClicked();
        if (magnetometerSensor == null) {
            // The device doesn't have a magnetometer sensor
            // Handle the case where the sensor is not available
        } else {
            // Register a listener for the magnetometer sensor
            // You'll need to define your own SensorEventListener
            sensorManager.registerListener(sensorEventListener, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String batteryText = battery_etv.getText().toString();
                if (!batteryText.equals(null) || !batteryText.isEmpty()) {
                    try {
                        contactSize = 0;    //make sure it will not add old size to new size if added new contact after clicking
                        getContactList();
                        float floatValue = Float.parseFloat(batteryText);
                        if (batteryLevel == floatValue
                                && isFacingNorth(latestAzimuth)
                                && contactSize % 2 == 0) {
                            Toast.makeText(getApplicationContext(), "Success"
                                    , Toast.LENGTH_SHORT).show();
                        } else
                            Toast.makeText(getApplicationContext(), "Failure"
                                    , Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(getApplicationContext(), "Failure, battery DOES NOT match input", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                else
                    Toast.makeText(getApplicationContext(), "Failure, battery DOES NOT match input", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void findViews() {
        loginBtn = findViewById(R.id.login_btn);
        direction_txt = findViewById(R.id.direction_etv);
        battery_etv = findViewById(R.id.battery_etv);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        contactSize = 0;
    }

    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] magneticValues = event.values;
            float azimuth = (float) Math.atan2(magneticValues[1], magneticValues[0]);
            azimuth = (float) Math.toDegrees(azimuth);
            if (azimuth < 0) {
                azimuth += 360;
            }
            latestAzimuth = azimuth;
            // Now, azimuth holds the degrees from north
            // You can compare it to a desired range for "facing north"
            if (isFacingNorth(azimuth)) {
                direction_txt.setBackgroundResource(R.drawable.login_textfield_bg_green);
            } else {
                direction_txt.setBackgroundResource(R.drawable.login_textfield_bg_red);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not used in this example
        }
    };

    @SuppressLint("Range")
    private void getContactList() {
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                @SuppressLint("Range") String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                @SuppressLint("Range") String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }
                    pCur.close();
                    contactSize++;
                }
            }
        }
        if(cur!=null){
            cur.close();
        }
    }

    private void permissionClicked() {
        ActivityCompat.requestPermissions(LoginActivity.this,
                new String[]{"android.permission.READ_CONTACTS"},
                1);
    }

    private boolean isFacingNorth(float azimuth) {
        // Define a threshold angle for "facing north"
        // For example, if you consider facing north within +/- 45 degrees
        return (azimuth >= 315 || azimuth <= 45);
    }
}
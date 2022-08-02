package com.rolex.secquraise;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {
    Button update;
    TextView imeitext,internet_connectivity,battery_percentage,battery_charging_status,location_text;
    String IMEINumber;
    Context context;
    FusedLocationProviderClient mFusedLocationClient;
    ArrayList<Map<String, String>> test;
    private static final int REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=getApplicationContext();
        imeitext = findViewById(R.id.IMEI);
        update = findViewById(R.id.Update);
        battery_percentage = findViewById(R.id.Battery_Percentage);
        internet_connectivity = findViewById(R.id.Internet_Connectivity);
        battery_charging_status = findViewById(R.id.Battery_Charging_Status);
        location_text = findViewById(R.id.Location);
         test = new ArrayList<Map<String, String>>();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
       //button to update data
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adddata();
                for(Map<String,String> mydata: test){
                    postdata(mydata);
                }
            }
        });


                getLastLocation();

                this.registerReceiver(this.broadcastReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//to check for permissions
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(context.TELEPHONY_SERVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE);
                    return;
                }
  if(Build.VERSION.SDK_INT!=29) {
       IMEINumber = telephonyManager.getImei(1);
  }
                imeitext.setText(IMEINumber);
                checkConnection();
//handler loop for every 5 min
        final Handler handler = new Handler();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getLastLocation();
                checkConnection();
                MainActivity.this.registerReceiver(MainActivity.this.broadcastReceiver, new
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                Date date = new Date();
                handler.postDelayed(this, 300000);
                adddata();
                if(checkConnection()){
                    for(Map<String,String> mydata: test){
                        postdata(mydata);
                    }
                }
            }
        }, 300000);
//a timer to contnously check for the internet and only call api when internet is available
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                checkConnection();
                if(checkConnection()&& test!=null){
                    for(Map<String,String> mydata: test){
                        postdata(mydata);
                    }

                }
            }
        }, 0, 1000);




    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show();
                    getLastLocation();
                } else {
                    Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    //to check internet connection
    private boolean checkConnection() {
        ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting();
        internet_connectivity.setText(String.valueOf(isConnected));
        return isConnected;
    }
// for battery data
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            battery_percentage.setText("Battery Percentage: " + level);
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status ==
                    BatteryManager.BATTERY_STATUS_FULL;
            battery_charging_status.setText(String.valueOf(isCharging));


        }
    };

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        // check if permissions are given
        if (checkPermissions()) {

            // check if location is enabled
            if (isLocationEnabled()) {

                // getting last
                // location from
                // FusedLocationClient
                // object
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            location_text.setText(""+location.getLatitude()+", "+location.getLongitude());

                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            // if permissions aren't available,
            // request for permissions
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {

        // Initializing LocationRequest
        // object with appropriate methods
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5);
        mLocationRequest.setFastestInterval(0);
        mLocationRequest.setNumUpdates(1);

        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    private LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
           location_text.setText(""+mLastLocation.getLatitude()+mLastLocation.getLongitude());
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (checkPermissions()) {
            getLastLocation();
        }
    }
    public void postdata(Map<String,String> data){


        RequestQueue MyRequestQueue = Volley.newRequestQueue(this);
        String url = "http://143.244.138.96:2110/api/status";
        StringRequest MyStringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {

            public void onResponse(String response) {
                //on successfull data submission we clear the arraylist
                test.clear();
                Toast.makeText(MainActivity.this, "Uploaded Succefully.", Toast.LENGTH_SHORT).show();

                //This code is executed if the server responds, whether or not the response contains data.
                //The String 'response' contains the server's response.
            }
        }, new Response.ErrorListener() { //Create an error listener to handle errors appropriately.

            public void onErrorResponse(VolleyError error) {
//if api call fails the data still exits in the array and will be uploaded when the internet is accessible
                //This code is executed if there is an error.
            }
        }) {
            protected Map<String, String> getParams() {
                return data;
            }
        };


        MyRequestQueue.add(MyStringRequest);



    }
    //to add data to array every 5 mins
    public void adddata(){
        getLastLocation();
        checkConnection();
        this.registerReceiver(this.broadcastReceiver, new
                IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        Date date = new Date();
        Map<String, String> MyData = new HashMap<String, String>();
        MyData.put("device",imeitext.getText().toString());
        MyData.put("internet-connected",internet_connectivity.getText().toString());
        MyData.put("charging",battery_charging_status.getText().toString());
        MyData.put("battery",battery_percentage.getText().toString());
        MyData.put("location",location_text.getText().toString());
        MyData.put("time-stamp",date.toString());
        test.add(MyData);

    }

}
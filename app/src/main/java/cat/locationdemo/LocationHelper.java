package cat.locationdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import static android.graphics.Color.WHITE;
import static android.support.v4.app.ActivityCompat.requestPermissions;

/**
 * Created by Cat on 07/05/2017.
 * This class demonstrates the alternative to combining all of your location and permissions
 * boilerplate code together with the rest of your app's functionality - separating it in an
 * independent class such as this one keeps the code much cleaner and more manageable.
 */

public class LocationHelper implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final String TAG = "LocationHelper";
    private static final int locationPermissionsRequest = 20; //arbitrary constant
    private GoogleApiClient client;
    private Context context;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    interface LocationCallback {
        void setLocation(Location location);
    }

    LocationHelper(Context activityContext, LocationCallback callback) {
        context = activityContext;
        locationCallback = callback;
        client = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }


    /* Connection management functions
    ------------------------------------ */


    //MapActivity can tell it to connect or disconnect based on activity state
    void connect(){
        if(!client.isConnected()){
            client.connect();
        }
    }

    void disconnect(){
        if(client.isConnected()){
            client.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle connectionData){
        if(locationPermissionGranted()){
            getLocation();
        }
        else {
            requestLocationPermission();
        }
    }

    @Override
    public void onConnectionSuspended(int reason){
        Log.d(TAG, "Connection to Google Play services lost, trying to reconnect");
        client.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result){ 
        Log.d(TAG, "Connection failed with error code " + result.getErrorCode());
    }



    /* Permissions functions
    ------------------------ */


    private void askUserForLocationPermission() {
        final View.OnClickListener settingsButton = new View.OnClickListener() {
            @Override
            public void onClick(View view){
                //Open phone settings menu to get them to change the permission
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.getPackageName()));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        };

        View.OnClickListener okButton = new View.OnClickListener() {
            String userMsg2 = "You can enable location permissions in the app's settings.";
            @Override
            public void onClick(View view){
                Snackbar snackbar = Snackbar.make(view, userMsg2, Snackbar.LENGTH_INDEFINITE)
                        .setAction("Settings", settingsButton)
                        .setActionTextColor(WHITE);
                snackbar.show();
            }
        };
        View currentParentView = ((Activity) context).findViewById(android.R.id.content);
        String userMsg1 = "You've denied location permissions. The app can't create location-based puzzles.";
        Snackbar snackbar = Snackbar.make(currentParentView, userMsg1, Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", okButton)
                .setActionTextColor(WHITE);
        snackbar.show();
    }

    private boolean locationPermissionGranted(){
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if(result == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        else {
            return false;
        }
    }

    private void requestLocationPermission() {
        requestPermissions( (Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationPermissionsRequest);
    }


    //MapActivity's onRequestPermissionsResult() function forwards to here
    void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode) {
            case locationPermissionsRequest: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    getLocation();
                }
                else { //user denied permission
                    askUserForLocationPermission();
                }
                break;
            }
        }
    }


    /* Location functions
    --------------------- */
    
    @SuppressWarnings({"MissingPermission"}) //security exceptions ARE handled but in a way that confuses Android Studio
    private void getLocation(){
        Location cachedLocation = LocationServices.FusedLocationApi.getLastLocation(client);
        if(cachedLocation != null){
            locationCallback.setLocation(cachedLocation);
        }
        //delete above code if you do not want your activities to receive cached (potentially stale) locations
        PendingResult<Status> result = LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        //if you are testing this in the emulator, you need to manually send the phone a GPS fix from the settings menu after this code runs
        Log.d(TAG, "Requested location updates");
    }


    @Override
    //Called when we receive a location update
    public void onLocationChanged(Location location){
        Log.d(TAG, "Received a location update");
        //LocationServices.FusedLocationApi.removeLocationUpdates(client, this);  //turn off location updates if you only need one location fix
        locationCallback.setLocation(location); //send location to MapActivity
    }

}
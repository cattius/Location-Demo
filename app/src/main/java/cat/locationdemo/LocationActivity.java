package cat.locationdemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static android.graphics.Color.WHITE;
import static cat.locationdemo.R.id.map;

public class LocationActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient client;
    public static final String TAG = "LocationActivity";
    private LocationRequest locationRequest;
    private Location cachedLocation;
    private static final int locationPermissionsRequest = 20; //arbitrary constant
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        if (client == null) {
            client = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
    }

    @Override
    protected void onStart() {
        client.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        client.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if(Build.VERSION.SDK_INT > 22 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationPermissionsRequest);
        }
        else { //we have permission - user granted at runtime if SDK version > 22 or if <= 22, user must have granted it at install time
            getLocation();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        switch(requestCode) {
            case locationPermissionsRequest: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //user gave us runtime permission
                    getLocation();
                }
                else { //user denied runtime permission
                    askUserForLocationPermission();
                }
                break;
            }
        }
    }

    private void askUserForLocationPermission() {
        final View.OnClickListener settingsButton = new View.OnClickListener() {
            @Override
            public void onClick(View view){
                //open phone settings menu to so they can change the permission
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + context.getPackageName()));
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
        };

        View.OnClickListener okButton = new View.OnClickListener() {
                String userMsg = "You can enable location permissions in the app's settings.";
                @Override
                public void onClick(View view){
                    Snackbar snackbar = Snackbar.make(view, userMsg, Snackbar.LENGTH_INDEFINITE)
                            .setAction("Settings", settingsButton)
                            .setActionTextColor(WHITE);
                    snackbar.show();
                }
        };
        View currentParentView = ((Activity) context).findViewById(android.R.id.content);
        String userMsg = "You've denied location permissions. This app won't function correctly.";
        Snackbar snackbar = Snackbar.make(currentParentView, userMsg, Snackbar.LENGTH_INDEFINITE)
                .setAction("OK", okButton)
                .setActionTextColor(WHITE);
        snackbar.show();
        }

    @SuppressWarnings({"MissingPermission"}) //security exceptions ARE handled but in a way that confuses Android Studio
    private void getLocation(){
        cachedLocation = LocationServices.FusedLocationApi.getLastLocation(client);
        if(cachedLocation != null){
            if(mMap != null){
                LatLng cachedLocationLatLng = new LatLng(cachedLocation.getLatitude(), cachedLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(cachedLocationLatLng).title("Cached last known location"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cachedLocationLatLng, 14.0f));
            }
            else {
                Log.d(TAG, "Map not initialised correctly"); //this is unlikely to occur as the map initialises very quickly
            }
        }
        else{
            Log.d(TAG, "cached location is null");
        }
        PendingResult<Status> newLocation = LocationServices.FusedLocationApi.requestLocationUpdates(client, locationRequest, this);
        //if you are testing this in the emulator, you need to manually send the phone a GPS fix from the settings menu after this code runs
    }

    @Override
    //Called when we receive a location update from PendingResult
    public void onLocationChanged(Location location){
        if(mMap != null){
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(currentLocation).title("User's current location"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 14.0f));
        }
        else {
            Log.d(TAG, "Map not initialised correctly"); //this is highly unlikely to occur as getting a location fix is slow in comparison to initialising the map
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

}

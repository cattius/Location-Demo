package cat.locationdemo;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Cat on 07/05/2017.
 * This class demonstrates how easily you can use LocationHelper to reuse Google API client /
 * FusedLocationApi code in a larger project.
 */

public class MapActivity extends AppCompatActivity implements LocationHelper.LocationCallback {

    public static final String TAG = "MapActivity";
    LocationHelper locationHelper;
    Location latestLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationHelper = new LocationHelper(this, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationHelper.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationHelper.disconnect();
    }

    @Override
    //Forward this callback to locationHelper
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        locationHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //Receive location from locationHelper
    public void setLocation(Location location){
        latestLocation = location;
        /* YOUR LOCATION-DEPENDENT CODE/FUNCTION CALLS GO HERE */
    }

}

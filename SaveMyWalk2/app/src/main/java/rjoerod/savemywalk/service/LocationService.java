/**
 * Created by Ryan on 6/13/2017.
 */

package rjoerod.savemywalk.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.content.Context;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Calendar;
import rjoerod.savemywalk.fragment.MapViewFragment;

// Location
// implemented based on code from link below
// https://stackoverflow.com/questions/28535703/best-way-to-get-user-gps-location-in-background-in-android
// Arslan Sohail

// User Activity (e.g. walking, driving, etc.)
// Modified from: https://code.tutsplus.com/tutorials/
//                              how-to-recognize-user-activity-with-activity-recognition--cms-25851

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private LocationManager mLocationManager = null;
    private boolean writeNewFile = true; // the next FileIO operation will create a new file
    private boolean firstLocation;
    private BufferedWriter bufferedWriter;
    private String unfilteredFileName;
    private Context context;
    private Calendar cal;
    private Double previousLatitude;
    private Double previousLongitude;
    private final IBinder mBinder = new LocalBinder(); // Binder given to clients
    private boolean isTracking = false; //  if true, location services are active

    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    private static final Double minDistance = 25.0;


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public LocationService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocationService.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0)
    {
        return mBinder;
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }
        /**********************
        ****FILTER DISABLED****
        **********************/
        @Override
        public void onLocationChanged(Location location) {
            //Log.e(TAG, location.getProvider() + " and " + LocationManager.GPS_PROVIDER);
            // Condition: Provider is GPS or GPS Provider is not enabled
            if(   ( location.getProvider().equals(LocationManager.GPS_PROVIDER) )/* ||
                           !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)*/   ) {

                Log.e(TAG, "onLocationChanged: " + location);

                Double currentLatitude = location.getLatitude();
                Double currentLongitude = location.getLongitude();

                String local = Double.toString(currentLatitude) + " " +
                        Double.toString(currentLongitude);
                float[] distance = {-1};
                if(!firstLocation) {
                    Location.distanceBetween(previousLatitude, previousLongitude,
                                                currentLatitude, currentLongitude, distance);
                }

                // Condition: The distance was large enough
                if (  (distance[0] > minDistance) || firstLocation  ) {
                    Log.e(TAG, "Writing location to temp file");
                    writeToFile(local, writeNewFile);
                    //writeToPublicFile(local, writeNewFile);
                    firstLocation = false;
                    writeNewFile = false;
                    previousLatitude = currentLatitude;
                    previousLongitude = currentLongitude;
                    mLastLocation.set(location);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        context = getApplicationContext();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
    }

    public void toggleTracking() {

        isTracking = !isTracking;

        //
        // Starting location services
        //
        if(isTracking) {
            Log.e(TAG, "started tracking");
            firstLocation = true;
            cal = Calendar.getInstance();
            unfilteredFileName = "temp" + cal.getTimeInMillis();
            initializeLocationManager();
            resetTemp();

            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[1]);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "network provider does not exist, " + ex.getMessage());
            }
            try {
                mLocationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                        mLocationListeners[0]);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "gps provider does not exist " + ex.getMessage());
            }

            //
            // Ending location services
            //
        } else {
            Log.e(TAG, "stopped tracking");
            writeToFile("STOP\n", writeNewFile);
            writeNewFile = true;
            if (mLocationManager != null) {
                for (int i = 0; i < mLocationListeners.length; i++) {
                    try {
                        mLocationManager.removeUpdates(mLocationListeners[i]);
                    } catch (Exception ex) {
                        Log.i(TAG, "fail to remove location listners, ignore", ex);
                    }
                }
            }
        }
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void writeToFile(String text, Boolean isNewFile) {
        String FILENAME = "temp";
        context = getApplicationContext();
        try {
            File fileDirectory = context.getDir("temp", Context.MODE_PRIVATE);
            File temp = new File(fileDirectory, FILENAME);
            bufferedWriter = new BufferedWriter(new FileWriter(temp, !isNewFile));

            Log.e(TAG, "Writing " + text + " to temp");
            bufferedWriter.write(text);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void resetTemp() {
        context = getApplicationContext();
        File fileDirectory = context.getDir("temp", Context.MODE_PRIVATE);
        File temp = new File(fileDirectory, "temp");
        if(temp.delete()){
            Log.e(TAG, "temp is deleted.");
        } else {
            Log.e(TAG, "temp may or may not exist.");
        }
    }

    // DEBUGGING: For use when saving data points to public directory
    private void writeToPublicFile(String text, Boolean isNewFile) {
        context = getApplicationContext();
        cal = Calendar.getInstance();
        text = text + " " + cal.getTimeInMillis();
        try {
            File fileDirectory = context.getExternalFilesDir("temp");
            File temp = new File(fileDirectory, unfilteredFileName);
            bufferedWriter = new BufferedWriter(new FileWriter(temp, !isNewFile));

            Log.e(TAG, "Writing " + text + " to " + unfilteredFileName);
            bufferedWriter.write(text);
            bufferedWriter.newLine();
            bufferedWriter.flush();
            bufferedWriter.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getIsTracking() {
        return isTracking;
    }
}

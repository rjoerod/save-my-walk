package rjoerod.savemywalk.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import rjoerod.savemywalk.service.LocationService;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";
    private LocationService mBoundService = null;
    private boolean mBound = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            Log.e(TAG, "Connecting to Service");
            LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
            mBoundService = binder.getService();
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            Log.e(TAG, "Disconnecting from Service");
            mBoundService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(rjoerod.savemywalk.R.layout.activity_map);

        Toolbar myToolbar = (Toolbar) findViewById(rjoerod.savemywalk.R.id.map_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    public boolean getIsTrackingFromBoundService() {
        if (mBound) {
            return mBoundService.getIsTracking();
        } else {
            throw new Resources.NotFoundException("No Service Bound");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "Binding to Service");
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        Log.e(TAG, "Unbinding from Service");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}

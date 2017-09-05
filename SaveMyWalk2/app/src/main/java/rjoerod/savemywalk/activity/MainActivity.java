package rjoerod.savemywalk.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Button;
import android.widget.ToggleButton;
import android.view.View;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import rjoerod.savemywalk.fragment.ConfirmationFragment;
import rjoerod.savemywalk.service.LocationService;

public class MainActivity extends AppCompatActivity
                        implements ConfirmationFragment.ConfirmationListener {

    private LocationService mBoundService = null;
    private boolean mBound = false;
    private BufferedWriter bufferedWriter = null;
    private BufferedReader bufferedReader = null;
    private ArrayList<String> index = new ArrayList<>();
    private Calendar cal;
    private Context context;
    private ToggleButton toggleButton;

    private static Bundle bundle = new Bundle();
    private static final String TAG = "MainActivity";

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
        Log.e(TAG, "OnCreate");
        super.onCreate(savedInstanceState);
        setContentView(rjoerod.savemywalk.R.layout.activity_main);

        toggleButton = (ToggleButton)findViewById(rjoerod.savemywalk.R.id.trackingButton);
        context = getApplicationContext();
        initializeRouteInfo();

        Intent intent = new Intent(this, LocationService.class);
        startService(intent);

        // set up toolbar
        Toolbar myToolbar = (Toolbar) findViewById(rjoerod.savemywalk.R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        // Let clicking the "Start Tracking" button start tracking current location
        final Button trackingButton = (Button) findViewById(rjoerod.savemywalk.R.id.trackingButton);
        trackingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleButton.setChecked(!toggleButton.isChecked());
                if (!mBound) {
                    Log.e(TAG, "Service Not Bound");
                } else {
                    if (toggleButton.isChecked()) {
                        showConfirmationDialog("Are you sure? (Cannot resume again)",
                                ConfirmationFragment.Operation.STOP_TRACKING);
                    } else {
                        showConfirmationDialog("Overwrite unsaved route? (if any exists)",
                                ConfirmationFragment.Operation.START_TRACKING);
                    }
                }

            }
        });

        // Let clicking the "View Map" button switch to the map page
        final Button viewMapButton = (Button) findViewById(rjoerod.savemywalk.R.id.viewMapButton);
        viewMapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "Switching Activity to Map");
                Intent i = new Intent(MainActivity.this, MapActivity.class);
                startActivity(i);
            }
        });

        // Let clicking the "Save Last Route" button switch to the saved routes page
        final Button saveRouteButton = (Button) findViewById(rjoerod.savemywalk.R.id.saveRouteButton);
        saveRouteButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "Saving Route");
                if (!mBound) {
                    Log.e(TAG, "Service Not Bound");
                } else {
                    showConfirmationDialog("Are you sure?", ConfirmationFragment.Operation.SAVE);
                }
            }
        });

        // Let clicking the "View Saved Routes" button switch to the saved routes page
        final Button viewSavedRoutesButton = (Button) findViewById(rjoerod.savemywalk.R.id.viewSavedRoutesButton);
        viewSavedRoutesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.e(TAG, "Switching Activity to Saved Routes");
                Intent i = new Intent(MainActivity.this, SavedRoutesActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "Binding to Service");
        Intent intent = new Intent(this, LocationService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        toggleButton.setChecked(bundle.getBoolean("ToggleButtonState",false));
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

        bundle.putBoolean("ToggleButtonState", toggleButton.isChecked());
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        ConfirmationFragment confirmDialog = (ConfirmationFragment) dialog;
        ConfirmationFragment.Operation operation = confirmDialog.getOperation();

        switch(operation) {
            case SAVE:
                // Updating date
                cal = Calendar.getInstance() ;
                // check if tracking
                if (toggleButton.isChecked()) {
                    toggleButton.setChecked(false);
                    mBoundService.toggleTracking();
                }
                saveRoute();
                break;
            case START_TRACKING:
                toggleButton.setChecked(true);
                mBoundService.toggleTracking();
                break;
            case STOP_TRACKING:
                toggleButton.setChecked(false);
                mBoundService.toggleTracking();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        ConfirmationFragment confirmDialog = (ConfirmationFragment) dialog;
        ConfirmationFragment.Operation operation = confirmDialog.getOperation();

        switch(operation) {
            case SAVE:
                break;
            case START_TRACKING:
                toggleButton.setChecked(false);
                break;
            case STOP_TRACKING:
                toggleButton.setChecked(true);
                break;
            default:
                break;
        }
    }

    public void showConfirmationDialog(String title, ConfirmationFragment.Operation operation) {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = ConfirmationFragment.newDialogInstance(title, operation);
        // prevent dialog from being canceled on touch outside
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), "ConfirmationFragment");
    }

    // sets up file system if it doesn't already exist
    // fills array with index of routes
    private void initializeRouteInfo() {
        // check if directory exists
        File fileDirectory = context.getDir("routes", Context.MODE_PRIVATE);
        // create directory & file
        Log.e(TAG, "Creating routes directory");
        Log.e(TAG, fileDirectory.getAbsoluteFile().toString() );
        // check if file exists
        File newIndex = new File(fileDirectory,"index.txt");
        if (!newIndex.exists()) {
            // create file
            try {
                Log.e(TAG, newIndex.getAbsoluteFile().toString() );
                if(!newIndex.createNewFile()) {
                    Log.e(TAG, "Failed to create index file");
                }
                bufferedWriter = new BufferedWriter(new FileWriter(newIndex));
                bufferedWriter.write("0\n");
                bufferedWriter.flush();
                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // read file
            readIndexFile();
        }
    }

    // read the index file to array
    private void readIndexFile() {
        Log.e(TAG, "Reading Index File");
        try {
            File fileDirectory = context.getDir("routes", Context.MODE_PRIVATE);
            File newIndex = new File(fileDirectory,"index.txt");
            bufferedReader = new BufferedReader( new FileReader(newIndex));
            int length = Integer.parseInt( bufferedReader.readLine() );
            String temp;
            for(int i = 0; i < length; i++) {
                temp = bufferedReader.readLine();
                Log.e(TAG, temp);
                index.add(temp );
            }
            Log.e(TAG, "Saved to Index:");
            for(String routeName: index) {
                Log.e(TAG, routeName);
            }
            bufferedReader.close();
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    // save the previously tracked route
    private void saveRoute() {

        // check if "temp" exists
        File fileDirectory = context.getDir("temp", Context.MODE_PRIVATE);
        File temp = new File(fileDirectory, "temp");
        if (temp.exists()) {

            // append "rt#" to an array
            String routeName = "rt" + cal.getTimeInMillis();

            // Saving temp file to "routes" directory
            Boolean hasCoords = saveTemp(routeName);

            // Adding rt# to index array
            if(hasCoords) {
                Log.e(TAG, "Adding " + routeName + " to index array");
                index.add(routeName);
                // Rewriting index file
                rewriteIndex();
            }
        } else {
            Log.e(TAG, "No Route To Save");
        }
    }

    // save new route information
    private Boolean saveTemp(String routeName) {
        Boolean hasCoords = false;  // controls whether to save temp
        // only saves if the route has coordinates
        try {
            // create new route file to write to
            File fileDirectory = context.getDir("routes", Context.MODE_PRIVATE);
            File newRoute = new File(fileDirectory, routeName);
            Log.e(TAG, "Writing new route");
            bufferedWriter = new BufferedWriter(new FileWriter(newRoute));

            // open temp file to read from
            File tempDirectory = context.getDir("temp", Context.MODE_PRIVATE);
            File temp = new File(tempDirectory, "temp");
            bufferedReader = new BufferedReader(new FileReader(temp));

            // Replace with name from user input
            bufferedWriter.write(routeName);
            //

            bufferedWriter.newLine();
            String someLocation = bufferedReader.readLine();
            hasCoords = !someLocation.equals("STOP");
            // delete the newly created route file if no coords
            if( !hasCoords ){
                if( newRoute.delete() ) {
                    Log.e(TAG, "Successfully deleted " + routeName);
                } else {
                    Log.e(TAG, "Failed to delete " + routeName);
                }
            } else { // write to the newly created route file
                while (!someLocation.equals("STOP")) {
                    Log.e(TAG, "Writing " + someLocation + " to file");
                    bufferedWriter.write(someLocation);
                    bufferedWriter.newLine();
                    someLocation = bufferedReader.readLine();
                }
                bufferedWriter.write(someLocation);
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
            bufferedReader.close();
        } catch (IOException err) {
            err.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return hasCoords;
    }

    // rewrite index file to include new route
    private void rewriteIndex() {
        try {
            File fileDirectory = context.getDir("routes", Context.MODE_PRIVATE);
            File newIndex = new File(fileDirectory,"index.txt");

            Log.e(TAG, "Writing to " + newIndex.getAbsoluteFile().toString() );
            bufferedWriter = new BufferedWriter(new FileWriter(newIndex));
            bufferedWriter.write(Integer.toString(index.size()));

            Log.e(TAG, "Adding " + index.size() + " to index.txt" );
            bufferedWriter.newLine();

            for (String someRoute : index) {
                Log.e(TAG, "Adding " + someRoute + " to index.txt" );
                bufferedWriter.write(someRoute);
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

}


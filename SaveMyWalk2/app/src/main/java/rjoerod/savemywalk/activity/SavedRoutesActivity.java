// Modfied from Answer: https://stackoverflow.com/questions/17545060/custom-view-with-button-in-arrayadapter


package rjoerod.savemywalk.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ListView;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import rjoerod.savemywalk.fragment.ConfirmationFragment;
import rjoerod.savemywalk.adapter.InteractiveListAdapter;
import rjoerod.savemywalk.service.LocationService;
import rjoerod.savemywalk.route.RouteData;

public class SavedRoutesActivity extends AppCompatActivity
                                 implements ConfirmationFragment.ConfirmationListener {

    private static final String TAG = "SavedRoutesActivity";
    private LocationService mBoundService = null;
    private boolean mBound = false;
    private BufferedReader bufferedReader = null;
    private ArrayList<RouteData> routesInfo = new ArrayList<RouteData>();
    private Context context;

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
        setContentView(rjoerod.savemywalk.R.layout.activity_saved_routes);

        Toolbar myToolbar = (Toolbar) findViewById(rjoerod.savemywalk.R.id.saved_routes_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        context = getApplicationContext();
        readIndexFile(); // read into routesInfo
        updateArrayAdapter(); // Update adapter based on routesInfo
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

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        ConfirmationFragment confirmDialog = (ConfirmationFragment) dialog;
        ConfirmationFragment.Operation operation = confirmDialog.getOperation();
        String someRouteID = confirmDialog.getRouteID();

        switch(operation) {
            case DELETE:
                Log.e(TAG, "A Route " + someRouteID + " was deleted.");
                removeRoute(someRouteID);
                updateArrayAdapter();
                break;
            default:
                break;
        }
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}

    // read the index file to routesInfo
    private void readIndexFile() {
        try {
            // prepare to read file
            File fileDirectory = context.getDir("routes", Context.MODE_PRIVATE);
            File newIndex = new File(fileDirectory,"index.txt");
            bufferedReader = new BufferedReader( new FileReader(newIndex));

            // get length of file from first entry and iterate
            int length = Integer.parseInt( bufferedReader.readLine() );
            for(int i = 0; i < length; i++) {
                String someRouteName = bufferedReader.readLine();

                // for the moment, routeID == routeName
                RouteData someRoute = new RouteData(someRouteName, someRouteName); // route name
                someRoute = readSomeRoute(someRoute); // route coords
                routesInfo.add(someRoute);
            }
            bufferedReader.close();
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    // read some rt# file to RouteData instance
    private RouteData readSomeRoute(RouteData someRoute) {
        try {
            // prepare to read file
            File fileDirectory = context.getDir("routes", Context.MODE_PRIVATE);
            File newRouteFile = new File(fileDirectory, someRoute.getRouteID());
            Log.e(TAG, newRouteFile.getAbsoluteFile().toString() );
            BufferedReader routeReader = new BufferedReader( new FileReader(newRouteFile) );

            // Ignore first line
            String routeName = routeReader.readLine();

            // initialize variables
            String someLocation = routeReader.readLine();
            String[] coords;
            Double lat;
            Double lng;

            // add route's coordinates from file
            while (!someLocation.equals("STOP")) {
                coords = someLocation.split(" ");
                lat = Double.parseDouble(coords[0]);
                lng = Double.parseDouble(coords[1]);
                someRoute.addCoordinate(lat, lng);
                someLocation = routeReader.readLine();
            }

            routeReader.close();
        } catch (IOException err) {
            err.printStackTrace();
        }
        return someRoute;
    }

    private void updateArrayAdapter() {
        final ListView saved_routes_list = (ListView) findViewById(
                                                    rjoerod.savemywalk.R.id.saved_routes_listview);
        final List<String> routesList = new ArrayList<String>();
        for(RouteData someRoute : routesInfo) {
            routesList.add(someRoute.getRouteID());
        }

        // Create ArrayAdapter from List
        // for now, routesIDs == routesNames
        final InteractiveListAdapter routesAdapter = new InteractiveListAdapter
                (context, routesList, routesList, saved_routes_list);
        saved_routes_list.setAdapter(routesAdapter);
        routesAdapter.notifyDataSetChanged();
    }

    // removes route from index.txt and array
    private void removeRoute(String someRouteID) {
        for(int i = 0; i < routesInfo.size(); i++) {
            if(   ( routesInfo.get(i).getRouteID() ).equals(someRouteID)   ) {
                routesInfo.remove(i);
            }
        }
        removeFromIndex(someRouteID);
    }

    // Rewrites routes/index.txt to not include the given route ID
    // Also removes routes/rt# where rt# is the given route ID
    private void removeFromIndex(String someRouteID) {
        File fileDirectory = context.getDir("routes", Context.MODE_PRIVATE);
        File indexFile = new File(fileDirectory, "index.txt");
        Log.e(TAG, "Rewriting " + indexFile.getAbsoluteFile().toString());
        Log.e(TAG, "Removing " + someRouteID);

        //read index from routesInfo (ensure RoutesInfo has been updated!)
        ArrayList<String> linesFromFile = prepareIndexInput(someRouteID);

        // Write index
        writeIndex(indexFile, linesFromFile);

        // Remove rt# file
        File routeFile = new File(fileDirectory, someRouteID);
        if (routeFile.delete()) {
            Log.e(TAG, "Successfully deleted " + someRouteID);
        } else {
            Log.e(TAG, "Failed to delete " + someRouteID);
        }
    }

    // read routes/index.txt and ignore the line to be deleted
    // returns index as Array of Strings where each String is a line from file
    private ArrayList<String> prepareIndexInput(String someRouteID) {
        ArrayList<String> linesFromFile = new ArrayList<String>();
        linesFromFile.add(  Integer.toString( routesInfo.size() )  );
        for(RouteData someRoute : routesInfo) {
            linesFromFile.add(someRoute.getRouteID());
        }
        return linesFromFile;
    }

    // writes routes/index.txt
    private void writeIndex(File indexFile, ArrayList<String> linesFromFile) {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(indexFile));

            for (String aLine : linesFromFile) {
                bufferedWriter.write(aLine);
                bufferedWriter.newLine();
            }
            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

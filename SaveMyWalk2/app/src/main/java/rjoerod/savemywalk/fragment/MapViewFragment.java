package rjoerod.savemywalk.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import rjoerod.savemywalk.activity.MapActivity;
import rjoerod.savemywalk.route.RouteData;
import rjoerod.savemywalk.service.LocationService;

import static android.content.ContentValues.TAG;

public class MapViewFragment extends Fragment {

    private Context context;
    private CountDownTimer timer;
    private CountDownTimer retryTimer;
    private MapView mMapView;
    private GoogleMap googleMap;
    private Polyline routeLine;
    private RouteData routesInfo = new RouteData("temp", "temp");
    private LatLng latLngDefault = new LatLng(38.7355, -94.7005);
    final private int zoomLevel = 16;
    private int timerRetryCount;

    private static final int MAX_TIMER_RETRIES = 10;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                                                    Bundle savedInstanceState) {
        View rootView = inflater.inflate(rjoerod.savemywalk.R.layout.map_fragment, container, false);
        mMapView = (MapView) rootView.findViewById(rjoerod.savemywalk.R.id.mapView);
        mMapView.onCreate(savedInstanceState);
        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {

                context = getActivity().getApplicationContext();
                googleMap = mMap;
                // Read the "temp" file into routesInfo
                readTemp();

                // Display "temp" route on map
                routeLine = googleMap.addPolyline(new PolylineOptions());
                routeLine.setPoints(routesInfo.getAllCoords());
                int length = routesInfo.getLength();
                updateMarkers(length);
                timerRetryCount = 0;
                startRetryTimer();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        try {
            MapActivity activity = (MapActivity) getActivity();
            if( activity.getIsTrackingFromBoundService() && (timer != null) ) {
                timer.cancel();
            }
        } catch (Resources.NotFoundException e)  {
            Log.e(TAG, "Timer failed to cancel");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        try {
            MapActivity activity = (MapActivity) getActivity();
            if( activity.getIsTrackingFromBoundService() && (timer != null) ) {
                timer.cancel();
            }
        } catch (Resources.NotFoundException e)  {
            Log.e(TAG, "Timer failed to cancel");
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private void readTemp() {
        try {
            routesInfo.clear();

            // prepare to read file
            File fileDirectory = context.getDir("temp", Context.MODE_PRIVATE);
            File tempFile = new File(fileDirectory, "temp");
            Log.e(TAG, tempFile.getAbsoluteFile().toString() );
            BufferedReader routeReader = new BufferedReader( new FileReader(tempFile) );

            // initialize variables
            String someLocation = routeReader.readLine();
            Double lat = 0.0;
            Double lng = 0.0;
            String[] coords = {"", ""};

            // add route's coordinates from file
            Boolean continueReading = !someLocation.equals("STOP");
            while ( continueReading ) {
                coords = someLocation.split(" ");
                lat = Double.parseDouble(coords[0]);
                lng = Double.parseDouble(coords[1]);
                routesInfo.addCoordinate(lat, lng);
                someLocation = routeReader.readLine();

                if(someLocation != null) {
                    continueReading = !someLocation.equals("STOP");
                } else {
                    continueReading = false;
                }
            }
            routeReader.close();
        } catch (IOException err) {
            err.printStackTrace();
        }
    }

    // updates the route lines and tries to update the markers
    public void updateRouteLines() {
        Log.e(TAG, "Updating Route Lines.");
        this.googleMap.clear();
        readTemp();
        routeLine = googleMap.addPolyline(new PolylineOptions());
        routeLine.setPoints(routesInfo.getAllCoords());
        updateMarkers(routesInfo.getLength());
    }

    // updates the markers if there are enough coordinates (>0)
    private void updateMarkers(int length) {
        if (length != 0) {
            // Drop marker at start of route
            LatLng start = routesInfo.getCoordinate(0);
            googleMap.addMarker(new MarkerOptions().position(start)
                                                   .title("Start")
                                                   .snippet("You Started Here"));

            // Drop marker at end of route
            LatLng end = routesInfo.getCoordinate(length - 1);
            googleMap.addMarker(new MarkerOptions().position(end)
                                                   .title("End")
                                                   .snippet("You Ended Here"));

            // For zooming automatically to the location of the marker
            CameraPosition cameraPosition = new CameraPosition.Builder()
                                                              .target(end)
                                                              .zoom(zoomLevel)
                                                              .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            // For zooming automatically to the location of the marker
            CameraPosition cameraPosition = new CameraPosition.Builder()
                                                              .target(latLngDefault)
                                                              .zoom(zoomLevel)
                                                              .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private void startTimer() {
        timer = new CountDownTimer(3000, 1000) {

            public void onTick(long millisUntilFinished) {}

            public void onFinish() {
                updateRouteLines();
                timer.start();
            }

        }.start();
        Log.e(TAG, "Timer has started.");
    }

    private void startRetryTimer(){
        try {
            MapActivity activity = (MapActivity) getActivity();
            if( activity.getIsTrackingFromBoundService() ) {
                // timer updates route every 3 second
                startTimer();
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Timer failed to start.");
            if (timerRetryCount < MAX_TIMER_RETRIES) {
                retryTimer = new CountDownTimer(3000, 1000) {

                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        startRetryTimer();
                    }

                }.start();
                timerRetryCount++;
            }
        }
    }
}

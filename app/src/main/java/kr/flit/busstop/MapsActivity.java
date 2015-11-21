package kr.flit.busstop;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private float baseLat;
    private float baseLng;
    private HashMap<String, BusStop> map = new HashMap<>();
    private View layout;
    private Context context;
    private static final String TAG = MapsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_maps);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        baseLat = getIntent().getFloatExtra("lat",0.0f);
        baseLng = getIntent().getFloatExtra("lng",0.0f);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        layout = findViewById(R.id.layout);
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
//        Toast.makeText(this, "onMapReady", Toast.LENGTH_LONG).show();
        mMap = googleMap;
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            Location prevLocation;

            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                Location newLoc = new Location("new");
                newLoc.setLatitude(cameraPosition.target.latitude);
                newLoc.setLongitude(cameraPosition.target.longitude);

                if (prevLocation == null || newLoc.distanceTo(prevLocation) > 150) {
                    Log.d(TAG, "onCameraChange.reloadArroundBusstop");
                    reloadArroundBusstop(newLoc);
                }
                prevLocation = newLoc;
            }
        });
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
//                Snackbar.make(layout, marker.getTitle(), Snackbar.LENGTH_SHORT).show();

                return false;
            }
        });
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                BusStop stop =  map.get(marker.getSnippet());
                Intent data = new Intent();
                data.putExtra("busstop", stop);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(baseLat, baseLng), 18.0f));
    }

    private void reloadArroundBusstop(final Location newLoc) {

        AsyncTask<Void, Void, Boolean> task
                = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {

                String url = "http://m.bus.go.kr/mBus/bus/getStationByPos.bms";
        /*
        tmX:126.97796919999999
        tmY:37.566535
        radius:300
         */

                OkHttpClient client = new OkHttpClient();
                String tmX = String.valueOf(newLoc.getLongitude());
                String tmY = String.valueOf(newLoc.getLatitude());
                String radius = "300";
                Log.d(TAG, "x, y : " + tmX + "," +tmY );




                RequestBody body = new FormEncodingBuilder()
                        .add("tmX", tmX)
                        .add("tmY", tmY)
                        .add("radius", radius)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    if(response.isSuccessful()){
                        String res = response.body().string();
                        Log.d(TAG, "station by pos : " + res);
                        JSONObject json = new JSONObject(res);


                        JSONArray resultList = json.optJSONArray("resultList");
                        ArrayList<BusStop> newList = new ArrayList<BusStop>();

                        if (resultList != null && resultList.length() > 0) {

                            for (int i = 0; i < resultList.length(); i++) {
                                JSONObject stopObj = resultList.optJSONObject(i);
                                BusStop stop = new BusStop();

                                double gpsx = stopObj.optDouble("gpsX");
                                double gpsy = stopObj.optDouble("gpsY");
                                String arsId = stopObj.optString("arsId");
                                stop.setName(stopObj.optString("stationNm"));
                                stop.setArsId(arsId);
                                stop.setLatLng(gpsy, gpsx);
                                newList.add(stop);
                            }
                        }

                        synchronized (map) {
                            for (BusStop stop : newList) {
                                if(map.containsKey(stop.getArsId())==false){
                                    map.put(stop.getArsId(), stop);
                                }
                            }
                        }

                    } // response is successful
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return false;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if(result){
                    mMap.clear();
                    for(BusStop stop : map.values()){
                        MarkerOptions marker = new MarkerOptions()
                                .position(
                                        new LatLng(stop.getLat(), stop.getLng()))
                                .title(stop.getName())
                                .snippet(stop.getArsId())
                                .visible(true)
//                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.map_marker))
                                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_launcher))
//                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_grey600_36dp))
                                ;
                        Marker m = mMap.addMarker(marker);
                    }
                }
            }
        }.execute();


    }

}

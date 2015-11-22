package kr.flit.busstop;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.altbeacon.beacon.BeaconManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;


public class StopListActivity extends AppCompatActivity
implements BeaconService.StopListListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 10;
    private static final int REQUEST_PERMISSION_SETTING = 11;
    private static final int REQUEST_MAPS = 20;
    private static final int REQUSET_STOP_SELECT = 30;
    private AsyncTask<Void, Void, JSONObject> task;
    private JSONArray resultList ;

    private RecyclerView recyclerView;

    private EditText editBusSearch;
    private TextView textStopTitle;

//    private JSONObject stopList;

    RecyclerView.Adapter adapter;
    private Context context;
    private final String TAG = "StopList";

    private int busStopId ;
    private String busStopName;
    Typeface fontOldBaths = null;

    private View stopArrivalEmptyView ;
    private View stopArrivalProgress ;

    protected static StopListActivity instance;
    private View viewSplash;
    private ViewPager viewpager;

    float lastLat ;
    float lastLng ;
    Location curLocation;

    private SharedPreferences prefs;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        instance = this;
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        lastLat = prefs.getFloat("lastLat", 0.0f);
        lastLng = prefs.getFloat("lastLng", 0.0f);

        setContentView(R.layout.activity_stop_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        View appBarLayout = findViewById(R.id.appBarLayout);


        this.editBusSearch = (EditText) findViewById(R.id.editBusSearch);
        this.textStopTitle = (TextView) findViewById(R.id.textStopTitle);
        this.stopArrivalEmptyView = findViewById(R.id.stopArrivalEmptyView);
        this.stopArrivalProgress = findViewById(R.id.stopArrivalProgress);
        this.viewSplash = findViewById(R.id.splash);

        this.recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.LOLLIPOP){
            findViewById(R.id.contentSeparator).setVisibility(View.VISIBLE);
            findViewById(R.id.headerSeparator).setVisibility(View.VISIBLE);
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false ));


//        JSONArray source = getIntent().get
        JSONArray source = new JSONArray();
        String strSource = getIntent().getStringExtra("source");

        Log.d(TAG, "SOURCE " + strSource);

        if(strSource==null){
            showSplash();
        }else {
            viewSplash.setVisibility(View.GONE);
        }


        fontOldBaths = Typeface.createFromAsset(getAssets(),
                "BMHANNA_11yrs_otf.otf");


        stopArrivalProgress.setVisibility(View.GONE);

        textStopTitle.setTypeface(fontOldBaths);
        editBusSearch.setTypeface(fontOldBaths);

        textStopTitle.setText("");

//        startService(new Intent(this, BeaconService.class));

        editBusSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 5) {
                    int arsId = Integer.parseInt(s.toString());
                    onSearchStop(arsId);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        try {
            if(source == null)
                source = new JSONArray();
            else
                source = new JSONArray(strSource);
        }catch (Exception e){
//            ignore exception
//            e.printStackTrace();
        }


        adapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

                ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.listitem_stopinfo, parent, false);
                ViewHolder viewHolder = new ViewHolder(view);
                return viewHolder;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                ViewHolder h = (ViewHolder) holder;
                JSONObject item = resultList.optJSONObject(position);

/*
                    //
                    3 간선
                    4 지선
                    5 순환
                    6 광역
                    7 8 경기, 인천
                    default
                    */
                if(item!=null) {
                    int busColor = getBusColor(item.optInt("routeType"));
                    h.text1.setTextColor(busColor);

                    String adirection = item.optString("adirection");
                    if (adirection.length() > 0)
                        adirection += "행";

                    int isLast1 = item.optInt("isLast1");
                    String nextBus = item.optString("nextBus");

                    h.text1.setText(item.optString("rtNm"));
                    if (isLast1 == 1)
                        h.imgEndBus.setVisibility(View.VISIBLE);
                    else
                        h.imgEndBus.setVisibility(View.GONE);
//                    String prefix = String.format("[%d %s]", isLast1, nextBus);

                    h.text2.setText(
//                            prefix +
                            item.optString("arrmsg1"));
                    h.text3.setText(adirection);
//                    text1.setText(item.optString("name"));
//                    text2.setText(item.optString("arsId"));
//                    text3.setText(
//                            String.format("%2.2fm", item.optDouble("distance"))
//                    );
                }

            }

            @Override
            public int getItemCount() {
                return resultList==null ? 0 : resultList.length();
            }


        };
//        listViewInfo.setVisibility(View.GONE);
        recyclerView.setAdapter(adapter);
        buildGoogleApiClient();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_stop_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_maps) {
            onClickMap(null);
            return true;
        }else if(id == R.id.action_refresh){
            reload();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.d(TAG,"onConnected " + bundle );
        Log.d(TAG,"onConnected " + bundle );
        Log.d(TAG,"onConnected " + bundle );
        Log.d(TAG,"onConnected " + bundle );

        lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        if (lastLocation != null) {
            Log.d(TAG, "location : " + lastLocation.getLatitude() + " " + lastLocation.getLongitude());
            updateLastGpsStop();
        }

    }

    private void updateLastGpsStop() {

        String url = "http://m.bus.go.kr/mBus/bus/getStationByPos.bms";
        /*
        tmX:126.97796919999999
        tmY:37.566535
        radius:300
         */
        BusStopApplication.getApp().updateLastLocation(lastLocation);
        OkHttpClient client = new OkHttpClient();
        String tmX = String.valueOf(lastLocation.getLongitude());
        String tmY = String.valueOf(lastLocation.getLatitude());
        String radius = "200";
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
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String res = response.body().string();
                        Log.d(TAG, "station by pos : " + res);
                        JSONObject json = new JSONObject(res);


                        JSONArray resultList = json.optJSONArray("resultList");
                        ArrayList<BusStop> newList = new ArrayList<BusStop>();

                        if (resultList != null && resultList.length() > 0) {

                            for (int i = 0; i < resultList.length(); i++) {
                                JSONObject stopObj = resultList.optJSONObject(i);
                                BusStop stop = new BusStop();
                                stop.setIsBeacon(false);

                                double gpsx = stopObj.optDouble("gpsX");
                                double gpsy = stopObj.optDouble("gpsY");
                                String arsId = stopObj.optString("arsId");
                                stop.setName(stopObj.optString("stationNm"));
                                stop.setArsId(arsId);
                                stop.setLatLng(gpsy, gpsx);
                                Location loc = new Location("busstop");
                                loc.setLongitude(gpsx);
                                loc.setLatitude(gpsy);

                                stop.setDistance(lastLocation.distanceTo(loc));
                                newList.add(stop);
                            }
                        }
                        BusStopApplication.getApp().setGpsStop(newList);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    ;
                }

            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed " + connectionResult);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView text1;
        TextView text2;
        TextView text3;
        ImageView imgEndBus;

        public ViewHolder(View itemView) {
            super(itemView);
            imgEndBus = (ImageView) itemView.findViewById(R.id.imgEndBus);
            text1 = (TextView) itemView.findViewById(R.id.textView1);
            text2 = (TextView) itemView.findViewById(R.id.textView2);
            text3 = (TextView) itemView.findViewById(R.id.textView3);
            text1.setTypeface(null, Typeface.BOLD);
        }
    }

    private void showSplash() {
        findViewById(R.id.appBarLayout).setVisibility(View.GONE);
        viewSplash.setVisibility(View.VISIBLE);
//        viewSplash.bringToFront();
        viewSplash.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "viewSplash.postDelayed");
//                Animation anim = AnimationUtils.loadAnimation(context, android.R.anim.fade_out);
//                anim.setDuration(800);
//                anim.setAnimationListener(new Animation.AnimationListener() {
//                    public void onAnimationStart(Animation animation) {}
//                    public void onAnimationRepeat(Animation animation) {}
//                    public void onAnimationEnd(Animation animation) {
//                        viewSplash.setVisibility(View.GONE);
//                        Log.d(TAG, "viewSplash.gone");
//                    }
//                });
//                viewSplash.setAnimation(anim);
//                anim.start();
                viewSplash.setVisibility(View.GONE);
                findViewById(R.id.appBarLayout).setVisibility(View.VISIBLE);
            }
        }, 2200L);


    }

    private int getBusColor(int routeType) {

        int color = 0;
        switch (routeType) {
            case 2: // 마을
                color = 0x73B074;
                break;
            case 3: // 간선
                color = 0x176097;
                break;
            case 4: // 지선
                color = 0x73B074;
                break;
            case 5: // 순환
                color = 0xDBDF00;
                break;
            case 6: // ???
                color = 0xE62433;
                break;
            case 7: // 광역
                color = 0xe62333;
                break;
            case 8: // 인천
                color = 0x0D8B84;
                break;
            default:
                color = 0x2D383E;
                break;
        }
        return 0xff000000 | color;
    }

//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        JSONObject json = (JSONObject) adapterStop.getItem(position);
////        Intent intent = new Intent(this, StopInfoActivity.class);
////        intent.putExtra("info", json.toString());
////        startActivity(intent);
//        int arsId = json.optInt("arsId");
//
//        editBusSearch.setText(String.valueOf(arsId));
//        textStopTitle.setText(json.optString("name"));
//
//        onSearchStop(arsId);
//    }

    public void onSearchStop(int stopid){
        this.busStopId = stopid;
        reload();
    }


    public void reload() {

        editBusSearch.setSelection(editBusSearch.getText().length());

        if (task != null)
            return;

        task =
            new AsyncTask<Void, Void, JSONObject>() {

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();

                    if(adapter!=null) {
                        resultList = new JSONArray();
                        adapter.notifyDataSetChanged();
                    }

                    stopArrivalEmptyView.setVisibility(View.GONE);
                    stopArrivalProgress.setVisibility(View.VISIBLE);

//                        btnReload.setEnabled(false);
                }

                @Override
                protected JSONObject doInBackground(Void... params) {
                    try {
                        String url = "http://m.bus.go.kr/mBus/bus/getStationByUid.bms";
                        String search = String.valueOf(busStopId);
                        String arsId = search.replaceAll("-", "");

                        OkHttpClient client = new OkHttpClient();

                        RequestBody body = new FormEncodingBuilder()
                                .add("arsId", arsId)
                                .build();

                        Request request = new Request.Builder()
                                .url(url)
                                .post(body)
                                .build();

//                        if(prefs.contains("test")){
//                            Log.d(TAG, "from prefs");
//                            String jsonStr = prefs.getString("test","");
//                            JSONObject json = new JSONObject(jsonStr);
//                            return json;
//                        }
                        Response response = client.newCall(request).execute();
                        if(response.isSuccessful()){
                            try {
                                String resp = response.body().string();
//                                prefs.edit().putString("test", resp).commit();

                                JSONObject json = new JSONObject(resp);
                                return json;
                            }catch (JSONException e){
                                Log.d(TAG, response.body().string());
                                e.printStackTrace();
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
                ;

                @Override
                protected void onPostExecute(JSONObject jsonObject) {
                    super.onPostExecute(jsonObject);
                    JSONObject response = jsonObject;


                    float gpsX=0;
                    float gpsY=0;
                    String stNm = "";

                    stopArrivalEmptyView.setVisibility(View.GONE);
                    stopArrivalProgress.setVisibility(View.GONE);
                    if(response!=null ){
//                        Log.d(TAG, response.toString());

                        JSONArray resultList = response.optJSONArray("resultList");
                        if(resultList!=null && resultList.length()>0){
                            ArrayList<JSONObject> tmpList = new ArrayList<>(resultList.length());
                            for(int i=0;i<resultList.length();i++)
                                tmpList.add(resultList.optJSONObject(i));

                            Collections.sort(tmpList, new Comparator<JSONObject>() {
                                String keyIsEnd = "운행종료";

                                @Override
                                public int compare(JSONObject lhs, JSONObject rhs) {

                                    boolean lshEnd = lhs.optString("arrmsg1").equals(keyIsEnd);
                                    boolean rshEnd = rhs.optString("arrmsg1").equals(keyIsEnd);

//                                    if(lhs.optInt("routeType") != rhs.optInt("routeType")){
//                                        return lhs.optInt("routeType")-rhs.optInt("routeType");
//                                    }

                                    if (lshEnd == rshEnd)
                                        return lhs.optInt("traTime1") - rhs.optInt("traTime1");
                                    else {
                                        return rshEnd ? -1 : 1;
                                    }

                                }
                            });
                            JSONArray resultListNew = new JSONArray();

                            for(JSONObject jsonObj : tmpList) {
                                String rtNm = jsonObj.optString("rtNm");
                                if(stNm==null || stNm.length()==0)
                                    stNm = jsonObj.optString("stNm");
                                if(gpsX==0 || gpsY==0){
                                    gpsX = (float) jsonObj.optDouble("gpsX",0);
                                    gpsY = (float) jsonObj.optDouble("gpsY",0);
                                }

                                if(rtNm.length()>2 &&
                                        Character.isDigit(rtNm.charAt(0)) ){
                                    int numLength = 0;
                                    int idx = 0;
                                    for(int i=0;i<rtNm.length();i++) {
                                        if (Character.isDigit(rtNm.charAt(i)))
                                            numLength++;
                                        else if (idx == 0)
                                            idx = i;
                                    }

                                    if(numLength!=rtNm.length()){
                                        rtNm = rtNm.substring(0,idx) + " " + rtNm.substring(idx);
                                    }
                                    try {
                                        jsonObj.put("rtNm" , rtNm);
                                    } catch (JSONException e) {
                                    }

                                }
                                resultListNew.put(jsonObj);
                            }
                            StopListActivity.this.resultList = resultListNew;
                        }
                    }

                    String arsId = String.valueOf(busStopId).replaceAll("-", "");

                    findViewById(R.id.btnMessage).setVisibility(
                            BusStopApplication.getApp().isMessageRecvStop(arsId) ? View.VISIBLE : View.GONE
                    );

                    adapter.notifyDataSetChanged();;
                    lastLng = gpsX;
                    lastLat = gpsY;
                    prefs.edit().
                            putFloat("lastLng", lastLng).
                            putFloat("lastLat", lastLat).apply();
                    textStopTitle.setText(stNm);
                    task = null;

                }
            };
        task.execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        instance = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        instance = this;
        checkAppPermission();
        BeaconManager manager = BeaconManager.getInstanceForApplication(getApplicationContext());
        manager.setBackgroundMode(false);
        updateLocation();

    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    private void updateLocation() {
    }

    @Override
    protected void onDestroy() {
        BeaconManager manager = BeaconManager.getInstanceForApplication(getApplicationContext());
        manager.setBackgroundMode(true);

        super.onDestroy();
    }

    private void checkAppPermission(){
        final String permissionLocation = Manifest.permission.ACCESS_FINE_LOCATION;
        int hasLocationPermission = ContextCompat.checkSelfPermission(this, permissionLocation);
        Log.d(TAG, "hasLocationPermission : " + (hasLocationPermission == 0) + " " + hasLocationPermission);

        if (hasLocationPermission != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permissionLocation)) {
                Snackbar.make(recyclerView, R.string.msg_permission_denied, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivityForResult(intent, REQUEST_PERMISSION_SETTING);

                        ActivityCompat.requestPermissions(StopListActivity.this,
                                new String[]{permissionLocation},
                                REQUEST_CODE_ASK_PERMISSIONS);

                    }
                }).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{permissionLocation},
                    REQUEST_CODE_ASK_PERMISSIONS);

            return;
        }
    }


    private void requestLocationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
//                    insertDummyContact();
                    ((BusStopApplication)getApplication()).init();
                    startService(new Intent(this,BeaconService.class));
                } else {
                    // Permission Denied
                    Snackbar.make(recyclerView, R.string.msg_permission_denied, Snackbar.LENGTH_LONG).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_PERMISSION_SETTING){
            ((BusStopApplication)getApplication()).init();
//            startService(new Intent(this,BeaconService.class));
        }else if(requestCode==REQUEST_MAPS && data!=null){
            BusStop stop = (BusStop) data.getSerializableExtra("busstop");
            updateStop(stop);
        }else if(requestCode==REQUSET_STOP_SELECT && data!=null){
            BusStop stop = (BusStop) data.getSerializableExtra("busstop");
            updateStop(stop);
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void updateStop(BusStop stop) {
        resultList = new JSONArray(); // to force refresh
        JSONArray arr = new JSONArray();
        arr.put(stop.toJSON());
        updateStop(arr);
    }

    @Override
    public void updateStop(final JSONArray array) {

        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (resultList == null || resultList.length() == 0) {
                    resultList = array;
                    adapter.notifyDataSetChanged();

                    View emptyView = findViewById(R.id.stopEmptyView);
                    boolean exists = array.length() > 0;
                    emptyView.setVisibility(exists ? View.GONE : View.VISIBLE);
                    JSONObject item = (JSONObject) array.optJSONObject(0);
                    int arsId = item.optInt("arsId");
                    onSearchStop(arsId);
                    editBusSearch.setText(String.valueOf(arsId));
                    textStopTitle.setText(item.optString("name"));
                }
            }
        });

    }

    public void onClickMap(View v){
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("lat", lastLat);
        intent.putExtra("lng", lastLng);

        startActivityForResult(intent, REQUEST_MAPS);
    }

    public void onClickMessage(View v){
        Intent intent = new Intent(this, SendMessageActivity.class);
//        startActivityForResult(intent,REQUSET_STOP_SELECT);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    public void onClickMoreStop(View v){

        Intent intent = new Intent(this, StopSelectActivity.class);
        startActivityForResult(intent,REQUSET_STOP_SELECT);
        overridePendingTransition(0, 0);

    }


    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient");
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

}

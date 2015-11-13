package kr.flit.busstop;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

public class BeaconService extends Service
    implements BeaconConsumer, RangeNotifier
{
    private int NOTIFY_BEACON_SERVICE = 10;
    private static final String TAG = "BeaconService";
    private SharedPreferences prefsStation ;
//    private HashMap<Integer, String> stationNames = new HashMap<>();
    private HashMap<Integer, JSONObject> stationMap = new HashMap<>();

    private HashSet<Integer> reqProcessses = new HashSet<>();
    private ArrayList<Beacon> list = new ArrayList<>();

    interface StopListListener {
        void updateStop(JSONArray array);
    }

    BeaconManager manager;
    public BeaconService() {

    }
//    private StopListListener listener ;


//    public void setListener(StopListListener listener) {
//        this.listener = listener;
//    }

    @Override
    public void onCreate() {
        // workaround for http://code.google.com/p/android/issues/detail?id=20915
//        try {
//            Class.forName("android.os.AsyncTask");
//        } catch (ClassNotFoundException e) {}

        super.onCreate();
        Log.d(TAG, "onCreate");
        manager = BeaconManager.getInstanceForApplication(this);
        prefsStation = getSharedPreferences("station", Context.MODE_PRIVATE);
        manager.setForegroundBetweenScanPeriod(10*1000L);
        manager.setBackgroundMode(false);
        manager.bind(this);
//        sendNotification(new JSONArray());

        try {
            manager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {   }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFY_BEACON_SERVICE);

        super.onDestroy();
        manager.unbind(this);
        manager.setBackgroundMode(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
//        Log.d(TAG, "onBind " + intent.toString());
//        return null;
    }

    @Override
    public void onBeaconServiceConnect() {
        Log.d(TAG, "onBeaconServiceConnect");
        manager.setRangeNotifier(this);

    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
//        Iterator<Beacon> it = beacons.iterator();
//        logToDisplay("---------");

        list.clear();
        if(beacons.size()==0){
            stopSelf();
            return;
        }

        Log.i(TAG, "didRangeBeaconsInRegion");
        StringBuilder notifyMsg = new StringBuilder();


        for(Beacon beacon : beacons){
            String id1hex = beacon.getId1().toUuidString().toLowerCase();
            if (id1hex.length() > 8) {
                id1hex = id1hex.substring(4, 8);
            }
            if(id1hex.equals("ff00"))
                list.add(beacon);
        }

        JSONArray array = new JSONArray();

        Collections.sort(list, new Comparator<Beacon>() {
            @Override
            public int compare(Beacon lhs, Beacon rhs) {
                if (lhs.getDistance() == rhs.getDistance())
                    return 0;
                return lhs.getDistance() - rhs.getDistance() > 0 ? 1 : -1;
            }
        });
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        notifyMsg
            .append(sdf.format(Calendar.getInstance().getTime()))
            .append("\n")
        ;

        Iterator<Beacon> it = list.iterator();
        while(it.hasNext()) {
            Beacon beacon = it.next();
//            String id1uid = "FF10".toLowerCase();
//            Log.d(TAG, beacon.getId1().toUuidString());
            String id1hex = beacon.getId1().toUuidString().toLowerCase();
//            if (id1hex.length() > 8) {
//                id1hex = id1hex.substring(4, 8);
//            }

            int busStopId = beacon.getId2().toInt() << 8;// * 0x100;
            busStopId += beacon.getId3().toInt();
            final String sBusStopId = String.valueOf(busStopId);

            if(prefsStation.contains(sBusStopId)==false && reqProcessses.contains(busStopId)==false) {
//                stationNames.put(busStopId, "");
                final int fBusStopId = busStopId;
                String url = "http://m.bus.go.kr/mBus/bus/getStationByUid.bms";
                String search = String.valueOf(fBusStopId);
                String arsId = search.replaceAll("-", "");

                OkHttpClient client = new OkHttpClient();

                RequestBody body = new FormEncodingBuilder()
                        .add("arsId", arsId)
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        //TODO: failure handling
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        boolean isError = response.isSuccessful();

                        if (response.isSuccessful()) {
                            String res = response.body().string();
                            try {
                                JSONObject json = new JSONObject(res);

                                JSONArray resultList = json.optJSONArray("resultList");
                                if (resultList != null && resultList.length() > 0) {
                                    isError = false;
                                    JSONObject resultItem = resultList.optJSONObject(0);
                                    String stationName = resultItem.optString("stNm", "");
//                                stationNames.put(fBusStopId, stationName);
                                    prefsStation.edit().putString(sBusStopId, stationName).apply();
                                    JSONObject stopObj = stationMap.get(fBusStopId);
                                    if (stopObj != null)
                                        try {
                                            stopObj.put("result", resultList.toString());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    //gpsY
                                    //gpsX
                                }
                            } catch (JSONException e) {
                                isError = true;
                                e.printStackTrace();
                                ;
                            }
                            sendNotification();
                        }

                        if (isError) {
                            //TODO: error handling
                        }
                    } // end of onResponse
                }); // end of client.callback

            }else{

            }// end of if

            // 14-013 , 36 bd , 54 189
            // 14-014 , 36 be , 54 190

//                if (beacon.getId1().toUuidString().contains(id1uid))
            {

                String msg = String.format("name : %s, type : %s id1 : %s ids : %s distance : %2.2f busStopId : %d",
                        beacon.getBluetoothName(),
                        Integer.toHexString(beacon.getBeaconTypeCode()),
//                        Integer.toHexString(beacon.getId1().toInt()),

//                        beacon.getId1().toString(),
                        id1hex,
                        beacon.getId1().toHexString() + " " +
                                beacon.getId2().toHexString() + " " +
                                beacon.getId3().toHexString(),
                        beacon.getDistance(),
                        busStopId
                );
                Log.d(TAG, msg);
//                logToDisplay(msg);

            }


        } // end of iterator

        sendNotification();

    }


    private void sendNotification() {
        Log.d(TAG, "sendNotification");
        JSONArray stationList = new JSONArray();

        for(Beacon beacon : list){
            int busStopId = beacon.getId2().toInt() << 8;// * 0x100;
            busStopId += beacon.getId3().toInt();
            final String sBusStopId = String.valueOf(busStopId);

            JSONObject stopObj = stationMap.get(busStopId);
            if(stopObj==null){
                stopObj = new JSONObject();
                try {
                    stopObj.put("arsId", busStopId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            try {
                stopObj.put("name", prefsStation.getString(sBusStopId, ""));
                stopObj.put("distance", beacon.getDistance());
                stationList.put(stopObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        Intent contentIntent = new Intent(this, StopListActivity.class);
        contentIntent.putExtra("source", stationList.toString());

        StringBuilder msg = new StringBuilder();


        for(int i=0;i<stationList.length();i++){
            JSONObject stopObj = stationList.optJSONObject(i);
            String name = stopObj.optString("name");
            int busStopId = stopObj.optInt("arsId");
            String sBusStopId = String.valueOf(busStopId);
            double distance = stopObj.optDouble("distance");

            String msgIdNameDist = String.format("%s %s %2.2fm",
                    sBusStopId,
                    prefsStation.getString(sBusStopId,""),
                    distance
            );
            msg.append(msgIdNameDist).append("\n");
        }



        stackBuilder.addNextIntent(contentIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
//                        PendingIntent.FLAG_ONE_SHOT
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        Notification notification = new Notification.BigTextStyle(
                new Notification.Builder(this)
                        .setContentTitle("버스정류장이 근처에 있습니다")
                        .setContentText(msg.toString())
                        .setSmallIcon(R.drawable.ic_stat_icon)
                        .setLargeIcon(largeIcon)
                        .setContentIntent(resultPendingIntent)
                )

                .bigText(msg.toString())
                .build();



//        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_BEACON_SERVICE, notification);


        if(StopListActivity.instance!=null){
            StopListActivity.instance.updateStop(stationList);
        }
    }
}

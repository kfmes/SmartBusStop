package kr.flit.busstop;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class BeaconService extends Service
    implements BeaconConsumer, RangeNotifier
{
    private int NOTIFY_BEACON_SERVICE = 10;
    private static final String TAG = "BeaconService";
    private SharedPreferences prefsStation ;
//    private HashMap<Integer, String> stationNames = new HashMap<>();
    private HashMap<Integer, JSONObject> stationMap = new HashMap<>();

    private HashSet<Integer> reqProcessses = new HashSet<>();

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
        super.onDestroy();
        manager.unbind(this);
        manager.setBackgroundMode(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
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

        Log.i(TAG, "didRangeBeaconsInRegion");
        StringBuilder notifyMsg = new StringBuilder();
        ArrayList<Beacon> list = new ArrayList<>();

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
                if(lhs.getDistance()==rhs.getDistance())
                    return 0;
                return lhs.getDistance() - rhs.getDistance() >0 ? 1 : -1 ;
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

            if(prefsStation.contains(sBusStopId)==false && reqProcessses.contains(busStopId)==false){
//                stationNames.put(busStopId, "");
                final int fBusStopId = busStopId;
                reqProcessses.add(busStopId);

                new AsyncTask<Void, Void, JSONObject>(){
                    @Override
                    protected JSONObject doInBackground(Void... params) {
                        try {
                            HttpClient client = new DefaultHttpClient();
                            String uri = "http://m.bus.go.kr/mBus/bus/getStationByUid.bms";
                            String search = String.valueOf(fBusStopId);
                            HttpPost request = new HttpPost(uri);
                            String arsId = search.replaceAll("-","");
                            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                            nvps.add(new BasicNameValuePair("arsId", arsId));

                            request.setEntity(
                                    new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

                            HttpResponse response = client.execute(request);
                            String res = EntityUtils.toString(response.getEntity());
                            JSONObject json = new JSONObject(res);
                            return json;
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(JSONObject jsonObject) {
                        super.onPostExecute(jsonObject);
                        JSONObject response = jsonObject;
                        boolean isError = true;
                        if(response!=null ){
                            JSONArray resultList = response.optJSONArray("resultList");
                            if(resultList!=null && resultList.length()>0){
                                isError = false;
                                JSONObject resultItem = resultList.optJSONObject(0);
                                String stationName = resultItem.optString("stNm","");
//                                stationNames.put(fBusStopId, stationName);
                                prefsStation.edit().putString(sBusStopId, stationName).apply();
                                JSONObject stopObj =  stationMap.get(fBusStopId);
                                if(stopObj!=null)
                                    try {
                                        stopObj.put("result", resultList.toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                //gpsY
                                //gpsX
                            }
                        }

//                        if(isError) {
//                            stationNames.put(fBusStopId, "");
//                        }
//
                        reqProcessses.remove(fBusStopId);
                    }
                }.execute();

            }

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
                array.put(stopObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } // end of iterator

        sendNotification(array);
        if(StopListActivity.instance!=null){
            StopListActivity.instance.updateStop(array);
        }
//        if(listener!=null)
//            listener.updateStop(array);
////        sendNotification(notifyMsg.toString());

    }


    private void sendNotification(JSONArray stationList) {


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


        Notification notification = new Notification.BigTextStyle(
                new Notification.Builder(this)
                        .setContentTitle("버스정류장이 근처에 있습니다")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(resultPendingIntent)
                )
                .bigText(msg.toString())
                .build();



//        builder.setContentIntent(resultPendingIntent);
        NotificationManager notificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_BEACON_SERVICE, notification);
    }
}

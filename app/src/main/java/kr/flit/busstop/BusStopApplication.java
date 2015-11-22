package kr.flit.busstop;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by kfmes on 15. 5. 24..
 */
public class BusStopApplication extends Application
implements BootstrapNotifier {
    private static final String TAG = "AndroidProximityReferenceApplication";
    private RegionBootstrap regionBootstrap;
    private BackgroundPowerSaver backgroundPowerSaver;
    private boolean haveDetectedBeaconsSinceBoot = false;
//    private SharedPreferences prefsStation ;

    private long scanPeriodActive = 5 * 1000;
    private long betweenScanPeriodActive = 5 * 1000;


    private long scanPeriodDefault = 10000;
    private long betweenScanPeriodDefault =  5 * 60 * 1000;
    private static BusStopApplication app;
    private List<BusStop> gpsStop;
    private List<BusStop> beaconStop;
    private Location lastLocation;
    private Set<String> canMessageRecvStop = new HashSet<>();

//        beaconManager.setBackgroundScanPeriod(5000L);
//        beaconManager.setBackgroundBetweenScanPeriod(5000L);

    protected void init(){

        BeaconManager beaconManager = org.altbeacon.beacon.BeaconManager.getInstanceForApplication(this);

        // By default the AndroidBeaconLibrary will only find AltBeacons.  If you wish to make it
        // find a different type of beacon, you must specify the byte layout for that beacon's
        // advertisement with a line like below.  The example shows how to find a beacon with the
        // same byte layout as AltBeacon but with a beaconTypeCode of 0xaabb.  To find the proper
        // layout expression for other beacon types, do a web search for "setBeaconLayout"
        // including the quotes.
        //
        // beaconManager.getBeaconParsers().add(new BeaconParser().
        //        setBeaconLayout("m:2-3=aabb,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        //
        // http://stackoverflow.com/a/19026387/1660986

        beaconManager.getBeaconParsers().add(new BeaconParser().
                //0xFF10
                        setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        //a495ff10-c5b1-4b44
        //0215

//        beaconManager.setBackgroundBetweenScanPeriod(5000L);
//        beaconManager.setBackgroundScanPeriod(5000L);
//        beaconManager.setBackgroundBetweenScanPeriod(5000L);

//    beaconManager.setBackgroundScanPeriod(5000);

        Log.d(TAG, "setting up background monitoring for beacons and power saving");
        // wake up the app when a beacon is seen
        String id1Str = "a495ff00-c5b1-4b44-b512-1370f02d74de";
        Identifier id1 = Identifier.parse(id1Str);

        String id2Str = "a495ff01-c5b1-4b44-b512-1370f02d74de";
        Identifier id2 = Identifier.parse(id2Str);

        Region region = new Region("backgroundRegion", id1, null, null);
        Region region2 = new Region("backgroundRegion2", id2, null, null);

//        new Region()
        List<Region> regions = new ArrayList<>();
        regions.add(region);
        regions.add(region2);

        regionBootstrap = new RegionBootstrap(this, regions);

        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%
//        backgroundPowerSaver = new BackgroundPowerSaver(this);

        // If you wish to test beacon detection in the Android Emulator, you can use code like this:
        // BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        // ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
    }

    public void onCreate() {
        super.onCreate();
        app = this;
        gpsStop = new ArrayList<>();
        beaconStop = new ArrayList<>();

        Log.d(TAG, "onCreate Begin");
        init();
        Log.d(TAG, "onCreate End");
    }


    @Override
    public void didEnterRegion(Region region) {
        // In this example, this class sends a notification to the user whenever a Beacon
        // matching a Region (defined above) are first seen.
        Log.i(TAG, "==did enter region. " + region);
        haveDetectedBeaconsSinceBoot = true;


        BeaconManager manager = BeaconManager.getInstanceForApplication(this);
        manager.setBackgroundScanPeriod(scanPeriodActive);
        manager.setBackgroundBetweenScanPeriod(betweenScanPeriodActive);


        startService(new Intent(this, BeaconService.class));

    }

    @Override
    public void didExitRegion(Region region) {
        Log.i(TAG, "==didExitRegion " + region);
//        if (monitoringActivity != null) {
//            monitoringActivity.logToDisplay("I no longer see a beacon.");
//        }
//        sendNotification("didExitRegion"  + region.toString());


        BeaconManager manager = BeaconManager.getInstanceForApplication(this);
        manager.setBackgroundScanPeriod(scanPeriodDefault);
        manager.setBackgroundBetweenScanPeriod(betweenScanPeriodDefault);
        stopService(new Intent(this, BeaconService.class));


    }

    @Override
    public void didDetermineStateForRegion(int state, Region region) {
        Log.i(TAG, "==didDetermineStateForRegion" + state + " " + region);
    }

    public static BusStopApplication getApp() {
        return app;
    }
    public void setGpsStop(List<BusStop> busStop) {
        this.gpsStop = busStop;
    }
    public void setBeaconStop(List<BusStop> busStop){
        this.beaconStop = busStop;
    }

    public List<BusStop> getBusStop(){
        ArrayList<BusStop> stops = new ArrayList<>(beaconStop);
        for(BusStop stop : gpsStop){
            if(stops.contains(stop)==false)
                stops.add(stop);
        }
        return stops;
    }


    public void updateLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putString("lastProvider", lastLocation.getProvider())
                .putFloat("lastLat", (float) lastLocation.getLatitude())
                .putFloat("lastLng", (float) lastLocation.getLongitude())
                .apply();
    }

    public Location getLastLocation() {
        if(lastLocation==null ){
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if(prefs.contains("lastProvider")){
                String provider = prefs.getString("lastProvider","");
                double lastLat = prefs.getFloat("lastLat", 0);
                double lastLng = prefs.getFloat("lastLng", 0);

                Location location = new Location(provider);
                location.setLatitude(lastLat);
                location.setLongitude(lastLng);
                this.lastLocation = location;
            }
        }
        return lastLocation;
    }

    public boolean isMessageRecvStop(String arsId){
        return canMessageRecvStop.contains(arsId);
    }

    public void addMessageRecvStop(String arsId) {
        canMessageRecvStop.add(arsId);
    }
}
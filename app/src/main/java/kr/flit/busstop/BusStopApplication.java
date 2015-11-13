package kr.flit.busstop;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

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

//        beaconManager.setBackgroundScanPeriod(5000L);
//        beaconManager.setBackgroundBetweenScanPeriod(5000L);


    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate Begin");
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

        Region region = new Region("backgroundRegion",
                id1, null, null);
//        new Region()
        regionBootstrap = new RegionBootstrap(this, region);

        // simply constructing this class and holding a reference to it in your custom Application
        // class will automatically cause the BeaconLibrary to save battery whenever the application
        // is not visible.  This reduces bluetooth power usage by about 60%
//        backgroundPowerSaver = new BackgroundPowerSaver(this);

        // If you wish to test beacon detection in the Android Emulator, you can use code like this:
        // BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
        // ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
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


}

package kr.flit.busstop;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by kfmes on 15. 11. 17..
 */
public class BusStop implements Serializable{
    private String arsId;
    private String name;
    private double lat;
    private double lng;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BusStop busStop = (BusStop) o;
        if (arsId != null ? !arsId.equals(busStop.arsId) : busStop.arsId != null) return false;
        return !(name != null ? !name.equals(busStop.name) : busStop.name != null);
    }

    @Override
    public int hashCode() {
        int result = arsId != null ? arsId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject();
            json.put("arsId", arsId);
            json.put("name", name);
            json.put("stNm", name);
            json.put("gpsX", lng);
            json.put("gpsY", lat);
            return json;
        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
    }

//    public Location getLocation() {
//        Location location = new Location("busstop");
//        location.setLatitude(lat);
//        location.setLongitude(lng);
//        return location;
//    }

    public void setLatLng(double lat, double lng){
        this.lat = lat;
        this.lng = lng;
    }
//    public void setLocation(Location location) {
//        this.lat = location.getLatitude();
//        this.lng = location.getLongitude();
//    }

    public String getArsId() {
        return arsId;
    }

    public void setArsId(String arsId) {
        this.arsId = arsId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLat() {
        return lat;
    }
    public double getLng() {
        return lng;
    }
}

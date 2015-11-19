package kr.flit.busstop;

import android.location.Location;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by kfmes on 15. 11. 17..
 */
public class BusStop implements Serializable{
    private Location location;
    private String arsId;
    private String name;

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
            json.put("gpsX", location.getLongitude());
            json.put("gpsY", location.getLatitude());
            return json;
        }catch (JSONException e){
            e.printStackTrace();
            return null;
        }
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

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
}

package kr.flit.busstop.adapter;

import org.json.JSONObject;

public interface ViewProjector {
	/**
	 * set item data to view
	 * @param item
	 * @param v
	 */
	public void projectile(JSONObject item);
}

package kr.flit.busstop.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONObject;

public class JSONArrayAdapterItem extends JSONArrayAdapter {
	
	public JSONArrayAdapterItem(Context context) {
		super(context);
	}



    @Override
	public View getView(int position, View view, ViewGroup parent) {
		ViewProjector convertView;
		if(null == view)
			convertView = createObject();
		else
			convertView = (ViewProjector) view;
		
		JSONObject item = (JSONObject) getItem(position);
		
		convertView.projectile(item);
		
		return (View)convertView;
	}
	
	/**
	 * child should override
	 * @return ViewProjector
	 */
	public ViewProjector createObject() {
		return null;
	}

}

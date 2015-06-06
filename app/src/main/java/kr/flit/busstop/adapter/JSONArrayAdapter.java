package kr.flit.busstop.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONArrayAdapter extends BaseAdapter {
	protected JSONArray dataSource;
	protected Context context;
	
	public JSONArrayAdapter(Context context) {
		super();
		this.context = context;
	}

	public JSONArray getDataSource() {
		return dataSource;
	}
	
	public void setDataSource(JSONArray d) {
		this.dataSource = d;
		notifyDataSetChanged();
	}
	
	public void addDataSource(JSONArray d) {
		if(null == this.dataSource)
			this.dataSource = d;
		else
			addUpList(this.dataSource, d);
		
		notifyDataSetChanged();
	}

	@Override
	public int getCount() {
		if(null == dataSource) {
			return 0;
		}
		return dataSource.length();
	}

	@Override
	public Object getItem(int position) {
		if(null == dataSource)
			return null;
		return dataSource.optJSONObject(position);
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	/**
	 * Must override!
	 */
	@Override
	public View getView(int position, View view, ViewGroup parent) {
		
		return null;
	}
	
	/**
	 * jsonArray 붙이기 src jsonArray에 dest를 붙임
	 * @param src
	 *            : return
	 * @param dest
	 */
	static public void addUpList(JSONArray src, JSONArray dest) {
		if (null == src) {
			return;
		}

		if (null == dest) {
			return;
		}

		int lengthDest = dest.length();

		try {
			for (int i = 0; i < lengthDest; i++) {
				JSONObject item = dest.getJSONObject(i);
				src.put(item);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	
	public void addItemFirst(JSONObject json){
		JSONArray arr = new JSONArray();
		arr.put(json);
		
		int lengthDest = dataSource.length();

		try {
			for (int i = 0; i < lengthDest; i++) {
				JSONObject item = dataSource.getJSONObject(i);
				arr.put(item);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		this.dataSource = arr;
		notifyDataSetChanged();
	}	
	
	public void addItemLast(JSONObject json){
		this.dataSource.put(json);
		notifyDataSetChanged();
	}
	
	public void addItemLast(JSONArray array){
		if(dataSource==null){
			setDataSource(array);
			return;
		}
		int length = array.length();
		for(int i=0;i<length;i++){
			dataSource.put(array.opt(i));
		}
//		this.dataSource.put(json);
//		notifyDataSetChanged();
	}


}

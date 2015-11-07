package kr.flit.busstop;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import kr.flit.busstop.adapter.JSONArrayAdapterItem;
import kr.flit.busstop.adapter.ViewProjector;


public class StopListActivity extends ActionBarActivity
implements AbsListView.OnItemClickListener, BeaconService.StopListListener
{

    private AsyncTask<Void, Void, JSONObject> task;
    private JSONObject stopInfo ;
    private JSONArray resultList ;

    private ListView listViewStop;
    private ListView listViewInfo;


    private EditText editBusSearch;
    private TextView textStopTitle;

    JSONArrayAdapterItem adapterStop;
    JSONArrayAdapterItem adapter;
    private Context context;
    private final String TAG = "StopList";

    private int busStopId ;
    private String busStopName;
    Typeface fontOldBaths = null;

    private View stopArrivalEmptyView ;
    private View stopArrivalProgress ;

    protected static StopListActivity instance;
    private View viewSplash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        instance = this;
        setContentView(R.layout.activity_stop_list);


        this.listViewStop = (ListView) findViewById(R.id.listViewStop);
        this.editBusSearch = (EditText) findViewById(R.id.editBusSearch);
        this.listViewInfo = (ListView) findViewById(R.id.listView);
        this.textStopTitle = (TextView) findViewById(R.id.textStopTitle);
        this.stopArrivalEmptyView = findViewById(R.id.stopArrivalEmptyView);
        this.stopArrivalProgress = findViewById(R.id.stopArrivalProgress);
        this.viewSplash = findViewById(R.id.splash);

//        JSONArray source = getIntent().get
        JSONArray source = new JSONArray();
        String strSource = getIntent().getStringExtra("source");

        Log.d(TAG, "SOURCE " + strSource);

        if(strSource==null){
            showSplash();
        }else
            viewSplash.setVisibility(View.GONE);


//        TextView tv = (TextView) findViewById(R.id.CustomFontText);
//        tv.setTypeface(tf);
        fontOldBaths = Typeface.createFromAsset(getAssets(),
                "BMHANNA_11yrs_otf.otf");





        stopArrivalProgress.setVisibility(View.GONE);

        listViewStop.requestFocus();

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
                if(s.length()==5){
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


        adapterStop = new JSONArrayAdapterItem(this){
            @Override
            public ViewProjector createObject() {
                return new BusStopItem(context);
            }
            class BusStopItem extends FrameLayout implements ViewProjector{
                TextView text1;
                TextView text2;
                TextView text3;

                public BusStopItem(Context context){
                    super(context);
                    ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.listitem_stop,null);
                    text1 = (TextView) view.findViewById(R.id.textView1);
                    text2 = (TextView) view.findViewById(R.id.textView2);
                    text3 = (TextView) view.findViewById(R.id.textView3);
                    addView(view);
                }
                @Override
                public void projectile(JSONObject item) {
                    text1.setText(item.optString("name"));
                    text2.setText(item.optString("arsId"));
                    text3.setText(
                            String.format("%2.2fm", item.optDouble("distance"))
                    );
                }
            }

            @Override
            public void notifyDataSetChanged() {
                super.notifyDataSetChanged();
                View emptyView = findViewById(R.id.stopEmptyView);
                boolean exists = adapterStop.getCount() > 0;
                emptyView.setVisibility(exists ? View.GONE : View.VISIBLE);

                JSONArray stopList = adapterStop.getDataSource();
//                JSONArray buslist = adapter.getDataSource();


                if(stopList!=null && stopList.length()>0
                    && (adapter== null || adapter.getCount()==0)
                        //&& buslist==null
                        ){
                    JSONObject item = (JSONObject) adapterStop.getItem(0);
                    int arsId = item.optInt("arsId");
                    onSearchStop(arsId);
                    editBusSearch.setText(String.valueOf(arsId));
                    textStopTitle.setText(item.optString("name"));
                }
            }
        };

        adapterStop.setDataSource(source);
        listViewStop.setAdapter(adapterStop);
        listViewStop.setOnItemClickListener(this);


        adapter = new JSONArrayAdapterItem(this){
            @Override
            public ViewProjector createObject() {
                return new BusStopInfo(context);
            }

            class BusStopInfo extends FrameLayout implements ViewProjector {
                TextView text1;
                TextView text2;
                TextView text3;
                ImageView imgEndBus;

                public BusStopInfo(Context context) {
                    super(context);
                    ViewGroup view = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.listitem_stopinfo, null);
                    imgEndBus = (ImageView) view.findViewById(R.id.imgEndBus);
                    text1 = (TextView) view.findViewById(R.id.textView1);
                    text2 = (TextView) view.findViewById(R.id.textView2);
                    text3 = (TextView) view.findViewById(R.id.textView3);
                    text1.setTypeface(null, Typeface.BOLD);
                    addView(view);
                }

                @Override
                public void projectile(JSONObject item) {
                    /*
                    //
                    3 간선
                    4 지선
                    5 순환
                    6 광역
                    7 8 경기, 인천
                    default
                    */

                    int busColor = getBusColor(item.optInt("routeType"));
                    text1.setTextColor(busColor);

                    String adirection = item.optString("adirection");
                    if(adirection.length()>0)
                        adirection += "행";

                    int isLast1 = item.optInt("isLast1");
                    String nextBus = item.optString("nextBus");

                    text1.setText(item.optString("rtNm"));
                    if(isLast1==1)
                        imgEndBus.setVisibility(View.VISIBLE);
                    else
                        imgEndBus.setVisibility(View.GONE);
//                    String prefix = String.format("[%d %s]", isLast1, nextBus);

                    text2.setText(
//                            prefix +
                            item.optString("arrmsg1") );
                    text3.setText(adirection);



//                    text3.setText(item.optInt("traTime1") + " " );//+ item.optInt());
//                    text3.setText(
//                            String.format("%2.2fm", item.optDouble("distance"))
//                    );
                }
            }
        };
        listViewInfo.setAdapter(adapter);


    }

    private void showSplash() {
        viewSplash.setVisibility(View.VISIBLE);
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


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        JSONObject json = (JSONObject) adapterStop.getItem(position);
//        Intent intent = new Intent(this, StopInfoActivity.class);
//        intent.putExtra("info", json.toString());
//        startActivity(intent);
        int arsId = json.optInt("arsId");

        editBusSearch.setText(String.valueOf(arsId));
        textStopTitle.setText(json.optString("name"));

        onSearchStop(arsId);
    }

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
//                    Toast.makeText(context, "reload ", Toast.LENGTH_SHORT).show();

                    if(adapter!=null) {
                        adapter.setDataSource(new JSONArray());
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

                        Response response = client.newCall(request).execute();
                        if(response.isSuccessful()){
                            JSONObject json = new JSONObject(response.body().string());
                            return json;
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

                    stopArrivalEmptyView.setVisibility(View.GONE);
                    stopArrivalProgress.setVisibility(View.GONE);
//                        Toast.makeText(context, "onPostExcute", Toast.LENGTH_SHORT).show();
                    if(response!=null ){
                        Log.d(TAG, response.toString());

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

                    adapter.setDataSource(resultList);
                    adapter.notifyDataSetChanged();
//                        Toast.makeText(context, "!!!", Toast.LENGTH_SHORT).show();
                    task = null;


                }
            };
        task.execute();
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
    }

    @Override
    public void updateStop(final JSONArray array) {
        listViewInfo.post(new Runnable() {
            @Override
            public void run() {

                if(array.length()>0)
                    adapterStop.setDataSource(array);
            }
        });//, 200L);

    }


}

package kr.flit.busstop;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class StopSelectActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<BusStop> list ;
    private Location lastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_select);
        list = BusStopApplication.getApp().getBusStop();
        Collections.sort(list);
        this.lastLocation = BusStopApplication.getApp().getLastLocation();
        findViewById(R.id.noResults).setVisibility(list.size() == 0 ? View.VISIBLE : View.GONE);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false ));

        recyclerView.setAdapter(new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                View layout = getLayoutInflater().inflate(R.layout.listitem_stop_search, parent, false);
                StopViewHolder h = new StopViewHolder(layout);
                layout.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v) {
                        Object tag = v.findViewById(R.id.textView1).getTag();
                        if(tag!=null){
                            BusStop stop = (BusStop) tag;
                            Intent data = new Intent();
                            data.putExtra("busstop", stop);
                            setResult(RESULT_OK, data);
                            finish();
                        }

                    }
                });
                return h;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                BusStop stop = list.get(position);
                StopViewHolder h = (StopViewHolder) holder;
                h.name.setTag(stop);
                h.icon.setVisibility(stop.isBeacon() ? View.VISIBLE : View.GONE);
                h.name.setText(stop.getName());
                h.arsId.setText(String.valueOf(stop.getArsId()));
//                String.format("%2.2fm", item.optDouble("distance"))

                double distance = stop.getDistance();

                if(distance>0)
                    h.distance.setText(String.format("%2.2fm", distance));
                else
                    h.distance.setText("N/A");


            }

            @Override
            public int getItemCount() {
                System.out.println("item count : " +list.size());
                return list.size();
            }
        });
    }

    static class StopViewHolder extends RecyclerView.ViewHolder{
        ImageView icon;
        TextView name;
        TextView arsId;
        TextView distance;


        public StopViewHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(R.id.icon);
            name = (TextView) itemView.findViewById(R.id.textView1);
            arsId = (TextView) itemView.findViewById(R.id.textView2);
            distance = (TextView) itemView.findViewById(R.id.textView3);
        }

    }

    public void onClickDismiss(View v){
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }
}

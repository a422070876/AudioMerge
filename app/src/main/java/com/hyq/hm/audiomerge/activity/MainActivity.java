package com.hyq.hm.audiomerge.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hyq.hm.audiomerge.R;
import com.hyq.hm.audiomerge.audio.AudioHolder;
import com.hyq.hm.audiomerge.mediacodec.AudioEncoder;
import com.hyq.hm.audiomerge.mediacodec.AudioPlayer;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private List<AudioHolder> list = new ArrayList<>();
    private ListAdapter adapter;
    private AudioEncoder audioEncoder = new AudioEncoder();

    private AudioPlayer audioPlayer = new AudioPlayer();
    private int playerIndex = -1;

    private View loadingView;
    private TextView loadingTitleView;
    private ProgressBar loadingProgressBar;
    private TextView loadingTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadingView = findViewById(R.id.loading_view);
        loadingTitleView = findViewById(R.id.loading_title);
        loadingProgressBar = findViewById(R.id.loading_progress_bar);
        loadingTextView = findViewById(R.id.loading_text);

        ListView listView = findViewById(R.id.list_view);

        adapter = new ListAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(position == list.size()){
                    Intent intent = new Intent(MainActivity.this,AudioListActivity.class);
                    startActivityForResult(intent,10001);
                }else{

                }
            }
        });

        audioEncoder.setEncoderListener(new AudioEncoder.OnAudioEncoderListener() {
            @Override
            public void onDecoder(final AudioHolder decoderHolder, final int progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingTitleView.setText(decoderHolder.getName()+"处理中");
                        loadingProgressBar.setProgress(progress);
                        loadingTextView.setText(progress+"%");
                    }
                });
            }

            @Override
            public void onEncoder(final int progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingTitleView.setText("合成中");
                        loadingProgressBar.setProgress(progress);
                        loadingTextView.setText(progress+"%");
                    }
                });
            }

            @Override
            public void onOver(String path) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadingView.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });
    }
    private class ListAdapter extends BaseAdapter{
        private Context context;
        private ListAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getCount() {
            return list.size()+1;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if(position == list.size()){
                return 1;
            }
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if(position == list.size()){
                if(convertView == null){
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_add, parent,
                            false);
                }
            }else{

                ViewHolder viewHolder;

                if(convertView == null){
                    convertView = LayoutInflater.from(context).inflate(R.layout.item_audio, parent,
                            false);
                    viewHolder = new ViewHolder();
                    viewHolder.name = convertView.findViewById(R.id.name_text_view);
                    viewHolder.start = convertView.findViewById(R.id.start_text_view);
                    viewHolder.end = convertView.findViewById(R.id.end_text_view);
                    viewHolder.play = convertView.findViewById(R.id.play_text_view);
                    convertView.setTag(viewHolder);
                }else{
                    viewHolder = (ViewHolder) convertView.getTag();
                }

                AudioHolder audioHolder = list.get(position);
                viewHolder.name.setText(audioHolder.getName());
                viewHolder.start.setText(audioHolder.getStart()+"");
                viewHolder.end.setText(String.format("%.2f",audioHolder.getEnd()));
                viewHolder.play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playerIndex = position;
                    }
                });
            }
            return convertView;
        }
        private class ViewHolder{
            TextView name;
            TextView start;
            TextView end;
            TextView play;
        }
    }

    private void isPlayer(){
        loadingView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isResume){
                    if(playerIndex != -1){
                        if(audioPlayer.isStop()){
                            audioPlayer.start(list.get(playerIndex));
                            playerIndex = -1;
                        }else{
                            audioPlayer.stop();
                        }
                    }
                    isPlayer();
                }
            }
        },100);
    }
    private boolean isResume = false;
    @Override
    protected void onResume() {
        super.onResume();
        isResume = true;
        isPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResume = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == 10001){
                AudioHolder audioHolder = new AudioHolder();
                audioHolder.setFile(data.getStringExtra("path"));
                audioHolder.setName(data.getStringExtra("name"));
                audioHolder.setBitRate(data.getIntExtra("bitRate",0));
                audioHolder.setSampleRate(data.getIntExtra("sampleRate",0));
                audioHolder.setChannelCount(data.getIntExtra("channelCount",0));
                audioHolder.setStart(data.getDoubleExtra("startTime",0));
                audioHolder.setEnd(data.getDoubleExtra("endTime",0));
                list.add(audioHolder);
                adapter.notifyDataSetChanged();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onConfirm(View view){
        if(list.size() == 0){
            return;
        }
        loadingView.setVisibility(View.VISIBLE);
        audioEncoder.start(Environment.getExternalStorageDirectory().getAbsolutePath()+"/HMSDK/test_merge.mp3",list);
    }
    public void onNot(View view){

    }
}

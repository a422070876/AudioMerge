package com.hyq.hm.audiomerge.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.hyq.hm.audiomerge.R;
import com.hyq.hm.audiomerge.audio.AudioHolder;
import com.ringdroid.RingdroidEditActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 音频选择Activity
 * Created by 海米 on 2018/10/16.
 */

public class AudioListActivity extends AppCompatActivity {


    private String[] denied;
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};


    private List<String> audioFiles = new ArrayList<>();
    private List<String> audioNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_audio);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> list = new ArrayList<>();
            for (int i = 0; i < permissions.length; i++) {
                if (PermissionChecker.checkSelfPermission(this, permissions[i]) == PackageManager.PERMISSION_DENIED) {
                    list.add(permissions[i]);
                }
            }
            if (list.size() != 0) {
                denied = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    denied[i] = list.get(i);
                    ActivityCompat.requestPermissions(this, denied, 5);
                }

            } else {
                init();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 5) {
            boolean isDenied = false;
            for (int i = 0; i < denied.length; i++) {
                String permission = denied[i];
                for (int j = 0; j < permissions.length; j++) {
                    if (permissions[j].equals(permission)) {
                        if (grantResults[j] != PackageManager.PERMISSION_GRANTED) {
                            isDenied = true;
                            break;
                        }
                    }
                }
            }
            if (isDenied) {
                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
            } else {
                init();

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void init(){
        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                ContentResolver cr = getContentResolver();
                Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

                if(cursor != null){
                    if(cursor.moveToFirst()){
                        while (!cursor.isAfterLast()){
                            String fileName = cursor
                                    .getString(cursor
                                            .getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                            String path = cursor.getString(cursor
                                    .getColumnIndex(MediaStore.Audio.Media.DATA));
                            audioNames.add(fileName);
                            audioFiles.add(path);
                            cursor.moveToNext();
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        RecyclerView recyclerView = findViewById(R.id.recycler_view);
                        GridLayoutManager manager = new GridLayoutManager(AudioListActivity.this,4);
                        recyclerView.setLayoutManager(manager);
                        PathAdapter adapter = new PathAdapter(AudioListActivity.this);
                        recyclerView.setAdapter(adapter);
                    }
                });
            }
        };
        thread.start();


    }
    class PathAdapter extends RecyclerView.Adapter<PathAdapter.ViewHolder>{
        private Context context;
        public PathAdapter(Context context){
            this.context = context;
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


            return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_list_audio, parent,
                    false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.position = position;
            holder.textView.setText(audioNames.get(position));
        }

        @Override
        public int getItemCount() {
            return audioNames.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView textView;
            int position;
            public ViewHolder(View view)
            {
                super(view);
                textView = view.findViewById(R.id.name_text_view);
                textView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(AudioListActivity.this,RingdroidEditActivity.class);
                        intent.putExtra("path",audioFiles.get(position));
                        intent.putExtra("name",audioNames.get(position));
                        startActivityForResult(intent,10001);
                    }
                });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == 10001){
                setResult(RESULT_OK,data);
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
}

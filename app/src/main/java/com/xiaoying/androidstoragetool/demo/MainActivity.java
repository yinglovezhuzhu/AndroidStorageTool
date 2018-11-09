package com.xiaoying.androidstoragetool.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.xiaoying.androidstoragetool.R;
import com.xiaoying.storagetool.StorageTool;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Android-File-Util";

    private static final int RC_CHOOSE_FILE = 0x1000;
    private static final int RC_REQUEST_PERMISSION = 0x1001;

    private static final int TYPE_FILE = 1;
    private static final int TYPE_IMAGE = 2;
    private static final int TYPE_VIDEO = 3;
    private static final int TYPE_AUDIO = 4;

    private TextView mTvMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvMsg = findViewById(R.id.tv_msg);
//        mTvMsg.setMovementMethod(ScrollingMovementMethod.getInstance());

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityCompat.requestPermissions(MainActivity.this, new String [] {Manifest.permission.READ_EXTERNAL_STORAGE, }, RC_REQUEST_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(RC_REQUEST_PERMISSION == requestCode) {
            for(int result : grantResults) {
                if(PackageManager.PERMISSION_DENIED == result) {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_CHOOSE_FILE:
                if(RESULT_OK == resultCode) {
                    if(null == data) {
                        mTvMsg.setText(R.string.data_error);
                        return;
                    }
                    Uri uri = data.getData();
                    if(null == uri) {
                        mTvMsg.setText(R.string.data_error);
                        return;
                    }
                    mTvMsg.setText(String.format("Uri: %s\n\n", uri.toString()));
                    final String path = StorageTool.parseUriToPath(MainActivity.this, uri);
                    mTvMsg.append(String.format("Path: %s\n\n", path));

                    mTvMsg.append(String.format("File exist: %b\n\n", new File(path).exists()));
                    mTvMsg.append(String.format("File readable: %b\n\n", new File(path).canRead()));
                } else {
                    mTvMsg.setText(R.string.user_canceled);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_choose_file:
                chooseFile(TYPE_FILE);
                break;
            case R.id.btn_choose_image:
                chooseFile(TYPE_IMAGE);
                break;
            case R.id.btn_choose_video:
                chooseFile(TYPE_VIDEO);
                break;
            case R.id.btn_choose_audio:
                chooseFile(TYPE_AUDIO);
                break;
            default:
                break;
        }
    }

    private void chooseFile(int type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        switch (type) {
            case TYPE_FILE:
                intent.setType("*/*");
                break;
            case TYPE_IMAGE:
                intent.setType("image/*");
                break;
            case TYPE_VIDEO:
                intent.setType("video/*");
                break;
            case TYPE_AUDIO:
                intent.setType("audio/*");
                break;
            default:
                break;
        }
        try {
            startActivityForResult(intent, RC_CHOOSE_FILE);
        } catch (Exception e) {
            mTvMsg.setText(R.string.pick_error);
        }
    }
}

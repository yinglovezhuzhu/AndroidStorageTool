package com.xiaoying.androidstoragetool.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.xiaoying.androidstoragetool.R;
import com.xiaoying.storagetool.StorageTool;
import com.xiaoying.storagetool.VolumeInfo;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "Android-File-Util";

    private static final int RC_CHOOSE_FILE = 0x1000;

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
        mTvMsg.setMovementMethod(ScrollingMovementMethod.getInstance());

        showStorageInfo();
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
                    final String path = StorageTool.getInstance().parseUriToPath(MainActivity.this, uri);
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
            case R.id.btn_show_storage_info:
                showStorageInfo();
                break;
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

    private void showStorageInfo() {
        if(null == mTvMsg) {
            return;
        }
        StorageTool.getInstance().updateStorageInfo(MainActivity.this, true);

        List<VolumeInfo> volumeInfos = StorageTool.getInstance().getVolumes();
        int i = 0;
        for(VolumeInfo volumeInfo : volumeInfos) {
//            if(!volumeInfo.isVisible()) {
//                continue;
//            }
            mTvMsg.append(String.format("Volume: %d\n", i));
            mTvMsg.append(String.format("id: %s\n", volumeInfo.getId()));
            mTvMsg.append(String.format("type: %s\n", getVolumeTypeDesc(volumeInfo.getType())));
            mTvMsg.append(String.format("state: %s\n", getVolumeMountStateDesc(volumeInfo.getState())));
            mTvMsg.append(String.format("fs uuid: %s\n", volumeInfo.getFsUuid()));
            mTvMsg.append(String.format("path: %s\n", volumeInfo.getPath()));
            mTvMsg.append("=====================\\\n");
            i++;
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



    public String getVolumeTypeDesc(int type) {
        switch (type) {
            case VolumeInfo.TYPE_PRIVATE:
                return "TYPE_PRIVATE";
            case VolumeInfo.TYPE_PUBLIC:
                return "TYPE_PUBLIC";
            case VolumeInfo.TYPE_EMULATED:
                return "TYPE_EMULATED";
            case VolumeInfo.TYPE_ASEC:
                return "TYPE_ASEC";
            case VolumeInfo.TYPE_OBB:
                return "TYPE_OBB";
            default:
                return "UNKNOWN";
        }
    }

    public String getVolumeMountStateDesc(int state) {
        switch (state) {
            case VolumeInfo.STATE_UNMOUNTED:
                return "STATE_UNMOUNTED";
            case VolumeInfo.STATE_CHECKING:
                return "STATE_CHECKING";
            case VolumeInfo.STATE_MOUNTED:
                return "STATE_MOUNTED";
            case VolumeInfo.STATE_MOUNTED_READ_ONLY:
                return "STATE_MOUNTED_READ_ONLY";
            case VolumeInfo.STATE_FORMATTING:
                return "STATE_FORMATTING";
            case VolumeInfo.STATE_EJECTING:
                return "STATE_EJECTING";
            case VolumeInfo.STATE_UNMOUNTABLE:
                return "STATE_UNMOUNTABLE";
            case VolumeInfo.STATE_REMOVED:
                return "STATE_REMOVED";
            case VolumeInfo.STATE_BAD_REMOVAL:
                return "STATE_BAD_REMOVAL";
            default:
                return "UNKNOWN";
        }
    }

    public String getVolumeMountFlagDesc(int flag) {
        switch (flag) {
            case VolumeInfo.MOUNT_FLAG_PRIMARY:
                return "MOUNT_FLAG_PRIMARY";
            case VolumeInfo.MOUNT_FLAG_VISIBLE:
                return "MOUNT_FLAG_VISIBLE";
            default:
                return "UNKNOWN";
        }
    }
}

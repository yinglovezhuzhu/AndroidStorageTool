package com.xiaoying.storagetool;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <br/>Author：yunying.zhang
 * <br/>Email: yinglovezhuzhu@gmail.com
 * <br/>Date: 2018/6/15
 */
public class StorageTool {

    private static final String TAG = "StorageTool";

    private static final List<VolumeInfo> mVolumes = new ArrayList<>();
    private boolean mUpdated = false;

    private static StorageTool mInstance = null;

    private StorageTool() {

    }

    /**
     * Get Single instance
     * @return StorageTool instance
     */
    public static StorageTool getInstance() {
        synchronized (StorageTool.class) {
            if(null == mInstance) {
                mInstance = new StorageTool();
            }
            return mInstance;
        }
    }

    /**
     * Update system volume info
     * @param context Context instance.
     * @param force Force to update.
     */
    public void updateStorageInfo(Context context, boolean force) {
        if(mUpdated && !force) {
            return;
        }
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        if(null == sm) {
            Log.e(TAG, "Update storage info failed: Get StorageManager error!");
            return;
        }
        try {
            @SuppressWarnings("JavaReflectionMemberAccess")
            Method getMethod = StorageManager.class.getMethod("getVolumes", null);
            @SuppressWarnings("ConfusingArgumentToVarargsMethod")
            List<?> volumes = (List<?>) getMethod.invoke(sm, null);
            VolumeInfo volumeInfo;
            synchronized (mVolumes) {
                mVolumes.clear();
                for(Object volume : volumes) {
                    volumeInfo = parseVolumeInfoData(volume);
                    if(null != volumeInfo) {
                        mVolumes.add(volumeInfo);
                    }
                }
            }
            mUpdated = true;
        } catch (Exception e) {
            Log.e(TAG, "getPrimaryStoragePath() failed", e);
        }
    }

    /**
     * Get all volume.
     * @return volume info array list.
     */
    public List<VolumeInfo> getVolumes() {
        return new ArrayList<>(mVolumes);
    }

    /**
     * Get primary volume info.<br/>
     * If you only need get primary volume path, you can use {@linkplain Environment#getExternalStorageDirectory()}
     * @return primary volume info.
     */
    public VolumeInfo getPrimaryVolume() {
        if(!mUpdated) {
            return null;
        }
        synchronized (mVolumes) {
            for(VolumeInfo volumeInfo : mVolumes) {
                if(volumeInfo.isPrimary()) {
                    return volumeInfo;
                }
            }
            return null;
        }
    }

    /**
     * Get volume info by fs uuid.<br/>
     * @return volume info with the fs uuid, return null if not found.
     */
    public VolumeInfo getVolumeByFsUuid(String fsUuid) {
        if(!mUpdated || TextUtils.isEmpty(fsUuid)) {
            return null;
        }
        synchronized (mVolumes) {
            for(VolumeInfo volumeInfo : mVolumes) {
                if(fsUuid.equals(volumeInfo.getFsUuid())) {
                    return volumeInfo;
                }
            }
            return null;
        }
    }

    /**
     * Parse a content uri to a file path.<br/>
     * <li>Some file manager return Uri like "file:///sdcard/test.mp4",
     * In this case Uri.getPath() get the file path in file system,
     * so can create a file object with this path, if this file is exists,
     * means parse file success.<br/>
     * </li>
     * <li>Some file manager such as Gallery, return Uri like "content://video/8323",
     * In this case Uri.getPath() can't get file path in file system,
     * but can use ContentResolver to get file path from media database.<br/>
     * </li>
     * <li>
     * In Android os version kitkat(4.4) and higher, use {@linkplain android.content.Intent#ACTION_GET_CONTENT}
     * maybe return document type uri, you can't use either of above methods. In this case, you need to
     * parse document id from the uri and then  use ContentResolver to get file path from media database.
     * </li>
     * @param uri
     * @return
     */
    public String parseUriToPath(Context context, Uri uri) {
        if(uri == null) {
            return null;
        }

        String scheme = uri.getScheme();
        switch (scheme) {
            case "file":
                return uri.getPath();
            case "content":
                if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    return getMediaDataColumn(context, uri, null, null);
                } else {
                    if(DocumentsContract.isDocumentUri(context, uri)) {
                        final String docId = DocumentsContract.getDocumentId(uri);
                        if(isExternalStorageDocument(uri)) {
                            final String [] idSplit = docId.split(":"); // split array contains two data, array[0] is storage type, array[1] is relative path.
                            final String type = idSplit[0];
                            final String relativePath = idSplit[1];
                            if("primary".equals(type)) {
                                // primary storage.
                                return new File(Environment.getExternalStorageDirectory(), relativePath).getPath();
                            } else {
                                // not primary storage, array[0] always is storage fs uuid.
                                updateStorageInfo(context, true);
                                VolumeInfo volumeInfo = getVolumeByFsUuid(type);
                                if(null == volumeInfo) {
                                    return null;
                                }
                                return new File(volumeInfo.getPath(), relativePath).getPath();
                            }
                        } else if(isDownloadsDocument(uri)) {
                            final String id = DocumentsContract.getDocumentId(uri);
                            final Uri contentUri = ContentUris.withAppendedId(
                                    Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                            return getMediaDataColumn(context, contentUri, null, null);
                        } else if(isMediaDocument(uri)) {
                            final String [] idSplit = docId.split(":"); // split array contains two data, array[0] is media type, array[1] is _id in database.
                            final String type = idSplit[0];
                            final String columnId = idSplit[1];

                            Uri contentUri = null;
                            if ("image".equals(type)) {
                                contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            } else if ("video".equals(type)) {
                                contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            } else if ("audio".equals(type)) {
                                contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            }

                            if(null == contentUri) {
                                return null;
                            }

                            final String selection = "_id=?";
                            final String[] selectionArgs = new String[] {
                                    columnId,
                            };
                            return getMediaDataColumn(context, contentUri, selection, selectionArgs);
                        }
                    } else {
                        return getMediaDataColumn(context, uri, null, null);
                    }
                }
            default:
                break;
        }
        return null;
    }

    /**
     * Parse volume info data from android.os.storage.VolumeInfo instance object， which is hide in android api.
     * @param obj android.os.storage.VolumeInfo instance object.
     * @return VolumeInfo instance
     */
    private VolumeInfo parseVolumeInfoData(Object obj) {
        if(null == obj) {
            return null;
        }
        //android hide class: android.os.storage.VolumeInfo
        //public final String id;
        //public final int type;
        //public final DiskInfo disk;
        //public final String partGuid;
        //public int mountFlags = 0;
        //public int mountUserId = -1;
        //public int state = STATE_UNMOUNTED;
        //public String fsType;
        //public String fsUuid;
        //public String fsLabel;
        //public String path;
        //public String internalPath;

        try {
//            Class<?> cls = Class.forName("android.os.storage.VolumeInfo"); // This way also can work
            Class<?> cls = obj.getClass();
            Field idField = cls.getField("id");
            idField.setAccessible(true);
            Field typeField = cls.getField("type");
            typeField.setAccessible(true);
            Field partGuidField = cls.getField("partGuid");
            partGuidField.setAccessible(true);
            Field mountFlagsField = cls.getField("mountFlags");
            mountFlagsField.setAccessible(true);
            Field stateField = cls.getField("state");
            stateField.setAccessible(true);
            Field fsTypeField = cls.getField("fsType");
            fsTypeField.setAccessible(true);
            Field fsUuidField = cls.getField("fsUuid");
            fsUuidField.setAccessible(true);
            Field fsLabelTypeField = cls.getField("fsLabel");
            fsLabelTypeField.setAccessible(true);
            Field pathTypeField = cls.getField("path");
            pathTypeField.setAccessible(true);

            VolumeInfo volumeInfo = new VolumeInfo(String.valueOf(idField.get(obj)),
                    typeField.getInt(obj), String.valueOf(partGuidField.get(obj)));

            volumeInfo.setMountFlags(mountFlagsField.getInt(obj));
            volumeInfo.setState(stateField.getInt(obj));
            volumeInfo.setFsType(String.valueOf(fsTypeField.get(obj)));
            volumeInfo.setFsUuid(String.valueOf(fsUuidField.get(obj)));
            volumeInfo.setFsLabel(String.valueOf(fsLabelTypeField.get(obj)));
            volumeInfo.setPath(String.valueOf(pathTypeField.get(obj)));

            return volumeInfo;
//        } catch (ClassNotFoundException e) {
//            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Whether uri is external storage document type.
     * @param uri uri
     * @return is external storage document uri type
     */
    private boolean isExternalStorageDocument(Uri uri) {
        return null != uri && "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * Whether uri is downloads document type.
     * @param uri uri
     * @return is downloads document uri type
     */
    private boolean isDownloadsDocument(Uri uri) {
        return null != uri && "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * Whether uri is media document type.
     * @param uri uri
     * @return is media document uri type
     */
    private boolean isMediaDocument(Uri uri) {
        return null != uri && "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * Get media column data from ContentResolver.
     * @param context Context
     * @param uri uri
     * @param selection Query condition,like "_id=?"
     * @param selectionArgs Query condition value, replace the "?" symbol
     * @return Return the data field value.
     */
    private String getMediaDataColumn(Context context, @NonNull Uri uri, String selection, String [] selectionArgs) {
        ContentResolver cr = context.getContentResolver();
        String [] projection = new String [] {MediaStore.MediaColumns.DATA, };
        String path = null;
        Cursor cursor = cr.query(uri, projection, selection, selectionArgs, null);
        if(cursor != null) {
            if(cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
                path = cursor.getString(index);
            }
            cursor.close();
        }
        return path;
    }


}

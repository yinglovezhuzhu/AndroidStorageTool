package com.xiaoying.storagetool;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;

import java.io.File;

/**
 * <br/>Author：yunying.zhang
 * <br/>Email: yinglovezhuzhu@gmail.com
 * <br/>Date: 2018/6/15
 */
public class StorageTool {

    public static String getUriString(final Uri uri) {
        return (uri == null ? null : uri.toString());
    }

    public static boolean isWebUri(final Uri uri) {
        return (uri != null)
                && ("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme())
                || "fbstaging".equalsIgnoreCase(uri.getScheme()));
    }

    public static boolean isContentUri(final Uri uri) {
        return (uri != null) && ("content".equalsIgnoreCase(uri.getScheme()));
    }

    public static boolean isFileUri(final Uri uri) {
        return (uri != null) && ("file".equalsIgnoreCase(uri.getScheme()));
    }

    /**
     * Parse a content uri to a file path(Only file:// uri and content:// uri supported).<br/>
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
    public static String parseUriToPath(Context context, Uri uri) {
        if(uri == null) {
            return null;
        }

        if(isFileUri(uri)) {
            return uri.getPath();
        } else if(isContentUri(uri)) {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                return getMediaDataColumn(context, uri, null, null);
            } else {
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    if (isExternalStorageDocument(uri)) {
                        final String[] idSplit = docId.split(":"); // split array contains two data, array[0] is storage type, array[1] is relative path.
                        final String type = idSplit[0];
                        final String relativePath = idSplit[1];
                        if ("primary".equals(type)) {
                            // primary storage.
                            return new File(Environment.getExternalStorageDirectory(), relativePath).getPath();
                        } else {
                            // not primary storage, array[0] always is storage fs uuid.
                            final File dir = new File(Environment.getExternalStorageDirectory().getParent(), type);
                            return new File(dir, relativePath).getPath();
                        }
                    } else if (isDownloadsDocument(uri)) {
                        final String id = DocumentsContract.getDocumentId(uri);
                        final Uri contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                        return getMediaDataColumn(context, contentUri, null, null);
                    } else if (isMediaDocument(uri)) {
                        final String[] idSplit = docId.split(":"); // split array contains two data, array[0] is media type, array[1] is _id in database.
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

                        if (null == contentUri) {
                            return null;
                        }

                        final String selection = "_id=?";
                        final String[] selectionArgs = new String[]{
                                columnId,
                        };
                        return getMediaDataColumn(context, contentUri, selection, selectionArgs);
                    }
                } else {
                    return getMediaDataColumn(context, uri, null, null);
                }
            }
        } else {
            throw new IllegalArgumentException("The Uri must be either a file:// or content:// Uri");
        }
        return null;
    }

    /**
     * Whether uri is external storage document type.
     * @param uri uri
     * @return is external storage document uri type
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return null != uri && "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * Whether uri is downloads document type.
     * @param uri uri
     * @return is downloads document uri type
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return null != uri && "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * Whether uri is media document type.
     * @param uri uri
     * @return is media document uri type
     */
    private static boolean isMediaDocument(Uri uri) {
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
    private static String getMediaDataColumn(Context context, @NonNull Uri uri, String selection, String [] selectionArgs) {
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

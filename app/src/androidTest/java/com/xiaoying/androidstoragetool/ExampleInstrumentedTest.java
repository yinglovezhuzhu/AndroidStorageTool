package com.xiaoying.androidstoragetool;

import android.content.Context;
import android.os.storage.StorageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {

    private static final String TAG = "Android-Test";

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.xiaoying.androidmediautil", appContext.getPackageName());
    }

    @Test
    public void StorageManagerTest() {
        Context context = InstrumentationRegistry.getTargetContext();
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
//            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", null);
//            String[] paths = (String[]) getVolumePathsMethod.invoke(sm, null);
//            // first element in paths[] is primary storage path
//            for(String path : paths) {
//                Log.e(TAG, path);
//            }
            Method getMethod = StorageManager.class.getMethod("getVolumes", null);
            List<?> disks = (List<?>) getMethod.invoke(sm, null);
            Log.e(TAG, "" + disks.size());
            for(Object obj : disks) {
                Log.e(TAG, "_-------------------------------------------------\n");
                Class cls = obj.getClass();
                Field [] fields = cls.getDeclaredFields();
                for(Field field : fields) {
                    field.setAccessible(true);
                    Log.e(TAG, field.getName() + " = " + field.get(obj));
                }
                Log.e(TAG, "_-------------------------------------------------\n\n");
            }
        } catch (Exception e) {
            Log.e(TAG, "getPrimaryStoragePath() failed", e);
        }
    }
}

package com.citypeople.project.camera_preview;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.citypeople.project.R;

import java.io.File;

/**
 * Created by Abdul Haq (it.haq.life) on 11-07-2017.
 */

public class MyCanvas extends Activity {

    private static final String TAG = MyCanvas.class.getSimpleName();
    public static final int PERM_RQST_CODE = 110;
    public File dir;
    public String defaultVideo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
}

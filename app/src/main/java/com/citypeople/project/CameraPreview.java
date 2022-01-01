package com.citypeople.project;

/**
 * Created by Abdul Haq (it.haq.life) on 1/5/2017.
 */


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.citypeople.project.camera.MyCanvas;
import com.citypeople.project.views.FriendActivity;
import com.citypeople.project.views.GroupActivity;
import com.citypeople.project.views.VideoSendActivity;
import com.citypeople.project.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraPreview extends MyCanvas {

    private SurfaceView preview=null;
    private SurfaceHolder previewHolder=null;
    private Camera camera=null;
    private Camera.Parameters params;
    private boolean inPreview=false;
    private boolean cameraConfigured=false;
    private boolean isRecording;
    private boolean isFlashOn;
    private MediaRecorder mediaRecorder;
    private static int currentCameraId = 1;
    private Bitmap rotatedBitmap;
    private RelativeLayout captureMedia;
    private FrameLayout editMedia;
    private CircleProgressBar customButton;
    private CircleProgressBar switchCameraBtn;
    private TextView currentLocation;
    private ImageView flashButton;
    private ImageView uploadButton;
    private ImageView imgFriend;
    private ImageView imgGroup;
    private ImageView imgDelete;
    private TextView uploadButtonTxt;
    private ImageView EditCaptureSwitchBtn;
    private LinearLayout editTextBody;
    private ImageView capturedImage;
    private VideoView videoView;
    int VideoSeconds = 1;
    int noti_id;
    private Location myCurrentLocation;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2;
    private LocationAddressResultReceiver addressResultReceiver;

    private static final int MIN_CLICK_DURATION = 600;
    private long startClickTime;
    private boolean longClickActive;
    private boolean recording, pause = false;
    private long elapsed;
    private long remaningSecs = 0;
    private long elapsedSecs = 0;
    private Timer timer;
    private int ww;
    private int hh;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GetPermission();
        captureMedia = (RelativeLayout) findViewById(R.id.camera_view);
        editMedia = (FrameLayout) findViewById(R.id.edit_media);
        customButton = (CircleProgressBar) findViewById(R.id.custom_progressBar);
        switchCameraBtn = (CircleProgressBar) findViewById(R.id.custom_progressBar_back);
        currentLocation = (TextView) findViewById(R.id.current_location);
        //flashButton = (ImageView) findViewById(R.id.img_flash_control);
        uploadButton = (ImageView) findViewById(R.id.upload_media);
        imgFriend = (ImageView) findViewById(R.id.img_friend);
        imgGroup = (ImageView) findViewById(R.id.img_group);
        imgDelete = (ImageView) findViewById(R.id.img_delete);
        uploadButtonTxt = (TextView) findViewById(R.id.upload_media_txt);
        uploadButtonTxt.setText("");
        editTextBody = (LinearLayout) findViewById(R.id.editTextLayout);
        //selectSticker  = (LinearLayout) findViewById(R.id.select_sticker);
        ImageView addText = (ImageView) findViewById(R.id.add_text);
        ImageView addSticker = (ImageView) findViewById(R.id.add_stickers);
        addressResultReceiver = new LocationAddressResultReceiver(new Handler());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                myCurrentLocation = locationResult.getLocations().get(0);
                getAddress();
            }
        };
        startLocationUpdates();
        isRecording = false;
        isFlashOn = false;



        EditCaptureSwitchBtn = (ImageView) findViewById(R.id.cancel_capture);
        capturedImage = (ImageView) findViewById(R.id.captured_image);
        videoView = (VideoView) findViewById(R.id.captured_video);

        preview=(SurfaceView)findViewById(R.id.preview);
        previewHolder=preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);



        noti_id = (int) ((new Date().getTime()/1000L)%Integer.MAX_VALUE);


        //setting dir and VideoFile value
        File sdCard = Environment.getExternalStorageDirectory();
        dir = new File(sdCard.getAbsolutePath() + "/Opendp");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        defaultVideo =  dir + "/defaultVideo.mp4.nomedia";
        File createDefault = new File(defaultVideo);
        if (!createDefault.isFile()) {
            try {
                FileWriter writeDefault = new FileWriter(createDefault);
                writeDefault.append("yy");
                writeDefault.close();
                writeDefault.flush();
            } catch (Exception ex) {
            }
        }

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //perform upload function here
            }
        });

        imgFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), FriendActivity.class);
                startActivity(i);
            }
        });

        imgGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), GroupActivity.class);
                startActivity(i);
            }
        });


        try{
            customButton.setOnTouchListener(new View.OnTouchListener() {

                private Timer timer = new Timer();
                private long LONG_PRESS_TIMEOUT = 1000;
                private boolean wasLong = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(getClass().getName(), "touch event: " + event.toString());

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        imgDelete.setVisibility(View.VISIBLE);
                        switchCameraBtn.setVisibility(View.GONE);
                        // touch & hold started
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                wasLong = true;
                                // touch & hold was long
                                Log.i("Click","touch & hold was long");
                                VideoCountDown.start();

                             /*   try {
                                    startRecording();
                                } catch (IOException e) {
                                    String message = e.getMessage();
                                    Log.i(null, "Problem " + message);
                                    mediaRecorder.release();
                                    e.printStackTrace();
                                }*/
                            }
                        }, LONG_PRESS_TIMEOUT);
                        return true;
                    }

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        // touch & hold stopped
                        timer.cancel();
                        if(!wasLong){
                            // touch & hold was short
                            Log.i("Click","touch & hold was short");
                            if (isFlashOn && currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                params = camera.getParameters();
                                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                camera.setParameters(params);

                                camera.autoFocus(new Camera.AutoFocusCallback() {

                                    @SuppressLint("ClickableViewAccessibility")
                                    @Override
                                    public void onAutoFocus(final boolean success, final Camera camera) {
                                        //takePicture();
                                        switchCamera();
                                        switchCameraBtn.setVisibility(View.VISIBLE);
                                        imgDelete.setVisibility(View.GONE);
                                    }
                                });

                            } else {
                               // takePicture();
                                switchCamera();
                                switchCameraBtn.setVisibility(View.VISIBLE);
                                imgDelete.setVisibility(View.GONE);

                            }
                        } else {
                        //    stopRecording();
                            VideoCountDown.cancel();
                            VideoSeconds = 1;
                            customButton.setProgressWithAnimation(0);
                            wasLong = false;
                            imgDelete.setVisibility(View.GONE);
                            switchCameraBtn.setVisibility(View.VISIBLE);

                        }
                        timer = new Timer();
                        return true;
                    }

                    return false;
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }

        try{
            switchCameraBtn.setOnTouchListener(new View.OnTouchListener() {

                private Timer timer = new Timer();
                private long LONG_PRESS_TIMEOUT = 1000;
                private boolean wasLong = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d(getClass().getName(), "touch event: " + event.toString());

                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        imgDelete.setVisibility(View.VISIBLE);
                        customButton.setVisibility(View.GONE);
                        // touch & hold started
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                wasLong = true;
                                // touch & hold was long
                                Log.i("Click","touch & hold was long");
                                VideoCountDown1.start();

                             /*   try {
                                    startRecording();
                                } catch (IOException e) {
                                    String message = e.getMessage();
                                    Log.i(null, "Problem " + message);
                                    mediaRecorder.release();
                                    e.printStackTrace();
                                }*/
                            }
                        }, LONG_PRESS_TIMEOUT);
                        return true;
                    }

                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        // touch & hold stopped
                        timer.cancel();
                        if(!wasLong){
                            // touch & hold was short
                            Log.i("Click","touch & hold was short");
                            if (isFlashOn && currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                params = camera.getParameters();
                                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                                camera.setParameters(params);

                                camera.autoFocus(new Camera.AutoFocusCallback() {

                                    @SuppressLint("ClickableViewAccessibility")
                                    @Override
                                    public void onAutoFocus(final boolean success, final Camera camera) {
                                        //takePicture();
                                        switchCamera1();
                                        customButton.setVisibility(View.VISIBLE);
                                        imgDelete.setVisibility(View.GONE);
                                    }
                                });

                            } else {
                                // takePicture();
                                switchCamera1();
                                customButton.setVisibility(View.VISIBLE);
                                imgDelete.setVisibility(View.GONE);

                            }
                        } else {
                            //    stopRecording();
                            VideoCountDown1.cancel();
                            VideoSeconds = 1;
                            switchCameraBtn.setProgressWithAnimation(0);
                            wasLong = false;
                            imgDelete.setVisibility(View.GONE);
                            customButton.setVisibility(View.VISIBLE);

                        }
                        timer = new Timer();
                        return true;
                    }

                    return false;
                }
            });
        }catch (Exception e){
            e.printStackTrace();
        }


        preview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    FocusCamera();
                }catch (Exception e){
                   e.printStackTrace();
                }

            }
        });


        EditCaptureSwitchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditCaptureSwitch();
            }
        });

        editTextBody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {showHideEditText();
            }
        });
        addText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {showHideEditText();
            }
        });
        addSticker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stickerOptions();
            }
        });
        editMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //StickerView.invalidate();
            }
        });
    }



    @SuppressLint("NewApi")
    public void GetPermission() {

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.READ_CONTACTS};
        if (!hasPermission(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
            finish();
        }
    }

    public static boolean hasPermission(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new
                            String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
        else {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(2000);
            locationRequest.setFastestInterval(1000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void getAddress() {
        if (!Geocoder.isPresent()) {
            Toast.makeText(CameraPreview.this, "Can't find current address, ",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, GetAddressIntentService.class);
        intent.putExtra("add_receiver", addressResultReceiver);
        intent.putExtra("add_location", myCurrentLocation);
        startService(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull
            int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
            else {
                Toast.makeText(this, "Location permission not granted, " + "restart the app if you want the feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    CountDownTimer VideoCountDown = new CountDownTimer(2000,200) {
        @Override
        public void onTick(long millisUntilFinished) {
            VideoSeconds++;
            int VideoSecondsPercentage = VideoSeconds * 5;
            customButton.setProgressWithAnimation(VideoSecondsPercentage);

        }

        @Override
        public void onFinish() {
         //   stopRecording();
            customButton.setProgress(0);
            VideoSeconds = 0;
            Intent i = new Intent(getApplicationContext(), VideoSendActivity.class);
            startActivity(i);
        }
    };

    CountDownTimer VideoCountDown1 = new CountDownTimer(2000,200) {
        @Override
        public void onTick(long millisUntilFinished) {
            VideoSeconds++;
            int VideoSecondsPercentage = VideoSeconds * 5;
            switchCameraBtn.setProgressWithAnimation(VideoSecondsPercentage);

        }

        @Override
        public void onFinish() {
            //   stopRecording();
            switchCameraBtn.setProgress(0);
            VideoSeconds = 0;
            Intent i = new Intent(getApplicationContext(), VideoSendActivity.class);
            startActivity(i);
        }
    };


    public void FocusCamera(){
        if (camera.getParameters().getFocusMode().equals(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
        } else {
            camera.autoFocus(new Camera.AutoFocusCallback() {

                @Override
                public void onAutoFocus(final boolean success, final Camera camera) {
                }
            });
        }
    }

  /*  private void takePicture() {
        if (camera==null){
            return;
        }
        params = camera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPictureSizes();

        List<Integer> list = new ArrayList<Integer>();
        for (Camera.Size size : params.getSupportedPictureSizes()) {
            Log.i("ASDF", "Supported Picture: " + size.width + "x" + size.height);
            list.add(size.height);
        }

        Camera.Size cs = sizes.get(closest(1080, list));
        Log.i("Width x Height", cs.width+"x"+cs.height);
        params.setPictureSize(cs.width, cs.height); //1920, 1080

        //params.setRotation(90);
        camera.setParameters(params);
        camera.takePicture(null, null, new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, final Camera camera) {
                Bitmap bitmap;
                Matrix matrix = new Matrix();

                //if (bitmap.getWidth() > bitmap.getHeight()) {
                if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    matrix.postRotate(90);
                } else {
                    Matrix matrixMirrory = new Matrix();
                    float[] mirrory = {-1, 0, 0, 0, 1, 0, 0, 0, 1};
                    matrixMirrory.setValues(mirrory);
                    matrix.postConcat(matrixMirrory);
                    matrix.postRotate(90);
                }
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                *//*} else {
                    rotatedBitmap = bitmap;
                }*//*

                if (rotatedBitmap != null) {
                    setStickerView(0);
                    capturedImage.setVisibility(View.VISIBLE);
                    capturedImage.setImageBitmap(rotatedBitmap);
                    editMedia.setVisibility(View.VISIBLE);
                    captureMedia.setVisibility(View.GONE);

                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(params);
                    Log.i("Image bitmap", rotatedBitmap.toString()+"-");
                } else {
                    Toast.makeText(CameraPreview.this, "Failed to Capture the picture. kindly Try Again:",
                            Toast.LENGTH_LONG).show();
                }

            }
        });
    }*/


   /* protected void startRecording() throws IOException {

        try {
            if (camera == null) {
                camera = Camera.open(currentCameraId);
                Log.i("Camera","Camera open");
            }

            params = camera.getParameters();
            if (isFlashOn && currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(params);
            }

            mediaRecorder = new MediaRecorder();
            camera.lock();
            camera.unlock();
            // Please maintain sequence of following code.
            // If you change sequence it will not work.
            mediaRecorder.setCamera(camera);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setPreviewDisplay(previewHolder.getSurface());

            if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mediaRecorder.setOrientationHint(270);
            } else {
                mediaRecorder.setOrientationHint(setCameraDisplayOrientation(this, currentCameraId, camera));
            }
            mediaRecorder.setVideoEncodingBitRate(3000000);
            mediaRecorder.setVideoFrameRate(30);

            List<Integer> list = new ArrayList<Integer>();

            List<Camera.Size> VidSizes = params.getSupportedVideoSizes();
            if (VidSizes == null) {
                Log.i("Size length", "is null");
                mediaRecorder.setVideoSize(640,480);
            } else {
                Log.i("Size length", "is NOT null");
                for (Camera.Size sizesx : params.getSupportedVideoSizes()) {
                    Log.i("ASDF", "Supported Video: " + sizesx.width + "x" + sizesx.height);
                    list.add(sizesx.height);
                }
                Camera.Size cs = VidSizes.get(closest(1080, list));
                Log.i("Width x Height", cs.width+"x"+cs.height);
                mediaRecorder.setVideoSize(cs.width,cs.height);
            }

            mediaRecorder.setOutputFile(defaultVideo);
            mediaRecorder.prepare();
            isRecording = true;
            mediaRecorder.start();
        }catch (Exception e ){
            e.printStackTrace();
        }

    }

    public void stopRecording() {
        if (isRecording) {
            try {
                params = camera.getParameters();
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(params);

                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                playVideo();
            } catch (RuntimeException stopException) {
                Log.i("Stop Recoding", "Too short video");
                //takePicture();
            }
            camera.lock();
        } else {
            Log.i("Stop Recoding", "isRecording is true");
        }
    }*/

    public void playVideo() {
        videoView.setVisibility(View.VISIBLE);
        editMedia.setVisibility(View.VISIBLE);
        captureMedia.setVisibility(View.GONE);

        Uri video = Uri.parse(defaultVideo);
        videoView.setVideoURI(video);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setLooping(true);
            }
        });
        videoView.start();
        preview.setVisibility(View.INVISIBLE);
        setStickerView(1);
    }

    public void saveMedia(View v) throws IOException {
        if (!videoView.isShown()) {
            Toast.makeText(this, "Saving...",Toast.LENGTH_SHORT).show();
            File sdCard = Environment.getExternalStorageDirectory();
            dir = new File(sdCard.getAbsolutePath() + "/Opendp");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("ddMMyyHHmm").format(new Date());
            String ImageFile = "opendp-" + timeStamp + ".jpg"; //".png";
            File file = new File(dir, ImageFile);

            try {
                FileOutputStream fos = new FileOutputStream(file);
                stickerView.createBitmap().compress(Bitmap.CompressFormat.PNG, 90, fos);
                refreshGallery(file);
                Toast.makeText(this, "Saved!",Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                Toast.makeText(this, "Error saving!",Toast.LENGTH_LONG).show();
                Log.d("", "File not found: " + e.getMessage());
            }
        } else {
            if (defaultVideo != null) {
                String timeStamp = new SimpleDateFormat("ddMMyyHHmm").format(new Date());
                String VideoFile = "opendp-" + timeStamp + ".mp4";

                File from = new File(defaultVideo);
                File to = new File(dir,VideoFile);

                InputStream in = new FileInputStream(from);
                OutputStream out = new FileOutputStream(to);

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();
                refreshGallery(to);
                Toast.makeText(this, "Saved!",Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Error saving!",Toast.LENGTH_LONG).show();
            }
        }
    }

    public void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    public void FlashControl(View v) {
        Log.i("Flash", "Flash button clicked!");
        boolean hasFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if (!hasFlash) {
            AlertDialog alert = new AlertDialog.Builder(CameraPreview.this)
                    .create();
            alert.setTitle("Error");
            alert.setMessage("Sorry, your device doesn't support flash light!");
            alert.setButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            alert.show();
            return;
        } else {

            if (!isFlashOn) {
                isFlashOn = true;
                flashButton.setImageResource(R.drawable.black_back_arrow);
                Log.i("Flash", "Flash On");

            } else {
                isFlashOn = false;
                flashButton.setImageResource(R.drawable.black_back_arrow);
                Log.i("Flash", "Flash Off");
            }
        }
    }

    public void switchCamera() {
        if (!isRecording) {
            if (camera.getNumberOfCameras() != 1) {
                camera.release();
             //   if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
               // } else {
              //      currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
             //   }
                camera = Camera.open(currentCameraId);
                try {
                    camera.setPreviewDisplay(previewHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startPreview();
            }
        } else {
            Log.i("Switch Camera","isRecording true");
        }
    }

    public void switchCamera1() {
        if (!isRecording) {
            if (camera.getNumberOfCameras() != 1) {
                camera.release();
            //    if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    currentCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            //    } else {
            //        currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
              //  }
                camera = Camera.open(currentCameraId);
                try {
                    camera.setPreviewDisplay(previewHolder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                startPreview();
            }
        } else {
            Log.i("Switch Camera","isRecording true");
        }
    }

    public void EditCaptureSwitch() {
        preview.setVisibility(View.VISIBLE);
        captureMedia.setVisibility(View.VISIBLE);
        //capturedImage.setImageResource(android.R.color.transparent);
        startPreview(); //onResume();
        capturedImage.setVisibility(View.GONE);
        editMedia.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (selectSticker.getVisibility() == View.VISIBLE) {
            stickerOptions();
        } else if(editTextBody.getVisibility() == View.VISIBLE) {
            showHideEditText();
        } else if (editMedia.getVisibility() == View.VISIBLE) {
            EditCaptureSwitch();
            removeAllStickers();
        } else {
            finish();
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();
        customButton.setProgress(0);
        switchCameraBtn.setProgress(0);
        VideoSeconds = 0;
        imgDelete.setVisibility(View.GONE);
        switchCameraBtn.setVisibility(View.VISIBLE);
        customButton.setVisibility(View.VISIBLE);
        currentCameraId=1;
        camera=Camera.open(currentCameraId);
        try {
            camera.setPreviewDisplay(previewHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        startPreview();
        startLocationUpdates();
        //FocusCamera();
    }

    @Override
    public void onPause() {
        if (inPreview) {
            camera.stopPreview();
        }

        camera.release();
        camera=null;
        inPreview=false;

        super.onPause();

        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    public int closest(int of, List<Integer> in) {
        int min = Integer.MAX_VALUE;
        int closest = of;
        int position=0;
        int i = 0;

        for (int v: in) {
            final int diff = Math.abs(v - of);
            i++;

            if(diff < min) {
                min = diff;
                closest = v;
                position = i;
            }
        }
        int rePos = position - 1;
        Log.i("Value",closest+"-"+rePos);
        return rePos;
    }

    private void initPreview(int width, int height) {
        ww=width;
        hh=height;
        if (camera!=null && previewHolder.getSurface()!=null) {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(previewHolder);
            }
            catch (Throwable t) {
                Log.e("Preview:surfaceCallback", "Exception in setPreviewDisplay()", t);
                Toast.makeText(CameraPreview.this, t.getMessage(), Toast.LENGTH_LONG).show();
            }

            if (!cameraConfigured) {

                Camera.Parameters parameters = camera.getParameters();
                List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

                if (sizes != null) {
                    Camera.Size size = getOptimalPreviewSize(sizes, width, height);
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    camera.startPreview();
                    cameraConfigured=true;
                }


            }
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }


    private void startPreview() {
        if (cameraConfigured && camera!=null) {
            camera.setDisplayOrientation(setCameraDisplayOrientation(this, currentCameraId, camera));
            camera.startPreview();
            inPreview=true;
        }
    }

    private int setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }

    private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size: parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;
                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return (result);
    }


    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }

        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width,
                                   int height) {


            initPreview(width,height);
            startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }

    };






    private class LocationAddressResultReceiver extends ResultReceiver {
        LocationAddressResultReceiver(Handler handler) {
            super(handler);
        }
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            if (resultCode == 0) {
                Log.d("Address", "Location null retrying");
                getAddress();
            }
            if (resultCode == 1) {
                Toast.makeText(CameraPreview.this, "Address not found, ", Toast.LENGTH_SHORT).show();
            }
            String currentAdd = resultData.getString("address_result");
            showResults(currentAdd);
        }
    }
    private void showResults(String currentAdd) {
        currentLocation.setText(currentAdd);
    }

}






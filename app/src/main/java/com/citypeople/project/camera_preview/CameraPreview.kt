package com.citypeople.project.camera_preview

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
/*import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor.start
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.Configuration*/
import com.citypeople.project.Constants
import com.citypeople.project.R
import com.citypeople.project.storyvideo.StoryVideoActivity
import com.citypeople.project.views.FriendActivity
import com.citypeople.project.views.GroupActivity
import com.citypeople.project.views.VideoSendActivity
import com.kaopiz.kprogresshud.KProgressHUD
import kotlinx.android.synthetic.main.activity_camera_preview.*
import java.io.*
import java.util.*
import kotlin.math.abs

class CameraPreview : MyCanvas(){

    val TAG = CameraPreview::class.java.simpleName
    
    private var previewHolder: SurfaceHolder? = null
    private var camera: Camera? = null
    private var params: Camera.Parameters? = null
    private var inPreview = false
    private var cameraConfigured = false
    private var isRecording = false
    private var isFlashOn = false
    private var mediaRecorder: MediaRecorder? = null
    private var currentCameraId = 0
    private var rotatedBitmap: Bitmap? = null
    var mProgressDialog: KProgressHUD? = null

    var VideoSeconds = 1
    var noti_id = 0
    var dir: File? = null
    var defaultVideo: String? = null

    //
    var isFront = true
    var stopSwitchCameraAnimation = true
    private var front_animation: Animator? = null
    private var back_animation: Animator? = null
    private var mVideoUri: Uri? = null

    var surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            // no-op -- wait until surfaceChanged()
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int, width: Int,
            height: Int
        ) {
            initPreview()
            startPreview()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // no-op
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)
        
        getPermission()

        isRecording = false
        isFlashOn = false

        previewHolder = preview.holder
        previewHolder?.addCallback(surfaceCallback)
        previewHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        noti_id = (Date().time / 1000L % Int.MAX_VALUE).toInt()

        //setting dir and VideoFile value

        //setting dir and VideoFile value
        val sdCard = Environment.getExternalStorageDirectory()
        dir = File(sdCard.absolutePath + "/Opendp")
        if (dir?.exists() == false) {
            dir?.mkdirs()
        }
        defaultVideo = dir.toString() + "/defaultVideo.mp4.nomedia"
        val createDefault = File(defaultVideo ?: "")
        if (!createDefault.isFile) {
            try {
                val writeDefault = FileWriter(createDefault)
                writeDefault.append("yy")
                writeDefault.close()
                writeDefault.flush()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        custom_progressBar.setOnTouchListener(object : View.OnTouchListener {
            private var timer = Timer()
            private val LONG_PRESS_TIMEOUT: Long = 1000
            private var wasLong = false
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                Log.d(javaClass.name, "touch event: $event")
                if (event.action == MotionEvent.ACTION_DOWN) {
                    // touch & hold started
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            wasLong = true
                            // touch & hold was long
                            Log.i("Click", "touch & hold was long")
                            videoCountDown.start()
                            try {
                                startRecording()
                            } catch (e: IOException) {
                                val message = e.message
                                Log.i(null, "Problem $message")
                                mediaRecorder!!.release()
                                e.printStackTrace()
                            }
                        }
                    }, LONG_PRESS_TIMEOUT)
                    return true
                }
                if (event.action == MotionEvent.ACTION_UP) {
                    // touch & hold stopped
                    timer.cancel()
                    if (!wasLong) {
                        // touch & hold was short
                        Log.i("Click", "touch & hold was short")
                        /*if (isFlashOn && currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            params = camera!!.parameters
                            params?.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                            camera?.parameters = params
                            camera?.autoFocus { success, camera -> takePicture() }
                        } else {
                            takePicture()
                        }*/
                    } else {
                        stopRecording(false)
                        videoCountDown.cancel()
                        VideoSeconds = 1
                        custom_progressBar.setProgressWithAnimation(0F)
                        wasLong = false
                    }
                    timer = Timer()
                    return true
                }
                return false
            }
        })
        preview.setOnClickListener { focusCamera() }

        btnBack.setOnClickListener { onBackPressed() }

        btnAddGroup.setOnClickListener { startActivity(Intent(this, GroupActivity::class.java)) }

        btnAddFriend.setOnClickListener { startActivity(Intent(this, FriendActivity::class.java)) }

        btnViewStories.setOnClickListener { startActivity(Intent(this, StoryVideoActivity::class.java)) }

        btnBack.setOnClickListener { stopRecording(deleteVideo = true) }

        edit_media.setOnClickListener { }

        front_animation = AnimatorInflater.loadAnimator(applicationContext, R.animator.front_anim)
        back_animation = AnimatorInflater.loadAnimator(applicationContext, R.animator.back_anim)
        img_switch_camera.setOnClickListener { _: View? -> flipUpwards() }

        //change hight width of camera preview
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        //mTextureView.setAspectRatio(display.getWidth(), (display.getHeight() - 60));
        val lp = blockView.layoutParams
        //lp.width = display.width
        lp.height = (display.height * 0.18).toInt()
        preview.requestLayout()
    }
    
    private fun flipUpwards() {
        switchCamera(false)
        front_animation?.setTarget(preview)
        front_animation?.start()
        Log.e(
            TAG,
            "flipUpwards: FRONT"
        )
    }

    var videoCountDown: CountDownTimer = object : CountDownTimer(10000, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            VideoSeconds++
            val videoSecondsPercentage = VideoSeconds * 10
            custom_progressBar.setProgressWithAnimation(videoSecondsPercentage.toFloat())
        }

        override fun onFinish() {
            stopRecording(deleteVideo = false)
            custom_progressBar.progress = 0F
            VideoSeconds = 0
        }
    }

    private fun focusCamera() {
        if (camera!!.parameters.focusMode ==
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        ) {
        } else {
            camera?.autoFocus { _: Boolean, _: Camera? -> }
        }
    }

    private fun takePicture() {
        params = camera!!.parameters
        val sizes = params?.supportedPictureSizes
        val list: MutableList<Int> = ArrayList()
        for (size in params?.supportedPictureSizes!!) {
            Log.i("ASDF", "Supported Picture: " + size.width + "x" + size.height)
            list.add(size.height)
        }
        val cs = sizes?.get(closest(1080, list))
        Log.i("Width x Height", cs?.width.toString() + "x" + cs?.height)
        params?.setPictureSize(cs?.width ?: 1024, cs?.height ?: 1024) //1920, 1080

        //params.setRotation(90);
        camera?.parameters = params
        camera?.takePicture(null, null, { data, camera ->
            val matrix = Matrix()

            //if (bitmap.getWidth() > bitmap.getHeight()) {
            if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                matrix.postRotate(90f)
            } else {
                val matrixMirrory = Matrix()
                val mirrory = floatArrayOf(-1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
                matrixMirrory.setValues(mirrory)
                matrix.postConcat(matrixMirrory)
                matrix.postRotate(90f)
            }
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            rotatedBitmap =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            if (rotatedBitmap != null) {
                captured_image.visibility = View.VISIBLE
                captured_image.setImageBitmap(rotatedBitmap)
                edit_media.visibility = View.VISIBLE
                camera_view.visibility = View.GONE
                params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
                camera.parameters = params
                Log.i("Image bitmap", rotatedBitmap.toString() + "-")
            } else {
                Toast.makeText(
                    this, "Failed to Capture the picture. kindly Try Again:",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    fun closest(of: Int, `in`: List<Int>): Int {
        var min = Int.MAX_VALUE
        var closest = of
        var position = 0
        var i = 0
        for (v in `in`) {
            val diff = abs(v - of)
            i++
            if (diff < min) {
                min = diff
                closest = v
                position = i
            }
        }
        val rePos = position - 1
        Log.i("Value", "$closest-$rePos")
        return rePos
    }

    @Throws(Exception::class)
    private fun startRecording() {
        prepareFile()
        if (camera == null) {
            camera =
                Camera.open(currentCameraId)
            Log.i("Camera", "Camera open")
        }
        params = camera?.parameters
        if (isFlashOn && currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            params?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            camera!!.parameters = params
        }
        mediaRecorder = MediaRecorder()
        camera!!.lock()
        camera!!.unlock()
        // Please maintain sequence of following code.
        // If you change sequence it will not work.
        mediaRecorder?.setCamera(camera)
        mediaRecorder?.setVideoSource(MediaRecorder.VideoSource.CAMERA)
        mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
        mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder?.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder?.setPreviewDisplay(previewHolder!!.surface)
        if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mediaRecorder?.setOrientationHint(270)
        } else {
            mediaRecorder?.setOrientationHint(
                setCameraDisplayOrientation(
                    this,
                    currentCameraId,
                    camera ?: return
                )
            )
        }
        mediaRecorder?.setVideoEncodingBitRate(3000000)
        mediaRecorder?.setVideoFrameRate(30)
        val list: MutableList<Int> = ArrayList()
        val vidSizes = params?.supportedVideoSizes
        if (vidSizes == null) {
            Log.i("Size length", "is null")
            mediaRecorder?.setVideoSize(640, 480)
        } else {
            Log.i("Size length", "is NOT null")
            for (sizesx in params?.supportedVideoSizes ?: arrayListOf()) {
                Log.i("ASDF", "Supported Video: " + sizesx.width + "x" + sizesx.height)
                list.add(sizesx.height)
            }
            val cs = vidSizes[closest(1080, list)]
            Log.i("Width x Height", cs.width.toString() + "x" + cs.height)
            mediaRecorder?.setVideoSize(cs.width, cs.height)
        }
        mVideoUri = Uri.parse(defaultVideo)
        mediaRecorder?.setOutputFile(defaultVideo)
        mediaRecorder?.prepare()
        isRecording = true
        mediaRecorder?.start()

    }

    private fun prepareFile() {
        try {
            val file = File(
                getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "snap_" + Calendar.getInstance().timeInMillis + ".mp4"
            )
            if (!file.exists()) file.createNewFile()
            defaultVideo = file.absolutePath
            //OutputStream outputStream = contentResolver.openOutputStream(uri);
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording(deleteVideo: Boolean) {
        if (isRecording) {
            try {
                params = camera!!.parameters
                params?.flashMode = Camera.Parameters.FLASH_MODE_OFF
                camera?.parameters = params
                mediaRecorder?.stop()
                mediaRecorder?.reset()
                mediaRecorder?.release()
                btnDelete.visibility = View.GONE
                mediaRecorder = null
                isRecording = false
                if (deleteVideo){
                    defaultVideo = ""
                    return
                }
                val uri = defaultVideo
                val i =
                    Intent(this, VideoSendActivity::class.java).putExtra(Constants.INTENT_DATA, uri)
                startActivity(i)
                //finish()
                //compress video
                //startVideoCompress(defaultVideo)
            } catch (stopException: RuntimeException) {
                Log.i("Stop Recoding", "Too short video")
                //takePicture()
            }
            camera!!.lock()
        } else {
            Log.i("Stop Recoding", "isRecording is true")
        }
    }

    @SuppressLint("NewApi")
    private fun startVideoCompress(videoPath: String) {
        Log.w("startVideoCompress", videoPath)
        val output: File =
            File(externalCacheDir, "vid_\${System.currentTimeMillis()}.mp4")

//        start(
//            applicationContext,  // => This is required if srcUri is provided. If not, pass null.
//            //   uri, // => Source can be provided as content uri, it requires context.
//            //   path, // => This could be null if srcUri and context are provided.
//            //  desFile.path,
//            //streamableFile.path, /*String, or null*/
//            Uri.parse(videoPath),
//            videoPath,
//            output.absolutePath,
//            videoPath,
//            object : CompressionListener {
//                override fun onProgress(v: Float) {
//                    // Update UI with progress value
//                    runOnUiThread(Runnable {
//                        showProgress()
//                        Log.e("Compress file path:", v.toString())
//                    })
//                }
//
//                override fun onStart() {
//                    showProgress()
//                }
//
//                override fun onSuccess() {
//                    hideProgress()
//                    val uri: String = mVideoUri.toString()
//                    val i = Intent(
//                        this@CameraPreview,
//                        VideoSendActivity::class.java
//                    ).putExtra(Constants.INTENT_DATA, uri)
//                    startActivity(i)
//                }
//
//                override fun onFailure(failureMessage: String) {
//                    hideProgress();
//                    Toast.makeText(
//                        this@CameraPreview,
//                        failureMessage,
//                        Toast.LENGTH_SHORT
//                    ).show()
//                    Log.e("Compress file path:", output.absolutePath)
//                    // On Failure
//                }
//
//                override fun onCancelled() {
//                    hideProgress();
//                    Log.e("Compress file path:", output.toString())
//                }
//            }, Configuration(VideoQuality.MEDIUM, 24, false, null)
//        )
    }

    private fun showProgress() {
        if (mProgressDialog == null) {
            mProgressDialog = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please wait...")
                .setCancellable(false)
                .setAnimationSpeed(3)
                .setDimAmount(0.5f)
                .show()
        } else mProgressDialog?.show()
    }

    private fun hideProgress() {
        if (mProgressDialog != null && mProgressDialog?.isShowing == true) {
            mProgressDialog?.dismiss()
        }
    }

    private fun playVideo() {
        captured_video.visibility = View.VISIBLE
        edit_media.visibility = View.VISIBLE
        camera_view.visibility = View.GONE
        val video = Uri.parse(defaultVideo)
        captured_video.setVideoURI(video)
        captured_video.setOnPreparedListener { mp: MediaPlayer ->
            mp.isLooping = true
        }
        captured_video.start()
        preview.visibility = View.INVISIBLE
    }

    @Throws(IOException::class)
    fun saveMedia(v: View?) {
        if (!captured_video.isShown) {
            savePhoto()
            Toast.makeText(this, "Saving...", Toast.LENGTH_SHORT).show()
        } else {
            if (defaultVideo != null) {
                saveVideo()
                Toast.makeText(this, "Saved!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error saving!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun savePhoto() {
        val uriSavedImage: Uri?
        var createdImage: File? = null
        val resolver = contentResolver
        val imageFileName = "image_" + System.currentTimeMillis() + ".jpg"
        val contentValues: ContentValues = ContentValues()
        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Pictures/" + "Folder")
            contentValues.put(MediaStore.Video.Media.TITLE, imageFileName)
            contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, imageFileName)
            contentValues.put(MediaStore.Video.Media.MIME_TYPE, "image/jpg")
            contentValues.put(
                MediaStore.Video.Media.DATE_ADDED,
                System.currentTimeMillis() / 1000
            )
            val collection =
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            uriSavedImage = resolver.insert(collection, contentValues)
        } else {
            val directory = (Environment.getExternalStorageDirectory().absolutePath
                    + File.separator + Environment.DIRECTORY_PICTURES + "/" + "YourFolder")
            createdImage = File(directory, imageFileName)
            contentValues.put(MediaStore.Video.Media.TITLE, imageFileName)
            contentValues.put(MediaStore.Video.Media.DISPLAY_NAME, imageFileName)
            contentValues.put(MediaStore.Video.Media.MIME_TYPE, "image/jpg")
            contentValues.put(
                MediaStore.Video.Media.DATE_ADDED,
                System.currentTimeMillis() / 1000
            )
            contentValues.put(MediaStore.Images.Media.DATA, createdImage.absolutePath)
            uriSavedImage = contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }
        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val pfd: ParcelFileDescriptor?
        try {
            pfd = contentResolver.openFileDescriptor(uriSavedImage!!, "w")
            val out = FileOutputStream(pfd!!.fileDescriptor)
            // get the already saved video as fileinputstream
            // The Directory where your file is saved
            val storageDir = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Folder"
            )
            //Directory and the name of your video file to copy
            //stickerView.createBitmap().compress(Bitmap.CompressFormat.PNG, 90, out)
            //refreshGallery(s);
            out.close()
            pfd.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            contentResolver.update(uriSavedImage!!, contentValues, null, null)
        }
    }

    private fun saveVideo() {
        val uriSavedVideo: Uri?
        var createdVideo: File? = null
        val resolver = contentResolver
        val videoFileName = "video_" + System.currentTimeMillis() + ".mp4"
        val valuesvideos: ContentValues = ContentValues()
        if (Build.VERSION.SDK_INT >= 29) {
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder")
            valuesvideos.put(MediaStore.Video.Media.TITLE, videoFileName)
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            valuesvideos.put(
                MediaStore.Video.Media.DATE_ADDED,
                System.currentTimeMillis() / 1000
            )
            val collection =
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            uriSavedVideo = resolver.insert(collection, valuesvideos)
        } else {
            val directory = (Environment.getExternalStorageDirectory().absolutePath
                    + File.separator + Environment.DIRECTORY_MOVIES + "/" + "YourFolder")
            createdVideo = File(directory, videoFileName)
            valuesvideos.put(MediaStore.Video.Media.TITLE, videoFileName)
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, videoFileName)
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            valuesvideos.put(
                MediaStore.Video.Media.DATE_ADDED,
                System.currentTimeMillis() / 1000
            )
            valuesvideos.put(MediaStore.Video.Media.DATA, createdVideo.absolutePath)
            uriSavedVideo = contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                valuesvideos
            )
        }
        if (Build.VERSION.SDK_INT >= 29) {
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            valuesvideos.put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val pfd: ParcelFileDescriptor?
        try {
            pfd = contentResolver.openFileDescriptor(uriSavedVideo!!, "w")
            val out = FileOutputStream(pfd!!.fileDescriptor)
            // get the already saved video as fileinputstream
            // The Directory where your file is saved
            val storageDir = File(
                getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                "Folder"
            )
            //Directory and the name of your video file to copy
            val videoFile = File(storageDir, "Myvideo")
            val `in` = FileInputStream(defaultVideo)
            val buf = ByteArray(8192)
            var len: Int
            while (`in`.read(buf).also { len = it } > 0) {
                out.write(buf, 0, len)
            }
            refreshGallery(videoFile)
            out.close()
            `in`.close()
            pfd.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        if (Build.VERSION.SDK_INT >= 29) {
            valuesvideos.clear()
            valuesvideos.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(uriSavedVideo!!, valuesvideos, null, null)
        }
    }

    private fun refreshGallery(file: File?) {
        val mediaScanIntent = Intent(
            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
        )
        mediaScanIntent.data = Uri.fromFile(file)
        sendBroadcast(mediaScanIntent)
    }

    private fun flashControl(v: View?) {
        Log.i("Flash", "Flash button clicked!")
        val hasFlash = applicationContext.packageManager
            .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        if (!hasFlash) {
            val alert = AlertDialog.Builder(this)
                .create()
            alert.setTitle("Error")
            alert.setMessage("Sorry, your device doesn't support flash light!")
            alert.setButton(
                "OK"
            ) { _, _ -> finish() }
            alert.show()
        } else {
            if (!isFlashOn) {
                isFlashOn = true
                img_flash_control.setImageResource(R.drawable.ic_flash_on)
                Log.i("Flash", "Flash On")
            } else {
                isFlashOn = false
                img_flash_control.setImageResource(R.drawable.ic_flash_off)
                Log.i("Flash", "Flash Off")
            }
        }
    }

    private fun switchCamera(forceSwitchToFrontCamera: Boolean) {
        if (camera == null) return
        if (!isRecording) {
            if (Camera.getNumberOfCameras() != 1) {
                camera!!.release()
                if (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK || forceSwitchToFrontCamera) {
                    currentCameraId =
                        Camera.CameraInfo.CAMERA_FACING_FRONT
                } else {
                    currentCameraId =
                        Camera.CameraInfo.CAMERA_FACING_BACK
                }
                camera =
                    Camera.open(currentCameraId)
                try {
                    camera?.setPreviewDisplay(previewHolder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                startPreview()
            }
        } else {
            Log.i("Switch Camera", "isRecording true")
        }
    }

    private fun editCaptureSwitch() {
        preview.visibility = View.VISIBLE
        camera_view.visibility = View.VISIBLE

        startPreview()
        edit_media.visibility = View.GONE
        captured_video.visibility = View.GONE
    }

    override fun onBackPressed() {
        if (edit_media.visibility == View.VISIBLE) {
            editCaptureSwitch()
        } else {
            finish()
        }
    }

    @SuppressLint("NewApi")
    override fun onResume() {
        super.onResume()
        camera = Camera.open(currentCameraId)
        try {
            camera?.setPreviewDisplay(previewHolder)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        switchCamera(true)
        //startPreview();
        //FocusCamera();
    }

    override fun onPause() {
        if (inPreview) {
            camera!!.stopPreview()
        }
        camera!!.release()
        camera = null
        inPreview = false
        super.onPause()
    }

    private fun initPreview() {
        if (camera != null && previewHolder!!.surface != null) {
            try {
                camera!!.stopPreview()
                camera!!.setPreviewDisplay(previewHolder)
            } catch (t: Throwable) {
                Log.e("Preview:surfaceCallback", "Exception in setPreviewDisplay()", t)
                Toast.makeText(this, t.message, Toast.LENGTH_LONG).show()
            }
            if (!cameraConfigured) {
                val parameters = camera!!.parameters
                val sizes = parameters.supportedPreviewSizes
                val list: MutableList<Int> = ArrayList()
                for (i in sizes.indices) {
                    Log.i("ASDF", "Supported Preview: " + sizes[i].width + "x" + sizes[i].height)
                    list.add(sizes[i].width)
                }
                val cs = sizes[closest(1920, list)]
                Log.i("Width x Height", cs.width.toString() + "x" + cs.height)

                parameters.setPreviewSize(cs.width, cs.height)
                camera!!.parameters = parameters
                cameraConfigured = true
            }
        }
    }

    private fun startPreview() {
        if (cameraConfigured && camera != null) {
            camera!!.setDisplayOrientation(
                setCameraDisplayOrientation(
                    this,
                    currentCameraId,
                    camera ?: return
                )
            )
            camera!!.startPreview()
            inPreview = true
        }
    }

    private fun setCameraDisplayOrientation(
        activity: Activity,
        cameraId: Int,
        camera: Camera
    ): Int {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        var result: Int
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360
        } else {
            result = (info.orientation - degrees + 360) % 360
        }
        return result
    }

    @SuppressLint("NewApi")
    fun getPermission() {
        val PERMISSION_ALL = 1
        val PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE
        )
        if (!hasPermission(
                this, Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
            finish()
        }
    }

    private fun hasPermission(context: Context?, vararg permissions: String?): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        permission ?: ""
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

}
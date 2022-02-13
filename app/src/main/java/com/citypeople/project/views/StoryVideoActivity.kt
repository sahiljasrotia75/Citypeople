package com.citypeople.project.views

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.Camera
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.os.Build.VERSION_CODES
import android.util.Log
import android.util.Range
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.citypeople.project.R
import com.citypeople.project.adapters.StoryRecyclerAdapter
import com.citypeople.project.adapters.utils.OnSwipeTouchListener
import com.citypeople.project.adapters.utils.PlayerViewAdapter
import com.citypeople.project.adapters.utils.RecyclerViewScrollListener
import com.citypeople.project.cameranew.AutoFitTextureView
import com.citypeople.project.cameranew.Camera2BasicFragment
import com.citypeople.project.cameranew.Camera2BasicFragment.*
import com.citypeople.project.databinding.FragmentVideoNewBinding
import com.citypeople.project.findFirstVisibleItemPosition
import com.citypeople.project.models.signin.MediaObject
import com.citypeople.project.models.signin.StoryModel
import com.citypeople.project.retrofit.Status
import com.citypeople.project.viewmodel.StoryViewModel
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.firebase.auth.FirebaseAuth
import com.kaopiz.kprogresshud.KProgressHUD
import io.github.krtkush.lineartimer.LinearTimer
import io.github.krtkush.lineartimer.LinearTimerView
import kotlinx.android.synthetic.main.fragment_video_new.*
import org.json.JSONObject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class StoryVideoActivity : AppCompatActivity(), LinearTimer.TimerListener {


    private val ORIENTATIONS = SparseIntArray()
    private val REQUEST_CAMERA_PERMISSION = 1
    private val FRAGMENT_DIALOG = "dialog"
    private val flash_state = 0

/*    companion object {
        ORIENTATIONS.append(android.view.Surface.ROTATION_0, 90)
        ORIENTATIONS.append(android.view.Surface.ROTATION_90, 0)
        ORIENTATIONS.append(android.view.Surface.ROTATION_180, 270)
        ORIENTATIONS.append(android.view.Surface.ROTATION_270, 180)
    }*/


    private lateinit var layoutManager: LinearLayoutManager
    lateinit var mProgressDialog: KProgressHUD
    lateinit var frontCam: SurfaceView
    lateinit var btnMenu: ImageView
    private var sharedPool = RecyclerView.RecycledViewPool()
    lateinit var btnBack: ImageView
    private lateinit var simpleExoPlayer1: SimpleExoPlayer
    lateinit var bindingObject: FragmentVideoNewBinding
    val mViewModel by viewModel<StoryViewModel>()
    lateinit var mAuth: FirebaseAuth
    private var currentLocation: TextView? = null

    //for the Corner Camera
    private var preview: SurfaceView? = null
    private var camera: Camera? = null
    private var cameraConfigured = false
    private var currentCameraId = 1
    private var previewHolder: SurfaceHolder? = null
    private var inPreview = false
    private var currentItem: Int = 0

    lateinit var linearTimerView: LinearTimerView
    lateinit var linearBackTimerView: LinearTimerView

    //private var stories: ArrayList<StoryModel>? = arrayListOf()
    private val stories = mutableListOf<StoryModel>()
    private val lastVisibleItemPosition: Int
        get() = layoutManager.findLastVisibleItemPosition()
    private var recyclerView: RecyclerView? = null
    private var mAdapter: StoryRecyclerAdapter? = null
    private val modelList: ArrayList<MediaObject> = ArrayList<MediaObject>()
    var myLocation: String? = ""

    // for handle scroll and get first visible item index
    private lateinit var scrollListener: RecyclerViewScrollListener

    private var url: String = ""
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        showFullscreenFlag()
        if (hasFocus)
            hideSystemUI()
    }

    fun hideSystemUI() {
        showFullscreenFlag()
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN)


    }

    fun showFullscreenFlag() {
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    /**
     * Tag for the [Log].
     */
    private val TAG = "StoryVideoActivity"

    /**
     * Camera state: Showing camera preview.
     */
    private val STATE_PREVIEW = 0

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private val STATE_WAITING_LOCK = 1

    var mCameraLensFacingDirection = 0
    var picture: ImageView? = null
    var pictureBack: ImageView? = null
    var isSpeakButtonLongPressed = false

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private val STATE_WAITING_PRECAPTURE = 2

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private val STATE_WAITING_NON_PRECAPTURE = 3

    /**
     * Camera state: Picture was taken.
     */
    private val STATE_PICTURE_TAKEN = 4


    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private val MAX_PREVIEW_WIDTH = 1920

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private val MAX_PREVIEW_HEIGHT = 1080

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private val mSurfaceTextureListener: TextureView.SurfaceTextureListener? =
        object : TextureView.SurfaceTextureListener {
            @RequiresApi(api = VERSION_CODES.LOLLIPOP)
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera(width, height)
            }

            @RequiresApi(api = VERSION_CODES.LOLLIPOP)
            override fun onSurfaceTextureSizeChanged(
                texture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                configureTransform(width, height)
            }

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
        }

    /**
     * ID of the current [CameraDevice].
     */
    private var mCameraId: String? = null

    private val REQUEST_VIDEO_PERMISSIONS = 1
    private val VIDEO_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_CONTACTS
    )

    /**
     * An [AutoFitTextureView] for camera preview.
     */
    private var mTextureView: AutoFitTextureView? = null
    private val blockView: LinearLayout? = null

    private val camera_flash: ImageView? = null
    var startRecordingcalled = false

    private var linearTimer: LinearTimer? = null
    private var linearTimerback: LinearTimer? = null
    private val time: TextView? = null

    private var mPreviewSession: CameraCaptureSession? = null

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var mCaptureSession: CameraCaptureSession? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null

    /**
     * The [Size] of camera preview.
     */
    private var mPreviewSize: Size? = null

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */

    private val mStateCallback: CameraDevice.StateCallback? =
        object : CameraDevice.StateCallback() {
            @RequiresApi(api = VERSION_CODES.LOLLIPOP)
            override fun onOpened(cameraDevice: CameraDevice) {
                // This method is called when the camera is opened.  We start camera preview here.
                mCameraOpenCloseLock.release()
                mCameraDevice = cameraDevice
                createCameraPreviewSession()
            }

            @SuppressLint("NewApi")
            @RequiresApi(api = VERSION_CODES.LOLLIPOP)
            override fun onDisconnected(cameraDevice: CameraDevice) {
                mCameraOpenCloseLock.release()
                cameraDevice.close()
                mCameraDevice = null
            }

            @SuppressLint("NewApi")
            override fun onError(cameraDevice: CameraDevice, error: Int) {
                mCameraOpenCloseLock.release()
                cameraDevice.close()
                mCameraDevice = null
                val activity: Activity = this@StoryVideoActivity
                if (null != activity) {
                    activity.finish()
                }
            }
        }


    private var mVideoSize: Size? = null

    /**
     * MediaRecorder
     */
    private var mMediaRecorder: MediaRecorder? = null
    private var mIsRecordingVideo = false

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var mBackgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null

    /**
     * An [ImageReader] that handles still image capture.
     */
    private var mImageReader: ImageReader? = null

    /**
     * This is the output file for our picture.
     */
    private val mFile: File? = null

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private val mOnImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            mFile?.let {
                ImageSaver(
                    reader.acquireNextImage(),
                    it
                )
            }?.let {
                mBackgroundHandler!!.post(
                    it
                )
            }
        }

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    /**
     * [CaptureRequest] generated by [.mPreviewRequestBuilder]
     */
    private var mPreviewRequest: CaptureRequest? = null

    /**
     * The current state of camera state for taking pictures.
     *
     * @see .mCaptureCallback
     */
    private var mState = STATE_PREVIEW

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)

    /**
     * Whether the current camera device supports Flash or not.
     */
    private var mFlashSupported = false

    /**
     * Orientation of the camera sensor
     */
    private var mSensorOrientation = 0

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val mCaptureCallback: CameraCaptureSession.CaptureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            @RequiresApi(api = VERSION_CODES.LOLLIPOP)
            private fun process(result: CaptureResult) {
                when (mState) {
                    STATE_PREVIEW -> {}
                    STATE_WAITING_LOCK -> {
                        val afState = result.get(CaptureResult.CONTROL_AF_STATE)
                        if (afState == null) {
                            captureStillPicture()
                        } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState || CaptureResult.CONTROL_AF_MODE_AUTO == afState) {
                            // CONTROL_AE_STATE can be null on some devices
                            val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                            if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
                            ) {
                                mState = STATE_PICTURE_TAKEN
                                captureStillPicture()
                            } else {
                                runPrecaptureSequence()
                            }
                        }
                    }
                    STATE_WAITING_PRECAPTURE -> {

                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                            mState = STATE_WAITING_NON_PRECAPTURE
                        }
                    }
                    STATE_WAITING_NON_PRECAPTURE -> {

                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            mState = STATE_PICTURE_TAKEN
                            captureStillPicture()
                        }
                    }
                }
            }

            @RequiresApi(api = VERSION_CODES.LOLLIPOP)
            override fun onCaptureProgressed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                partialResult: CaptureResult
            ) {
                process(partialResult)
            }

            @RequiresApi(api = VERSION_CODES.LOLLIPOP)
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                process(result)
            }
        }
    private var mVideoPath: Uri? = null

    /**
     * Shows a [Toast] on the UI thread.
     *
     * @param text The message to show
     */
    private fun showToast(text: String) {
        val activity: Activity = this
        activity.runOnUiThread {
            Toast.makeText(
                activity,
                text,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private var mNextVideoAbsolutePath: String? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun chooseVideoSize(choices: Array<Size>): Size? {
        for (size in choices) {
            if (size.width == size.height * 16 / 9 && size.width <= 1080) {
                return size
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size")
        return choices[0]
    }

    /**
     * Given `choices` of `Size`s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     * class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun chooseOptimalSize(
        choices: Array<Size>, textureViewWidth: Int,
        textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
    ): Size? {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = java.util.ArrayList()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Size> = java.util.ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight && option.height == option.width * h / w) {
                if (option.width >= textureViewWidth &&
                    option.height >= textureViewHeight
                ) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            choices[0]
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size? {
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = java.util.ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.height == option.width * h / w && option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }

        // Pick the smallest of those, assuming we found any
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            choices[0]
        }
    }

    var mFriendId = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingObject = DataBindingUtil.setContentView(this, R.layout.fragment_video_new)
        //setContentView(R.layout.fragment_video_new)
        initViews()
        mAuth = FirebaseAuth.getInstance()
        extractIntent()
        setAdapter()
        apiObservers()

        val currentUser = mAuth.currentUser
        val jsonObject = JSONObject()
        jsonObject.put("phone", currentUser?.phoneNumber)
        mViewModel.stories(jsonObject)

//        val index =  bindingObject.feedsMediaRv.findFirstVisibleItemPosition()
//        val name =  stories[index].name
//        Log.e("name",name)
        // bindingObject.txtName.text = name

    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initViews() {

        // GetPermission();
        // frontCam = findViewById(R.id.front_cam)
        linearTimerView = findViewById(R.id.linearTimer)
        linearBackTimerView = findViewById(R.id.linearTimerback)
        btnMenu = findViewById(R.id.imageView3)
        btnBack = findViewById(R.id.btnBack)
        currentLocation = findViewById(R.id.current_location)
        recyclerView = findViewById(R.id.feeds_media_rv)
        picture = findViewById<ImageView>(R.id.picture)
        pictureBack = findViewById<ImageView>(R.id.pictureback)
        simpleExoPlayer1 = SimpleExoPlayer.Builder(this)
            .build()

        picture?.setOnLongClickListener(recordHoldListener)
        picture?.setOnTouchListener(recordTouchListener)
        pictureBack?.setOnLongClickListener(recordHoldListenerback)
        pictureBack?.setOnTouchListener(recordTouchListenerback)
        // camera_flash=(ImageView)view.findViewById(R.id.camera_flash);
        // camera_flash=(ImageView)view.findViewById(R.id.camera_flash);
        // camera_flash=(ImageView)view.findViewById(R.id.camera_flash);
        mTextureView = findViewById<View>(R.id.texture) as AutoFitTextureView?
        btnBack.setOnClickListener {
            onBackPressed()
        }

        val duration = (10 * 1000).toLong()

        linearTimer = LinearTimer.Builder()
            .linearTimerView(linearTimerView)
            .duration(duration)
            .timerListener(this)
            .getCountUpdate(LinearTimer.COUNT_DOWN_TIMER, 1000)
            .build()

        linearTimerback = LinearTimer.Builder()
            .linearTimerView(linearBackTimerView)
            .duration(duration)
            .timerListener(this)
            .getCountUpdate(LinearTimer.COUNT_DOWN_TIMER, 1000)
            .build()

        picture?.setOnClickListener {
            switchCameraFront()
        }
        pictureBack?.setOnClickListener {
            switchCameraBack()
        }


        bindingObject.mainRelative.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                Log.e("onLeftSwipe", "onLeft")
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                Log.e("onRightSwipe", "onRight")
            }
        })

        bindingObject.viewLeft.setOnClickListener {
            if (layoutManager.findFirstVisibleItemPosition() > 0) {
                //   recyclerView?.smoothScrollToPosition(layoutManager.findFirstVisibleItemPosition() - 1)
                var jumpTo = -2
                val arrayUserId = arrayListOf<Int>()
                val currentUserIdIndex = bindingObject.feedsMediaRv.findFirstVisibleItemPosition()

                if (currentUserIdIndex == RecyclerView.NO_POSITION) {
                    Toast.makeText(this, "There is no story", Toast.LENGTH_SHORT).show()
                } else {
                    stories[currentUserIdIndex].user_id
                    arrayUserId.addAll(listOf(stories[currentUserIdIndex].user_id))
                    Log.e("currentUserIdIndex", currentUserIdIndex.toString())
                    stories.forEachIndexed { index, storyModel ->
                        if (index < currentUserIdIndex && stories[currentUserIdIndex].user_id != stories[index].user_id) {
                            jumpTo = index
                        }
                    }

                    if (jumpTo >= 0) {
                        recyclerView?.smoothScrollToPosition(jumpTo)
                    } else {
                        finish()
                    }

                }
            } else {
                recyclerView?.smoothScrollToPosition(0)
            }
        }


        bindingObject.viewRight.setOnClickListener {
            // recyclerView?.smoothScrollToPosition(layoutManager.findLastVisibleItemPosition() + 1)

            var jumpTo = -2
            val arrayUserId = arrayListOf<Int>()
            val currentUserIdIndex = bindingObject.feedsMediaRv.findFirstVisibleItemPosition()

            if (currentUserIdIndex == RecyclerView.NO_POSITION) {
                Toast.makeText(this, "There is no story", Toast.LENGTH_SHORT).show()
            } else {
                stories[currentUserIdIndex].user_id
                arrayUserId.addAll(listOf(stories[currentUserIdIndex].user_id))

                Log.e("currentUserIdIndex", currentUserIdIndex.toString())
                stories.forEachIndexed { index, storyModel ->
                    if (index > currentUserIdIndex && jumpTo == -2 && stories[currentUserIdIndex].user_id != stories[index].user_id) {
                        jumpTo = index
                    }
                }

                if (jumpTo >= 0) {
                    recyclerView?.smoothScrollToPosition(jumpTo)
                } else {
                    finish()
                }

            }
        }

    }

    private fun extractIntent() {
        if (intent.extras?.containsKey("currentLocation") == false) return

        if (intent.extras?.containsKey("currentLocation") == true)
            myLocation = intent.extras?.getString("currentLocation")
        Log.e("MyLocation", myLocation.toString())


    }


    private fun apiObservers() {
        mViewModel.storyList?.observe(this, Observer {
            when (it.status) {
                Status.LOADING -> {
                    showProgress()
                }
                Status.ERROR -> {
                    hideProgress()
                    it.message?.let { msg ->
                        Toast.makeText(this, msg.message, Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {

                    hideProgress()
                    it.data?.apply {
                        it.data?.videos?.let { p ->
                            stories?.addAll(it.data.videos)
                            if (stories != null && stories?.isNotEmpty()) {
                                mAdapter?.updateList(stories)

                            } else {
                                Toast.makeText(applicationContext, "No Data", Toast.LENGTH_SHORT)
                                    .show()
                            }

                        }

                    }
                }
            }
        })

        mViewModel.sendVideo?.observe(this, Observer {
            when (it.status) {
                Status.LOADING -> showProgress()
                Status.ERROR -> {
                    hideProgress()
                    it.message?.let { msg ->
                        Toast.makeText(this, msg.message, Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    hideProgress()
                    if (it.data?.status == true) {
                        Toast.makeText(
                            applicationContext,
                            "Video send successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        // finish()
                    } else {
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

//        mViewModel.getMedia().observe(this, Observer {
//            mAdapter?.updateList(arrayListOf(*it.toTypedArray()))
//        })

    }

    private val recordHoldListener =
        View.OnLongClickListener { // Do something when your hold starts here.
            Log.d("things called", "onLongPressed")
            if (!startRecordingcalled) {
                startRecordingVideo()
                linearTimer!!.startTimer()
            }
            isSpeakButtonLongPressed = true
            true
        }

    @SuppressLint("ClickableViewAccessibility")
    private val recordTouchListener = View.OnTouchListener { pView, pEvent ->
        pView.onTouchEvent(pEvent)
        // We're only interested in when the button is released.
        if (pEvent.action == MotionEvent.ACTION_UP) {
            // We're only interested in anything if our speak button is currently pressed.
            if (isSpeakButtonLongPressed) {
                Log.d("things called", "onTouch")
                stopRecordingVideo()
                linearTimer!!.pauseTimer()
                linearTimer!!.resetTimer()
                startRecordingcalled = false
                // Do something when the button is released.
                isSpeakButtonLongPressed = false
            }
        }
        false
    }

    private val recordHoldListenerback =
        View.OnLongClickListener { // Do something when your hold starts here.
            Log.d("things called", "onLongPressed")
            if (!startRecordingcalled) {
                startRecordingVideo()
                linearTimerback!!.startTimer()
            }
            isSpeakButtonLongPressed = true
            true
        }

    @SuppressLint("ClickableViewAccessibility")
    private val recordTouchListenerback = View.OnTouchListener { pView, pEvent ->
        pView.onTouchEvent(pEvent)
        // We're only interested in when the button is released.
        if (pEvent.action == MotionEvent.ACTION_UP) {
            // We're only interested in anything if our speak button is currently pressed.
            if (isSpeakButtonLongPressed) {
                Log.d("things called", "onTouch")
                stopRecordingVideo()
                linearTimerback!!.pauseTimer()
                linearTimerback!!.resetTimer()
                startRecordingcalled = false
                // Do something when the button is released.
                isSpeakButtonLongPressed = false
            }
        }
        false
    }

    private fun setAdapter() {
        mAdapter = StoryRecyclerAdapter(this, stories)
        recyclerView!!.setHasFixedSize(true)
        // use a linear layout manager
        layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView!!.layoutManager = layoutManager
        recyclerView!!.adapter = mAdapter

        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView!!)

        scrollListener = object : RecyclerViewScrollListener() {
            override fun onScrollStateChanged(recycler: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recycler, newState)
                if (!recyclerView!!.canScrollHorizontally(1)) {
                    //function that add new elements to my recycler view
                    var jumpTo = -2
                    val arrayUserId = arrayListOf<Int>()
                    val currentUserIdIndex =
                        bindingObject.feedsMediaRv.findFirstVisibleItemPosition()
                    if (currentUserIdIndex == RecyclerView.NO_POSITION) {
                        Toast.makeText(applicationContext, "There is no story", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        stories[currentUserIdIndex].user_id
                        arrayUserId.addAll(listOf(stories[currentUserIdIndex].user_id))

                        Log.e("currentUserIdIndex", currentUserIdIndex.toString())
                        stories.forEachIndexed { index, storyModel ->
                            if (index > currentUserIdIndex && jumpTo == -2 && stories[currentUserIdIndex].user_id != stories[index].user_id) {
                                jumpTo = index
                            }
                        }

                        if (jumpTo >= 0) {
                            recyclerView?.smoothScrollToPosition(jumpTo)
                        } else {
                            // finish()
                        }
                    }
                }
            }

            override fun onItemIsFirstVisibleItem(index: Int) {
                Log.e("visible item index", index.toString())
                if (index != -1) {
                    // play just visible item
                    PlayerViewAdapter.playIndexThenPausePreviousPlayer(index)
                }

            }

        }
        recyclerView!!.addOnScrollListener(scrollListener)
        mAdapter!!.SetOnItemClickListener(object : StoryRecyclerAdapter.OnItemClickListener {
            override fun onItemClick(view: View?, position: Int, model: StoryModel?) {
                Log.e("userId", model?.user_id.toString())
            }
        })
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        mCameraLensFacingDirection = 0
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView!!.isAvailable) {
            openCamera(mTextureView!!.width, mTextureView!!.height)
        } else {
            mTextureView!!.surfaceTextureListener = mSurfaceTextureListener
        }

    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        PlayerViewAdapter.releaseAllPlayers()
        super.onPause()
    }

    private fun showProgress() {
        if (!::mProgressDialog.isInitialized) {
            mProgressDialog = KProgressHUD.create(this)
                .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
                .setLabel("Please wait...")
                .setCancellable(false)
                .setAnimationSpeed(3)
                .setDimAmount(0.5f)
                .show()
        } else mProgressDialog.show()
    }

    private fun hideProgress() {
        if (::mProgressDialog.isInitialized && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss()
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun switchCameraBack() {
        if (mCameraLensFacingDirection != 1) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_BACK
            closeCamera()
            reopenCamera()
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun switchCameraFront() {
        if (mCameraLensFacingDirection == 1) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_FRONT
            closeCamera()
            reopenCamera()
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun setUpCameraOutputs(width: Int, height: Int) {
        val activity: Activity = this
        val manager = activity.getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // We don't use a front facing camera in this sample.
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing != mCameraLensFacingDirection) {
                    continue
                }
                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )
                    ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                    Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )
                mImageReader = ImageReader.newInstance(
                    largest.width, largest.height,
                    ImageFormat.JPEG,  /*maxImages*/2
                )
                mImageReader!!.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler
                )

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                val displayRotation = activity.windowManager.defaultDisplay.rotation
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
                var swappedDimensions = false
                when (displayRotation) {
                    Surface.ROTATION_0, Surface.ROTATION_180 -> if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                        swappedDimensions = true
                    }
                    Surface.ROTATION_90, Surface.ROTATION_270 -> if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                        swappedDimensions = true
                    }
                    else -> Log.e(
                        TAG,
                        "Display rotation is invalid: $displayRotation"
                    )
                }
                val displaySize = Point()
                activity.windowManager.defaultDisplay.getSize(displaySize)
                var rotatedPreviewWidth = width
                var rotatedPreviewHeight = height
                var maxPreviewWidth = displaySize.x
                var maxPreviewHeight = displaySize.y
                if (swappedDimensions) {
                    rotatedPreviewWidth = height
                    rotatedPreviewHeight = width
                    maxPreviewWidth = displaySize.y
                    maxPreviewHeight = displaySize.x
                }
                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH
                }
                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(
                    map.getOutputSizes(
                        SurfaceTexture::class.java
                    ),
                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                    maxPreviewHeight, largest
                )

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mPreviewSize?.let {
                        mTextureView!!.setAspectRatio(
                            it.width, mPreviewSize!!.height
                        )
                    }
                } else {
                    mPreviewSize?.let {
                        mTextureView!!.setAspectRatio(
                            it.height, mPreviewSize!!.width
                        )
                    }
                }

                // Check if the flash is supported.
                val available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                mFlashSupported = available ?: false
                mCameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
        }
    }

    /**
     * Opens the camera specified by [Camera2BasicFragment.mCameraId].
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun openCamera(width: Int, height: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // requestCameraPermission()
            return
        }
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        /*  if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
              requestVideoPermissions()
              return
          }*/
        val activity: Activity = this
        if (null == activity || activity.isFinishing) {
            return
        }
        val manager = activity.getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            Log.d(TAG, "tryAcquire")
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            //  String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            val characteristics = manager.getCameraCharacteristics(
                mCameraId!!
            )
            val map = characteristics
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            if (map == null) {
                throw RuntimeException("Cannot get available preview/video sizes")
            }
            mVideoSize = chooseVideoSize(
                map.getOutputSizes(
                    MediaRecorder::class.java
                )
            )
            mPreviewSize = mVideoSize?.let {
                chooseOptimalSize(
                    map.getOutputSizes(
                        SurfaceTexture::class.java
                    ),
                    width, height, it
                )
            }
            val orientation = resources.configuration.orientation
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mPreviewSize?.let {
                    mTextureView!!.setAspectRatio(
                        it.width,
                        mPreviewSize!!.height
                    )
                }
            } else {
                mPreviewSize?.let {
                    mTextureView!!.setAspectRatio(
                        it.getHeight(),
                        mPreviewSize!!.getWidth()
                    )
                }
            }
            configureTransform(width, height)
            mMediaRecorder = MediaRecorder()
            manager.openCamera(mCameraId!!, mStateCallback!!, null)
        } catch (e: CameraAccessException) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show()
            activity.finish()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
//            ErrorDialog.newInstance(getString(R.string.camera_error))
//                .show(this, FRAGMENT_DIALOG)
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.")
        }
    }

    /**
     * Closes the current [CameraDevice].
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            if (null != mCaptureSession) {
                mCaptureSession!!.close()
                mCaptureSession = null
            }
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (null != mImageReader) {
                mImageReader!!.close()
                mImageReader = null
            }
            if (null != mMediaRecorder) {
                mMediaRecorder!!.release()
                mMediaRecorder = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.getLooper())
    }

    /**
     * Stops the background thread and its [Handler].
     */
    @RequiresApi(api = VERSION_CODES.JELLY_BEAN_MR2)
    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun createCameraPreviewSession() {
        try {
            val texture = mTextureView!!.surfaceTexture!!

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(surface)

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice!!.createCaptureSession(
                Arrays.asList(surface, mImageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (null == mCameraDevice) {
                            return
                        }

                        // When the session is ready, we start displaying the preview.
                        mCaptureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            mPreviewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                getRange()
                            ) //This line of code is used for adjusting the fps range and fixing the dark preview
                            mPreviewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AF_MODE_AUTO
                            )
                            // Flash is automatically enabled when necessary.
                            setFlash(mPreviewRequestBuilder!!)


                            // Finally, we start displaying the camera preview.
                            mPreviewRequest = mPreviewRequestBuilder!!.build()
                            mCaptureSession!!.setRepeatingRequest(
                                mPreviewRequest!!,
                                mCaptureCallback, mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(
                        cameraCaptureSession: CameraCaptureSession
                    ) {
                        showToast("Failed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun getRange(): Range<Int>? {
        val mCameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
        var chars: CameraCharacteristics? = null
        try {
            chars = mCameraManager.getCameraCharacteristics(mCameraId!!)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        val ranges = chars!!.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)!!
        var result: Range<Int>? = null
        for (range in ranges) {
            val upper = range.upper

            // 10 - min range upper for my needs
            if (upper >= 10) {
                if (result == null || upper < result.upper.toInt()) {
                    result = range
                }
            }
        }
        return result
    }

    /**
     * Configures the necessary [Matrix] transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity: Activity = this
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(
            0F, 0F,
            viewWidth.toFloat(), viewHeight.toFloat()
        )
        val bufferRect = RectF(
            0F, 0F,
            mPreviewSize!!.height.toFloat(), mPreviewSize!!.width.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / mPreviewSize!!.height,
                viewWidth.toFloat() / mPreviewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        mTextureView!!.setTransform(matrix)
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                getRange()
            ) //This line of code is used for adjusting the fps range and fixing the dark preview
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK
            mCaptureSession!!.capture(
                mPreviewRequestBuilder!!.build(), mCaptureCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in [.mCaptureCallback] from [.lockFocus].
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                getRange()
            ) //This line of code is used for adjusting the fps range and fixing the dark preview
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE
            mCaptureSession!!.capture(
                mPreviewRequestBuilder!!.build(), mCaptureCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * [.mCaptureCallback] from both [.lockFocus].
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun captureStillPicture() {
        try {
            val activity: Activity = this
            if (null == activity || null == mCameraDevice) {
                return
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mImageReader!!.surface)


            // Use the same AE and AF modes as the preview.
            captureBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AF_MODE_AUTO
            )
            setFlash(captureBuilder)

            // Orientation
            val rotation = activity.windowManager.defaultDisplay.rotation
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation))
            val CaptureCallback: CameraCaptureSession.CaptureCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        showToast("Saved: $mFile")
                        Log.d(TAG, mFile.toString())
                        Log.d("absolute path:", mFile!!.absolutePath)
                        unlockFocus()


                        //  Intent i=new Intent(getContext(),NewActivity.class);

                        //   i.putExtra("filepath",mFile.toString());
                        //   startActivity(i);
                    }
                }
            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.abortCaptures()
            mCaptureSession!!.capture(captureBuilder.build(), CaptureCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private fun getOrientation(rotation: Int): Int {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS[rotation] + mSensorOrientation + 270) % 360
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                getRange()
            ) //This line of code is used for adjusting the fps range and fixing the dark preview
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL
            )
            setFlash(mPreviewRequestBuilder!!)
            mCaptureSession!!.capture(
                mPreviewRequestBuilder!!.build(), mCaptureCallback,
                mBackgroundHandler
            )
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW
            mCaptureSession!!.setRepeatingRequest(
                mPreviewRequest!!, mCaptureCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    fun onClick(view: View) {
        when (view.id) {
            R.id.picture -> {
                switchCameraFront()
            }
            R.id.pictureback -> {
                switchCameraBack()
            }
        }
    }


    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun reopenCamera() {
        if (mTextureView!!.isAvailable) {
            openCamera(mTextureView!!.width, mTextureView!!.height)
        } else {
            mTextureView!!.surfaceTextureListener = mSurfaceTextureListener
        }
    }


    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun setFlash(requestBuilder: CaptureRequest.Builder) {
        if (flash_state == 0) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
            //  camera_flash.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_flash_auto));
        } else if (flash_state == -1) {
            requestBuilder.set(
                CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_OFF
            )
            //   camera_flash.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_flash_off));
        } else if (flash_state == 1) {
            requestBuilder.set(
                CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_SINGLE
            )
            //   camera_flash.setImageDrawable(getContext().getResources().getDrawable(R.drawable.ic_flash_on));
        }
    }


    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (mFlashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun setOnFlash(requestBuilder: CaptureRequest.Builder) {
        if (mFlashSupported) {
            requestBuilder.set(
                CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_SINGLE
            )
        }
    }


    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun setOffFlash(requestBuilder: CaptureRequest.Builder) {
        if (mFlashSupported) {
            requestBuilder.set(
                CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_OFF
            )
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    override fun animationComplete() {
        Log.d("things called", "onTouch")
        stopRecordingVideo()
        linearTimer!!.resetTimer()
        linearTimerback!!.resetTimer()
        startRecordingcalled = false
        // Do something when the button is released.
        isSpeakButtonLongPressed = false
    }

    override fun timerTick(tickUpdateInMillis: Long) {
        val formattedTime = String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(tickUpdateInMillis),
            TimeUnit.MILLISECONDS.toSeconds(tickUpdateInMillis)
                    - TimeUnit.MINUTES
                .toSeconds(TimeUnit.MILLISECONDS.toHours(tickUpdateInMillis))
        )
        //time!!.text = formattedTime
    }

    override fun onTimerReset() {
        //  time!!.text = ""
    }


    /**
     * Saves a JPEG [Image] into the specified [File].
     */
    private class ImageSaver internal constructor(
        /**
         * The JPEG image
         */
        private val mImage: Image,
        /**
         * The file we save the image into.
         */
        private val mFile: File
    ) :
        Runnable {
        @RequiresApi(api = VERSION_CODES.KITKAT)
        override fun run() {
            val buffer = mImage.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]
            var output: FileOutputStream? = null
            try {
                output = FileOutputStream(mFile)
                output.write(bytes)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                mImage.close()
                if (null != output) {
                    try {
                        output.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }


    //for video
    /**
     * Start the camera preview.
     */
    @JvmName("startPreview1")
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun startPreview() {
        if (null == mCameraDevice || !mTextureView!!.isAvailable || null == mPreviewSize) {
            return
        }
        try {
            closePreviewSession()
            val texture = mTextureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val previewSurface = Surface(texture)
            mPreviewBuilder!!.addTarget(previewSurface)
            mCameraDevice!!.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mPreviewSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        val activity: Activity = this@StoryVideoActivity
                        if (null != activity) {
                            Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }, mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Update the camera preview. [.startPreview] needs to be called in advance.
     */
    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun updatePreview() {
        if (null == mCameraDevice) {
            return
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder!!)
            val thread = HandlerThread("CameraPreview")
            thread.start()
            mPreviewSession!!.setRepeatingRequest(
                mPreviewBuilder!!.build(),
                null,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }


    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    @Throws(
        IOException::class
    )
    private fun setUpMediaRecorder() {
        val activity: Activity = this ?: return
        Log.d("things called", "setUpMediaRecording")
        val cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath!!.isEmpty()) {
            mVideoPath = getOutputMediaFileUriV(this)
        }
        mMediaRecorder!!.setOutputFile(mNextVideoAbsolutePath)
        mMediaRecorder!!.setVideoEncodingBitRate(cpHigh.videoBitRate)
        mMediaRecorder!!.setVideoFrameRate(cpHigh.videoFrameRate)
        mMediaRecorder!!.setVideoSize(mVideoSize!!.width, mVideoSize!!.height)
        mMediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mMediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        if (mCameraDevice!!.id == "1") {
            mMediaRecorder!!.setOrientationHint(270)
        } else {
            mMediaRecorder!!.setOrientationHint(90)
        }
        mMediaRecorder!!.prepare()
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private fun getOutputMediaFileUriV(context: Context): Uri {
        return FileProvider.getUriForFile(
            this, this.getPackageName() + ".fileprovider",
            createVideoFile()!!
        )
    }

    private fun createVideoFile(): File? {
        // Check that the SDCard is mounted
        val mediaStorageDir: File = File(
            this.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            "CitypeopleVideos"
        )
        // Create the storage directory(MyCameraVideo) if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return File("")
            }
        }
        val mediaFile =
            File(mediaStorageDir.path + File.separator + "CitypeopleVideos" + System.currentTimeMillis() + ".mp4")
        mNextVideoAbsolutePath = mediaFile.absolutePath
        Log.e("mediaFile.getName() ", mediaFile.absolutePath)
        return mediaFile
    }


    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun startRecordingVideo() {
        if (null == mCameraDevice || !mTextureView!!.isAvailable || null == mPreviewSize) {
            return
        }
        startRecordingcalled = true
        Log.d("things called", "startrecording")
        try {
            closePreviewSession()
            setUpMediaRecorder()
            val texture = mTextureView!!.surfaceTexture!!
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)
            mPreviewBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            val surfaces: MutableList<Surface> = java.util.ArrayList()

            // Set up Surface for the camera preview
            val previewSurface = Surface(texture)
            surfaces.add(previewSurface)
            mPreviewBuilder!!.addTarget(previewSurface)
            val manager = this.getSystemService(CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(
                mCameraId!!
            )
            mPreviewBuilder!!.set(
                CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(
                    characteristics,
                    this.getWindowManager().getDefaultDisplay().getRotation()
                )
            )

            // Set up Surface for the MediaRecorder
            val recorderSurface = mMediaRecorder!!.surface
            surfaces.add(recorderSurface)
            mPreviewBuilder?.addTarget(recorderSurface)
            mPreviewBuilder?.set(
                CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(
                    characteristics,
                    this.getWindowManager().getDefaultDisplay().getRotation()
                )
            )

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice!!.createCaptureSession(
                surfaces,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        mPreviewSession = cameraCaptureSession
                        updatePreview()
                        this@StoryVideoActivity.runOnUiThread(Runnable { // UI
                            // mButtonVideo.setText(R.string.stop);
                            mIsRecordingVideo = true

                            // Start recording
                            mMediaRecorder!!.start()
                        })
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        val activity: Activity = this@StoryVideoActivity
                        if (null != activity) {
                            Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getJpegOrientation(
        c: CameraCharacteristics,
        deviceOrientation: Int
    ): Int {
        var deviceOrientation = deviceOrientation
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation =
            c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
        val facingFront =
            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
    }

    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }


    @RequiresApi(api = VERSION_CODES.LOLLIPOP)
    private fun stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false

        // Stop recording
        try {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val temp_path = mNextVideoAbsolutePath!!
        val activity: Activity = this
        if (null != activity) {
            Log.d(TAG, "Video saved: $mNextVideoAbsolutePath")
        }
        mNextVideoAbsolutePath = null
        startVideoCompress(temp_path)
        startPreview()
    }

    @SuppressLint("NewApi")
    private fun startVideoCompress(videoPath: String) {
        Log.w("startVideoCompress", videoPath)
        val uri = Uri.parse(videoPath).toString()
        val arrayUserId = arrayListOf<Int>()
        val groupList = arrayListOf<Int>()
        val index = bindingObject.feedsMediaRv.findFirstVisibleItemPosition()
        if (index == RecyclerView.NO_POSITION) {
            Toast.makeText(this, "Could not reply.There is no story", Toast.LENGTH_SHORT).show()
        } else {
            if (myLocation.isNullOrEmpty()) {
                Toast.makeText(this, "Please turn on your location", Toast.LENGTH_SHORT).show()
            } else {
                stories[index].user_id
                arrayUserId.addAll(listOf(stories[index].user_id))
                val currentUser = mAuth.currentUser
                val jsonObject = JSONObject()
                jsonObject.put("friends", arrayUserId)
                jsonObject.put("groups", groupList)
                jsonObject.put("phone", currentUser?.phoneNumber)
                jsonObject.put("location", myLocation)
                val file =
                    File(uri)//ImagePickerUtils.getFilePathFromURI(this, Uri.parse(recordedVideoPath)))
                mViewModel.sendVideo(jsonObject, file)
            }

        }

//        val i = Intent(this, VideoSendActivity::class.java).putExtra(Constants.INTENT_DATA, uri)
//        startActivity(i)
    }


    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size?> {
        @RequiresApi(api = VERSION_CODES.LOLLIPOP)
        override fun compare(o1: Size?, o2: Size?): Int {
            // We cast here to ensure the multiplications won't overflow
            return (o1?.width?.toLong()
                ?.times(o1!!.height))?.minus(((o2?.width?.toLong())?.times(o2!!.height)!!))
                ?.let {
                    java.lang.Long.signum(
                        it
                    )
                }!!
        }


    }

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity: Activity? = activity
            return AlertDialog.Builder(activity)
                .setMessage(arguments!!.getString(ARG_MESSAGE))
                .setPositiveButton(
                    android.R.string.ok
                ) { dialogInterface, i -> activity!!.finish() }
                .create()
        }

        companion object {
            private const val ARG_MESSAGE = "message"
            fun newInstance(message: String?): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }
    }

}

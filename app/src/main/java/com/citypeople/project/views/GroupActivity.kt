package com.citypeople.project.views

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.Camera
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.ContactsContract
import android.util.Log
import android.util.Range
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.citypeople.project.databinding.ActivityGroupBinding
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.flexbox.AlignItems
import java.io.IOException
import androidx.recyclerview.widget.DividerItemDecoration
import com.citypeople.project.BaseActivity
import com.citypeople.project.adapters.FriendAdapter
import com.citypeople.project.adapters.GroupListAdapter
import com.citypeople.project.R
import com.citypeople.project.cameranew.AutoFitTextureView
import com.citypeople.project.makeGone
import com.citypeople.project.makeVisible
import com.citypeople.project.models.signin.User
import com.citypeople.project.retrofit.Status
import com.citypeople.project.utilities.common.BaseViewModel
import com.citypeople.project.viewmodel.GroupViewModel
import com.google.firebase.auth.FirebaseAuth
import io.github.krtkush.lineartimer.LinearTimer
import org.json.JSONObject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class GroupActivity : BaseActivity(), FriendListener, FriendAdapter.FriendItemListener {

    private val ORIENTATIONS = SparseIntArray()
    private val REQUEST_CAMERA_PERMISSION = 1
    private val FRAGMENT_DIALOG = "dialog"
    private val flash_state = 0


    lateinit var bindingObj: ActivityGroupBinding
    lateinit var mFriendAdapter: FriendAdapter
    lateinit var mGroupListAdapter: GroupListAdapter
    private var inPreview = false
    val mViewModel by viewModel<GroupViewModel>()
    var aa = ArrayList<FriendModel>()
    var gg = ArrayList<String>()
    lateinit var mAuth: FirebaseAuth
    var contacts= hashMapOf<String,String>()
    private var preview: SurfaceView? = null
    private var camera: Camera? = null
    private var cameraConfigured = false
    private var currentCameraId = 1
    private var previewHolder: SurfaceHolder? = null



    /**
     * Tag for the [Log].
     */
    private val TAG = "GroupActivity"

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
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                openCamera(width, height)
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun onOpened(cameraDevice: CameraDevice) {
                // This method is called when the camera is opened.  We start camera preview here.
                mCameraOpenCloseLock.release()
                mCameraDevice = cameraDevice
                createCameraPreviewSession()
            }

            @SuppressLint("NewApi")
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
                val activity: Activity = this@GroupActivity
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
                GroupActivity.ImageSaver(
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
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            override fun onCaptureProgressed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                partialResult: CaptureResult
            ) {
                process(partialResult)
            }

            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
            Collections.min(bigEnough, GroupActivity.CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            choices[0]
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
            Collections.min(bigEnough, GroupActivity.CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            choices[0]
        }
    }

    var mFriendId = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingObj = DataBindingUtil.setContentView(this, R.layout.activity_group)
        bindingObj.listener = this
        mAuth = FirebaseAuth.getInstance()
        initilization()
        initArray()
        getNumber(this.contentResolver)
        setAdapter()
        apiObserver()

    }

    override fun bindViewModel(): BaseViewModel {
        return mViewModel
    }

    private fun initArray() {
        gg.add("Francine Valdez")
        gg.add("Mike")
        gg.add("Jonathan Clark")
        gg.add("Debra")
        gg.add("Beverly")
    }

    private fun initilization() {
//        preview = findViewById<View>(R.id.front_cam) as SurfaceView
//        previewHolder = preview?.holder
//        preview!!.visibility = View.VISIBLE
//        previewHolder?.addCallback(surfaceCallback)
//        previewHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        mTextureView = findViewById<View>(R.id.texture) as AutoFitTextureView?


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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun switchCameraBack() {
        if (mCameraLensFacingDirection != 1) {
            mCameraLensFacingDirection = CameraCharacteristics.LENS_FACING_BACK
            closeCamera()
            reopenCamera()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
                    GroupActivity.CompareSizesByArea()
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun reopenCamera() {
        if (mTextureView!!.isAvailable) {
            openCamera(mTextureView!!.width, mTextureView!!.height)
        } else {
            mTextureView!!.surfaceTextureListener = mSurfaceTextureListener
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
        if (mFlashSupported) {
            requestBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
            )
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun setOnFlash(requestBuilder: CaptureRequest.Builder) {
        if (mFlashSupported) {
            requestBuilder.set(
                CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_SINGLE
            )
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun setOffFlash(requestBuilder: CaptureRequest.Builder) {
        if (mFlashSupported) {
            requestBuilder.set(
                CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_OFF
            )
        }
    }

    /**
     * Start the camera preview.
     */
    @JvmName("startPreview1")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
                        val activity: Activity = this@GroupActivity
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
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun setUpCaptureRequestBuilder(builder: CaptureRequest.Builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }


    private fun setAdapter() {
        bindingObj.rvContactList.layoutManager = LinearLayoutManager(this)
        mFriendAdapter = FriendAdapter(this)
        bindingObj.rvContactList.adapter = mFriendAdapter
        mFriendAdapter.clearList()
       // mFriendAdapter.setDataList(aa.toMutableList())
        bindingObj.rvContactList.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )

        val layoutManager = FlexboxLayoutManager(this)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.CENTER
        layoutManager.alignItems = AlignItems.CENTER
        bindingObj.rvGroupList.layoutManager = layoutManager
        mGroupListAdapter = GroupListAdapter(this)
        bindingObj.rvGroupList.adapter = mGroupListAdapter
        mGroupListAdapter.clearList()
        mGroupListAdapter.setDataList(gg.toMutableList())


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
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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

    @SuppressLint("Range")
    fun getNumber(cr: ContentResolver) {
        val phones =
            cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        // use the cursor to access the contacts
        while (phones!!.moveToNext()) {
            val name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            // get display name
            var phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            // get phone number
            phoneNumber = phoneNumber.replace("(","").replace(")","").replace("-","").replace(" ","")
            // get phone number
            contacts[phoneNumber]=name
        }
        getListOfGroup()
    }

    private fun getListOfGroup(){
        val currentUser = mAuth.currentUser
        val jsonObject = JSONObject()
        val list = ArrayList<String>(contacts.keys)
        //jsonObject.put("contacts", list);
        jsonObject.put("phone", currentUser?.phoneNumber);
        mViewModel.groupContacts(jsonObject)
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        mFriendAdapter.filter.filter(s.toString())
    }

    override fun addFriend() {
        val friendList = mFriendAdapter?.getCurrentItems()?.filter { it.isSelected}?.map {
            it.id
        }
        if (friendList.isNullOrEmpty()){
            Toast.makeText(this, "Your group list is empty", Toast.LENGTH_SHORT).show();
        }else if(bindingObj.tvNewGroup.text.toString().trim().isEmpty()){
            Toast.makeText(this, "Please enter group name", Toast.LENGTH_SHORT).show();
        } else{
            val currentUser = mAuth.currentUser
            val jsonObject = JSONObject()
            val list = ArrayList<Int>(friendList.toMutableList())
            jsonObject.put("ids", list);
            jsonObject.put("phone", currentUser?.phoneNumber);
            jsonObject.put("name", bindingObj.tvNewGroup.text.toString().trim());
            mViewModel.addGroup(jsonObject)
        }
        Log.e("mySeletedList",friendList.toString())
    }

    fun apiObserver(){
        mViewModel.groupList?.observe(this, Observer {
            when(it.status){
                Status.LOADING -> mViewModel.loader.postValue(true)
                Status.ERROR -> {
                    mViewModel.loader.postValue(false)
                    it.message?.let {msg->
                        Toast.makeText(this,msg.message,Toast.LENGTH_SHORT).show()
                    }?:Toast.makeText(this,"Something went wrong",Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS->{
                    mViewModel.loader.postValue(false)
                    it.data?.apply {
//                        it.data.users.map {
//                                T ->
//                            T.name = contacts[T.phone].toString()
//                        }
                        if(it.data.users.isNullOrEmpty()){
                            bindingObj.emptyChatTv.makeVisible()
                        }else{
                            bindingObj.emptyChatTv.makeGone()
                            mFriendAdapter.setDataList(it.data.users.toMutableList())
                            mFriendAdapter.notifyDataSetChanged()
                            Log.e("ContactList",it.data.toString())
                        }
                        Toast.makeText(applicationContext,"Group list fetched",Toast.LENGTH_SHORT).show()
//                        val i = Intent(applicationContext, HomeActivity::class.java)
//                        startActivity(i)
//                        finish()
                    }
                }
            }
        })

        mViewModel.addGroup?.observe(this, Observer {
            when(it.status){
                Status.LOADING -> mViewModel.loader.postValue(true)
                Status.ERROR -> {
                    mViewModel.loader.postValue(false)
                    it.message?.let {msg->
                        Toast.makeText(this,msg.message,Toast.LENGTH_SHORT).show()
                    }?:Toast.makeText(this,"Something went wrong",Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS->{
                    mViewModel.loader.postValue(false)
                    if (it.data?.status == true){
                        Toast.makeText(applicationContext,"Group created successfully",Toast.LENGTH_SHORT).show()
                        finish()
                    }else{
                        Toast.makeText(this,"Something went wrong",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

    }

    override fun onBack() {
        finish()
    }

    override fun onSelectName(name: String, is_selected: Boolean, is_other: Boolean) {

    }

    override fun onEmptySearch(boolean: Boolean) {
        runOnUiThread {
            if (boolean){
                bindingObj.emptyChatTv.makeVisible()
            }else  bindingObj.emptyChatTv.makeGone()
        }
    }

    override fun setOtherFieldTextBreed(isOtherBreedText: String, user: User) {
      
    }

    override fun onSelection(item: User, position: Int) {

    }
    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size?> {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
}
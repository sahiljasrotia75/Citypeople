package com.citypeople.project.cameranew

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.*
import android.hardware.camera2.*
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.CamcorderProfile
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.util.*
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.legacy.app.FragmentCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.citypeople.project.*
import com.citypeople.project.R
import com.citypeople.project.adapters.UserPostMediaAdapter
import com.citypeople.project.adapters.UserPostMediaAdapter.ProfileMediaItemListener
import com.citypeople.project.databinding.FragmentCameraBasicBinding
import com.citypeople.project.models.signin.StoryModel
import com.citypeople.project.retrofit.Status
import com.citypeople.project.utilities.extensions.GridSpacingItemDecoration
import com.citypeople.project.utilities.extensions.isNetworkActiveWithMessage
import com.citypeople.project.viewmodel.StoryViewModel
import com.citypeople.project.views.FriendActivity
import com.citypeople.project.views.GroupActivity
import com.citypeople.project.views.StoryVideoActivity
import com.citypeople.project.views.VideoSendActivity
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.kaopiz.kprogresshud.KProgressHUD
import io.github.krtkush.lineartimer.LinearTimer
import io.github.krtkush.lineartimer.LinearTimerView
import org.json.JSONException
import org.json.JSONObject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Camera2BasicFragmentKt : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback,
    FragmentCompat.OnRequestPermissionsResultCallback, LinearTimer.TimerListener,
    ProfileMediaItemListener, UserListener {

    private val SENSOR_ORIENTATION_DEFAULT_DEGREES = 90
    private val SENSOR_ORIENTATION_INVERSE_DEGREES = 270
    private val DEFAULT_ORIENTATIONS = SparseIntArray()
    private val INVERSE_ORIENTATIONS = SparseIntArray()

    private val flash_state = 0
    private var _binding: FragmentCameraBasicBinding? = null
    val mViewModel by viewModel<StoryViewModel>()
    private var myCurrentLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var addressResultReceiver: LocationAddressResultReceiver? = null
    private var imgFriend: ImageView? = null
    private var imgGroup: ImageView? = null
    private var imgDelete: ImageView? = null
    private var imgChat: ImageView? = null
    private var mainConstraint1: ConstraintLayout? = null
    private var mAuth: FirebaseAuth? = null


    /**
     * Camera state: Waiting for the focus to be locked.
     */

    var mCameraLensFacingDirection = 0
    var picture: ImageView? = null
    var pictureBack: ImageView? = null
    private var currentLocation: TextView? = null
    var isSpeakButtonLongPressed = false
    private val stories = mutableListOf<StoryModel>()
    private val hashmapStories: HashMap<Int, MutableList<StoryModel>> = HashMap()
    var listStories = mutableListOf<Int>()
    var mProgressDialog: KProgressHUD? = null


    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    private val mSurfaceTextureListener: TextureView.SurfaceTextureListener =
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

    companion object {
        val VIDEO_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
        val REQUEST_VIDEO_PERMISSIONS = 1
        val LOCATION_PERMISSION_REQUEST_CODE = 2
        val REQUEST_CAMERA_PERMISSION = 1
        val TAG = "Camera2BasicFragmentKt"
        val STATE_PREVIEW = 0
        val FRAGMENT_DIALOG = "dialog"
        val STATE_WAITING_PRECAPTURE = 2
        val STATE_WAITING_NON_PRECAPTURE = 3
        val STATE_PICTURE_TAKEN = 4
        val MAX_PREVIEW_WIDTH = 1920
        val MAX_PREVIEW_HEIGHT = 1080
        val STATE_WAITING_LOCK = 1
        val ORIENTATIONS = SparseIntArray()
        fun newInstance(): Camera2BasicFragmentKt? {
            return Camera2BasicFragmentKt()
        }
    }


    /**
     * An [AutoFitTextureView] for camera preview.
     */
    private var mTextureView: AutoFitTextureView? = null
    private var blockView: LinearLayout? = null
    private var rvUserList: RecyclerView? = null

    private val camera_flash: ImageView? = null
    var startRecordingcalled = false

    private var linearTimer: LinearTimer? = null
    private var linearTimerback: LinearTimer? = null
    private var time: TextView? = null
    private var emptyChatTv: TextView? = null
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
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
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
            val activity: Activity? = activity
            activity?.finish()
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
    private var mFile: File? = null

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private val mOnImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            mBackgroundHandler!!.post(
                mFile?.let { Camera2BasicFragmentKt.ImageSaver(reader.acquireNextImage(), it) }!!
            )
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
    private lateinit var locationManager: LocationManager
    private lateinit var tvGpsLocation: TextView
    private val locationPermissionCode = 2

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
    private var currentLoc: String? = null
    private var mAdapter: UserPostMediaAdapter? = null

    private fun showToast(text: String) {
        val activity: Activity = requireActivity()
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun chooseOptimalSize(
        choices: Array<Size>, textureViewWidth: Int,
        textureViewHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size
    ): Size? {

        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = ArrayList()
        // Collect the supported resolutions that are smaller than the preview Surface
        val notBigEnough: MutableList<Size> = ArrayList()
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
            Collections.min(bigEnough, Camera2BasicFragmentKt.CompareSizesByArea())
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
        val bigEnough: MutableList<Size> = ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.height == option.width * h / w && option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }

        // Pick the smallest of those, assuming we found any
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough, Camera2BasicFragmentKt.CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            choices[0]
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCameraBasicBinding.inflate(inflater, container, false)
        // _binding!!.viewModel = viewModel
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        _binding?.listener = this
        val linearTimerView: LinearTimerView = view.findViewById(R.id.linearTimer)
        val linearTimerViewback: LinearTimerView = view.findViewById(R.id.linearTimerback)
        blockView = view.findViewById(R.id.blockView)
        rvUserList = view.findViewById(R.id.rvVideoList)
        time = view.findViewById(R.id.time)
        emptyChatTv = view.findViewById(R.id.emptyChatTv)
        picture = view.findViewById(R.id.picture)
        pictureBack = view.findViewById(R.id.pictureback)
        imgFriend = view.findViewById(R.id.img_friend)
        imgChat = view.findViewById(R.id.chat_od_logo)
        imgGroup = view.findViewById(R.id.img_group)
        imgDelete = view.findViewById(R.id.img_delete)
        currentLocation = view.findViewById(R.id.current_location)
        mainConstraint1 = view.findViewById(R.id.mainConstraint1)
        picture?.setOnLongClickListener(recordHoldListener)
        picture?.setOnTouchListener(recordTouchListener)
        pictureBack?.setOnLongClickListener(recordHoldListenerback)
        pictureBack?.setOnTouchListener(recordTouchListenerback)
        mTextureView = view.findViewById<View>(R.id.texture) as AutoFitTextureView

        addressResultReceiver = LocationAddressResultReceiver(Handler())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                myCurrentLocation = locationResult.locations[0]
                getAddress()
            }
        }
        startLocationUpdates()
        val duration = (10 * 1000).toLong()

        linearTimer = LinearTimer.Builder()
            .linearTimerView(linearTimerView)
            .duration(duration)
            .timerListener(this)
            .getCountUpdate(LinearTimer.COUNT_DOWN_TIMER, 1000)
            .build()

        linearTimerback = LinearTimer.Builder()
            .linearTimerView(linearTimerViewback)
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

        imgFriend?.setOnClickListener(View.OnClickListener {
            val i = Intent(activity, FriendActivity::class.java)
            startActivity(i)
        })

        imgGroup?.setOnClickListener(View.OnClickListener {
            val i = Intent(activity, GroupActivity::class.java)
            startActivity(i)
        })

        imgChat?.setOnClickListener(View.OnClickListener { view1: View? ->
            if (currentLoc == null) {
                Toast.makeText(
                    context,
                    "Please turn on your location",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val i =
                    Intent(activity, StoryVideoActivity::class.java)
                i.putExtra("currentLocation", currentLoc)
                startActivity(i)
            }
        })

        _binding?.swipeRefresh?.setOnRefreshListener {
            if (requireActivity().isNetworkActiveWithMessage()) {
                if (_binding?.swipeRefresh?.isRefreshing == true)
                    _binding?.swipeRefresh?.isRefreshing = false
                val currentUser = mAuth?.currentUser
                val jsonObject = JSONObject()
                try {
                    jsonObject.put("phone", currentUser!!.phoneNumber)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                listStories.clear()
                hashmapStories.clear()
                mViewModel.stories(jsonObject)
            } else _binding?.swipeRefresh?.isRefreshing = false
        }

        val wm = requireContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        val lp = blockView?.layoutParams as ConstraintLayout.LayoutParams
        lp.width = display.width
        lp.height = display.height - display.width * 16 / 9
        blockView?.requestLayout()
        setAdapter()
        apiObservers()
        val currentUser = mAuth?.currentUser
        val jsonObject = JSONObject()
        try {
            jsonObject.put("phone", currentUser!!.phoneNumber)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        mViewModel.stories(jsonObject)
    }


    private fun setAdapter() {
        // added data from arraylist to adapter class.
        mAdapter = UserPostMediaAdapter(this)
        // setting grid layout manager to implement grid view.
        // in this method '2' represents number of columns to be displayed in grid view.
        val layoutManager = GridLayoutManager(
            activity, 3
        )
        val spanCount = 3 //3 columns
        val spacing = 10//10 px
        val indulgeEdge = true

        rvUserList?.addItemDecoration(GridSpacingItemDecoration(spanCount, spacing, indulgeEdge))
        rvUserList!!.setHasFixedSize(true)
        // at last set adapter to recycler view.
        rvUserList?.layoutManager = layoutManager
        rvUserList?.adapter = mAdapter
    }

    private fun apiObservers() {
        mViewModel.storyList?.observe(requireActivity(), androidx.lifecycle.Observer { it ->
            when (it.status) {
                Status.LOADING -> {
                    mViewModel.loader.postValue(true)
                }
                Status.ERROR -> {
                    mViewModel.loader.postValue(false)
                    _binding?.swipeRefresh?.isRefreshing = false

                    it.message?.let { msg ->
                        Toast.makeText(requireActivity(), msg.message, Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(
                        requireActivity(),
                        "Something went wrong",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                Status.SUCCESS -> {
                    mViewModel.loader.postValue(false)
                    it.data?.apply {
                        it.data?.videos?.let { p ->
                            if (stories.size > 0) stories.clear()
                            stories?.addAll(it.data.videos)
                            if (stories.isNotEmpty()) {
                                //  mAdapter?.updateList(stories)
                                stories.forEach {
                                    var listOfData: ArrayList<StoryModel> = ArrayList()
                                    if (!hashmapStories.contains(it.user_id)) {
                                        listOfData.add(it)
                                        hashmapStories[it.user_id] = listOfData
                                        listStories.add(it.user_id)
                                    } else {
                                        listOfData =
                                            (hashmapStories[it.user_id] as ArrayList<StoryModel>?)!!
                                        listOfData.add(it)
                                        listOfData.reverse()
                                        hashmapStories[it.user_id] = listOfData
                                        Log.e("HashSize", hashmapStories[it.user_id].toString())
                                    }
                                }
                                _binding?.swipeRefresh?.isRefreshing = false
                                Log.e("HashSizeSend", hashmapStories.toString())
                                //  hashmapStories.values.reversed()
                                mAdapter?.setDataList(listStories.toMutableList(), hashmapStories)
                                mAdapter?.notifyDataSetChanged()
                                emptyChatTv!!.visibility = View.GONE
                            } else {
                                emptyChatTv!!.visibility = View.VISIBLE
                                Toast.makeText(requireActivity(), "No Data", Toast.LENGTH_SHORT)
                                    .show()
                            }

                        }

                    }
                }
            }
        })
    }

    private val recordHoldListener =
        View.OnLongClickListener { // Do something when your hold starts here.
            Log.d("things called", "onLongPressed")
            if (!startRecordingcalled) {
                startRecordingVideo()
                linearTimer!!.startTimer()
                _binding?.strokeView?.visibility=View.VISIBLE
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
                _binding?.strokeView?.visibility=View.GONE
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
                _binding?.strokeView?.visibility=View.VISIBLE

            }
            isSpeakButtonLongPressed = true
            true
        }
    private val recordTouchListenerback =
        View.OnTouchListener { pView, pEvent ->
            pView.onTouchEvent(pEvent)
            // We're only interested in when the button is released.
            if (pEvent.action == MotionEvent.ACTION_UP) {
                // We're only interested in anything if our speak button is currently pressed.
                if (isSpeakButtonLongPressed) {
                    Log.d("things called", "onTouch")
                    startRecordingcalled = false
                    // Do something when the button is released.
                    isSpeakButtonLongPressed = false
                    linearTimerback!!.pauseTimer()
                    linearTimerback!!.resetTimer()
                    _binding?.strokeView?.visibility=View.GONE
                    stopRecordingVideo()
                }
            }
            false
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mFile = File(requireActivity().getExternalFilesDir(null), "pic.jpg")
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun onResume() {
        super.onResume()
        startBackgroundThread()
        mCameraLensFacingDirection = 0
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView!!.isAvailable) {
            openCamera(mTextureView!!.width, mTextureView!!.height)
        } else {
            mTextureView!!.surfaceTextureListener = mSurfaceTextureListener
        }
        startLocationUpdates()
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            ConfirmationDialog().show(childFragmentManager, Camera2BasicFragmentKt.FRAGMENT_DIALOG)
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                Camera2BasicFragmentKt.REQUEST_CAMERA_PERMISSION
            )
        }
    }


    private fun shouldShowRequestPermissionRationale(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (shouldShowRequestPermissionRationale(permission)) {
                return true
            }
        }
        return false
    }


    private fun requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(Camera2BasicFragmentKt.VIDEO_PERMISSIONS)) {
            ConfirmationDialog().show(childFragmentManager, Camera2BasicFragmentKt.FRAGMENT_DIALOG)
        } else {
            requestPermissions(
                Camera2BasicFragmentKt.VIDEO_PERMISSIONS,
                Camera2BasicFragmentKt.REQUEST_VIDEO_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        if (requestCode == Camera2BasicFragmentKt.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(
                    activity,
                    "Location permission not granted, " + "restart the app if you want the feature",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        if (requestCode == Camera2BasicFragmentKt.REQUEST_CAMERA_PERMISSION) {
            if (grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            }
        }
        if (requestCode == Camera2BasicFragmentKt.REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.size == Camera2BasicFragmentKt.VIDEO_PERMISSIONS.size) {
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        break
                    }
                }
            } else {
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
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
        val activity: Activity? = activity
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
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
                    Camera2BasicFragmentKt.CompareSizesByArea()
                )
                mImageReader = ImageReader.newInstance(
                    largest.width, largest.height,
                    ImageFormat.JPEG,  /*maxImages*/2
                )
                mImageReader?.setOnImageAvailableListener(
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
                        Camera2BasicFragmentKt.TAG,
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
                if (maxPreviewWidth > Camera2BasicFragmentKt.MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = Camera2BasicFragmentKt.MAX_PREVIEW_WIDTH
                }
                if (maxPreviewHeight > Camera2BasicFragmentKt.MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = Camera2BasicFragmentKt.MAX_PREVIEW_HEIGHT
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

                // Check if the flash is supported.
                val available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                mFlashSupported = available ?: false
                mCameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Camera2BasicFragmentKt.ErrorDialog.newInstance(getString(R.string.camera_error))
                .show(childFragmentManager, Camera2BasicFragmentKt.FRAGMENT_DIALOG)
        }
    }

    private fun hasPermissionsGranted(permissions: Array<String>): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(requireContext(), permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Opens the camera specified by [Camera2BasicFragmentKt.mCameraId].
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun openCamera(width: Int, height: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestCameraPermission()
            return
        }
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        if (!hasPermissionsGranted(Camera2BasicFragmentKt.VIDEO_PERMISSIONS)) {
            requestVideoPermissions()
            return
        }
        val activity: Activity? = activity
        if (null == activity || activity.isFinishing) {
            return
        }
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            Log.d(Camera2BasicFragmentKt.TAG, "tryAcquire")
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
            mPreviewSize = chooseOptimalSize(
                map.getOutputSizes(
                    SurfaceTexture::class.java
                ),
                width, height, mVideoSize!!
            )
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
            manager.openCamera(mCameraId!!, mStateCallback, null)
        } catch (e: CameraAccessException) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show()
            activity.finish()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Camera2BasicFragmentKt.ErrorDialog.newInstance(getString(R.string.camera_error))
                .show(childFragmentManager, Camera2BasicFragmentKt.FRAGMENT_DIALOG)
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
                mCaptureSession?.close()
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
            throw java.lang.RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread?.start()
        mBackgroundHandler = mBackgroundThread?.looper?.let { Handler(it) }
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
            mPreviewRequestBuilder?.addTarget(surface)

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
                            mPreviewRequestBuilder?.set(
                                CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                                getRange()
                            ) //This line of code is used for adjusting the fps range and fixing the dark preview
                            mPreviewRequestBuilder?.set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AF_MODE_AUTO
                            )
                            // Flash is automatically enabled when necessary.
                            setFlash(mPreviewRequestBuilder!!)


                            // Finally, we start displaying the camera preview.
                            mPreviewRequest = mPreviewRequestBuilder?.build()
                            mCaptureSession?.setRepeatingRequest(
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
        val mCameraManager =
            requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
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
        val activity: Activity? = activity
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
            mState = Camera2BasicFragmentKt.STATE_WAITING_LOCK
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
            mState = Camera2BasicFragmentKt.STATE_WAITING_PRECAPTURE
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
            val activity: Activity? = activity
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
                        Log.d(Camera2BasicFragmentKt.TAG, mFile.toString())
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
        return (Camera2BasicFragmentKt.ORIENTATIONS[rotation] + mSensorOrientation + 270) % 360
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
            mState = Camera2BasicFragmentKt.STATE_PREVIEW
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

    override fun animationComplete() {
        try {
            Log.d("things called", "onTouch")
            startRecordingcalled = false
            // Do something when the button is released.
            isSpeakButtonLongPressed = false
            linearTimer!!.resetTimer()
            linearTimerback!!.resetTimer()
            _binding?.strokeView?.visibility=View.GONE
            stopRecordingVideo()

        } catch (e: Exception) {
            e.printStackTrace()
            stopRecordingVideo()
        }
    }

    override fun timerTick(tickUpdateInMillis: Long) {
        val formattedTime = String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(tickUpdateInMillis),
            TimeUnit.MILLISECONDS.toSeconds(tickUpdateInMillis)
                    - TimeUnit.MINUTES
                .toSeconds(TimeUnit.MILLISECONDS.toHours(tickUpdateInMillis))
        )

        time!!.text = formattedTime
    }

    override fun onTimerReset() {
        time!!.text = ""

    }

    override fun onMediaThumbnailClick(position: Int, storyModel: StoryModel?) {
        if (currentLoc == null) {
            Toast.makeText(
                context,
                "Please turn on your location",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val i = Intent(activity, StoryVideoActivity::class.java)
            i.putExtra("id", storyModel?.id)
            i.putExtra("userId", storyModel?.user_id)
            i.putExtra("currentLocation", currentLoc)
            startActivity(i)
        }


    }


    override fun onMentionedUserClick(username: String) {
        TODO("Not yet implemented")
    }

    override fun onEmptySearch(boolean: Boolean) {
        requireActivity().runOnUiThread {
            if (boolean) {
                _binding?.emptyChatTv?.makeVisible()
            } else _binding?.emptyChatTv?.makeGone()
        }
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

    //for video
    /**
     * Start the camera preview.
     */
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
            mPreviewBuilder?.addTarget(previewSurface)
            mCameraDevice!!.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mPreviewSession = session
                        updatePreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        val activity: Activity? = activity
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
    @Throws(
        IOException::class
    )
    private fun setUpMediaRecorder() {
        val activity = activity ?: return
        Log.d("things called", "setUpMediaRecording")
        val cpHigh = CamcorderProfile.get(CamcorderProfile.QUALITY_480P)
        mMediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mMediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath!!.isEmpty()) {
            mVideoPath = getActivity()?.let { getOutputMediaFileUriV(it) }
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
    //    private String getVideoFilePath(Context context) {
    //        final File dir = context.getExternalFilesDir(null);
    //        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
    //                + System.currentTimeMillis() + ".mp4";
    //    }
    /**
     * Create a file Uri for saving an image or video
     */
    private fun getOutputMediaFileUriV(context: Context): Uri? {
        return FileProvider.getUriForFile(
            requireActivity(), requireActivity().packageName + ".fileprovider",
            createVideoFile()!!
        )
    }

    private fun createVideoFile(): File? {
        // Check that the SDCard is mounted
        val mediaStorageDir = File(
            requireActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES),
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


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
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
            val surfaces: MutableList<Surface> = ArrayList()

            // Set up Surface for the camera preview
            val previewSurface = Surface(texture)
            surfaces.add(previewSurface)
            mPreviewBuilder!!.addTarget(previewSurface)
            val manager =
                requireActivity().getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristics = manager.getCameraCharacteristics(
                mCameraId!!
            )
            mPreviewBuilder!!.set(
                CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(
                    characteristics,
                    requireActivity().windowManager.defaultDisplay.rotation
                )
            )

            // Set up Surface for the MediaRecorder
            val recorderSurface = mMediaRecorder!!.surface
            surfaces.add(recorderSurface)
            mPreviewBuilder!!.addTarget(recorderSurface)
            mPreviewBuilder!!.set(
                CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(
                    characteristics,
                    requireActivity().windowManager.defaultDisplay.rotation
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
                        activity!!.runOnUiThread { // UI
                            // mButtonVideo.setText(R.string.stop);
                            mIsRecordingVideo = true

                            // Start recording
                            mMediaRecorder!!.start()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        val activity: Activity? = activity
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun closePreviewSession() {
        if (mPreviewSession != null) {
            mPreviewSession!!.close()
            mPreviewSession = null
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun stopRecordingVideo() {
        // UI
        mIsRecordingVideo = false

        // Stop recording
        try {
            mMediaRecorder!!.stop()
            mMediaRecorder!!.reset()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        val temp_path = mNextVideoAbsolutePath!!
        val activity: Activity? = activity
        if (null != activity) {
            Log.d(Camera2BasicFragmentKt.TAG, "Video saved: $mNextVideoAbsolutePath")
        }
        mNextVideoAbsolutePath = null
        startVideoCompress(temp_path)
        startPreview()
    }

    @SuppressLint("NewApi")
    private fun startVideoCompress(videoPath: String) {
        Log.w("startVideoCompress", videoPath)
        val uri = Uri.parse(videoPath).toString()
        if (currentLoc == null) {
            Toast.makeText(context, "Please turn on your location", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(context, VideoSendActivity::class.java)
            i.putExtra(Constants.INTENT_DATA, uri)
            i.putExtra("currentLocation", currentLoc)
            startActivity(i)
        }
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

    /**
     * Shows an error message dialog.
     */
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity: Activity? = activity
            return AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
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

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    class ConfirmationDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val parent = parentFragment
            return AlertDialog.Builder(activity)
                .setMessage(R.string.request_permission)
                .setPositiveButton(
                    android.R.string.ok
                ) { dialog, which ->
                    parent!!.requestPermissions(
                        arrayOf(Manifest.permission.CAMERA),
                        Camera2BasicFragmentKt.REQUEST_CAMERA_PERMISSION
                    )
                }
                .setNegativeButton(
                    android.R.string.cancel
                ) { dialog, which ->
                    val activity: Activity? = parent!!.activity
                    activity?.finish()
                }
                .create()
        }
    }

    private fun getAddress() {
        if (!Geocoder.isPresent()) {
            Toast.makeText(
                context, "Can't find current address, ",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val intent = Intent(
            requireContext(),
            GetAddressIntentService::class.java
        )
        intent.putExtra("add_receiver", addressResultReceiver)
        intent.putExtra("add_location", myCurrentLocation)
        requireContext().startService(intent)
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                Camera2BasicFragmentKt.LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            val locationRequest = LocationRequest()
            locationRequest.interval = 2000
            locationRequest.fastestInterval = 1000
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            fusedLocationClient!!.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    inner class LocationAddressResultReceiver internal constructor(handler: Handler?) :
        ResultReceiver(handler) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {
            if (resultCode == 0) {
                Log.d("Address", "Location null retrying")
                getAddress()
            }
            val currentAdd = resultData.getString("address_result")
            currentAdd?.let { showResults(it) }
        }
    }

    private fun showResults(currentAdd: String) {
        currentLoc = currentAdd
        currentLocation!!.text = currentLoc
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        //   mAdapter?.filter?.filter(s.toString())
    }

}

interface UserListener {
    fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int)
}
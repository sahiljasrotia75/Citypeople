package com.citypeople.project.views

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.hardware.Camera
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.citypeople.project.*
import com.citypeople.project.adapters.GroupListAdapter
import com.citypeople.project.adapters.VideoSendAdapter
import com.citypeople.project.databinding.ActivityVideoSendBinding
import com.citypeople.project.models.signin.User
import com.citypeople.project.retrofit.Status
import com.citypeople.project.utilities.ImagePickerUtils
import com.citypeople.project.utilities.common.BaseViewModel
import com.citypeople.project.viewmodel.VideoSendViewModel
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File
import java.io.IOException
import kotlin.math.abs


class VideoSendActivity : BaseActivity(), VideoSendListener,
    VideoSendAdapter.VideoSendItemListener {

    private var preview: SurfaceView? = null
    private var camera: Camera? = null
    private var cameraConfigured = false
    private var currentCameraId = 1
    lateinit var mAuth: FirebaseAuth
    val mViewModel by viewModel<VideoSendViewModel>()
    private var previewHolder: SurfaceHolder? = null
    lateinit var bindingObj: ActivityVideoSendBinding
    private var inPreview = false
    lateinit var mVideoAdapter: VideoSendAdapter
    lateinit var mGroupListAdapter: GroupListAdapter
    var contacts = hashMapOf<String, String>()
    var gg = ArrayList<String>()

    var recordedVideoPath: String? = ""
    var myLocation: String? = ""
    var recordedVideoFile: File? = null

    companion object {
        var context: Context? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingObj = DataBindingUtil.setContentView(this, R.layout.activity_video_send)
        bindingObj.listener = this
        context = this
        mAuth = FirebaseAuth.getInstance()
        //initilization()
        extractIntent()
        //initArray()
        getNumber(this.contentResolver)
        setAdapter()
        apiObserver()
    }

    override fun bindViewModel(): BaseViewModel {
        return mViewModel
    }

    private fun extractIntent() {
        if (intent?.extras == null || intent.extras?.containsKey(Constants.INTENT_DATA) == false) return
        if (intent.extras?.containsKey("currentLocation") == false) return

        if (intent.extras?.containsKey(Constants.INTENT_DATA) == true && intent.extras?.containsKey(
                "currentLocation"
            ) == true
        )
            recordedVideoPath = intent.extras?.getString(Constants.INTENT_DATA)
        myLocation = intent.extras?.getString("currentLocation")
        Log.e("MyLocation", myLocation.toString())


    }


    private fun initilization() {
        preview = findViewById<View>(R.id.front_cam) as SurfaceView
        previewHolder = preview?.holder
        preview!!.visibility = View.VISIBLE
        previewHolder?.addCallback(surfaceCallback)
        previewHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

    }

    @SuppressLint("Range")
    fun getNumber(cr: ContentResolver) {
        val phones =
            cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        // use the cursor to access the contacts
        while (phones!!.moveToNext()) {
            val name =
                phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
            // get display name
            var phoneNumber =
                phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            // get phone number
            phoneNumber =
                phoneNumber.replace("(", "").replace(")", "").replace("-", "").replace(" ", "")
            // get phone number
            contacts[phoneNumber] = name
        }
        getGroupFriend()
    }

    private fun getGroupFriend() {
        val currentUser = mAuth.currentUser
        val jsonObject = JSONObject()
        val list = ArrayList<String>(contacts.keys)
        //jsonObject.put("contacts", list)
        jsonObject.put("phone", currentUser?.phoneNumber)
        mViewModel.groupContacts(jsonObject)
    }

    fun apiObserver() {
        mViewModel.videoList?.observe(this, Observer {
            when (it.status) {
                Status.LOADING -> mViewModel.loader.postValue(true)
                Status.ERROR -> {
                    mViewModel.loader.postValue(false)
                    it.message?.let { msg ->
                        Toast.makeText(this, msg.message, Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    mViewModel.loader.postValue(false)
                    it.data?.apply {
                        mVideoAdapter.setDataList(it.data.users.toMutableList())
                        mVideoAdapter.notifyDataSetChanged()
                        Log.e("ContactList", it.data.toString())
                        //Toast.makeText(applicationContext,"Group list fetched",Toast.LENGTH_SHORT).show()

                    }
                }
            }
        })


        mViewModel.sendVideo?.observe(this, Observer {
            when (it.status) {
                Status.LOADING -> mViewModel.loader.postValue(true)
                Status.ERROR -> {
                    mViewModel.loader.postValue(false)
                    it.message?.let { msg ->
                        Toast.makeText(this, msg.message, Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                }
                Status.SUCCESS -> {
                    mViewModel.loader.postValue(false)
                    if (it.data?.status == true) {
                        Toast.makeText(
                            applicationContext,
                            "Video uploaded successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private var surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            // no-op -- wait until surfaceChanged()
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int, width: Int,
            height: Int
        ) {
            initPreview(width, height)
            startPreview()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // no-op
        }
    }

    override fun onResume() {
        super.onResume()
        initilization()
        camera = Camera.open(currentCameraId)
        try {
            camera?.setPreviewDisplay(previewHolder)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        startPreview()
        //FocusCamera()
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, w: Int, h: Int): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = h.toDouble() / w
        if (sizes == null) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
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

    private fun initPreview(width: Int, height: Int) {
        if (camera != null && previewHolder!!.surface != null) {
            try {
                camera?.stopPreview()
                camera?.setPreviewDisplay(previewHolder)
            } catch (t: Throwable) {
                Log.e("Preview:surfaceCallback", "Exception in setPreviewDisplay()", t)
                Toast.makeText(this, t.message, Toast.LENGTH_LONG).show()
            }
            if (!cameraConfigured) {
                val parameters = camera!!.parameters
                val sizes = parameters.supportedPreviewSizes
                if (sizes != null) {
                    val size = getOptimalPreviewSize(sizes, width, height)
                    parameters.setPreviewSize(size!!.width, size.height)
                    camera!!.parameters = parameters
                    camera!!.startPreview()
                    cameraConfigured = true
                }
            }

        }
    }

    private fun closest(of: Int, `in`: List<Int>): Int {
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

    private fun startPreview() {
        if (cameraConfigured && camera != null) {
            camera?.setDisplayOrientation(
                setCameraDisplayOrientation(
                    this,
                    currentCameraId,
                    camera!!
                )
            )
            camera?.startPreview()
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


    private fun setAdapter() {
        bindingObj.rvSendList.layoutManager = LinearLayoutManager(this)
        mVideoAdapter = VideoSendAdapter(this)
        bindingObj.rvSendList.adapter = mVideoAdapter
        mVideoAdapter.clearList()
        //  mVideoAdapter.setDataList(gg.toMutableList())
        bindingObj.rvSendList.addItemDecoration(
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
        //mGroupListAdapter.setDataList(gg.toMutableList())
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        mVideoAdapter.filter.filter(s.toString())
    }

    override fun onBack() {
        finish()
    }

    override fun sendVideo() {
        val groupList = arrayListOf<Int>()
        //groupList.add(16)
        groupList.addAll(
            mVideoAdapter.getCurrentItems().filter { it.isSelected && it.is_group == true }.map {
                it.id
            })
        val friendList = arrayListOf<Int>()
        //friendList.add(3)
        friendList.addAll(
            mVideoAdapter.getCurrentItems().filter { it.isSelected && it.is_group == false }.map {
                it.id
            })
        if (friendList.isNullOrEmpty() && groupList.isEmpty()) {
            Toast.makeText(this, "Your friend List is Empty", Toast.LENGTH_SHORT).show()
        } else {
            if (myLocation.isNullOrEmpty()) {
                Toast.makeText(this, "Please turn on your location", Toast.LENGTH_SHORT).show()
            } else {
                val currentUser = mAuth.currentUser
                val jsonObject = JSONObject()
                jsonObject.put("friends", friendList)
                jsonObject.put("groups", groupList)
                jsonObject.put("phone", currentUser?.phoneNumber)
                jsonObject.put("location", myLocation)

                val file =
                    File(recordedVideoPath)//ImagePickerUtils.getFilePathFromURI(this, Uri.parse(recordedVideoPath)))
                mViewModel.sendVideo(jsonObject, file)
            }

        }
    }

    override fun onSelectName(name: String, id: String, isGroup: Boolean) {
        if (gg.contains(name)) {
            val index = gg.indexOf(name)
            gg.removeAt(index)
        } else {
            gg.add(name)
        }
        mGroupListAdapter.updateData(gg)
        Log.e("userId", id.toString())

        val groupList = arrayListOf<Int>()
        val friendList = arrayListOf<Int>()
        if (isGroup) {
            groupList.add(id.toInt())
        } else {
            friendList.add(id.toInt())
        }
        if (myLocation.isNullOrEmpty()) {
            Toast.makeText(this, "Please turn on your location", Toast.LENGTH_SHORT).show()
        } else {
            val currentUser = mAuth.currentUser
            val jsonObject = JSONObject()
            jsonObject.put("friends", friendList)
            jsonObject.put("groups", groupList)
            jsonObject.put("phone", currentUser?.phoneNumber)
            jsonObject.put("location", myLocation)

            val file =
                File(recordedVideoPath)//ImagePickerUtils.getFilePathFromURI(this, Uri.parse(recordedVideoPath)))
            mViewModel.sendVideo(jsonObject, file)
        }


    }

    override fun onEmptySearch(boolean: Boolean) {
        runOnUiThread {
            if (boolean) {
                bindingObj.emptyChatTv.makeVisible()
            } else bindingObj.emptyChatTv.makeGone()
        }
    }

    override fun setOtherFieldTextBreed(isOtherBreedText: String, user: User) {

    }


}

interface VideoSendListener {
    fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int)
    fun onBack()
    fun sendVideo()
}
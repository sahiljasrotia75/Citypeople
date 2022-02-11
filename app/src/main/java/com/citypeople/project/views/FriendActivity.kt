package com.citypeople.project.views
// Add country code in phone contacts
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.hardware.Camera
import android.os.Bundle
import android.provider.ContactsContract.*
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
import com.citypeople.project.BaseActivity
import com.citypeople.project.adapters.FriendAdapter
import com.citypeople.project.R
import com.citypeople.project.databinding.ActivityFriendBinding
import com.citypeople.project.makeGone
import com.citypeople.project.makeVisible
import com.citypeople.project.models.signin.User
import com.citypeople.project.retrofit.Status
import com.citypeople.project.utilities.common.BaseViewModel
import com.citypeople.project.viewmodel.FriendViewModel
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.IOException

class FriendActivity : BaseActivity(), FriendListener, FriendAdapter.FriendItemListener {
    private var preview: SurfaceView? = null
    private var camera: Camera? = null
    private var cameraConfigured = false
    private var currentCameraId = 1
    private var previewHolder: SurfaceHolder? = null
    lateinit var bindingObj: ActivityFriendBinding
    private var inPreview = false
    var contacts= hashMapOf<String,String>()
    val mViewModel by viewModel<FriendViewModel>()
    lateinit var mFriendAdapter: FriendAdapter
    lateinit var mAuth: FirebaseAuth
    var aa = ArrayList<FriendModel>()
    var phone = ArrayList<FriendModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingObj = DataBindingUtil.setContentView(this, R.layout.activity_friend)
        bindingObj.listener = this
        mAuth = FirebaseAuth.getInstance()
        //initilization()
        getNumber(this.contentResolver)
        setAdapter()
        apiObserver()

    }

    override fun bindViewModel(): BaseViewModel {
        return mViewModel
    }

    private fun initilization() {
        preview = findViewById<View>(R.id.front_cam) as SurfaceView
        previewHolder = preview?.holder
        preview!!.visibility = View.VISIBLE
        previewHolder?.addCallback(surfaceCallback)
        previewHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

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
        }


    @SuppressLint("Range")
    fun getNumber(cr: ContentResolver) {
        val phones = cr.query(CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)
        // use the cursor to access the contacts

        while (phones!!.moveToNext()) {
           // val arrayNumber = FriendModel()
            val name = phones.getString(phones.getColumnIndex(CommonDataKinds.Phone.DISPLAY_NAME))
            // get display name
            var phone = phones.getString(phones.getColumnIndex(CommonDataKinds.Phone.NUMBER))
            phone = phone.replace("(","").replace(")","").replace("-","").replace(" ","")
            // get phone number
           contacts[phone]=name

        }
        getListOfUsers()
    }

    private fun getListOfUsers(){
        val currentUser = mAuth.currentUser
        val jsonObject = JSONObject()
        val list = ArrayList<String>(contacts.keys)
        jsonObject.put("contacts", list);
        jsonObject.put("phone", currentUser?.phoneNumber);
        mViewModel.contacts(jsonObject)
    }

    fun apiObserver(){
        mViewModel.contactsList?.observe(this, Observer {
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
                        it.data.users.map {
                            T ->
                            T.name = contacts[T.phone].toString()
                        }
                        if(it.data.users.isNullOrEmpty()){
                            bindingObj.emptyChatTv.makeVisible()
                        }else{
                            bindingObj.emptyChatTv.makeGone()
                            mFriendAdapter.setDataList(it.data.users.toMutableList())
                            mFriendAdapter.notifyDataSetChanged()
                            Log.e("ContactList",it.data.toString())
                        }
                        Toast.makeText(applicationContext,"Friend list fetched",Toast.LENGTH_SHORT).show()
//                        val i = Intent(applicationContext, HomeActivity::class.java)
//                        startActivity(i)
//                        finish()
                    }
                }
            }
        })

        mViewModel.addFriend?.observe(this, Observer {
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
                        Toast.makeText(applicationContext,"Friends updated",Toast.LENGTH_SHORT).show()
                        finish()
                    }else{
                        Toast.makeText(this,"Something went wrong",Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })

    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        mFriendAdapter.filter.filter(s.toString())
    }

    override fun addFriend() {
     val friendList = mFriendAdapter?.getCurrentItems()?.filter { it.isSelected}?.map {
           it.id
       }
        if (friendList.isNullOrEmpty()){
            Toast.makeText(this, "Your friend List is Empty", Toast.LENGTH_SHORT).show();
        }else{
            val currentUser = mAuth.currentUser
            val jsonObject = JSONObject()
            val list = ArrayList<Int>(friendList.toMutableList())
            jsonObject.put("ids", list);
            jsonObject.put("phone", currentUser?.phoneNumber);
            mViewModel.addFriend(jsonObject)
        }
        Log.e("mySeletedList",friendList.toString())
    }

    override fun onBack() {
        finish()
    }

    override fun onSelectName(name: String, is_selected: Boolean, is_other: Boolean) {
        //listener.onSelectName(name, is_selected, is_other)
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
        item.isSelected=!item.isSelected
        mFriendAdapter?.notifyItemChanged(position)
    }


}


interface FriendListener {
    fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int)
    fun addFriend()
    fun onBack()
}

class FriendModel() {
    var name: String = ""
    var isSelected = false
    var phone :String=""
}
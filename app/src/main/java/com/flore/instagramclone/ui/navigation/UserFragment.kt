package com.flore.instagramclone.ui.navigation

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.flore.instagramclone.ui.LoginActivity
import com.flore.instagramclone.ui.MainActivity
import com.flore.instagramclone.R
import com.flore.instagramclone.model.ContentDTO
import com.flore.instagramclone.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.android.synthetic.main.fragment_user.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UserFragment : Fragment() {
    var fragmentView: View? = null
    var mainContext : Activity? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var tempFile : File? = null

    companion object {
        var PICK_PROFILE_FROM_ALBUM = 10
        var PICK_IMAGE_FROM_CAMERA = 11
        val TAG = "UserFragment"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttach")
        if (context is Activity){
            mainContext = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("myUid") // 자신의 uid를 가져옴
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 로그아웃 하고 로그인 페이지로 이동
        fragmentView?.btn_account_follow_signout?.setOnClickListener {
            activity?.finish()
            val intent = Intent(activity, LoginActivity::class.java)
            startActivity(intent)
            auth?.signOut() // auth의 인자값에 있는 uid를 로그아웃 요청
        }

        // 리사이클러 뷰 설정
        fragmentView?.recyclerview_account?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.recyclerview_account?.layoutManager = GridLayoutManager(activity!!, 3) // 한 줄에 그리드 레이아웃 3개로 리사이클러뷰 설정

        // 자신의 프로필 이미지 설정
        getProfileImage()

        // 자신의 팔로우 확인
        getFollowerAndFollowing()

        // 프로필 이미지 변경 엑티비티 요청 (프로필 이미지 클릭 이벤트)
        fragmentView?.iv_account_profile?.setOnClickListener {
            val pictureArray = arrayOf("사진 촬영", "사진 선택")
            val alertDialog = AlertDialog.Builder(activity!!)
                .setItems(pictureArray, DialogInterface.OnClickListener { _, which ->
                    when (which) {
                        0 -> {
                            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            tempFile = createImageFile()

                            if (tempFile != null){
                                (activity as MainActivity).captureUri = FileProvider.getUriForFile(activity!!, "com.flore.instagramclone.provider", tempFile!!)
                                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, (activity as MainActivity).captureUri)
                                activity?.startActivityForResult(cameraIntent, PICK_IMAGE_FROM_CAMERA)
                            }
                        }

                        1 -> {
                            val photoPickerIntent = Intent(Intent.ACTION_PICK)
                            photoPickerIntent.type = "image/*"
                            activity?.startActivityForResult(
                                photoPickerIntent,
                                PICK_PROFILE_FROM_ALBUM
                            )
                        }
                    }
                })
            alertDialog.create()
            alertDialog.show()
        }

        return fragmentView // 프레그먼트 뷰 설정
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() { // 어댑터 설정
        private var myContentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")
                ?.whereEqualTo("uid", uid) // 자신의 uid 값을 참조해 images 컬렉션에 있는 모든 이미지를 가져옴
                ?.addSnapshotListener { querySnapshot, _ ->
                    // 리스트 초기화
                    myContentDTOs.clear()

                    if (querySnapshot == null) { // 오류 시
                        return@addSnapshotListener
                    }

                    for (snapshot in querySnapshot.documents) {
                        val item = snapshot.toObject(ContentDTO::class.java)
                        myContentDTOs.add(item!!) // 유저 데이터를 ContentDTO에 케스팅
                    }

                    // 포스팅 개수를 post_count text에 설정
                    fragmentView?.tv_account_post_count?.text = myContentDTOs.size.toString()

                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3 // 화면의 폭의 3분의 1 값 (한줄에 3개의 그리드 레이아웃이 들어가야 함)
            val imageView = ImageView(parent.context) // 새로운 이미지 뷰 설정
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width) // 정사각형 이미지 뷰의 크기 설정 (업로드한 이미지 한 개)

            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun getItemCount(): Int {
            return myContentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageview = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context)
                .load(myContentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageview)
        }
    }

    private fun createImageFile() : File? {
        val timeStamp : String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val path = activity!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
        val storageDir = File(path)

        if (!storageDir.exists()){
            storageDir.mkdirs()
        }
        try{
            val tempFile = File.createTempFile(imageFileName, ".jpg", storageDir)

            return tempFile
        } catch (e: IOException){
            e.printStackTrace()
        }

        return null
    }

    private fun getFollowerAndFollowing() {
        // 현재 시점에서 uid는 자기 자신의 uid임
        // 원격저장소에서 참조하는 것은 자신의 uid를 참조
        firestore?.collection("users")?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, _ ->
                if (documentSnapshot == null) {
                    return@addSnapshotListener
                }

                // followDTO 객체 컬렉션에 데이터 불러옴
                val followDTO = documentSnapshot.toObject(FollowDTO::class.java)

                // 팔로잉 카운터 가져오기
                if (followDTO?.followingCount != null) {
                    fragmentView?.tv_account_following_count?.text = followDTO.followingCount.toString()
                }

                // 팔로우 카운터 가져오기
                if (followDTO?.followerCount != null) {
                    fragmentView?.tv_account_follower_count?.text = followDTO.followerCount.toString()
                }
            }
    }

    // 자신 프로필 이미지 가져오기 (addSnapshotListener로 대기 상태)
    private fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)
            ?.addSnapshotListener { documentSnapshot, _ ->
                if (documentSnapshot == null) {
                    return@addSnapshotListener
                }
                if (documentSnapshot.data != null) {
                    val url = documentSnapshot.data!!["image"]
                    Glide.with(mainContext!!).load(url).apply(RequestOptions().circleCrop()).into(mainContext!!.iv_account_profile)
                }
            }
    }
}
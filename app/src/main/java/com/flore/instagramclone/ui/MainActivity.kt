package com.flore.instagramclone.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.flore.instagramclone.R
import com.flore.instagramclone.model.FollowDTO
import com.flore.instagramclone.ui.navigation.*
import com.flore.instagramclone.util.PermissionCheck
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    var captureUri: Uri? = null
    var imageUri: Uri? = null
    private val userP = PermissionCheck(this)
    private val permissionStrings = arrayOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottom_navigation.setOnNavigationItemSelectedListener(this)

        // 권한 체크
        userP.grantPer()

        // 디테일 뷰 프레그먼트를 메인화면으로 세팅
        bottom_navigation.selectedItemId = R.id.home_menu

        // FCM pushToken 세팅
        registerPushToken()

        // user document 존재 여부 확인
        userDocumentInit()
    }

    private fun userDocumentInit() {
        // 현재 로그인한 유저의 uid 값
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val fireStore = FirebaseFirestore.getInstance()

        val followCountRef = fireStore.collection("users").document(uid!!)

        followCountRef.get().addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot != null) {
                var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

                if (followDTO == null) {
                    followDTO = FollowDTO()
                    followDTO.followerCount = 0
                    followDTO.followers = HashMap()
                    followDTO.followingCount = 0
                    followDTO.followings = HashMap()

                    FirebaseFirestore.getInstance().collection("users").document(uid).set(followDTO)
                } else {
                    Log.d("MainActivity", "결과 데이터가 있음")
                }
            } else {
                Log.d("MainActivity", "결과 데이터가 없음")
            }
        }.addOnFailureListener { exception ->
            Log.d("MainActivity", "실패 : $exception")
        }
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        setToolbarDefault()

        when (p0.itemId) {
            R.id.home_menu -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_main, DetailViewFragment()).commit()
                return true
            }
            R.id.search_menu -> {
                supportFragmentManager.beginTransaction().replace(R.id.content_main, GridFragment())
                    .commit()
                return true
            }
            R.id.add_photo_menu -> {
                val pictureArray = arrayOf("사진 촬영", "사진 선택")
                val alertDialog = AlertDialog.Builder(this)
                    .setItems(pictureArray, DialogInterface.OnClickListener { dialog, which ->
                        when (which) {
                            0 -> {
                                if (TedPermission.isGranted(this, *permissionStrings)) {
                                    startActivity(Intent(this, AddPhotoActivity::class.java).apply
                                    { putExtra("menuSelect", "camera") })
                                } else {
                                    userP.grantPer()
                                }
                            }

                            1 -> {
                                if (TedPermission.isGranted(this, *permissionStrings)) {
                                    startActivity(Intent(this, AddPhotoActivity::class.java).apply
                                    { putExtra("menuSelect", "gallery") })
                                } else {
                                    userP.grantPer()
                                }
                            }
                        }
                    })
                alertDialog.create().show()
                return true
            }
            R.id.favorite_alarm_menu -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_main, AlarmFragment())
                    .commit()
                return true
            }

            R.id.account_menu -> {
                val userFragment = UserFragment()
                val bundle = Bundle()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("myUid", uid)
                userFragment.arguments = bundle

                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_main, userFragment)
                    .commit()
                return true
            }
        }
        return false
    }

    private fun setToolbarDefault() {
        toolbar_title_image.visibility = View.VISIBLE
    }

    // FCM Token 설정
    private fun registerPushToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val token = task.result
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            val map = mutableMapOf<String, Any>()
            map["pushToken"] = token!!

            FirebaseFirestore.getInstance().collection("pushtokens").document(uid!!).set(map)
        }
    }

    // 엑티비티 실행 후 결과 (자기 자신 프로필 이미지 변경에 의한 onActivityResult)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val uid = FirebaseAuth.getInstance().currentUser?.uid // 어짜피 자기 자신
        val storageRef =
            FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)

        if (requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            // FireStorage 이미지 저장
            storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                // 오류 예외 처리
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                // 업로드 후 결과 url 리턴
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                val map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }

        } else if (requestCode == UserFragment.PICK_IMAGE_FROM_CAMERA && resultCode == Activity.RESULT_OK) {
            imageUri = captureUri
            // FireStorage 이미지 저장
            storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                if (!task.isSuccessful) {
                    throw task.exception!!
                }
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                val map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid).set(map)
            }
        }
    }

}
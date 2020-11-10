package com.flore.instagramclone.ui

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.flore.instagramclone.R
import com.flore.instagramclone.navigation.*
import com.flore.instagramclone.navigation.util.PermissionCheck
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.gun0912.tedpermission.TedPermission
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    var captureUri: Uri? = null
    var imageUri: Uri? = null
    private val userP = PermissionCheck(this)
    private val permissionStrings = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

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
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        setToolbarDefault()
        when (p0.itemId) {
            R.id.home_menu -> {
                var detailViewFragment = DetailViewFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_main, detailViewFragment).commit()
                return true
            }
            R.id.search_menu -> {
                var gridFragment = GridFragment()
                supportFragmentManager.beginTransaction().replace(R.id.content_main, gridFragment)
                    .commit()
                return true
            }
            R.id.add_photo_menu -> {
                    var pictureArray = arrayOf("사진 촬영", "사진 선택")
                    val alertDialog = AlertDialog.Builder(this)
                        .setItems(pictureArray, DialogInterface.OnClickListener { dialog, which ->
                            when (which) {
                                0 -> {
                                    if (TedPermission.isGranted(this, *permissionStrings)){
                                        startActivity(Intent(this, AddPhotoActivity::class.java).apply
                                        { putExtra("menuSelect", "camera") })
                                    } else{
                                        userP.grantPer()
                                    }
                                }

                                1 -> {
                                    if (TedPermission.isGranted(this, *permissionStrings)){
                                        startActivity(Intent(this, AddPhotoActivity::class.java).apply
                                        { putExtra("menuSelect", "gallery") })
                                    } else{
                                        userP.grantPer()
                                    }
                                }
                            }
                        })
                alertDialog.create()
                alertDialog.show()
                return true
            }
            R.id.favorite_alarm_menu -> {
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_main, alarmFragment)
                    .commit()
                return true
            }
            R.id.account_menu -> {
                val userFragment = UserFragment()
                val bundle = Bundle()
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("destinationUid", uid)
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
        toolbar_username.visibility = View.GONE
        btn_toolbar_back.visibility = View.GONE
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

    // 엑티비티 실행 후 결과
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == UserFragment.PICK_PROFILE_FROM_ALBUM && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            var storageRef =
                FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)

            // FireStorage 이미지 저장
            storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                var map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid)
                    .set(map) // FireStore(DB) 쿼리 저장
            }
        } else if (requestCode == UserFragment.PICK_IMAGE_FROM_CAMERA && resultCode == Activity.RESULT_OK) {
            imageUri = captureUri
            var uid = FirebaseAuth.getInstance().currentUser?.uid
            var storageRef =
                FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)

            // FireStorage 이미지 저장
            storageRef.putFile(imageUri!!).continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                return@continueWithTask storageRef.downloadUrl
            }.addOnSuccessListener { uri ->
                var map = HashMap<String, Any>()
                map["image"] = uri.toString()
                FirebaseFirestore.getInstance().collection("profileImages").document(uid)
                    .set(map) // FireStore(DB) 쿼리 저장
            }
        }
    }

}
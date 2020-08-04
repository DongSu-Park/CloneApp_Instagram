package com.flore.instagramclone

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.flore.instagramclone.navigation.*
import com.flore.instagramclone.navigation.util.FcmPush
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottom_navigation.setOnNavigationItemSelectedListener(this)

        // 퍼미션 권한 요청 (스토리지 권한, 사진 촬영 권한)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            1
        )
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
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) { // 권한 흭득 성공 다이얼로그 화면 나타내기 (수정 필요)
                    var pictureArray = arrayOf("사진 촬영", "사진 선택")
                    val alertDialog = AlertDialog.Builder(this)
                        .setItems(pictureArray, DialogInterface.OnClickListener { dialog, which ->
                            when (which) {
                                0 -> {
                                    startActivity(
                                        Intent(
                                            this,
                                            AddPhotoActivity::class.java
                                        ).apply { putExtra("menuSelect", "camera") })
                                }

                                1 -> {
                                    startActivity(
                                        Intent(
                                            this,
                                            AddPhotoActivity::class.java
                                        ).apply { putExtra("menuSelect", "gallery") })
                                }
                            }
                        })
                    alertDialog.create()
                    alertDialog.show()

                } else {
                    Toast.makeText(this, "Please check the Permission!", Toast.LENGTH_LONG).show()
                }
                return true
            }

            R.id.favorite_alarm_menu -> {
                var alarmFragment = AlarmFragment()
                supportFragmentManager.beginTransaction().replace(R.id.content_main, alarmFragment)
                    .commit()
                return true
            }

            R.id.account_menu -> {
                var userFragment = UserFragment()
                var bundle = Bundle()
                var uid = FirebaseAuth.getInstance().currentUser?.uid
                bundle.putString("destinationUid", uid)
                userFragment.arguments = bundle

                supportFragmentManager.beginTransaction().replace(R.id.content_main, userFragment)
                    .commit()
                return true
            }
        }
        return false
    }

    fun setToolbarDefault() {
        toolbar_username.visibility = View.GONE
        btn_toolbar_back.visibility = View.GONE
        toolbar_title_image.visibility = View.VISIBLE
    }

    // FCM Token 설정
    fun registerPushToken() {
        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
            val token = task.result?.token
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
            var imageUri = data?.data
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
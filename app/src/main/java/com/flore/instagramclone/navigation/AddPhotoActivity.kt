package com.flore.instagramclone.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flore.instagramclone.R
import com.flore.instagramclone.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0 // 리퀘스트 번호
    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // Firebase 초기화
        storage = FirebaseStorage.getInstance() // Cloud Storage
        auth = FirebaseAuth.getInstance() // Personal Auth
        firestore = FirebaseFirestore.getInstance() // Cloud Store (DB)

        // 엑티비티 인텐트 실행하자마자 갤러리 화면이 열림
        var photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startActivityForResult(
            photoPickerIntent,
            PICK_IMAGE_FROM_ALBUM
        ) // 갤러리 화면을 보여줌 (Scoped Storage 적용)

        // 이미지 업로드 이벤트
        btn_addphoto_upload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM) {
            if (resultCode == Activity.RESULT_OK) { // 엑티비티 요청 및 결과가 성공적으로 끝났을 때
                // 이미지의 경로가 리턴
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)

            } else {
                // 이미지 선택 취소의 예외 상황
                finish()
            }
        }
    }

    private fun contentUpload() {
        // 업로드할 이미지의 메타 태그 작성
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "Image_" + timestamp + "_.png" // ex. Image_20200720_162710_.png

        var storageRef =
            storage?.reference?.child("images")?.child(imageFileName) // 파이어베이스 클라우드 스토리지 저장 경로

        // 업로드 이벤트 (Store에 업로드하고 해당 주소를 가져옴, 콜백 형식)
        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                var contentDTO = ContentDTO()

                contentDTO.imageUrl = uri.toString()
                contentDTO.uid = auth?.currentUser?.uid
                contentDTO.userId = auth?.currentUser?.email
                contentDTO.explain = addphoto_edit_explain.text.toString()
                contentDTO.timestamp = System.currentTimeMillis()

                var firestoreUpload = firestore?.collection("images")?.document()?.set(contentDTO)

                if (firestoreUpload != null) {
                    Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG)
                        .show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }
}

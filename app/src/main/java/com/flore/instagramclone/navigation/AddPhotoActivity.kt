package com.flore.instagramclone.navigation

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.flore.instagramclone.R
import com.flore.instagramclone.navigation.model.ContentDTO
import com.flore.instagramclone.navigation.util.UploadDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_add_photo.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest

class AddPhotoActivity : AppCompatActivity() {
    var PICK_IMAGE_FROM_ALBUM = 0 // 사진 앨범 엑티비티 리퀘스트
    var PICK_IMAGE_FROM_CAMERA = 1 // 카메라 촬영 엑티비티 리퀘스트
    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null
    var tempFile : File? = null
    var captureUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)

        // Firebase 초기화
        storage = FirebaseStorage.getInstance() // Cloud Storage
        auth = FirebaseAuth.getInstance() // Personal Auth
        firestore = FirebaseFirestore.getInstance() // Cloud Store (DB)

        var menu = intent.getStringExtra("menuSelect")

        when (menu){
            "camera" ->{
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                tempFile = createImageFile()

                if (tempFile != null){
                    // FileProvider를 통해 임시로 만들어진 jpg 파일에 이미지를 덮어 쓰기 (content Uri 리턴)
                    captureUri = FileProvider.getUriForFile(this, "com.flore.instagramclone.provider", tempFile!!)
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, captureUri)
                    startActivityForResult (cameraIntent, PICK_IMAGE_FROM_CAMERA)
                }
            }

            "gallery" ->{
                // 엑티비티 인텐트 실행하자마자 갤러리 화면이 열림
                var photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"
                startActivityForResult(
                    photoPickerIntent,
                    PICK_IMAGE_FROM_ALBUM
                )
            }
        }

        // 이미지 업로드 이벤트
        btn_addphoto_upload.setOnClickListener {
            contentUpload()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_FROM_ALBUM) { // 카메라 앨범 선택 후
            if (resultCode == Activity.RESULT_OK) {
                // 이미지의 경로가 리턴
                photoUri = data?.data
                addphoto_image.setImageURI(photoUri)

            } else {
                // 이미지 선택 취소의 예외 상황
                finish()
            }
        } else if (requestCode == PICK_IMAGE_FROM_CAMERA){ // 카메라 촬영 후
            if (resultCode == Activity.RESULT_OK){
                photoUri = captureUri
                addphoto_image.setImageURI(photoUri)
            } else{
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

        var uploadDialog = UploadDialog(this)
        uploadDialog.startLoadingDialog()

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
                    uploadDialog.dismissDialog()
                    Toast.makeText(this, getString(R.string.upload_success), Toast.LENGTH_LONG).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            }
        }
    }

    fun createImageFile(): File? { // 임시로 이미지 파일 추가
        val timeStamp : String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val path = getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.absolutePath
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
}

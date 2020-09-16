package com.flore.instagramclone.navigation.util

import android.content.Context
import android.widget.Toast
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission

class PermissionCheck (context : Context){
    var context : Context = context

    var permissionListener : PermissionListener = object : PermissionListener{
        override fun onPermissionGranted() {

        }

        override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            Toast.makeText(context, "권한을 얻지 못했습니다. 앱 권한에서 확인해주세요", Toast.LENGTH_LONG).show()
        }
    }

    fun grantPer() {
        TedPermission.with(context)
            .setPermissionListener(permissionListener)
            .setRationaleMessage("해당 앱의 기능을 사용하기 위해서 권한을 허용해야 합니다.")
            .setDeniedMessage("권한을 허용해야 앱의 기능이 정상적으로 실행 됩니다.")
            .setPermissions(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .check()
    }
}
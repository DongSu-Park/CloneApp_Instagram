package com.flore.instagramclone.navigation.util

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import com.flore.instagramclone.R

class UploadDialog {
    var activity : Activity? = null
    var alertDialog : AlertDialog? = null

    constructor(activity: Activity?) {
        this.activity = activity
    }


    fun startLoadingDialog(){
        val builder : AlertDialog.Builder? = AlertDialog.Builder(activity)
        val inflater : LayoutInflater? = activity!!.layoutInflater
        builder?.setView(inflater?.inflate(R.layout.custom_dialog,null))
        builder?.setCancelable(true)

        alertDialog = builder?.create()
        alertDialog?.show()
    }

    fun dismissDialog(){
        alertDialog?.dismiss()
    }
}
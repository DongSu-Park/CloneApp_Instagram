package com.flore.instagramclone.ui.navigation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.flore.instagramclone.R
import kotlinx.android.synthetic.main.activity_detail_image.*

class DetailImageActivity : AppCompatActivity(){
    private var imageUri : String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_image)
        imageUri = intent.getStringExtra("imageUri")

        iv_content_image.setOnClickListener {
            contentBoxVisible()
        }
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        setContent()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportFinishAfterTransition()
        finish()
    }

    private fun contentBoxVisible() {
        if (layout_content_box.visibility == View.VISIBLE){
            // 페이드 아웃 효과 및 가시성 GONE
            layout_content_box.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter(){
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        layout_content_box.visibility = View.GONE
                    }
                })
        } else {
            // 페이드 인 효과 및 가시성 VISIBLE
            layout_content_box.apply {
                alpha = 0f
                visibility = View.VISIBLE

                animate().alpha(1f).setDuration(200).setListener(null)
            }
        }
    }

    private fun setContent() {
        // 이미지 로딩
        Glide.with(this)
            .load(imageUri)
            .error(R.drawable.ic_baseline_error_24)
            .into(iv_content_image)
    }
}
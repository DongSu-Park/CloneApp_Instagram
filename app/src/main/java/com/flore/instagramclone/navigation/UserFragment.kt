package com.flore.instagramclone.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.flore.instagramclone.R
import com.flore.instagramclone.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    var fragmentView: View? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentView =
            LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fragmentView?.recyclerview_account?.adapter =
            UserFragmentRecyclerViewAdapter() // 리사이클러뷰 어댑터
        fragmentView?.recyclerview_account?.layoutManager =
            GridLayoutManager(activity!!, 3) // 한 줄에 그리드 레이아웃 3개로 리사이클러뷰 설정

        return fragmentView // 프레그먼트 뷰 설정
    }

    inner class UserFragmentRecyclerViewAdapter :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() { // 어댑터 설정
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (querySnapshot == null) { // 오류 시
                        return@addSnapshotListener
                    }

                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!) // 유저 데이터를 ContentDTO에 케스팅
                    }
                    fragmentView?.tv_account_post_count?.text =
                        contentDTOs.size.toString() // 유저 데이터의 개수 값을 post_count에 설정

                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width =
                resources.displayMetrics.widthPixels / 3 // 화면의 폭의 3분의 1 값 (한줄에 3개의 그리드 레이아웃이 들어가야 함)

            var imageView = ImageView(parent.context) // 새로운 이미지 뷰 설정
            imageView.layoutParams =
                LinearLayoutCompat.LayoutParams(width, width) // 정사각형 이미지 뷰의 크기 설정 (업로드한 이미지 한 개)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) :
            RecyclerView.ViewHolder(imageView) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageview)
        }

    }
}
package com.flore.instagramclone.navigation

import android.content.Intent
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
import com.flore.instagramclone.LoginActivity
import com.flore.instagramclone.MainActivity
import com.flore.instagramclone.R
import com.flore.instagramclone.navigation.model.ContentDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.view.*

class UserFragment : Fragment() {
    var fragmentView: View? = null
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    var auth: FirebaseAuth? = null
    var currentUserUid: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid") // 상대방 또는 자신의 uid 가져옴
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid // 현재 로그인한 유저의 uid를 가져옴

        if (uid == currentUserUid) { // 자신의 uid와 가져온 uid 값이 일치하면 자신 페이지로 이동
            fragmentView?.btn_account_follow_signout?.text = getString(R.string.signout) // 자신 페이지의 경우 버튼을 "Signout"
            fragmentView?.btn_account_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java)) // 현재 엑티비티를 종료하고 로그인 엑티비티로 이동
                auth?.signOut() // auth의 인자값에 있는 uid를 로그아웃 요청
            }
        } else { // 다른 경우 상대방의 유저페이지 나타내기
            fragmentView?.btn_account_follow_signout?.text = getString(R.string.follow) // 상대 페이지의 경우 버튼을 "follow"
            var mainactivity = (activity as MainActivity)
            mainactivity?.toolbar_username?.text = arguments?.getString("userId") // 로그인한 유저 id를 가져와서 텍스트 세팅
            mainactivity?.btn_toolbar_back?.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId = R.id.home_menu // 뒤로가기 버튼을 누르면 홈 메뉴로 이동하는 이벤트로 설정
            }
            // 유저 상세 페이지에는 타이틀 이미지를 없에고 back 버튼과 해당 유저 id만 나타나게 함.
            mainactivity?.toolbar_title_image?.visibility = View.GONE
            mainactivity?.toolbar_username?.visibility = View.VISIBLE
            mainactivity?.btn_toolbar_back?.visibility = View.VISIBLE
        }

        fragmentView?.recyclerview_account?.adapter = UserFragmentRecyclerViewAdapter() // 리사이클러뷰 어댑터
        fragmentView?.recyclerview_account?.layoutManager = GridLayoutManager(activity!!, 3) // 한 줄에 그리드 레이아웃 3개로 리사이클러뷰 설정

        return fragmentView // 프레그먼트 뷰 설정
    }

    inner class UserFragmentRecyclerViewAdapter :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() { // 어댑터 설정
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")
                ?.whereEqualTo("uid", uid) // uid 값에 따라 자신 또는 상대방의 firestore 참조
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    if (querySnapshot == null) { // 오류 시
                        return@addSnapshotListener
                    }

                    for (snapshot in querySnapshot.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!) // 유저 데이터를 ContentDTO에 케스팅
                    }
                    fragmentView?.tv_account_post_count?.text = contentDTOs.size.toString() // 유저 데이터의 개수 값을 post_count에 설정

                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3 // 화면의 폭의 3분의 1 값 (한줄에 3개의 그리드 레이아웃이 들어가야 함)

            var imageView = ImageView(parent.context) // 새로운 이미지 뷰 설정
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width) // 정사각형 이미지 뷰의 크기 설정 (업로드한 이미지 한 개)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView) {

        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageview)
        }

    }
}
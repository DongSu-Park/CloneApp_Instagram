package com.flore.instagramclone.navigation

import android.content.Intent
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.flore.instagramclone.LoginActivity
import com.flore.instagramclone.MainActivity
import com.flore.instagramclone.R
import com.flore.instagramclone.navigation.model.ContentDTO
import com.flore.instagramclone.navigation.model.FollowDTO
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
    companion object{
        var PICK_PROFILE_FROM_ALBUM = 10
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        fragmentView = LayoutInflater.from(activity).inflate(R.layout.fragment_user, container, false)
        uid = arguments?.getString("destinationUid") // 상대방 또는 자신의 uid 가져옴
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid // 현재 로그인한 유저의 uid를 가져옴


        if (uid == currentUserUid) { // 자신의 uid와 가져온 uid 값이 일치하면 자신 페이지로 이동
            fragmentView?.btn_account_follow_signout?.text = getString(R.string.signout)
            fragmentView?.btn_account_follow_signout?.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java)) // 현재 엑티비티를 종료하고 로그인 엑티비티로 이동
                auth?.signOut() // auth의 인자값에 있는 uid를 로그아웃 요청
            }
        } else { // 다른 경우 상대방의 유저페이지 나타내기
            fragmentView?.btn_account_follow_signout?.text = getString(R.string.follow)
            var mainactivity = (activity as MainActivity)
            mainactivity?.toolbar_username?.text = arguments?.getString("userId")

            // 유저 상세 페이지에는 타이틀 이미지를 없에고 back 버튼과 해당 유저 id만 나타나게 함.
            setToolbarUpdate(mainactivity)

            mainactivity?.btn_toolbar_back?.setOnClickListener {
                mainactivity.bottom_navigation.selectedItemId = R.id.home_menu
            }

            // follow 버튼
            fragmentView?.btn_account_follow_signout?.setOnClickListener {
                requestFollow()
            }
        }

        fragmentView?.recyclerview_account?.adapter = UserFragmentRecyclerViewAdapter()
        fragmentView?.recyclerview_account?.layoutManager = GridLayoutManager(activity!!, 3) // 한 줄에 그리드 레이아웃 3개로 리사이클러뷰 설정

        // 자신의 프로필 이미지 설정
        getProfileImage()

        // 프로필 이미지 변경 엑티비티 요청 (프로필 이미지 클릭 이벤트)
        fragmentView?.iv_account_profile?.setOnClickListener {
            var photoPickerIntent = Intent(Intent.ACTION_PICK)
            photoPickerIntent.type = "image/*"
            activity?.startActivityForResult(photoPickerIntent, PICK_PROFILE_FROM_ALBUM)
        }

        // 팔로우 확인
        getFollowerAndFollowing()

        return fragmentView // 프레그먼트 뷰 설정
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() { // 어댑터 설정
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

    fun requestFollow(){
        var tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if (followDTO.followings.containsKey(uid)){
               // 팔로워를 누른 상태이면 지우면 됨
                followDTO?.followingCount = followDTO?.followingCount - 1
                followDTO?.followings?.remove(uid)
            } else{
                followDTO?.followingCount = followDTO?.followingCount + 1
                followDTO?.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing,followDTO)
            return@runTransaction
        }

        var tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null){
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if (followDTO!!.followers.containsKey(currentUserUid)){
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)
            } else {
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    fun getFollowerAndFollowing(){
        firestore?.collection("users")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null){
                return@addSnapshotListener
            }
            var followDTO = documentSnapshot.toObject(FollowDTO::class.java)
            if (followDTO?.followingCount != null){
                fragmentView?.tv_account_following_count?.text = followDTO?.followingCount?.toString()
            }

            if (followDTO?.followerCount != null){
                fragmentView?.tv_account_follower_count?.text = followDTO?.followerCount?.toString()
                if (followDTO?.followers?.containsKey(currentUserUid!!)){

                    fragmentView?.btn_account_follow_signout?.text = getString(R.string.follow_cancel)

                    // BlendModeColorFilter 사용 (setColorFilter Deprecated 됨)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        fragmentView?.btn_account_follow_signout?.background?.colorFilter =
                            BlendModeColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), BlendMode.MULTIPLY)
                    } else {
                        fragmentView?.btn_account_follow_signout?.background?.setColorFilter(ContextCompat.getColor(activity!!, R.color.colorLightGray), PorterDuff.Mode.MULTIPLY)
                    }

                } else{
                    if (uid != currentUserUid){
                        fragmentView?.btn_account_follow_signout?.text = getString(R.string.follow)
                        fragmentView?.btn_account_follow_signout?.background?.colorFilter = null
                    }
                }
            }
        }
    }

    fun getProfileImage(){
        firestore?.collection("profileImages")?.document(uid!!)?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (documentSnapshot == null){
                return@addSnapshotListener
            }

            if (documentSnapshot.data != null){
                var url = documentSnapshot.data!!["image"]
                Glide.with(activity!!).load(url).apply(RequestOptions().circleCrop()).into(fragmentView?.iv_account_profile!!)
            }
        }
    }

    fun setToolbarUpdate(mainactivity: MainActivity) {
        mainactivity.toolbar_title_image?.visibility = View.GONE
        mainactivity.toolbar_username?.visibility = View.VISIBLE
        mainactivity.btn_toolbar_back?.visibility = View.VISIBLE
    }
}
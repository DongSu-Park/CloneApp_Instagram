package com.flore.instagramclone.ui.navigation

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.flore.instagramclone.R
import com.flore.instagramclone.model.AlarmDTO
import com.flore.instagramclone.model.ContentDTO
import com.flore.instagramclone.model.FollowDTO
import com.flore.instagramclone.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_others_user.*
import kotlin.collections.ArrayList

class OthersUserActivity : AppCompatActivity() {
    var firestore: FirebaseFirestore? = null
    var auth: FirebaseAuth? = null
    var myUid: String? = null
    var otherUid: String? = null
    var otherUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_others_user)

        // 상대의 uid, id 정보 가져오기
        myUid = intent.getStringExtra("myUid") // 현재 로그인 한 대상인의 uid 값
        otherUid = intent.getStringExtra("otherUid") // 해당 리스트 대상인의 uid 값
        otherUserId = intent.getStringExtra("otherUserId") // 해당 리스트 대상인의 id 값

        // firebase init
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // 상대의 프로필 이미지 가져오기
        getProfileImage()

        // 상대의 팔로우 팔로워 가져오기
        getFollowerAndFollowing()

        // 팔로우 버튼을 눌렀을 경우
        btn_others_follow_signout.setOnClickListener {
            requestFollow()
        }

        // 리사이클러뷰 초기화
        recyclerview_others.adapter = OthersUserRecyclerViewAdapter()
        recyclerview_others.layoutManager = GridLayoutManager(this, 3)
    }

    private fun getProfileImage() {
        firestore?.collection("profileImages")?.document(otherUid!!)
            ?.addSnapshotListener { documentSnapshot, _ ->
                if (documentSnapshot == null) {
                    return@addSnapshotListener
                }

                if (documentSnapshot.data != null) {
                    val url = documentSnapshot.data!!["image"]
                    Glide.with(this).load(url)
                        .error(R.drawable.ic_account)
                        .apply(RequestOptions().circleCrop())
                        .into(iv_others_profile)
                }
            }
    }

    // 리사이클러뷰 세팅
    inner class OthersUserRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var othersContentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")
                ?.whereEqualTo("uid", otherUid)
                ?.addSnapshotListener { querySnapshot, _ ->
                    othersContentDTOs.clear()

                    if (querySnapshot == null) {
                        return@addSnapshotListener
                    }

                    for (snapshot in querySnapshot.documents) {
                        val item = snapshot.toObject(ContentDTO::class.java)
                        othersContentDTOs.add(item!!)
                    }

                    tv_others_post_count.text = othersContentDTOs.size.toString()

                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            // 동적 화면 폭의 1/3 값
            val width = resources.displayMetrics.widthPixels / 3
            val imageView = ImageView(this@OthersUserActivity)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)

            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) : RecyclerView.ViewHolder(imageView)

        override fun getItemCount(): Int {
            return othersContentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageview = (holder as CustomViewHolder).imageView
            Glide.with(holder.imageView.context)
                .load(othersContentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageview)
        }
    }

    // 팔로우 요청
    private fun requestFollow() {
        val tsDocFollowing = firestore?.collection("users")?.document(myUid!!)
        firestore?.runTransaction { transaction ->
            // 팔로우 트랜젝션 실행
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)

            // 최초 회원 팔로잉 이벤트의 경우 document 레코드 추가
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followingCount = 1
                followDTO!!.followings[otherUid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            // 이미 당사자에게 팔로워 되어 있는 경우와 그렇지 않는 경우
            if (followDTO.followings.containsKey(otherUid)) {
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(otherUid)
            } else {
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[otherUid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        // 팔로워 트랜젝션 실행
        val tsDocFollower = firestore?.collection("users")?.document(otherUid!!)
        firestore?.runTransaction { transition ->
            var followDTO = transition.get(tsDocFollower!!).toObject(FollowDTO::class.java)

            // 최초 회원의 팔로워 이벤트의 경우 document 레코드 추가
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[myUid!!] = true
                followerAlarm(otherUid!!)

                transition.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            // 이미 팔로워를 하는 경우와 그렇지 않는 경우
            if (followDTO!!.followers.containsKey(myUid)) {
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(myUid)
            } else {
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[myUid!!] = true
                // FCM 알람 추가
                followerAlarm(otherUid!!)
            }
            transition.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    // 당사자에게 팔로워 알람 메세지 보내기 (FCM)
    private fun followerAlarm(destinationUid: String) {
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        val message = "${auth?.currentUser?.email} ${getString(R.string.alarm_follow)}"
        FcmPush.instance.sendMessage(destinationUid, "InstagramClone", message)
    }

    // 팔로워, 팔로잉 개수 가져오기
    private fun getFollowerAndFollowing() {
        firestore?.collection("users")?.document(otherUid!!)
            ?.addSnapshotListener { documentSnapshot, _ ->
                if (documentSnapshot == null) {
                    return@addSnapshotListener
                }
                val followDTO = documentSnapshot.toObject(FollowDTO::class.java)

                // 팔로잉 카운터 가져오기
                if (followDTO?.followingCount != null) {
                    tv_others_following_count.text = followDTO.followingCount.toString()
                }

                // 팔로우 카운터 가져오기
                if (followDTO?.followerCount != null) {
                    tv_others_follower_count.text = followDTO.followerCount.toString()
                }

                // 팔로우 상태인지 아닌지에 따라 팔로우 버튼의 글자 및 색상이 달라짐
                if (followDTO!!.followers.containsKey(myUid)) {
                    // 팔로우 되어 있는 상태일 경우
                    btn_others_follow_signout.text = getString(R.string.follow_cancel)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        btn_others_follow_signout.background.colorFilter =
                            BlendModeColorFilter(
                                ContextCompat.getColor(this, R.color.colorLightGray),
                                BlendMode.MULTIPLY
                            )
                    } else {
                        @Suppress("DEPRECATION")
                        btn_others_follow_signout.background.setColorFilter(
                            ContextCompat.getColor(this, R.color.colorLightGray),
                            PorterDuff.Mode.MULTIPLY
                        )
                    }
                } else {
                    btn_others_follow_signout.text = getString(R.string.follow)
                    btn_others_follow_signout.background.colorFilter = null
                }
            }
    }
}
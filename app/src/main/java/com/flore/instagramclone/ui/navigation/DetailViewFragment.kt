package com.flore.instagramclone.ui.navigation

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.flore.instagramclone.R
import com.flore.instagramclone.model.AlarmDTO
import com.flore.instagramclone.model.ContentDTO
import com.flore.instagramclone.util.FcmPush
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {
    var firestore: FirebaseFirestore? = null // DB 내용에 담긴 데이터를 가져오기 위해 사용
    var uid: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid // 유저 ID

        view.detailviewfragment_recycleview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recycleview.layoutManager = LinearLayoutManager(activity)

        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var contentDTOs: ArrayList<ContentDTO> = arrayListOf() // 컨텐츠 내용 리스트
        private var contentUidList: ArrayList<String> = arrayListOf() // Uid 내용 리스트

        init { // 생성자
            firestore?.collection("images")?.orderBy("timestamp", Query.Direction.DESCENDING)
                ?.addSnapshotListener { querySnapshot, _ ->
                    contentDTOs.clear() // 리스트 초기화
                    contentUidList.clear() // 리스트 초기화
                    if (querySnapshot == null) { // 로그아웃된 경우 null이 발생하기 때문에 이에 대한 exception 처리
                        return@addSnapshotListener
                    }

                    for (snapshot in querySnapshot.documents) {
                        val item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!) // 이미지 컨텐츠 내용 추가
                        contentUidList.add(snapshot.id) // Uid 내용 추가
                    }

                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewHolder = (holder as CustomViewHolder).itemView

            // 유저 id
            viewHolder.detailviewitem_profile_textview.text = contentDTOs[position].userId

            // 유저 자신의 게시글 삭제 버튼 활성화 여부
            if (uid == contentDTOs[position].uid) {
                viewHolder.detailviewitem_delete_imageview.visibility = View.VISIBLE
            } else {
                viewHolder.detailviewitem_delete_imageview.visibility = View.GONE
            }

            // 삭제 버튼 클릭 (서버, 로컬 컬렉션 삭제)
            viewHolder.detailviewitem_delete_imageview.setOnClickListener {
                val alertDialog = AlertDialog.Builder(holder.itemView.context)
                alertDialog.apply {
                    setTitle("컨텐츠 삭제 확인")
                    setMessage("해당 게시글을 삭제하겠습니까?")
                    setPositiveButton("예") { _, _ ->
                        // 해당 게시글 삭제
                        // 서버 내역 삭제
                        deleteContent(holder.itemView.context, contentDTOs[position].imageUrl)
                        // 로컬 내역 삭제
                        contentDTOs.removeAt(position)
                        contentUidList.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, contentDTOs.size)
                    }
                    setNegativeButton("아니오", null)
                }.create().show()
            }

            // 이미지 로딩 (Glide)
            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .error(R.drawable.ic_baseline_error_24)
                .override(410, 250)
                .into(viewHolder.detailviewitem_imageview_content)

            // 이미지 클릭 시 크게 보기 화면으로 인텐트
            viewHolder.detailviewitem_imageview_content.setOnClickListener {
                val intent = Intent(activity, DetailImageActivity::class.java)
                intent.putExtra("imageUri", contentDTOs[position].imageUrl)

                // 전환 애니메이션 도입
                val pairImage = Pair.create(viewHolder.detailviewitem_imageview_content as View, viewHolder.detailviewitem_imageview_content.transitionName)
                val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), pairImage)

                startActivity(intent, optionsCompat.toBundle())
            }

            // 이미지 설명
            viewHolder.detailviewitem_explain_textview.text = contentDTOs[position].explain

            // 좋아요 내용 표시
            viewHolder.detailviewitem_favoritecounter_textview.text =
                "Likes : ${contentDTOs[position].favoriteCount}"

            // 유저 프로필 이미지 로딩 (Glide)
            FirebaseFirestore.getInstance()
                .collection("profileImages")
                .document(contentDTOs[position].uid!!)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result!!["image"]
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .error(R.drawable.ic_account)
                            .apply(RequestOptions().circleCrop())
                            .into(viewHolder.detailviewitem_profile_image)
                    }
                }

            // 좋아요 버튼 클릭 이벤트
            viewHolder.detailviewitem_favorite_imageview.setOnClickListener {
                favoriteEvent(position)
            }

            // 좋아요 버튼 클릭 후 이미지 변경
            if (contentDTOs[position].favorites.containsKey(uid)) {
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite)
            } else {
                viewHolder.detailviewitem_favorite_imageview.setImageResource(R.drawable.ic_favorite_border)
            }

            // 프로필 이미지 클릭 이벤트
            viewHolder.detailviewitem_profile_image.setOnClickListener {
                // uid는 자기 자신, contentDTOs에 있는 uid는 자기자신 또는 타인
                if (uid == contentDTOs[position].uid){
                    // 자신의 uid의 경우 자신의 유저 상세 프레그먼트로 이동
                    val fragment = UserFragment()
                    val bundle = Bundle()
                    bundle.putString("myUid", contentDTOs[position].uid)
                    bundle.putString("myUserId", contentDTOs[position].userId)
                    fragment.arguments = bundle
                    activity?.supportFragmentManager?.beginTransaction()
                        ?.replace(R.id.content_main, fragment)?.commit()
                } else{
                    // 타인의 uid의 경우 타인의 유저 상세 엑티비티로 이동 (테스트 필요)
                    val intent = Intent(activity, OthersUserActivity::class.java)
                    intent.putExtra("myUid", uid)
                    intent.putExtra("otherUid", contentDTOs[position].uid)
                    intent.putExtra("otherUserId", contentDTOs[position].userId)
                    startActivity(intent)
                }
            }

            // 댓글 클릭 이벤트
            viewHolder.detailviewitem_comment_imageview.setOnClickListener { v ->
                val intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        private fun deleteContent(context: Context, imageUrl: String?) {
            firestore!!.collection("images").whereEqualTo("imageUrl", imageUrl).get()
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        // 원격저장소 삭제
                        firestore!!.collection("images").document(it.result.documents[0].id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "해당 게시글은 삭제되었습니다.", Toast.LENGTH_LONG)
                                    .show()
                            }
                    }
                }
        }

        private fun favoriteEvent(position: Int) { // 좋아요 표시 이벤트
            val tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->

                val contentDTO = transaction.get(tsDoc!!)
                    .toObject(ContentDTO::class.java) // 파이어베이스에 있는 content 데이터를 ContentDTO로 케스팅

                if (contentDTO!!.favorites.containsKey(uid)) { // 좋아요 버튼이 이미 클릭 되어 있을 경우
                    contentDTO.favoriteCount = contentDTO.favoriteCount!! - 1
                    contentDTO.favorites.remove(uid) // uid 값 삭제
                } else {
                    contentDTO.favoriteCount = contentDTO.favoriteCount!! + 1
                    contentDTO.favorites[uid!!] = true // uid 값 hashMap에 리스트로 저장
                    favoriteAlarm(contentDTOs[position].uid!!)
                }
                transaction.set(tsDoc, contentDTO) // 트렌젝션을 종료하고 변경된 값을 온라인에서도 변경
            }
        }

        private fun favoriteAlarm(destinationUid: String?) {
            val alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

            val message =
                FirebaseAuth.getInstance().currentUser?.email + getString(R.string.alarm_favorite)
            FcmPush.instance.sendMessage(destinationUid!!, "InstagramClone", message)
        }
    }
}
package com.flore.instagramclone.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.flore.instagramclone.R
import com.flore.instagramclone.navigation.model.ContentDTO
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_detail.view.*
import kotlinx.android.synthetic.main.item_detail.view.*

class DetailViewFragment : Fragment() {
    var firestore: FirebaseFirestore? = null // DB 내용에 담긴 데이터를 가져오기 위해 사용
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var view = LayoutInflater.from(activity).inflate(R.layout.fragment_detail, container, false)
        firestore = FirebaseFirestore.getInstance()

        view.detailviewfragment_recycleview.adapter = DetailViewRecyclerViewAdapter()
        view.detailviewfragment_recycleview.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = arrayListOf() // 컨텐츠 내용 리스트
        var contentUidList: ArrayList<String> = arrayListOf() // Uid 내용 리스트

        init { // 생성자
            firestore?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { value, error ->
                    contentDTOs.clear() // 리스트 초기화
                    contentUidList.clear() // 리스트 초기화

                    for (snapshot in value!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!) // 이미지 컨텐츠 내용 추가
                        contentUidList.add(snapshot.id) // Uid 내용 추가
                    }

                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_detail, parent, false)
            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as CustomViewHolder).itemView
            // 유저 id
            viewHolder.detailviewitem_profile_textview.text = contentDTOs!![position].userId

            // 이미지 로딩 (Glide)
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl)
                .into(viewHolder.detailviewitem_imageview_content)

            // 이미지 설명
            viewHolder.detailviewitem_explain_textview.text = contentDTOs!![position].explain

            // 좋아요 표시
            viewHolder.detailviewitem_favoritecounter_textview.text =
                "Likes " + contentDTOs!![position].favoriteCount

            // 유저 프로필 이미지 로딩 (Glide)
            Glide.with(holder.itemView.context).load(contentDTOs!![position].imageUrl)
                .into(viewHolder.detailviewitem_profile_image)
        }

    }
}
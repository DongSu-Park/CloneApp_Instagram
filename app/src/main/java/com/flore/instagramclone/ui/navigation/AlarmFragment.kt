package com.flore.instagramclone.ui.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.flore.instagramclone.R
import com.flore.instagramclone.model.AlarmDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_alarm.view.*
import kotlinx.android.synthetic.main.item_alarm.view.*

class AlarmFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = LayoutInflater.from(activity).inflate(R.layout.fragment_alarm, container, false)
        view.rv_alarmfragment.adapter = AlarmRecyclerviewAdapter()
        view.rv_alarmfragment.layoutManager = LinearLayoutManager(activity)
        return view
    }

    inner class AlarmRecyclerviewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        var alarmDTOList : ArrayList<AlarmDTO> = arrayListOf()

        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid", uid).addSnapshotListener { querySnapshot, _ ->
                alarmDTOList.clear()

                if(querySnapshot == null){
                    return@addSnapshotListener
                }

                for (snapshot in querySnapshot.documents){
                    alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                }
                notifyDataSetChanged()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            // item_comment 레이아웃을 재사용
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)

            return CustomViewHolder(view)
        }

        inner class CustomViewHolder(view : View) : RecyclerView.ViewHolder(view)

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val view = holder.itemView

            FirebaseFirestore.getInstance().collection("profileImages").document(alarmDTOList[position].uid!!).get().addOnCompleteListener { task ->
                val url = task.result!!["image"]
                Glide.with(view.context).load(url).error(R.drawable.ic_account).apply(RequestOptions().circleCrop()).into(view.iv_alarmviewitem_profile)
            }

            when(alarmDTOList[position].kind){
                0 -> {
                    val str0 = alarmDTOList[position].userId + " " + getString(R.string.alarm_favorite)
                    view.tv_alarmviewitem_profile.text = str0
                }

                1 -> {
                    val str1 = alarmDTOList[position].userId + " " + getString(R.string.alarm_comment) + "\n메세지 내용 : " + alarmDTOList[position].message
                    view.tv_alarmviewitem_profile.text = str1
                }

                2 -> {
                    val str2 = alarmDTOList[position].userId + " " + getString(R.string.alarm_follow)
                    view.tv_alarmviewitem_profile.text = str2
                }
            }

        }

    }
}
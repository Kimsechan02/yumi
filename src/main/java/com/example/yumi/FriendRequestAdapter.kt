package com.example.yumi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage

class FriendRequestAdapter(
    private val requests: MutableList<Map<String, String>>,
    private val listener: ActionListener
) : RecyclerView.Adapter<FriendRequestAdapter.VH>() {

    interface ActionListener {
        fun onAccept(requesterId: String)
        fun onReject(requesterId: String)
        fun onBlock(requesterId: String)
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.friendProfileImage)
        val name: TextView = itemView.findViewById(R.id.friendName)
        val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
        val btnBlock: Button = itemView.findViewById(R.id.btnBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false))

    override fun getItemCount() = requests.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val req = requests[position]
        val id = req["id"] ?: return
        holder.name.text = req["nickname"]
        val imageUrl = req["profileImageUrl"].orEmpty()

        if (imageUrl == "default") {
            // 🔥 Firebase Storage에서 `default_profile.jpg` 가져오기
            val storageRef = FirebaseStorage.getInstance().reference.child("default_profile.jpg")
            storageRef.downloadUrl
                .addOnSuccessListener { uri ->
                    Glide.with(holder.img.context)
                        .load(uri.toString()) // ✅ Storage에서 가져온 URL 사용
                        .circleCrop()
                        .placeholder(R.drawable.error_image)
                        .into(holder.img)
                }
                .addOnFailureListener {
                    // 🔥 Storage에서 가져오기 실패하면 로컬 기본 이미지 사용
                    holder.img.setImageResource(R.drawable.error_image)
                }
        } else {
            // ✅ 기본 프로필 이미지가 아니라면 Glide로 로드
            Glide.with(holder.img.context)
                .load(imageUrl)
                .circleCrop()
                .placeholder(R.drawable.error_image)
                .into(holder.img)
        }

        holder.btnAccept.setOnClickListener { listener.onAccept(id) }
        holder.btnReject.setOnClickListener { listener.onReject(id) }
        holder.btnBlock.setOnClickListener { listener.onBlock(id) }
    }
}

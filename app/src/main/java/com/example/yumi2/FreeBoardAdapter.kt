package com.example.yumi2

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yumi2.Post
import com.example.yumi2.PostDetailActivity
import com.example.yumi2.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FreeBoardAdapter(private val context: Context, private val posts: List<Post>) :
    RecyclerView.Adapter<FreeBoardAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        val params = view.layoutParams as RecyclerView.LayoutParams
        // 아이템 크기 설정 (여기서 높이를 120dp로 설정)
        val height = (70 * context.resources.displayMetrics.density).toInt()  // 120dp를 픽셀로 변환
        params.height = height
        view.layoutParams = params
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.titleTextView.text = post.title
        holder.contentTextView.text = post.content
        holder.categoryTextView.text = post.category

        // timestamp를 사람이 읽을 수 있는 형태로 변환
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(post.timestamp))
        holder.timestampTextView.text = formattedDate

        // ✅ 클릭 이벤트 추가: 게시글 상세보기로 이동
        holder.itemView.setOnClickListener {
            val intent = Intent(context, PostDetailActivity::class.java).apply {
                putExtra("title", post.title)
                putExtra("content", post.content)
                putExtra("category", post.category)
                putExtra("timestamp", post.timestamp)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = posts.size

    inner class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.text_title)
        val contentTextView: TextView = view.findViewById(R.id.text_content)
        val categoryTextView: TextView = view.findViewById(R.id.text_category)
        val timestampTextView: TextView = view.findViewById(R.id.text_timestamp)
    }
}

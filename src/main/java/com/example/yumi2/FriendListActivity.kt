package com.example.yumi2

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var friendsAdapter: FriendsAdapter
    private lateinit var emptyText: TextView

    // <-- 제네릭을 Map<String,String> 으로 선언해야 Adapter와 호환됩니다.
    private val friendsList = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friend_list)

        recyclerView = findViewById(R.id.recyclerViewFriends)
        emptyText = findViewById(R.id.emptyText)

        recyclerView.layoutManager = LinearLayoutManager(this)
        friendsAdapter = FriendsAdapter(friendsList, friendsList, R.layout.item_friend_list)
        recyclerView.adapter = friendsAdapter

        // 이제 adapter 초기화가 끝난 뒤에 검색 리스너 설정
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener { query ->
            friendsAdapter.filter.filter(query.toString())
        }

        findViewById<LinearLayout>(R.id.llFriendRequests)
            .setOnClickListener { startActivity(Intent(this, FriendRequestActivity::class.java)) }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvFriendFind).setOnClickListener {
            startActivity(Intent(this, FindFriendActivity::class.java))
            findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        }

        loadFriendList()
    }


    private fun loadFriendList() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("friends")
            .get()
            .addOnSuccessListener { docs ->
                friendsList.clear()
                docs.forEach { doc ->
                    // Firestore 는 Map<String,Any> 반환 → String:String 으로 변환
                    val friendMap = HashMap<String, String>()
                    doc.data.forEach { (key, value) ->
                        friendMap[key] = value.toString()
                    }
                    friendMap["id"] = doc.id
                    friendsList.add(friendMap)
                }
                if (friendsList.isEmpty()) {
                    emptyText.visibility = android.view.View.VISIBLE
                    recyclerView.visibility = android.view.View.GONE
                } else {
                    emptyText.visibility = android.view.View.GONE
                    recyclerView.visibility = android.view.View.VISIBLE
                    friendsAdapter.notifyDataSetChanged()
                    friendsAdapter.filter.filter("")
                }
            }
    }
}

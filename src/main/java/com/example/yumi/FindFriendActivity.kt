package com.example.yumi

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FindFriendActivity : AppCompatActivity(), FindFriendAdapter.FriendRequestListener {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: FindFriendAdapter
    private val users = mutableListOf<Map<String, String>>()
    private val sentRequests = mutableSetOf<String>() // 이미 요청한 사용자 ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_friend)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        adapter = FindFriendAdapter(users, sentRequests, this)
        val rv = findViewById<RecyclerView>(R.id.recyclerViewSearchResults)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<EditText>(R.id.etSearchUser).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrBlank()) searchUsers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        loadSentRequests()
    }

    private fun searchUsers(query: String) {
        val uid = FirebaseAuth.getInstance().uid!!
        db.collection("user_profiles")
            .whereGreaterThanOrEqualTo("nickname", query)
            .whereLessThanOrEqualTo("nickname", query + "\uf8ff")
            .get().addOnSuccessListener { docs ->
                users.clear()
                docs.forEach { doc ->
                    if (doc.id != uid) {
                        val profileUrl =
                            doc.getString("profileImageUrl")?.takeIf { it.isNotBlank() }
                                ?: "default" // profileImageUrl이 없으면 기본 이미지 표시
                        users.add(
                            mapOf(
                                "id" to doc.id,
                                "nickname" to doc.getString("nickname").orEmpty(),
                                "profileImageUrl" to profileUrl
                            )
                        )
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }


    private fun loadSentRequests() {
        val uid = FirebaseAuth.getInstance().uid!!
        db.collection("users").document(uid).collection("friend_requests")
            .get().addOnSuccessListener { docs ->
                sentRequests.clear()
                docs.forEach { doc -> sentRequests.add(doc.id) }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onSendRequest(userId: String) {
        val uid = FirebaseAuth.getInstance().uid!!
        val db = FirebaseFirestore.getInstance()

        val myRequestRef = db.collection("users").document(uid)
            .collection("sent_requests").document(userId)

        val theirRequestRef = db.collection("users").document(userId)
            .collection("friend_requests").document(uid)

        if (sentRequests.contains(userId)) {
            // 🔥 이미 요청 보낸 상태 → Firestore에서 요청 취소 (양쪽 삭제)
            db.runBatch { batch ->
                batch.delete(myRequestRef)
                batch.delete(theirRequestRef)
            }.addOnSuccessListener {
                sentRequests.remove(userId)  // 🔥 요청 리스트에서 제거
                adapter.notifyDataSetChanged()
            }
        } else {
            // 🔥 요청 보내기 (양쪽 저장)
            db.runBatch { batch ->
                batch.set(myRequestRef, mapOf("to" to userId, "status" to "pending"))
                batch.set(theirRequestRef, mapOf("from" to uid, "status" to "pending"))
            }.addOnSuccessListener {
                sentRequests.add(userId)  // 🔥 요청 리스트에 추가
                adapter.notifyDataSetChanged()
            }
        }
    }
}


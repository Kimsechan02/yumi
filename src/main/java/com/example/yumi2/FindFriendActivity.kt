package com.example.yumi2

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class FindFriendActivity : AppCompatActivity(), FindFriendAdapter.FriendRequestListener {
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: FindFriendAdapter
    private val users = mutableListOf<Map<String, String>>()
    private val sentRequests = mutableSetOf<String>() // 이미 요청한 사용자 ID
    private val friendListIDs = mutableSetOf<String>()

    private val searchHistory = mutableListOf<String>()
    private lateinit var historyAdapter: ArrayAdapter<String>



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_friend)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        adapter = FindFriendAdapter(users, sentRequests, this)
        val rv = findViewById<RecyclerView>(R.id.recyclerViewSearchResults)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val searchView = findViewById<MaterialAutoCompleteTextView>(R.id.etSearchUser)
        val btnSearch = findViewById<ImageButton>(R.id.btnSearch)
        historyAdapter = object : ArrayAdapter<String>(
            this, R.layout.item_search_history, R.id.tvHistory, searchHistory
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_search_history, parent, false)
                val tv = view.findViewById<TextView>(R.id.tvHistory)
                val btn = view.findViewById<ImageButton>(R.id.btnDeleteHistory)

                tv.text = getItem(position)
                btn.setOnClickListener {
                    // 리스트에서 제거 + SharedPreferences 업데이트
                    val removed = searchHistory.removeAt(position)
                    notifyDataSetChanged()
                    getSharedPreferences("search_history", MODE_PRIVATE)
                        .edit()
                        .putString("history", Gson().toJson(searchHistory))
                        .apply()
                }
                return view
            }
        }
        searchView.setAdapter(historyAdapter)

        searchView.inputType = android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        searchView.setDropDownWidth(ViewGroup.LayoutParams.MATCH_PARENT)
        searchView.threshold = 0
        searchView.setDropDownHeight(400)

        searchView.post {
            if (historyAdapter.count > 0) searchView.showDropDown()
        }

        searchView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && historyAdapter.count > 0) {
                searchView.post { searchView.showDropDown() }
            }
            false
        }

        searchView.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && historyAdapter.count > 0) {
                searchView.post { searchView.showDropDown() }
            }
        }


        btnSearch.setOnClickListener {
            val query = searchView.text.toString().trim()
            if (query.isNotEmpty()) {
                searchUsers(query)
                saveQueryToHistory(query)
            }
        }
        searchView.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchUsers(query)
                    saveQueryToHistory(query)
                }
                true
            } else false
        }

        loadSearchHistory()
        loadSentRequests()
        loadFriendListIDs()


        val tmp = findViewById<AutoCompleteTextView>(R.id.tmp)
        tmp.setAdapter(historyAdapter)
    }

    private fun loadFriendListIDs() {
        val uid = FirebaseAuth.getInstance().uid!!
        db.collection("users").document(uid).collection("friends")
            .get().addOnSuccessListener { docs ->
                friendListIDs.clear()
                docs.forEach { doc ->
                    friendListIDs.add(doc.id)
                }
            }
    }


    private fun searchUsers(query: String) {
        val uid = FirebaseAuth.getInstance().uid!!
        db.collection("user_profiles")
            .whereGreaterThanOrEqualTo("nickname", query)
            .whereLessThanOrEqualTo("nickname", query + "\uf8ff")
            .get().addOnSuccessListener { docs ->
                users.clear()
                docs.forEach { doc ->
                    // 현재 사용자, 이미 친구 목록에 포함된 사용자는 제외
                    if (doc.id != uid && !friendListIDs.contains(doc.id)) {
                        val profileUrl = doc.getString("profileImageUrl")?.takeIf { it.isNotBlank() }
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


    private fun loadSearchHistory() {
        val prefs = getSharedPreferences("search_history", MODE_PRIVATE)
        val json = prefs.getString("history", "[]")
        Log.d("FindFriendActivity", "🔍 SharedPrefs history JSON = $json")
        searchHistory.clear()
        searchHistory.addAll(Gson().fromJson(json, Array<String>::class.java).toList())
        historyAdapter.notifyDataSetChanged()
    }

    private fun saveQueryToHistory(query: String) {
        if (searchHistory.contains(query)) return
        searchHistory.add(0, query)
        val json = Gson().toJson(searchHistory)
        getSharedPreferences("search_history", MODE_PRIVATE)
            .edit()
            .putString("history", json)
            .apply()
        historyAdapter.notifyDataSetChanged()
    }



    private fun loadSentRequests() {
        val uid = FirebaseAuth.getInstance().uid!!
        db.collection("users").document(uid).collection("sent_requests")
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


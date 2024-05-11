package com.example.api_practice

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class SelectedSong : AppCompatActivity() {
    private lateinit var tvSelectedSong: TextView
    private lateinit var ivSelectedSong: ImageView
    private lateinit var pbSelectedSong: ProgressBar
    private lateinit var tvTimer: TextView
    private lateinit var tvLyrics: TextView
    private lateinit var imageButton: ImageButton

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_song)

        tvSelectedSong = findViewById(R.id.tvSelectedSong)
        ivSelectedSong = findViewById(R.id.ivSelectedSong)
        pbSelectedSong = findViewById(R.id.pbSelectedSong)
        tvTimer = findViewById(R.id.tvTimer)
        tvLyrics = findViewById(R.id.tvLyrics)
        imageButton = findViewById(R.id.imageButton)

        Picasso.get().load(intent.getStringExtra("track_image")).into(ivSelectedSong)
        tvSelectedSong.text = intent.getStringExtra("track_name")
        val trackName = intent.getStringExtra("track_name")
        val artistName = intent.getStringExtra("artist")
        tvTimer.text = intent.getStringExtra("track_duration")

        imageButton.isVisible = false

        getToken {
            apiKey ->
            GlobalScope.launch(Dispatchers.Main) {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.musixmatch.com/ws/1.1/matcher.lyrics.get?q_track=$trackName&q_artist=$artistName&apikey=$apiKey")
                    .build()

                try {
                    val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val responseBody = response.body()?.string()
                    responseBody?.let {
                        val jsonResponse = JSONObject(it)
                        val messageBody = jsonResponse.getJSONObject("message").getJSONObject("body")
                        val lyrics = messageBody.getJSONObject("lyrics")
                        val lyricsBody = lyrics.getString("lyrics_body")
                        Log.d("SearchActivity", "Response: $lyricsBody")
                        tvLyrics.text = lyricsBody
                    }
                } catch (e: IOException) {
                    Log.e("SearchActivity", "Error: ${e.printStackTrace()}")
                }
            }
        }
    }

    private fun getToken(callback: (String) -> Unit) {
        val database = Firebase.database
        val myRef = database.getReference("Musixmatch")

        myRef.addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (tokenSnapshot in snapshot.children) {
                    val token = tokenSnapshot.getValue(String::class.java)
                    token?.let {
                        callback(it)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }
}
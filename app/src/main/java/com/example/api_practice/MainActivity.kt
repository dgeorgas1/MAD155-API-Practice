package com.example.api_practice

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.api_practice.adapter.Adapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), Adapter.OnAlbumClickListener {
    private lateinit var etArtist: EditText
    private lateinit var btnSearch: Button
    private lateinit var rv: RecyclerView
    private lateinit var progressBar: ProgressBar

    private var artist = ""
    private var albumName = ""
    private var artistID = ""
    private var masterID = ""
    private var imageURL = ""
    private var apiURL = ""

    private var masterIDTitles = mutableListOf<String>()
    private var masterIDs = mutableListOf<String>()
    private var albums = mutableListOf<String>()
    private var tracks = mutableListOf<String>()

    private lateinit var adapter: Adapter
    private lateinit var jsonObject: JSONObject

    enum class RecyclerViewState {
        ALBUMS,
        TRACKS
    }
    private var recyclerViewState: RecyclerViewState = RecyclerViewState.ALBUMS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etArtist = findViewById(R.id.etArtist)
        btnSearch = findViewById(R.id.btnSearch)
        rv = findViewById(R.id.rv)
        progressBar = findViewById(R.id.progressBar)

        //Recycler View
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rv.layoutManager = layoutManager
        adapter = Adapter(mutableListOf(), this, recyclerViewState)
        rv.adapter = adapter

        //Add Divider
        rv.addItemDecoration(
            DividerItemDecoration(
                baseContext,
                layoutManager.orientation
            )
        )

        btnSearch.setOnClickListener {
            adapter.clearData()
            recyclerViewState = RecyclerViewState.ALBUMS
            adapter.updateRecyclerViewState(recyclerViewState)
            artist = etArtist.text.toString()
            masterIDTitles = mutableListOf()
            masterIDs = mutableListOf()
            albums = mutableListOf()
            tracks = mutableListOf()

            val artistLength = artist.split(" ").size

            getToken {
                    accessToken ->
                apiURL = "https://api.discogs.com/database/search?q=$artist&per_page=100&token=$accessToken"
                lifecycleScope.launch {
                    artist = artist.replace(" ", "-")
                    displaySongs(apiURL, artistLength, artist)
                }
            }
        }
    }

    private fun getToken(callback: (String) -> Unit) {
        val database = Firebase.database
        val myRef = database.getReference("Discogs")

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

    private suspend fun displaySongs(apiURL: String, artistLength: Int, artist: String) {
        getArtistID(apiURL, artistLength, artist)
        Log.d("SearchActivity", "Function displaySongs was completed")
    }

    private suspend fun getJSONResponse(url: URL): JSONObject {
        val maxRetries = 8
        var currentRetry = 0
        var delayMillis = 1000L // Initial delay

        while (true) {
            try {
                return withContext(Dispatchers.IO) {
                    Log.d("SearchActivity", "getJSONResponse was called")
                    with(url.openConnection() as HttpURLConnection) {
                        Log.d("SearchActivity", "Opened a connection in getJSONResponse")

                        // Get the response code
                        val responseCode = responseCode
                        Log.d("SearchActivity", "Response Code: $responseCode")

                        // Check if the request was successful
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val reader = BufferedReader(InputStreamReader(inputStream))
                            val response = StringBuilder()
                            var line: String?

                            while (reader.readLine().also { line = it } != null) {
                                response.append(line)
                            }
                            reader.close()

                            val jsonResponse = response.toString()
                            return@withContext JSONObject(jsonResponse)
                        } else {
                            Log.e("SearchActivity", "HTTP error: $responseCode")
                            // Throw an exception to trigger retry
                            throw HttpErrorException(responseCode)
                        }
                    }
                }
            } catch (e: HttpErrorException) {
                if (currentRetry >= maxRetries) {
                    // If maximum retries reached
                    throw e
                }

                // Increase the delay
                delay(delayMillis)
                delayMillis *= 2
                currentRetry++
            } catch (e: Exception) {
                Log.e("SearchActivity", "Error in getJSONResponse: ${e.message}", e)
                throw e
            }
        }
    }

    class HttpErrorException(responseCode: Int) : Exception("HTTP error: $responseCode")

    private suspend fun getArtistID(apiURL: String, artistLength: Int, artistName: String) {
        Log.d("SearchActivity", "getArtistID was called")
        Log.d("SearchActivity", "Now working with $apiURL")
        val url = URL(apiURL)
        jsonObject = getJSONResponse(url)
        Log.d("SearchActivity", "Got JSONResponse in getArtistID")

        //Artist ID
        val resultsArray = jsonObject.getJSONArray("results")
        val size = resultsArray.length()

        for (index in 0 until size) {
            val resultsObject = resultsArray.getJSONObject(index)
            val type = resultsObject.getString("type")
            val artist = resultsObject.getString("id")

            if (type == "artist") {
                artistID = artist
            }

            val uri = resultsObject.getString("uri")
            Log.d("SearchActivity", "Artist: $artistName")

            if (uri.startsWith("/master/")) {
                if (uri.contains(artistName)) {
                    Log.d("SearchActivity", "URI: $uri")
                    Log.d("SearchActivity", "Found $artist in uri")
                    val uriSplit = uri.split("-")
                    val a = uriSplit.subList(artistLength + 1, uriSplit.size).joinToString(" ")
                    Log.d("SearchActivity", "Found Album: $a")
                    val coverImage = resultsObject.getString("cover_image")

                    albums.add("$coverImage;$a;")
                    adapter.updateData(albums)
                }
                else {
                    Log.d("SearchActivity", "Couldn't find $artistName in uri")
                    val uriS = uri.split("-").toMutableList()
                    val indexToReplace = 1
                    uriS[indexToReplace] = artistName
                    val joinedUri = uriS.joinToString("-")
                    Log.d("SearchActivity", "Artist: $joinedUri")

                    val uriSplit = joinedUri.split("-")
                    val a = uriSplit.subList(artistLength + 1, uriSplit.size).joinToString(" ")
                    Log.d("SearchActivity", "Found Album: $a")
                    val coverImage = resultsObject.getString("cover_image")

                    albums.add("$coverImage;$a;")
                    adapter.updateData(albums)
                }
            }
        }
    }

    override fun onAlbumClick(album: String) {
        val pageNum = "1"
        imageURL = album.split(";")[0]
        albumName = album.split(";")[1]

        recyclerViewState = RecyclerViewState.TRACKS
        adapter.updateRecyclerViewState(recyclerViewState)

        Log.d("SearchActivity", "Album Was Clicked")
        getToken {
                accessToken ->
            lifecycleScope.launch {
                getTracks(imageURL, accessToken, pageNum, albumName)
            }
        }
    }

    private suspend fun getTracks(imageURL: String, accessToken: String, pageNum: String, albumName: String) {
        Log.d("SearchActivity", "getTracks Was Called")
        var page = pageNum
        var artistFound = false

        while (!artistFound) {
            val apiURL = "https://api.discogs.com/database/search?release_title=$albumName&page=$page&per_page=100&token=$accessToken"
            Log.d("SearchActivity", "onAlbumClick: $apiURL")
            Log.d("SearchActivity", "onAlbumClick Outside: $apiURL")
            var url = URL(apiURL)
            jsonObject = getJSONResponse(url)

            //Master ID
            val resultsArray = jsonObject.getJSONArray("results")
            var size = resultsArray.length()

            for (index in 0 until size) {
                val resultsObject = resultsArray.getJSONObject(index)
                masterID = resultsObject.getString("master_id")
                val uri = resultsObject.getString("uri")
                Log.d("SearchActivity", "URI: $uri")
                Log.d("SearchActivity", "Artist: $artist")

                Toast.makeText(this, masterID, Toast.LENGTH_SHORT).show()

                if (masterID != "null" && masterID != "0" && uri.contains(artist)) {
                    artistFound = true
                    Log.d("SearchActivity", "MasterID: $masterID")
                    val apiUrl = "https://api.discogs.com/masters/$masterID"
                    url = URL(apiUrl)
                    jsonObject = getJSONResponse(url)

                    //Artist
                    val artistArray = jsonObject.getJSONArray("artists")
                    val artistObject = artistArray.getJSONObject(0)
                    val artist = artistObject.getString("name")

                    //Tracks
                    val trackListArray = jsonObject.getJSONArray("tracklist")
                    size = trackListArray.length()

                    for (position in 0 until size) {
                        val trackListObject = trackListArray.getJSONObject(position)

                        val title = trackListObject.getString("title")
                        val duration = trackListObject.getString("duration")

                        Log.d("SearchActivity", "$imageURL-$title-$duration")
                        tracks.add("$imageURL;$title;$duration;$artist")
                        adapter.updateData(tracks)
                    }
                    break
                }
                else {
                    Log.d("SearchActivity", "Master ID Is Not Valid")
                }
            }
            page = (page.toInt() + 1).toString()
        }
        rv.scrollToPosition(0)
    }
}
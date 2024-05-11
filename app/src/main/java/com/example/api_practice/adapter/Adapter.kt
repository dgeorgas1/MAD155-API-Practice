package com.example.api_practice.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.api_practice.MainActivity
import com.example.api_practice.R
import com.example.api_practice.SelectedSong
import com.squareup.picasso.Picasso

class Adapter(private var songs: MutableList<String>, private val itemClickListener: OnAlbumClickListener, private var recyclerViewState: MainActivity.RecyclerViewState) : RecyclerView.Adapter<Adapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.imageView)
        private val tvSong: TextView = itemView.findViewById(R.id.tvSong)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                Log.d("SearchActivity", "RecyclerViewState: $recyclerViewState")
                if (position != RecyclerView.NO_POSITION) {
                    if (recyclerViewState == MainActivity.RecyclerViewState.ALBUMS) {
                        val album = songs[position]
                        Log.d("SearchActivity", "Adapter Album: $album")
                        itemClickListener.onAlbumClick(album)
                        recyclerViewState = MainActivity.RecyclerViewState.TRACKS
                    }
                    else if (recyclerViewState == MainActivity.RecyclerViewState.TRACKS) {
                        val track = songs[position]
                        val image = track.split(";")[0]
                        val songName = track.split(";")[1]
                        val timer = track.split(";")[2]
                        val artist = track.split(";")[3]
                        val intent = Intent(itemView.context, SelectedSong::class.java)
                        intent.putExtra("track_image", image)
                        intent.putExtra("track_name", songName)
                        intent.putExtra("track_duration", timer)
                        intent.putExtra("artist", artist)
                        itemView.context.startActivity(intent)
                    }
                }
            }
        }

        fun bind(song: String) {
            Log.d("SearchActivity", "song: $song")
            val data = song.split(";")

            if (data[0] != "") {
                Log.d("SearchActivity", "data: $data")
                Picasso.get().load(data[0]).into(image)
                tvSong.text = data[1]
                tvDuration.text = data[2]
            }
            else {
                Log.d("SearchActivity", "data: $data")
                image.setImageResource(R.drawable.ic_launcher_foreground)
                tvSong.text = data[1]
                tvDuration.text = data[2]
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_songs, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song)
    }

    override fun getItemCount(): Int {
        return songs.size
    }

    fun updateData(newSongs: MutableList<String>) {
        songs.clear()
        songs.addAll(newSongs)
        notifyDataSetChanged()
    }

    fun clearData() {
        songs.clear()
    }

    fun updateRecyclerViewState(newState: MainActivity.RecyclerViewState) {
        recyclerViewState = newState
        notifyDataSetChanged()
    }

    interface OnAlbumClickListener {
        fun onAlbumClick(album: String)
    }
}

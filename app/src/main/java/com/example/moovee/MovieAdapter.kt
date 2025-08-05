package com.example.moovee

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import androidx.appcompat.widget.TooltipCompat

class MovieAdapter(
    private val movies: List<Movie>,
    private val onMovieLongClick: (Movie) -> Unit // callback for long click
) : RecyclerView.Adapter<MovieAdapter.MovieViewHolder>() {

    class MovieViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.movieTitle)
        val year: TextView = view.findViewById(R.id.movieYear)
        val poster: ImageView = view.findViewById(R.id.moviePoster)
        val overview: TextView = view.findViewById(R.id.movieOverview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.movie_item, parent, false)
        return MovieViewHolder(view)
    }

    override fun getItemCount(): Int = movies.size

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        val movie = movies[position]
        holder.title.text = movie.title
        TooltipCompat.setTooltipText(holder.overview, "Long click title to get recommendations")

        holder.overview.text = movie.overview
        holder.year.text = if (movie.releaseDate.isNotEmpty())
            movie.releaseDate.substring(0, 4)
        else
            "Unknown"

        Glide.with(holder.itemView)
            .load("https://image.tmdb.org/t/p/w500${movie.posterPath}")
            .into(holder.poster)

        // Handle long click on the title
        holder.title.setOnLongClickListener {
            onMovieLongClick(movie)
            true
        }
    }
}

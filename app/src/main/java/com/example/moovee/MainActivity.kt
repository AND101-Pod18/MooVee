package com.example.moovee

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import com.bumptech.glide.Glide
import okhttp3.Headers
import org.json.JSONObject
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class MainActivity : AppCompatActivity() {

    private val apiKey = "7471e6ac9ac158fec47498616b4ca277"

    private val genreMap = mapOf(
        "Action" to "28",
        "Adventure" to "12",
        "Animation" to "16",
        "Comedy" to "35",
        "Crime" to "80",
        "Documentary" to "99",
        "Drama" to "18",
        "Family" to "10751",
        "Fantasy" to "14",
        "History" to "36",
        "Horror" to "27",
        "Music" to "10402",
        "Mystery" to "9648",
        "Romance" to "10749",
        "Science Fiction" to "878",
        "Thriller" to "53",
        "TV Movie" to "10770",
        "War" to "10752",
        "Western" to "37"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Genre Spinner
        val spinner1: Spinner = findViewById(R.id.genres_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.genres_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner1.adapter = adapter
        }

        // Year Spinner
        val spinner2: Spinner = findViewById(R.id.years_spinner)
        ArrayAdapter.createFromResource(
            this,
            R.array.years_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner2.adapter = adapter
        }

        // Set default year to 2025
        val yearsArray = resources.getStringArray(R.array.years_array)
        val defaultYearIndex = yearsArray.indexOf("2025")
        if (defaultYearIndex != -1) {
            spinner2.setSelection(defaultYearIndex)
        }


        // Find keyword input and filter button
        val keywordInput: EditText = findViewById(R.id.keyword_input)
        val filterButton: Button = findViewById(R.id.filter_button)
        val recyclerView: RecyclerView = findViewById(R.id.movieRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        filterButton.setOnClickListener {
            val selectedGenre = spinner1.selectedItem.toString()
            val selectedYear = spinner2.selectedItem.toString()
            val keyword = keywordInput.text.toString().trim()
            val genreId = genreMap[selectedGenre] ?: ""
            val year = if (selectedYear == "Year") "2025" else selectedYear

            val client = AsyncHttpClient()
            val url = if (keyword.isNotEmpty()) {
                "https://api.themoviedb.org/3/search/movie?api_key=$apiKey&query=${keyword}"
            } else {
                val baseUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey"
                val withGenres = if (selectedGenre == "Genre") "" else "&with_genres=$genreId"
                val yearParam = "&primary_release_year=$year"
                "$baseUrl$withGenres$yearParam"
            }

            client.get(url, object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                    val results = json.jsonObject.getJSONArray("results")
                    val movieList = mutableListOf<Movie>()

                    for (i in 0 until results.length()) {
                        val obj = results.getJSONObject(i)
                        val title = obj.optString("title", "Untitled")
                        val release = obj.optString("release_date", "")
                        val poster = obj.optString("poster_path", "")
                        if (poster.isNotEmpty()) {
                            movieList.add(Movie(title, release, poster))
                        }
                    }

                    recyclerView.adapter = MovieAdapter(movieList)
                    Toast.makeText(this@MainActivity, "Found ${movieList.size} movies", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(statusCode: Int, headers: Headers, response: String, throwable: Throwable) {
                    Toast.makeText(this@MainActivity, "Failed to fetch data: $statusCode", Toast.LENGTH_LONG).show()
                }
            })
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

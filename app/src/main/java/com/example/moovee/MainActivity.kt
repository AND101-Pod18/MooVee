package com.example.moovee

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import okhttp3.Headers
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private val apiKey = "7471e6ac9ac158fec47498616b4ca277"

    private val genreMap = mapOf(
        "Action" to "28", "Adventure" to "12", "Animation" to "16", "Comedy" to "35",
        "Crime" to "80", "Documentary" to "99", "Drama" to "18", "Family" to "10751",
        "Fantasy" to "14", "History" to "36", "Horror" to "27", "Music" to "10402",
        "Mystery" to "9648", "Romance" to "10749", "Science Fiction" to "878",
        "Thriller" to "53", "TV Movie" to "10770", "War" to "10752", "Western" to "37"
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var spinnerGenre: Spinner
    private lateinit var spinnerYear: Spinner
    private lateinit var keywordInput: EditText
    private lateinit var filterButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize Views
        spinnerGenre = findViewById(R.id.genres_spinner)
        spinnerYear = findViewById(R.id.years_spinner)
        keywordInput = findViewById(R.id.keyword_input)
        filterButton = findViewById(R.id.filter_button)
        recyclerView = findViewById(R.id.movieRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Setup Genre Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.genres_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerGenre.adapter = adapter
        }

        // Setup Year Spinner
        ArrayAdapter.createFromResource(
            this,
            R.array.years_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerYear.adapter = adapter
        }

        // Set default year to 2025
        val yearsArray = resources.getStringArray(R.array.years_array)
        val defaultYearIndex = yearsArray.indexOf("2025")
        if (defaultYearIndex != -1) spinnerYear.setSelection(defaultYearIndex)

        // Button Click Listener
        filterButton.setOnClickListener {
            val selectedGenre = spinnerGenre.selectedItem.toString()
            val selectedYear = spinnerYear.selectedItem.toString()
            val keyword = keywordInput.text.toString().trim()
            fetchMovies(selectedGenre, selectedYear, keyword)
        }

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun fetchMovies(selectedGenre: String, selectedYear: String, keyword: String) {
        val genreId = genreMap[selectedGenre] ?: ""
        val year = if (selectedYear == "Year") "2025" else selectedYear

        val client = AsyncHttpClient()

        if (keyword.isNotEmpty()) {
            // Step 1: Fetch keyword ID
            val keywordUrl =
                "https://api.themoviedb.org/3/search/keyword?api_key=$apiKey&query=${keyword}"

            client.get(keywordUrl, object : JsonHttpResponseHandler() {
                override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                    val results = json.jsonObject.getJSONArray("results")
                    if (results.length() > 0) {
                        val keywordId =
                            results.getJSONObject(0).getInt("id")

                        val baseUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey"
                        val withGenres =
                            if (selectedGenre == "Genre") "" else "&with_genres=$genreId"
                        val yearParam = "&primary_release_year=$year"
                        val keywordParam = "&with_keywords=$keywordId"

                        val discoverUrl = "$baseUrl$withGenres$yearParam$keywordParam"
                        fetchMoviesFromUrl(discoverUrl)
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "No keyword found for \"$keyword\"",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(
                    statusCode: Int,
                    headers: Headers,
                    response: String,
                    throwable: Throwable
                ) {
                    Toast.makeText(
                        this@MainActivity,
                        "Keyword search failed: $statusCode",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        } else {
            // No keyword, just use discover
            val baseUrl = "https://api.themoviedb.org/3/discover/movie?api_key=$apiKey"
            val withGenres = if (selectedGenre == "Genre") "" else "&with_genres=$genreId"
            val yearParam = "&primary_release_year=$year"
            val discoverUrl = "$baseUrl$withGenres$yearParam"
            fetchMoviesFromUrl(discoverUrl)
        }
    }

    private fun fetchMoviesFromUrl(url: String) {
        val client = AsyncHttpClient()
        client.get(url, object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                val results = json.jsonObject.getJSONArray("results")
                val movieList = mutableListOf<Movie>()

                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val id = obj.optInt("id", -1)
                    val title = obj.optString("title", "Untitled")
                    val release = obj.optString("release_date", "")
                    val poster = obj.optString("poster_path", "")
                    val overview = obj.optString("overview", "No overview available")
                    if (poster.isNotEmpty()) {
                        movieList.add(Movie(id, title, release, poster, overview))
                    }
                }

                val adapter = MovieAdapter(movieList) { movie ->
                    fetchRecommendedMovies(movie.id) // Long press callback
                }
                recyclerView.adapter = adapter

                Toast.makeText(
                    this@MainActivity,
                    "Found ${movieList.size} movies",
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onFailure(
                statusCode: Int,
                headers: Headers,
                response: String,
                throwable: Throwable
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to fetch data: $statusCode",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }


    private fun fetchRecommendedMovies(movieId: Int) {
        val client = AsyncHttpClient()
        val url = "https://api.themoviedb.org/3/movie/$movieId/recommendations?api_key=$apiKey"

        client.get(url, object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                val results = json.jsonObject.getJSONArray("results")
                val recommendedMovies = mutableListOf<Movie>()

                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    val id = obj.optInt("id", -1)
                    val title = obj.optString("title", "Untitled")
                    val release = obj.optString("release_date", "")
                    val poster = obj.optString("poster_path", "")
                    val overview = obj.optString("overview", "No overview available")

                    if (poster.isNotEmpty()) {
                        recommendedMovies.add(Movie(id, title, release, poster, overview))
                    }
                }

                if (recommendedMovies.isNotEmpty()) {
                    recyclerView.adapter = MovieAdapter(recommendedMovies) { selectedMovie ->
                        fetchRecommendedMovies(selectedMovie.id) // Allow chaining
                    }

                    Toast.makeText(
                        this@MainActivity,
                        "Showing ${recommendedMovies.size} recommended movies",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this@MainActivity, "No recommendations found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(
                statusCode: Int,
                headers: Headers,
                response: String,
                throwable: Throwable
            ) {
                Toast.makeText(
                    this@MainActivity,
                    "Failed to fetch recommendations: $statusCode",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }







}


















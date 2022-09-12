package co.tiagoaguiar.netflixremake.util

import android.content.ReceiverCallNotAllowedException
import android.os.Handler
import android.os.Looper
import android.util.Log
import co.tiagoaguiar.netflixremake.model.Category
import co.tiagoaguiar.netflixremake.model.Movie
import co.tiagoaguiar.netflixremake.model.MovieDetail
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class MovieTask(private val callback: Callback) {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback {
        fun onPreExecute()
        fun onResult(movieDetail: MovieDetail)
        fun onFailure(message: String)
    }

    fun execute(url: String) {
        callback.onPreExecute()

        executor.execute {
            var urlConnection: HttpsURLConnection? = null
            var buffer: BufferedInputStream? = null
            var stream: InputStream? = null
            try {

                val requestURL = URL(url)// abrir uma url
                urlConnection = requestURL.openConnection() as HttpsURLConnection // Abrir a conexão
                urlConnection.readTimeout = 2000 // tempo de leitura (2s)
                urlConnection.connectTimeout = 2000 // tempo de conexão do  ep com o servido //

                val statusCode: Int = urlConnection.responseCode

                if (statusCode == 400) {
                    stream = urlConnection.errorStream
                    buffer = BufferedInputStream(stream)
                    val jsonAsString = toString(buffer)
                    //aqui eu ja tenho o JSON
                    val json = JSONObject(jsonAsString)
                    val message = json.getString("message")
                    throw IOException("Erro na comunicação com o servidor")

                } else if (statusCode > 400) {
                    throw IOException("Erro na comunicação com o servidor!")
                }

                stream = urlConnection.inputStream // sequencia de bytes

                buffer = BufferedInputStream(stream)
                val jsonAsString = toString(buffer)

                val movieDetail = toMovieDetail(jsonAsString)

                handler.post {
                    callback.onResult(movieDetail)
                }

            } catch (e: Exception) {
                val message = e.message ?: "erro desconhecido"
                Log.e("Teste", message, e)

                handler.post {
                    callback.onFailure(message)
                }
            } finally {
                urlConnection?.disconnect()
                stream?.close()
                buffer?.close()
            }
        }
    }

    private fun toMovieDetail(jsonAsString: String): MovieDetail {
        val json = JSONObject(jsonAsString)

        val id = json.getInt("id")
        val title = json.getString("title")
        val desc = json.getString("desc")
        val cast = json.getString("cast")
        val coverUrl = json.getString("cover_url")
        val jsonMovies = json.getJSONArray("movie")

        val similars = mutableListOf<Movie>()
        for (i in 0 until jsonMovies.length()) {
            val jsonMovie = jsonMovies.getJSONObject(i)

            val similarId = jsonMovie.getInt("id")
            val similarCoverUrl = jsonMovie.getString("cover_url")

            val m = Movie(similarId, similarCoverUrl)
            similars.add(m)
        }
        val movie = Movie(id, coverUrl, title)
        return MovieDetail(movie, similars)

    }

    private fun toString(stream: InputStream): String {
        val bytes = ByteArray(1024)
        val baos = ByteArrayOutputStream()
        var read: Int
        while (true) {
            read = stream.read(bytes)
            if (read <= 0) {
                break
            }
            baos.write(bytes, 0, read)
        }
        return String(baos.toByteArray())
    }

}


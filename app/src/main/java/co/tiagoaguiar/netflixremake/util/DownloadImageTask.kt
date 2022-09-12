package co.tiagoaguiar.netflixremake.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import co.tiagoaguiar.netflixremake.model.Category
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

class DownloadImageTask (private val callback: Callback){  // ver se importou o certo

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    interface Callback{
        fun onResult(bitmap: Bitmap)
    }
    fun execute(url: String){
        executor.execute {
            var urlConnection: HttpsURLConnection? = null
            var stream: InputStream? = null
            try {
                val requestURL = URL(url)// abrir uma url
                urlConnection = requestURL.openConnection() as HttpsURLConnection // Abrir a conexão
                urlConnection.readTimeout = 2000 // tempo de leitura (2s)
                urlConnection.connectTimeout = 2000 // tempo de conexão do  ep com o servidor

                val statusCode = urlConnection.responseCode
                if(statusCode > 400) {
                    throw IOException("Erro na comunicação com o servidor!")
                }
                stream = urlConnection.inputStream
                val bitmap = BitmapFactory.decodeStream(stream)

                handler.post {
                callback.onResult(bitmap)
                }

            } catch (e: IOException){
                val message = e.message ?:"erro desconhecido"
                Log.e("Teste", message, e)
            }finally {
                urlConnection?.disconnect()
                stream?.close()
            }
        }
    }
}


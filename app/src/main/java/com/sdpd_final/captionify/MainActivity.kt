package com.sdpd_final.captionify

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import android.content.Context
import android.net.Uri
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import android.widget.Toast
import com.sdpd_final.captionify.tensorflow.Classifier
import com.sdpd_final.captionify.tensorflow.TensorFlowImageClassifier
import com.wonderkiln.camerakit.CameraKitImage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.util.*
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import java.io.*

import java.util.ArrayList
import java.util.HashMap


private const val REQUEST_PERMISSIONS = 1

class MainActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "MainActivity"
        private const val INPUT_WIDTH = 128
        private const val INPUT_HEIGHT = 128
        private const val IMAGE_MEAN = 128
        private const val IMAGE_STD = 128f
        private const val INPUT_NAME = "Placeholder"
        private const val OUTPUT_NAME = "final_result"
        private const val MODEL_FILE = "file:///android_asset/hero_stripped_graph.pb"
        private const val LABEL_FILE = "file:///android_asset/hero_labels.txt"
    }

    private var classifier: Classifier? = null
    private var initializeJob: Job? = null


    internal var quotes: HashMap<String, ArrayList<String>> =  HashMap<String, ArrayList<String>>()
    internal var happinessQuotes = ArrayList<String>()
    internal var sadQuotes = ArrayList<String>()
    internal var disgustQuotes = ArrayList<String>()
    internal var neutralQuotes = ArrayList<String>()
    internal var fearQuotes = ArrayList<String>()
    internal var surpriseQuotes = ArrayList<String>()
    internal var angerQuotes = ArrayList<String>()







    private fun File.writeBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int) {
        outputStream().use{ out->
            bitmap.compress(format, quality, out)
            out.flush()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        try {
            val inputStream:InputStream = assets.open("happy_only.txt")
            val inputStreamReader = InputStreamReader(inputStream)
            val sb = StringBuilder()
            var line: String?
            val br = BufferedReader(inputStreamReader)
            line = br.readLine()
            while (br.readLine() != null) {
                sb.append(line)
                line = br.readLine()
                happinessQuotes.add(line)
            }
            br.close()
            Log.d(TAG,sb.toString())
        } catch (e:Exception){
            Log.d(TAG, e.toString())
        }

        try {
            val inputStream:InputStream = assets.open("inspirational_only.txt")
            val inputStreamReader = InputStreamReader(inputStream)
            val sb = StringBuilder()
            var line: String?
            val br = BufferedReader(inputStreamReader)
            line = br.readLine()
            while (br.readLine() != null) {
                sb.append(line)
                line = br.readLine()
                surpriseQuotes.add(line)
                sadQuotes.add(line)
                angerQuotes.add(line)
                neutralQuotes.add(line)
                fearQuotes.add(line)
                disgustQuotes.add(line)
            }
            br.close()
            Log.d(TAG,sb.toString())
        } catch (e:Exception){
            Log.d(TAG, e.toString())
        }

        quotes.put("happiness",happinessQuotes)
        quotes.put("sadness",sadQuotes)
        quotes.put("disgust",disgustQuotes)
        quotes.put("fear",fearQuotes)
        quotes.put("neutral",neutralQuotes)
        quotes.put("surprise",surpriseQuotes)
        quotes.put("anger",angerQuotes)


        requestPermissions()

        initializeTensorClassifier()

        switch1.setOnClickListener {

            cameraView.toggleFacing()
        }

        buttonRecognize.setOnClickListener {
            setVisibilityOnCaptured(false)
            cameraView.captureImage {
                onImageCaptured(it)
            }
        }
    }


    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_PERMISSIONS)
    }

    private fun checkPermissions() {
        if (arePermissionAlreadyGranted()) {

        } else {
            requestPermissions()
        }
    }

    private fun arePermissionAlreadyGranted() =
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun onImageCaptured(it: CameraKitImage) {
        val bitmap = Bitmap.createScaledBitmap(it.bitmap, INPUT_WIDTH, INPUT_HEIGHT, false)
        showCapturedImage(bitmap)

        saveImageToExternalStorage(it.bitmap)


        classifier?.let {
            try {
                showRecognizedResult(it.recognizeImage(bitmap))
                Log.e(TAG, "Done!")
            } catch (e: java.lang.RuntimeException) {
                Log.e(TAG, "Crashing due to classification.closed() before the recognizer finishes!")
            }
        }
    }


    // Method to save an image to external storage
    private fun saveImageToExternalStorage(bitmap:Bitmap):Uri{
        // Get the external storage directory path
        val path = Environment.getExternalStorageDirectory().toString()

        // Create a file to save the image
        val file = File(path, "${UUID.randomUUID()}.jpg")

        try {
            // Get the file output stream
            val stream: OutputStream = FileOutputStream(file)

            // Compress the bitmap
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            // Flush the output stream
            stream.flush()

            // Close the output stream
            stream.close()
            toast("Image saved successful.")
        } catch (e: IOException){ // Catch the exception
            e.printStackTrace()
            toast("Error to save image.")
        }

        // Return the saved image path to uri
        return Uri.parse(file.absolutePath)
    }

    fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    private fun showRecognizedResult(results: MutableList<Classifier.Recognition>) {
        runOnUiThread {
            setVisibilityOnCaptured(true)
            if (results.isEmpty()) {
                textResult.text = getString(R.string.result_no_hero_found)
            } else {
                val emotion = results[0].title

                val confidence = results[0].confidence
                val i = (1..10).shuffled().first()
                val txt = quotes[emotion]?.get(i)
                textResult.text = emotion+ " : "+ txt

                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT,txt )
                    type = "text/plain"

                }
                startActivity(Intent.createChooser(sendIntent, "None"))


            }
        }
    }

    private fun showCapturedImage(bitmap: Bitmap?) {
        runOnUiThread {
            imageCaptured.visibility = View.VISIBLE
            imageCaptured.setImageBitmap(bitmap)
        }
    }

    private fun setVisibilityOnCaptured(isDone: Boolean) {
        buttonRecognize.isEnabled = isDone
        if (isDone) {
            imageCaptured.visibility = View.VISIBLE
            textResult.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        } else {
            imageCaptured.visibility = View.GONE
            textResult.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        }
    }

    private fun initializeTensorClassifier() {
        initializeJob = launch {
            try {
                classifier = TensorFlowImageClassifier.create(
                        assets, MODEL_FILE, LABEL_FILE, INPUT_WIDTH, INPUT_HEIGHT,
                        IMAGE_MEAN, IMAGE_STD, INPUT_NAME, OUTPUT_NAME)

                runOnUiThread {
                    buttonRecognize.isEnabled = true
                }
            } catch (e: Exception) {
                throw RuntimeException("Error initializing TensorFlow!", e)
            }
        }
    }

    private fun clearTensorClassifier() {
        initializeJob?.cancel()
        classifier?.close()
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        super.onPause()
        cameraView.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearTensorClassifier()
    }
}

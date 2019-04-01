package com.elyeproj.superherotensor

import android.Manifest
import android.content.ContextWrapper
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
import com.elyeproj.superherotensor.tensorflow.Classifier
import com.elyeproj.superherotensor.tensorflow.TensorFlowImageClassifier
import com.wonderkiln.camerakit.CameraKitImage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

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


        happinessQuotes.add("Time you enjoy wasting is not wasted time")
        happinessQuotes.add("Count your age by friends, not years. Count your life by smiles, not tears")
        happinessQuotes.add("No medicine cures what happiness cannot")
        happinessQuotes.add("I'd far rather be happy than right any day")
        happinessQuotes.add("With mirth and laughter let old wrinkles come")
        sadQuotes.add("Some days are just bad days, that's all. You have to experience sadness to know happiness, and I remind myself that not every day is going to be a good day, that's just the way it is!")
        sadQuotes.add("Tears come from the heart and not from the brain")
        sadQuotes.add("It's sad when someone you know becomes someone you knew")
        sadQuotes.add("First, accept sadness. Realize that without losing, winning isn't so great")
        sadQuotes.add("Tears are nature's lotion for the eyes. The eyes see better for being washed by them")
        disgustQuotes.add("I am hard to disgust, but a pretentious poet can do it")
        disgustQuotes.add("Disgusting are not men but their behaviours")
        disgustQuotes.add("All those big words produce disgust today")
        disgustQuotes.add("Love is a disease")
        disgustQuotes.add("a")
        neutralQuotes.add("b")
        neutralQuotes.add("c")
        neutralQuotes.add("d")
        neutralQuotes.add("e")
        neutralQuotes.add("f")
        fearQuotes.add("g")
        fearQuotes.add("h")
        fearQuotes.add("i")
        fearQuotes.add("j")
        fearQuotes.add("k")
        surpriseQuotes.add("l")
        surpriseQuotes.add("m")
        surpriseQuotes.add("n")
        surpriseQuotes.add("o")
        surpriseQuotes.add("p")
        surpriseQuotes.add("q")
        angerQuotes.add("r")
        angerQuotes.add("s")
        angerQuotes.add("t")
        angerQuotes.add("u")
        angerQuotes.add("v")
        quotes.put("happiness",happinessQuotes)
        quotes.put("sadness",sadQuotes)
        quotes.put("disgust",disgustQuotes)
        quotes.put("fear",fearQuotes)
        quotes.put("neutral",neutralQuotes)
        quotes.put("surprise",surpriseQuotes)
        quotes.put("anger",angerQuotes)







        requestPermissions()

        initializeTensorClassifier()

        toggleButton.setOnClickListener {

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
                val i = Math.random().toInt() * 5
                val txt = quotes[emotion]?.get(i)
                textResult.text = txt

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

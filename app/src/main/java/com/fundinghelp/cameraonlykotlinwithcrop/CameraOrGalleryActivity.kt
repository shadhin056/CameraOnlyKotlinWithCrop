package com.fundinghelp.cameraonlykotlinwithcrop

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.system.ErrnoException
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_camera_or_gallery.*
import java.io.File
import java.io.FileNotFoundException
import java.util.*

class CameraOrGalleryActivity : AppCompatActivity() {
    private var mCropImageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_or_gallery)
    }
    fun onLoadImageClick(view: View) {

        startActivityForResult(getPickImageChooserIntent(), 200)

    }
    fun getPickImageChooserIntent(): Intent {

        // Determine Uri of camera image to  save.
        val outputFileUri = getCaptureImageOutputUri()

        val allIntents = ArrayList<Intent>()
        val packageManager = packageManager

        // collect all camera intents
        /* val captureIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
         val listCam = packageManager.queryIntentActivities(captureIntent, 0)
         for (res in listCam) {
             val intent = Intent(captureIntent)
             intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
             intent.setPackage(res.activityInfo.packageName)
             if (outputFileUri != null) {
                 intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
             }
             allIntents.add(intent)
         }*/

        // collect all gallery intents
        val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        //galleryIntent.type = "image/*"
        val listGallery = packageManager.queryIntentActivities(galleryIntent, 0)
        for (res in listGallery) {
            val intent = Intent(galleryIntent)
            intent.component = ComponentName(res.activityInfo.packageName, res.activityInfo.name)
            intent.setPackage(res.activityInfo.packageName)
            allIntents.add(intent)
        }

        // the main intent is the last in the  list (fucking android) so pickup the useless one
        var mainIntent = allIntents[allIntents.size - 1]
        for (intent in allIntents) {
            if (intent.component!!.className == "com.android.documentsui.DocumentsActivity") {
                mainIntent = intent
                break
            }
        }
        allIntents.remove(mainIntent)

        // Create student chooser from the main  intent
        val chooserIntent = Intent.createChooser(mainIntent, "Select source")

        // Add all other intents
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, allIntents.toTypedArray<Parcelable>())

        return chooserIntent
    }
    private fun getCaptureImageOutputUri(): Uri? {
        var outputFileUri: Uri? = null
        val getImage = externalCacheDir
        if (getImage != null) {
            outputFileUri = Uri.fromFile(File(getImage.path, "pickImageResult.jpeg"))
        }
        return outputFileUri
    }
    fun getPickImageResultUri(data: Intent?): Uri? {
        var isCamera = true
        if (data != null && data.data != null) {
            val action = data.action
            isCamera = action != null && action == MediaStore.ACTION_IMAGE_CAPTURE
        }
        return if (isCamera) getCaptureImageOutputUri() else data!!.data
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            val imageUri = getPickImageResultUri(data)

            // For API >= 23 we need to check specifically that we have permissions to read external storage,
            // but we don't know if we need to for the URI so the simplest is to try open the stream and see if we get error.
            var requirePermissions = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                isUriRequiresPermissions(imageUri)
            ) {

                // request permissions and handle the result in onRequestPermissionsResult()
                requirePermissions = true
                mCropImageUri = imageUri
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
            }

            if (!requirePermissions) {
                mCropImageView!!.setImageUriAsync(imageUri)
            }

            mCropImageView!!.setBackgroundColor(Color.TRANSPARENT)
        }
    }
    fun isUriRequiresPermissions(uri: Uri?): Boolean {
        try {
            val resolver = contentResolver
            val stream = resolver.openInputStream(uri!!)
            stream!!.close()
            return false
        } catch (e: FileNotFoundException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (e.cause is ErrnoException) {
                    return true
                }
            }
        } catch (e: Exception) {
        }

        return false
    }
}
package com.example.kidsdrawing

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_brush_size.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {

    private var mImageCurrentButtonPaint: ImageButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mImageCurrentButtonPaint = lyt_pallete[1] as ImageButton
        mImageCurrentButtonPaint!!.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.pallet_pressed
            )
        )

        drawing_view.setSizeForBrush(20.toFloat())
        ib_brush.setOnClickListener {
            showBrushSizeDialog()
        }

        ib_image.setOnClickListener {
            if (isReadStorageAllowed()) {
                //run our code to get the image from the gallery.
                val pickPhotoIntent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)

            } else {
                requestPermission()
            }
        }
        ib_undo.setOnClickListener {
            drawing_view.onClickUndo()
        }

        ib_save.setOnClickListener {
            if (isReadStorageAllowed()) {
                BitMapAsyncTask(getBitmapFromView(fl_drwing_view_container)).execute()
            } else {
                requestPermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY) {
                try {
                    if (data!!.data != null) {
                        iv_background.visibility = View.VISIBLE
                        iv_background.setImageURI(data.data)
                    } else {
                        Toast.makeText(
                            this, "Error in parsing the image or its corrupted",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeDialog() {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size);
        brushDialog.setTitle("Brush size: ")
        val smallBtn = brushDialog.ibtn_small_brush
        val mediumBtn = brushDialog.ibtn_medium_brush
        val largeBrush = brushDialog.ibtn_large_brush
        smallBtn.setOnClickListener {
            drawing_view.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        mediumBtn.setOnClickListener {
            drawing_view.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        }
        largeBrush.setOnClickListener {
            drawing_view.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()

    }

    fun paintClicked(view: View) {
        if (view != mImageCurrentButtonPaint) {
            val imageButton = view as ImageButton
            var colorTag = imageButton.tag.toString()
            drawing_view.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageCurrentButtonPaint!!.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.pallet_normal
                )
            )
            mImageCurrentButtonPaint = view
        }
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
            )
        ) {
            Toast.makeText(this, "Need permission to add a background", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Permission granted now you can read the storage file",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Oops you just denied the permission", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val returnBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnBitmap)
        val bgdrawable = view.background
        if (bgdrawable != null) {
            bgdrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnBitmap;
    }

    private inner class BitMapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>() {

        private lateinit var mProgressDialog: Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f =
                        File(externalCacheDir!!.absoluteFile.toString() + File.separator + "KidsDrawingApp_" + System.currentTimeMillis() / 1000 + ".png")
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            onCancelDialog()
            if (!result!!.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved succesfully:$result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went erong while saving the file.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result), null) { path, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent, "share"))
            }
        }

        private fun showProgressDialog() {
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }

        private fun onCancelDialog() {
            mProgressDialog.dismiss()
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }


}
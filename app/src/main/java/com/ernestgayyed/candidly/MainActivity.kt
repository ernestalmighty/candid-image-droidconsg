package com.ernestgayyed.candidly

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.TextureView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File
import java.util.*


private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity(), GalleryAdapterListener {

    private lateinit var galleryView: RecyclerView
    private lateinit var swipeImage: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        galleryView = findViewById(R.id.gallery_view)
        swipeImage = findViewById(R.id.swipe_gallery)

        val galleryAdapter = GalleryAdapter(this, this)
        galleryAdapter.imageList = getAllShownImagesPath()

        galleryView.layoutManager = GridLayoutManager(this, 3)
        galleryView.adapter = galleryAdapter

        swipeImage.setOnRefreshListener {
            swipeImage.isRefreshing = false
            galleryAdapter.setData(getAllShownImagesPath())
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            val intent = Intent(this@MainActivity, CameraService::class.java)
            val pintent = PendingIntent.getService(this@MainActivity, 0, intent, 0)
            val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.setRepeating(AlarmManager.RTC_WAKEUP, Calendar.getInstance().timeInMillis, (30 * 1000).toLong(), pintent)
            startService(Intent(this, CameraService::class.java))
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startService(Intent(this, CameraService::class.java))
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getAllShownImagesPath(): List<String> {
        val listOfAllImages = ArrayList<String>()
        val pictureFileDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        for (strFile in pictureFileDir.list()!!) {
            val file = File(pictureFileDir.absolutePath + "/" + strFile)

            listOfAllImages.add(file.absolutePath)
        }

        return listOfAllImages
    }

    override fun onItemClicked(imagePath: String) {
        val bundle = Bundle()
        bundle.putString("image_path", imagePath)

        val fragment = FullImageFragment()
        fragment.arguments = bundle
        supportFragmentManager.beginTransaction().replace(R.id.fragment_layout, fragment).addToBackStack("FullFragment").commit()
    }
}

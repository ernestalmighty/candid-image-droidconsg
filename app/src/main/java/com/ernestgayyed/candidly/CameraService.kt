package com.ernestgayyed.candidly

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.lifecycle.LifecycleService
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import android.os.SystemClock
import android.app.AlarmManager
import android.app.PendingIntent




private var lastAnalyzedTimestamp = 0L
lateinit var imageCapture: ImageCapture
var hasCaptured = false

class CameraService : LifecycleService() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "My Background Service")
            } else {
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.sym_def_app_icon)
            .setPriority(PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        startForeground(1337, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        CameraX.unbindAll()

        val previewConfig = PreviewConfig.Builder()
            .setLensFacing(CameraX.LensFacing.FRONT)
            .build()
        val preview = Preview(previewConfig)

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            val analyzerThread = HandlerThread(
                "LabelAnalysis"
            ).apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setLensFacing(CameraX.LensFacing.FRONT)
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
        }
            .build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = LabelAnalyzer(this@CameraService)
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .setLensFacing(CameraX.LensFacing.FRONT)
            .build()

        imageCapture = ImageCapture(imageCaptureConfig)

        CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase)

        val serviceIntent = Intent(applicationContext, CameraService::class.java)
        val pendingIntent = PendingIntent.getService(this, 1, serviceIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, SystemClock.elapsedRealtime() + 30000, pendingIntent)

        return START_STICKY
    }
}

private class LabelAnalyzer(val cameraService: CameraService) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp >=
            TimeUnit.SECONDS.toMillis(25)) {
            lastAnalyzedTimestamp = currentTimestamp
        } else {
            return
        }

        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]

        val Yb = y.buffer.remaining()
        val Ub = u.buffer.remaining()
        val Vb = v.buffer.remaining()

        val data = ByteArray(Yb + Ub + Vb)

        y.buffer.get(data, 0, Yb)
        u.buffer.get(data, Yb, Ub)
        v.buffer.get(data, Yb + Ub, Vb)

        val metadata = FirebaseVisionImageMetadata.Builder()
            .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            .setHeight(image.height)
            .setWidth(image.width)
            .setRotation(getRotation(rotationDegrees))
            .build()

        val labelImage = FirebaseVisionImage.fromByteArray(data, metadata)

        val highAccuracyOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
            .build()

        val realTimeOpts = FirebaseVisionFaceDetectorOptions.Builder()
            .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
            .build()

        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(highAccuracyOpts)

        val result = detector.detectInImage(labelImage)
            .addOnSuccessListener { faces ->
                if(faces.size > 0) {
                    if(!hasCaptured) {
                        hasCaptured = true

                        val file = createImageFile()
                        imageCapture.takePicture(file, object : ImageCapture.OnImageSavedListener {
                            override fun onError(error: ImageCapture.UseCaseError,
                                                 message: String, exc: Throwable?) {

                            }
                            override fun onImageSaved(file: File) {
                                hasCaptured = false
                            }
                        })
                    }
                }
            }
            .addOnFailureListener(
                object : OnFailureListener {
                    override fun onFailure(e: Exception) {
                        Log.d("test", "test")
                    }
                })
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = cameraService.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        return image
    }

    private fun getRotation(rotationCompensation: Int) : Int{
        val result: Int
        when (rotationCompensation) {
            0 -> result = FirebaseVisionImageMetadata.ROTATION_0
            90 -> result = FirebaseVisionImageMetadata.ROTATION_90
            180 -> result = FirebaseVisionImageMetadata.ROTATION_180
            270 -> result = FirebaseVisionImageMetadata.ROTATION_270
            else -> {
                result = FirebaseVisionImageMetadata.ROTATION_0
            }
        }
        return result
    }
}
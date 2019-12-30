package com.riluq.trythemlkit

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseCameraActivity()  {

    private val qrList = arrayListOf<QrCode>()
    private val adapter = QrCodeAdapter(qrList)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rvQrCode.layoutManager = LinearLayoutManager(this)
        rvQrCode.adapter = adapter

        cameraView.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                result.toBitmap (5000, 5000) {
                    it?.let {
                        runBarcodeScanner(it)
                    }
                    it?.let {
                        runTextRecognition(it)
                    }
                    showPreview()
                    imagePreview.setImageBitmap(it)
                }
            }

        })
    }

    private fun runBarcodeScanner(bitmap: Bitmap) {
        //Create a FirebaseVisionImage
        val image = FirebaseVisionImage.fromBitmap(bitmap)

        //Optional : Define what kind of barcodes you want to scan
        val options  = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_ALL_FORMATS
            )
            .build()

        //Get access to an instance of FirebaseBarcodeDetector
        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)

        detector.detectInImage(image)
            .addOnSuccessListener {
                qrList.clear()
                adapter.notifyDataSetChanged()

                for (firebaseBarcode in it) {
                    when(firebaseBarcode.valueType) {
                        // Handle the URL here
                        FirebaseVisionBarcode.TYPE_URL ->
                            qrList.add(QrCode("URL", firebaseBarcode.displayValue, null))
                        FirebaseVisionBarcode.TYPE_CONTACT_INFO ->
                            qrList.add(QrCode("Contact", firebaseBarcode.contactInfo?.title, null))
                        FirebaseVisionBarcode.TYPE_WIFI ->
                            qrList.add(QrCode("WiFi", firebaseBarcode.wifi?.ssid, null))
                        FirebaseVisionBarcode.TYPE_DRIVER_LICENSE ->
                            qrList.add(QrCode("Driver License", firebaseBarcode.driverLicense?.licenseNumber, null))
                        else ->
                            qrList.add(QrCode("Generic", firebaseBarcode.displayValue, null))
                    }
                }
                adapter.notifyDataSetChanged()
                progressBar.visibility = View.GONE
                sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(baseContext, "Sorry, something went wrong!", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                progressBar.visibility = View.GONE
            }

    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        val recognizer = FirebaseVision.getInstance()
            .onDeviceTextRecognizer

        recognizer.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->
                Toast.makeText(this@MainActivity, firebaseVisionText.text, Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    override fun onClick(v: View?) {
        progressBar.visibility = View.VISIBLE
        cameraView.takePictureSnapshot()
    }

}

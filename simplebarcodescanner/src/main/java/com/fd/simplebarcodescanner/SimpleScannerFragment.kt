package com.fd.simplebarcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.fd.simplebarcodescanner.databinding.FragmentSimpleScannerBinding
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SimpleScannerFragment : Fragment() {
    private var binding: FragmentSimpleScannerBinding? = null
    private var camera: Camera? = null
    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraExecutor: ExecutorService? = null
    private val imageAnalyzer = ImageAnalyzer()

    private var imageAnalysis = ImageAnalysis.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build()
        )
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    private var options = BarcodeScannerOptions.Builder().setBarcodeFormats(
        Barcode.FORMAT_QR_CODE,
        Barcode.FORMAT_CODE_128
    ).build()

    fun setImageAnalysis(imageAnalysis: ImageAnalysis) {
        this.imageAnalysis = imageAnalysis
    }

    fun setBarcodeScannerOption(barcodeScannerOption: BarcodeScannerOptions) {
        options = barcodeScannerOption
    }

    private var scanListener: ScanListener? = null

    interface ScanListener {
        fun onScanResult(barcode: Barcode?)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): PreviewView? {
        binding = FragmentSimpleScannerBinding.inflate(
            layoutInflater
        )
        scanListener = requireActivity() as ScanListener
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!fragmentIsActive()) return
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
        cameraProviderFuture?.addListener({
            val cameraPermission = ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.CAMERA
            )
            if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                bindPreview()
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.CAMERA),
                    SimpleScannerRequestCode
                )
            }
        }, ContextCompat.getMainExecutor(requireActivity()))
    }

    private fun bindPreview() {
        val preview = Preview.Builder().build()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(
            CameraSelector.LENS_FACING_BACK
        ).build()
        preview.surfaceProvider = binding?.previewView?.surfaceProvider
        val imageCapture = ImageCapture.Builder().build()
        startAnalyzer()
        try {
            cameraProviderFuture?.get()?.unbindAll()
            camera = cameraProviderFuture?.get()?.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalysis
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startAnalyzer() {
        cameraExecutor?.let { imageAnalysis.setAnalyzer(it, imageAnalyzer) }
    }

    fun stopAnalyzer() {
        imageAnalysis.clearAnalyzer()
    }

    fun turnOnFlashLight() {
        if (camera != null && camera?.cameraInfo?.hasFlashUnit() == true) {
            camera?.cameraControl?.enableTorch(true)
        }
    }

    fun turnOffFlashLight() {
        if (camera != null && camera?.cameraInfo?.hasFlashUnit() == true) {
            camera?.cameraControl?.enableTorch(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (camera != null) startAnalyzer()
    }

    override fun onPause() {
        super.onPause()
        if (camera != null) stopAnalyzer()
    }

    private fun fragmentIsActive(): Boolean {
        return activity != null && !isDetached
    }

    internal inner class ImageAnalyzer : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {
            scanBarcode(image)
        }
    }

    private fun scanBarcode(image: ImageProxy) {
        @SuppressLint("UnsafeOptInUsageError") val myImage = image.image
        if (myImage != null) {
            val inputImage = InputImage.fromMediaImage(myImage, image.imageInfo.rotationDegrees)

            val scanner = BarcodeScanning.getClient(options)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes: List<Barcode?> ->
                    if (barcodes.isNotEmpty()) scanListener?.onScanResult(barcodes[0])
                }
                .addOnCompleteListener { image.close() }
        }
    }

    companion object {
        @JvmField
        var SimpleScannerRequestCode: Int = 99
    }
}
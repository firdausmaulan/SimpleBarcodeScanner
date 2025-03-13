package com.fd.sbs

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.fd.sbs.databinding.ActivityMainBinding
import com.fd.simplebarcodescanner.SimpleScannerFragment
import com.fd.simplebarcodescanner.SimpleScannerFragment.ScanListener
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), ScanListener {
    private var binding: ActivityMainBinding? = null
    private val simpleScannerFragment = SimpleScannerFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        val cameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
            addFragmentScanner()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                SimpleScannerFragment.SimpleScannerRequestCode
            )
        }

        binding?.switchFlash?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                simpleScannerFragment.turnOnFlashLight()
            } else {
                simpleScannerFragment.turnOffFlashLight()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SimpleScannerFragment.SimpleScannerRequestCode && grantResults.size > 0) {
            addFragmentScanner()
        }
    }

    private fun addFragmentScanner() {
        // Optional : for advance configuration
        /*
        simpleScannerFragment.setImageAnalysis(
            ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build())
        */

        // Optional : for advance configuration
        /*
        simpleScannerFragment.setBarcodeScannerOption(
            BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128
        ).build())
        */

        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container_view, simpleScannerFragment, null)
            .commit()

        // Show flash control when simpleScannerFragment added
        binding?.switchFlash?.visibility = View.VISIBLE
    }

    override fun onScanResult(barcode: Barcode?) {
        if (barcode == null) return
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                binding?.tvResult?.text = barcode.rawValue
                Toast.makeText(this@MainActivity, barcode.rawValue, Toast.LENGTH_SHORT).show()

                // Optional : add delay
                simpleScannerFragment.stopAnalyzer()
                delay(1000)
                simpleScannerFragment.startAnalyzer()
            }
        }
    }
}

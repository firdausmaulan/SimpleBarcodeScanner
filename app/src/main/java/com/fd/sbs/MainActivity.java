package com.fd.sbs;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.fd.sbs.databinding.ActivityMainBinding;
import com.fd.simplebarcodescanner.SimpleScannerFragment;
import com.google.mlkit.vision.barcode.common.Barcode;

public class MainActivity extends AppCompatActivity implements SimpleScannerFragment.ScanListener {

    private ActivityMainBinding binding;
    private final SimpleScannerFragment simpleScannerFragment = new SimpleScannerFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        int cameraPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
            addFragmentScanner();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    SimpleScannerFragment.SimpleScannerRequestCode
            );
        }

        binding.switchFlash.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                simpleScannerFragment.turnOnFlashLight();
            } else {
                simpleScannerFragment.turnOffFlashLight();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SimpleScannerFragment.SimpleScannerRequestCode && grantResults.length > 0) {
            addFragmentScanner();
        }
    }

    private void addFragmentScanner() {
        // Optional : for advance configuration
        /* simpleScannerFragment.setImageAnalysis(new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()); */
        // Optional : for advance configuration
        /*simpleScannerFragment.setBarcodeScannerOption(new BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128
        ).build());*/
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container_view, simpleScannerFragment, null)
                .commit();

        // Show flash control when simpleScannerFragment added
        binding.switchFlash.setVisibility(View.VISIBLE);
    }

    @Override
    public void onScanResult(Barcode barcode) {
        binding.tvResult.setText(barcode.getRawValue());
        Toast.makeText(this, barcode.getRawValue(), Toast.LENGTH_SHORT).show();

        // Optional : add delay
        simpleScannerFragment.stopAnalyzer();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isDestroyed()) simpleScannerFragment.startAnalyzer();
        }, 1000);
    }
}

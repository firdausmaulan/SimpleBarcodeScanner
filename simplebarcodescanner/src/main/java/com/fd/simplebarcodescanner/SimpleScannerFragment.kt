package com.fd.simplebarcodescanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.fd.simplebarcodescanner.databinding.FragmentSimpleScannerBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleScannerFragment extends Fragment {

    public static int SimpleScannerRequestCode = 99;

    private FragmentSimpleScannerBinding binding;
    private Camera camera;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private final ImageAnalyzer imageAnalyzer = new ImageAnalyzer();

    private ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
            .setTargetResolution(new Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build();

    private BarcodeScannerOptions options = new BarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128
    ).build();

    public void setImageAnalysis(ImageAnalysis imageAnalysis) {
        this.imageAnalysis = imageAnalysis;
    }

    public void setBarcodeScannerOption(BarcodeScannerOptions barcodeScannerOption) {
        options = barcodeScannerOption;
    }

    private ScanListener scanListener;

    public interface ScanListener {
        void onScanResult(Barcode barcode);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSimpleScannerBinding.inflate(getLayoutInflater());
        scanListener = (ScanListener) getActivity();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getActivity() == null) return;
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(getActivity());
        cameraProviderFuture.addListener(() -> {
            int cameraPermission = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
            if (cameraPermission == PackageManager.PERMISSION_GRANTED) {
                bindPreview();
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, SimpleScannerRequestCode);
            }
        }, ContextCompat.getMainExecutor(getActivity()));
    }

    private void bindPreview() {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
        ImageCapture imageCapture = new ImageCapture.Builder().build();
        startAnalyzer();
        try {
            cameraProviderFuture.get().unbindAll();
            camera = cameraProviderFuture.get().bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startAnalyzer() {
        imageAnalysis.setAnalyzer(cameraExecutor, imageAnalyzer);
    }

    public void stopAnalyzer() {
        imageAnalysis.clearAnalyzer();
    }

    public void turnOnFlashLight() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(true);
        }
    }

    public void turnOffFlashLight() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (camera != null) startAnalyzer();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (camera != null) stopAnalyzer();
    }

    class ImageAnalyzer implements ImageAnalysis.Analyzer {

        @Override
        public void analyze(@NonNull ImageProxy image) {
            scanBarcode(image);
        }
    }

    private void scanBarcode(ImageProxy image) {
        @SuppressLint("UnsafeOptInUsageError") Image myImage = image.getImage();
        if (myImage != null) {
            InputImage inputImage = InputImage.fromMediaImage(myImage, image.getImageInfo().getRotationDegrees());

            BarcodeScanner scanner = BarcodeScanning.getClient(options);

            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.size() > 0) scanListener.onScanResult(barcodes.get(0));
                    })
                    .addOnCompleteListener(task -> image.close());
        }
    }
}
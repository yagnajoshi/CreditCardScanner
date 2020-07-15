package com.yagna.sample.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.lang.ref.WeakReference;

import com.yagna.cardscanner.sdk.ScanCardIntent;
import com.yagna.cardscanner.sdk.camera.RecognitionAvailabilityChecker;
import com.yagna.cardscanner.sdk.camera.RecognitionCoreUtils;
import com.yagna.cardscanner.sdk.camera.RecognitionUnavailableException;
import com.yagna.cardscanner.sdk.ndk.RecognitionCore;

public class IntroActivity extends AppCompatActivity {

    private static final String TAG = "IntroActivity";
    private Toolbar mToolbar;
    private ProgressDialog progrss;
    private Bundle savedInstanceState;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        this.savedInstanceState =   savedInstanceState;

        mToolbar = findViewById(R.id.toolbar);
        setupToolbar();

        findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToCardDetails();
            }
        });

    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToCardDetails();
            }
        });
    }

    private void goToCardDetails() {
        if (savedInstanceState == null) {
            RecognitionAvailabilityChecker.Result checkResult = RecognitionAvailabilityChecker.doCheck(this);
            if (checkResult.isFailed()
                    && !checkResult.isFailedOnCameraPermission()) {
                onScanCardFailed(new RecognitionUnavailableException(checkResult.getMessage()));
            } else {
                if (RecognitionCoreUtils.isRecognitionCoreDeployRequired(this)
                        || checkResult.isFailedOnCameraPermission()) {
                      showInitLibrary(savedInstanceState);
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showScanCard();
                        }
                    });
                }
            }
        }

    }

    private void showScanCard() {
        Intent intent = new Intent(this, CardDetailsActivity.class);
        startActivity(intent);
        finish();
    }

    public void onScanCardFailed(Exception e) {
        Log.e(TAG, "Scan card failed", new RuntimeException("onScanCardFinishedWithError()", e));
        setResult(ScanCardIntent.RESULT_CODE_ERROR);
        //finish();
    }

    //////////////
    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1;



    private DeployCoreTask mDeployCoreTask;

    private void showInitLibrary(Bundle savedInstanceState) {
        RecognitionAvailabilityChecker.Result checkResult = RecognitionAvailabilityChecker.doCheck(getActivity());
        if (checkResult.isFailedOnCameraPermission()) {
            if (savedInstanceState == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[] {Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
                }
            }
        } else {
            subscribeToInitCore(getActivity());
        }
    }

    private void subscribeToInitCore(Context context) {
         progrss =new ProgressDialog(this);
         progrss.show();
        if (mDeployCoreTask != null) mDeployCoreTask.cancel(false);
        mDeployCoreTask = new DeployCoreTask(this);
        mDeployCoreTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private Activity getActivity() {
        return IntroActivity.this;
    }


    private static class DeployCoreTask extends AsyncTask<Void, Void, Throwable> {

        private final WeakReference<IntroActivity> fragmentRef;

        @SuppressLint("StaticFieldLeak")
        private final Context appContext;

        DeployCoreTask(IntroActivity parent) {
            this.fragmentRef = new WeakReference<IntroActivity>(parent);
            this.appContext = parent;
        }

        @Override
        protected Throwable doInBackground(Void... voids) {
            try {
                RecognitionAvailabilityChecker.Result checkResult = RecognitionAvailabilityChecker.doCheck(appContext);
                if (checkResult.isFailed()) {
                    throw new RecognitionUnavailableException();
                }
                RecognitionCoreUtils.deployRecognitionCoreSync(appContext);
                if (!RecognitionCore.getInstance(appContext).isDeviceSupported()) {
                    throw new RecognitionUnavailableException();
                }
                return null;
            } catch (RecognitionUnavailableException e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(@Nullable Throwable lastError) {
            super.onPostExecute(lastError);
            IntroActivity fragment = fragmentRef.get();
            if (fragment == null) return;
              if (lastError == null) {
                fragment.onInitLibraryComplete();
            } else {
                fragment.onInitLibraryFailed(lastError);
            }
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    subscribeToInitCore(getActivity());
                } else {
                    onInitLibraryFailed(
                            new RecognitionUnavailableException(RecognitionUnavailableException.ERROR_NO_CAMERA_PERMISSION));
                }
                return;
            default:
                break;
        }
    }


    public void onInitLibraryComplete() {
        if (isFinishing()) return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               showScanCard();
            }
        });
    }

    public void onInitLibraryFailed(Throwable e) {
        Log.e(TAG, "Init library failed", new RuntimeException("onInitLibraryFailed()", e));
        setResult(ScanCardIntent.RESULT_CODE_ERROR);
        ///finish();
    }

}

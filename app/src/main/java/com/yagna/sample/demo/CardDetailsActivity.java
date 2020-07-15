package com.yagna.sample.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;

import com.google.android.material.textfield.TextInputLayout;
import com.yagna.cardscanner.sdk.Card;
import com.yagna.cardscanner.sdk.ScanCardIntent;
import com.yagna.cardscanner.sdk.ScanCardIntent.CancelReason;
import com.yagna.cardscanner.sdk.camera.RecognitionAvailabilityChecker;
import com.yagna.cardscanner.sdk.camera.RecognitionCoreUtils;
import com.yagna.cardscanner.sdk.camera.RecognitionUnavailableException;
import com.yagna.cardscanner.sdk.camera.ScanManager;
import com.yagna.cardscanner.sdk.camera.widget.CameraPreviewLayout;
import com.yagna.cardscanner.sdk.ndk.RecognitionResult;
import com.yagna.cardscanner.sdk.ui.ScanCardRequest;
import com.yagna.cardscanner.sdk.ui.views.ProgressBarIndeterminate;
import com.yagna.cardscanner.sdk.utils.Constants;
import com.yagna.sample.demo.validation.CardExpiryDateValidator;
import com.yagna.sample.demo.validation.CardHolderValidator;
import com.yagna.sample.demo.validation.CardNumberValidator;
import com.yagna.sample.demo.validation.ValidationResult;
import com.yagna.sample.demo.widget.CardNumberEditText;

import static com.yagna.cardscanner.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_DATE;
import static com.yagna.cardscanner.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_GRAB_CARD_IMAGE;
import static com.yagna.cardscanner.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NAME;
import static com.yagna.cardscanner.sdk.ndk.RecognitionConstants.RECOGNIZER_MODE_NUMBER;

public class CardDetailsActivity extends AppCompatActivity {

    private static final String TAG = "CardDetailsActivity";

    private static final int REQUEST_CODE_SCAN_CARD = 1;

    private Toolbar mToolbar;

    private TextInputLayout mCardNumberField;

    private TextInputLayout mCardholderField;

    private TextInputLayout mExpiryField;

    private CardNumberValidator mCardNumberValidator;
    private CardHolderValidator mCardHolderValidator;
    private CardExpiryDateValidator mExpiryDateValidator;
    Bundle savedInstanceState;
    private boolean isLibraryReturned = false;
    private ProgressBarIndeterminate mProgressBar;
    private CameraPreviewLayout mCameraPreviewLayout;
    private ViewGroup mMainContent;
    private @Nullable
    View mFlashButton;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_card_details);

        this.savedInstanceState =   savedInstanceState;
        mMainContent = findViewById(R.id.ocr_main_content);
        mProgressBar = findViewById(R.id.ocr_progress_bar);
        mFlashButton = findViewById(R.id.ocr_iv_flash_id);
        mCameraPreviewLayout = findViewById(R.id.ocr_card_recognition_view);


        mToolbar = findViewById(R.id.toolbar);
        mCardNumberField = findViewById(R.id.card_number_field);
        mCardholderField = findViewById(R.id.cardholder_field);
        mExpiryField = findViewById(R.id.expiry_date_field);
        setupToolbar();

        findViewById(R.id.scan_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanCard();
            }
        });

        if (savedInstanceState == null) {
            scanCard();
        }
    }


    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mToolbar.findViewById(R.id.button_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Card card = readForm();
                ValidationResult validationResult = validateForm(card);
                setValidationResult(validationResult);
                if (validationResult.isValid()) {
                    goToFinalScreen(view);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCAN_CARD) {
            if (resultCode == Activity.RESULT_OK) {
                Card card = data.getParcelableExtra(ScanCardIntent.RESULT_PAYCARDS_CARD);
                if (BuildConfig.DEBUG) Log.i(TAG, "Card info: " + card);
                setCard(card);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                @CancelReason final int reason;
                if (data != null) {
                    reason = data.getIntExtra(ScanCardIntent.RESULT_CANCEL_REASON, ScanCardIntent.BACK_PRESSED);
                } else {
                    reason = ScanCardIntent.BACK_PRESSED;
                }

                if (reason == ScanCardIntent.ADD_MANUALLY_PRESSED) {
                    showIme(mCardNumberField.getEditText());
                }
            } else if (resultCode == ScanCardIntent.RESULT_CODE_ERROR) {
                Log.i(TAG, "Scan failed");
            }
        }
    }

    private Card readForm() {
        String cardNumber = ((CardNumberEditText) mCardNumberField.getEditText()).getCardNumber();
        String holder = mCardholderField.getEditText().getText().toString();
        String expiryDate = mExpiryField.getEditText().getText().toString();
        return new Card(cardNumber, holder, expiryDate);
    }

    private ValidationResult validateForm(Card card) {
        if (mCardNumberValidator == null) {
            mCardNumberValidator = new CardNumberValidator();
            mExpiryDateValidator = new CardExpiryDateValidator();
            mCardHolderValidator = new CardHolderValidator();
        }


        ValidationResult results = new ValidationResult(3);
        results.put(R.id.card_number_field, mCardNumberValidator.validate(card.getCardNumber()));
        results.put(R.id.cardholder_field, mCardHolderValidator.validate(card.getCardHolderName()));
        results.put(R.id.expiry_date_field, mExpiryDateValidator.validate(card.getExpirationDate()));
        return results;
    }

    private void setValidationResult(ValidationResult result) {
        mCardNumberField.setError(result.getMessage(R.id.card_number_field, getResources()));
        mCardholderField.setError(result.getMessage(R.id.cardholder_field, getResources()));
        mExpiryField.setError(result.getMessage(R.id.expiry_date_field, getResources()));
    }

    private void goToFinalScreen(View root) {
        Intent intent = new Intent(this, FinalActivity.class);
        startActivity(intent);
    }

    private void setCard(@NonNull Card card) {
        mCardNumberField.getEditText().setText(card.getCardNumber());
        mCardholderField.getEditText().setText(card.getCardHolderName());
        mExpiryField.getEditText().setText(card.getExpirationDate());
        setValidationResult(ValidationResult.empty());
    }


    private static void showIme(@Nullable View view) {
        if (view == null) return;
        if (view instanceof EditText) view.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService
                (Context.INPUT_METHOD_SERVICE);
        if (imm == null) return;
        try {
            Method showSoftInputUnchecked = InputMethodManager.class.getMethod(
                    "showSoftInputUnchecked", int.class, ResultReceiver.class);
            showSoftInputUnchecked.setAccessible(true);
            showSoftInputUnchecked.invoke(imm, 0, null);
        } catch (Exception e) {
            // ho hum
            imm.showSoftInput(view, 0);
        }
    }


///////////////////////////////////////
    private void scanCard() {
       /* Intent intent = new ScanCardIntent.Builder(this).build();
        startActivityForResult(intent, REQUEST_CODE_SCAN_CARD);*/
        if (savedInstanceState == null) {
            RecognitionAvailabilityChecker.Result checkResult = RecognitionAvailabilityChecker.doCheck(this);
            if (checkResult.isFailed()
                    && !checkResult.isFailedOnCameraPermission()) {
                onScanCardFailed(new RecognitionUnavailableException(checkResult.getMessage()));
            } else {
                if (RecognitionCoreUtils.isRecognitionCoreDeployRequired(this)
                        || checkResult.isFailedOnCameraPermission()) {
                  //  showInitLibrary(savedInstanceState);
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






    public void onScanCardFailed(Exception e) {
        Log.e(TAG, "Scan card failed", new RuntimeException("onScanCardFinishedWithError()", e));
        setResult(ScanCardIntent.RESULT_CODE_ERROR);
        //finish();
    }

    public void onScanCardFinished(Card card, @Nullable byte cardImage[]) {
        setCard(card);
       /* Intent intent = new Intent();
        intent.putExtra(ScanCardIntent.RESULT_PAYCARDS_CARD, (Parcelable) card);
        if (cardImage != null) intent.putExtra(ScanCardIntent.RESULT_CARD_IMAGE, cardImage);
        setResult(RESULT_OK, intent);
        finish();*/
    }



    public void onScanCardCanceled(@ScanCardIntent.CancelReason int actionId) {
        Intent intent = new Intent();
        intent.putExtra(ScanCardIntent.RESULT_CANCEL_REASON, actionId);
        setResult(RESULT_CANCELED, intent);
        //finish();
    }



    @Nullable
    private ScanManager mScanManager;

    private SoundPool mSoundPool;

    private int mCapturedSoundId = -1;


    private ScanCardRequest mRequest;

    private void showScanCard() {



        mRequest = ScanCardRequest.getDefault();
        initView();

        showMainContent();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        int recognitionMode = RECOGNIZER_MODE_NUMBER;
        if (mRequest.isScanCardHolderEnabled()) recognitionMode |=  RECOGNIZER_MODE_NAME;
        if (mRequest.isScanExpirationDateEnabled()) recognitionMode |= RECOGNIZER_MODE_DATE;
        if (mRequest.isGrabCardImageEnabled()) recognitionMode |= RECOGNIZER_MODE_GRAB_CARD_IMAGE;

        mScanManager = new ScanManager(recognitionMode, this, mCameraPreviewLayout, new ScanManager.Callbacks() {

            private byte mLastCardImage[] = null;

            @Override
            public void onCameraOpened(final Camera.Parameters cameraParameters) {

              runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      boolean isFlashSupported = (cameraParameters.getSupportedFlashModes() != null
                              && !cameraParameters.getSupportedFlashModes().isEmpty());
                      //if (getView() == null) return;
                      mProgressBar.setVisibility(View.GONE);
                      mCameraPreviewLayout.setBackgroundDrawable(null);
                      if (mFlashButton != null) mFlashButton.setVisibility(isFlashSupported ? View.VISIBLE : View.GONE);

                      innitSoundPool();
                  }
              });
            }

            @Override
            public void onOpenCameraError(Exception exception) {
                mProgressBar.hideSlow();
                hideMainContent();
                finishWithError(exception);
            }

            @Override
            public void onRecognitionComplete(RecognitionResult result) {
                if (result.isFirst()) {
                    if (mScanManager != null) mScanManager.freezeCameraPreview();
                    playCaptureSound();
                }
                if (result.isFinal()) {
                    String date;
                    if (TextUtils.isEmpty(result.getDate())) {
                        date = null;
                    } else {
                        date =  result.getDate().substring(0, 2) + '/' + result.getDate().substring(2);
                    }

                    Card card = new Card(result.getNumber(), result.getName(), date);
                    byte cardImage[] = mLastCardImage;
                    mLastCardImage = null;
                    finishWithResult(card, cardImage);
                }
            }

            @Override
            public void onCardImageReceived(Bitmap cardImage) {
                mLastCardImage = compressCardImage(cardImage);
            }

            @Override
            public void onFpsReport(String report) {}

            @Override
            public void onAutoFocusMoving(boolean start, String cameraFocusMode) {}

            @Override
            public void onAutoFocusComplete(boolean success, String cameraFocusMode) {}

            @Nullable
            private byte[] compressCardImage(Bitmap img) {
                byte result[];
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (img.compress(Bitmap.CompressFormat.JPEG, 80, stream)) {
                    result = stream.toByteArray();
                } else {
                    result = null;
                }
                return result;
            }
        });
    }

    private void innitSoundPool() {
        if (mRequest.isSoundEnabled()) {
            mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 0);
            mCapturedSoundId = mSoundPool.load(this, com.yagna.cardscanner.sdk.R.raw.wocr_capture_card, 0);
        }
    }

    private void initView() {

        if(mFlashButton != null) {
            mFlashButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (mScanManager != null) mScanManager.toggleFlash();
                }
            });
        }

        TextView paycardsLink = (TextView)findViewById(R.id.ocr_powered_by_paycards_link);
        SpannableString link = new SpannableString(getResources().getString(R.string.wocr_powered_by_pay_cards));
        link.setSpan(new URLSpan(Constants.PAYCARDS_URL), 0, link.length(), SpannableString.SPAN_INCLUSIVE_EXCLUSIVE);
        paycardsLink.setText(link);
        paycardsLink.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void showMainContent() {
        mMainContent.setVisibility(View.VISIBLE);
        mCameraPreviewLayout.setVisibility(View.VISIBLE);
    }

    private void hideMainContent() {
        mMainContent.setVisibility(View.INVISIBLE);
        mCameraPreviewLayout.setVisibility(View.INVISIBLE);
    }

    private void finishWithError(Exception exception) {
         onScanCardFailed(exception);
    }

    private void finishWithResult(Card card, @Nullable byte cardImage[]) {
       onScanCardFinished(card, cardImage);
    }

    private boolean isTablet() {
        return getResources().getBoolean(com.yagna.cardscanner.sdk.R.bool.wocr_is_tablet);
    }

    private void playCaptureSound() {
        if (mCapturedSoundId >= 0) mSoundPool.play(mCapturedSoundId, 1, 1, 0, 0, 1);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mScanManager != null) mScanManager.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
        mCapturedSoundId = -1;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mScanManager != null) mScanManager.onResume();
    }
}

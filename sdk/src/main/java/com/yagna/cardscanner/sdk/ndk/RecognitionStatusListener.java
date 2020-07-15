package com.yagna.cardscanner.sdk.ndk;


import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface RecognitionStatusListener {

    void onRecognitionComplete(RecognitionResult result);

    void onCardImageReceived(Bitmap bitmap);

}

package com.yagna.cardscanner.sdk.camera;



import java.io.IOException;

public final class BlockingOperationException extends IOException {
    public BlockingOperationException() {
    }

    public BlockingOperationException(Throwable cause) {
        super(cause);
    }

    public BlockingOperationException(String detailMessage) {
        super(detailMessage);
    }

    public BlockingOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}

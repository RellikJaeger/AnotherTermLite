package green_green_avk.anotherterm.utils;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

public interface BytesSink {
    void feed(@NonNull ByteBuffer v);

    void invalidateSink();
}

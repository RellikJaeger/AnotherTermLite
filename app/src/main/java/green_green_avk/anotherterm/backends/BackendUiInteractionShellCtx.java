package green_green_avk.anotherterm.backends;

import android.support.annotation.Nullable;

import java.io.InputStream;
import java.io.OutputStream;

public interface BackendUiInteractionShellCtx {
    void setIO(
            @Nullable InputStream stdIn,
            @Nullable OutputStream stdOut,
            @Nullable OutputStream stdErr
    );
}

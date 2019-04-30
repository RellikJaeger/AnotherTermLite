package green_green_avk.anotherterm.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import java.io.IOException;
import java.io.InputStream;

import green_green_avk.anotherterm.utils.BlockingSync;
import green_green_avk.anotherterm.utils.Misc;

public final class ContentRequester extends Requester {
    private ContentRequester() {
    }

    public enum Type {
        BYTES,
        STREAM,
        URI
    }

    public static final class UIFragment extends Requester.UiFragment {
        private final int requestCode = generateRequestCode();

        private BlockingSync<?> result = null; // TODO: Add file descriptor request
        private Type type = null;

        @Override
        public void onActivityResult(final int requestCode, final int resultCode,
                                     final Intent data) {
            if (requestCode != this.requestCode) return;
            recycle();
            if (data == null) {
                result.set(null);
                return;
            }
            final Uri uri = data.getData();
            if (uri == null) {
                result.set(null);
                return;
            }
            if (type == Type.URI) {
                ((BlockingSync<Uri>) result).set(uri);
                return;
            }
            final InputStream is;
            try {
                is = getContext().getContentResolver().openInputStream(uri);
            } catch (final Throwable e) {
                // TODO: Error reporting
                result.set(null);
                return;
            }
            if (is == null) {
                result.set(null);
                return;
            }
            if (type == Type.STREAM) {
                ((BlockingSync<InputStream>) result).set(is);
                return;
            }
            @SuppressLint("StaticFieldLeak") final AsyncTask<Object, Object, Object> task =
                    new AsyncTask<Object, Object, Object>() {
                        @Override
                        protected Object doInBackground(Object... params) {
                            final byte[] buf;
                            try {
                                buf = Misc.toArray(is);
                            } catch (final IOException ignored) {
                                result.set(null);
                                return null;
                            } finally {
                                try {
                                    is.close();
                                } catch (final IOException ignored) {
                                }
                            }
                            ((BlockingSync<byte[]>) result).set(buf);
                            return null;
                        }
                    };
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Object[]) null);
        }

        public void requestContent(@NonNull final BlockingSync<?> result, final Type type,
                                   @NonNull final String message, @NonNull final String mimeType) {
            this.result = result;
            this.type = type;
            final Intent i = new Intent(Intent.ACTION_GET_CONTENT)
                    .addCategory(Intent.CATEGORY_OPENABLE).setType(mimeType);
            startActivityForResult(Intent.createChooser(i, message), requestCode);
        }
    }

    public static void request(@NonNull final BlockingSync<?> result, final Type type,
                               @NonNull final Context ctx, @NonNull final String message,
                               @NonNull final String mimeType) {
        ((FragmentActivity) ctx).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prepare(ctx, new UIFragment()).requestContent(result, type, message, mimeType);
            }
        });
    }
}

package green_green_avk.anotherterm.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import green_green_avk.anotherterm.utils.BlockingSync;

public final class Permissions extends Requester {
    private Permissions() {
    }

    public static final class UIFragment extends Requester.UiFragment {
        private static final int REQUEST_CODE = generateRequestCode();

        public BlockingSync<int[]> result = null;

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (requestCode != REQUEST_CODE) return;
            recycle();
            result.set(grantResults);
        }

        public void requestPermissions(@NonNull final BlockingSync<int[]> result, @NonNull final String[] perms) {
            this.result = result;
            requestPermissions(perms, REQUEST_CODE);
        }
    }

    public static void request(@NonNull final BlockingSync<int[]> result, @NonNull final Context ctx, @NonNull final String[] perms) {
        if (perms.length == 0) {
            result.set(new int[0]);
            return;
        }
        ((FragmentActivity) ctx).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                prepare(ctx, new UIFragment()).requestPermissions(result, perms);
            }
        });
    }

    protected static final Object requestBlockingLock = new Object();

    @NonNull
    public static int[] requestBlocking(@NonNull final Context ctx, @NonNull final String[] perms) throws InterruptedException {
        if (perms.length == 0) return new int[0];
        synchronized (requestBlockingLock) {
            final BlockingSync<int[]> r = new BlockingSync<>();
            request(r, ctx, perms);
            return r.get();
        }
    }
}

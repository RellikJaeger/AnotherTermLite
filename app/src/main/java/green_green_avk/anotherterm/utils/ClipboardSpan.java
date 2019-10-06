package green_green_avk.anotherterm.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;

import green_green_avk.anotherterm.R;

public final class ClipboardSpan extends ClickableSpan {
    @Nullable
    public String content;

    public ClipboardSpan() {
        super();
    }

    public void setContent(@Nullable final String content) {
        this.content = content;
    }

    @Override
    public void onClick(@NonNull final View widget) {
        if (content == null) return;
        final ClipboardManager clipboard =
                (ClipboardManager) widget.getContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText(
                null, content));
        Toast.makeText(widget.getContext(), R.string.msg_copied_to_clipboard,
                Toast.LENGTH_SHORT).show();
    }
}

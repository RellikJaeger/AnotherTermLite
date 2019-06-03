package green_green_avk.anotherterm;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

public final class LinksProvider extends ContentProvider {
    private static final int CODE_LINK_HTML = 1;

    private static LinksProvider instance = null;

    private String authority = null;
    private final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

    private String contentTitle = null;
    private String contentFilename = null;
    private String contentFmt = null;

    public static Uri getHtmlWithLink(@NonNull final Uri uri) {
        return getHtmlWithLink(uri.toString());
    }

    public static Uri getHtmlWithLink(@NonNull final String link) {
        if (instance == null) return null;
        return Uri.parse("content://" + instance.authority + "/html/" + Uri.encode(link));
    }

    @Override
    public boolean onCreate() {
        contentTitle = getContext().getString(R.string.terminal_link_s);
        contentFilename = contentTitle + ".html";
        contentFmt =
                "<html><body><p>" + contentTitle + "</p><p><a href='%2$s'>%2$s</a></p></body></html>";
        return true;
    }

    @Override
    public void attachInfo(final Context context, final ProviderInfo info) {
        super.attachInfo(context, info);
        authority = info.authority;
        matcher.addURI(authority, "/html/*", CODE_LINK_HTML);
        instance = this;
    }

    @Override
    public int delete(@NonNull final Uri uri, final String selection,
                      final String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Nullable
    @Override
    public String getType(@NonNull final Uri uri) {
        return "text/html";
    }

    @Nullable
    @Override
    public String[] getStreamTypes(@NonNull final Uri uri, @NonNull final String mimeTypeFilter) {
        return new String[]{"text/html"};
    }

    @Override
    public Uri insert(@NonNull final Uri uri, final ContentValues values) {
        throw new UnsupportedOperationException("Not supported");
    }

    @NonNull
    private static Uri getTargetUri(@NonNull final Uri uri) {
        return Uri.parse(uri.getLastPathSegment());
    }

    @NonNull
    private byte[] buildContent(@NonNull final Uri uri) {
        String name = null;
        try {
            name = uri.getQueryParameter("name");
        } catch (final UnsupportedOperationException ignored) {
        }
        if (name == null) name = uri.toString();
        return String.format(Locale.getDefault(), contentFmt, name, uri.toString()).getBytes();
    }

    private final PipeDataWriter<String> streamWriter = new PipeDataWriter<String>() {
        @Override
        public void writeDataToPipe(@NonNull final ParcelFileDescriptor output,
                                    @NonNull final Uri uri, @NonNull final String mimeType,
                                    @Nullable final Bundle opts, @Nullable final String args) {
            final FileOutputStream os = new FileOutputStream(output.getFileDescriptor());
            try {
                os.write(buildContent(getTargetUri(uri)));
            } catch (final IOException ignored) {
            }
        }
    };

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull final Uri uri, @NonNull final String mode)
            throws FileNotFoundException {
//        Log.d("QUERY", String.format(Locale.ROOT, "[%s] %s", uri, mode));
        switch (matcher.match(uri)) {
            case CODE_LINK_HTML: {
                return openPipeHelper(uri, "text/html", null, null, streamWriter);
            }
        }
        return super.openFile(uri, mode);
    }

    @Override
    public Cursor query(@NonNull final Uri uri, final String[] projection, final String selection,
                        final String[] selectionArgs, final String sortOrder) {
//        Log.d("QUERY", String.format(Locale.ROOT, "[%s] {%s}", uri, Arrays.toString(projection)));
        switch (matcher.match(uri)) {
            case CODE_LINK_HTML: {
                final Uri targetUri = getTargetUri(uri);
                final MatrixCursor cursor =
                        new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE},
                                1); // Bluetooth should be happy
                cursor.addRow(new Object[]{
                        String.format(Locale.getDefault(), contentFilename, targetUri.getQueryParameter("name")),
                        buildContent(targetUri).length
                });
                return cursor;
            }
        }
        return null;
    }

    @Override
    public int update(@NonNull final Uri uri, final ContentValues values, final String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("Not supported");
    }
}

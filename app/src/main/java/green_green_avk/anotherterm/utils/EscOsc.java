package green_green_avk.anotherterm.utils;

import android.support.annotation.NonNull;

import java.nio.CharBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EscOsc {
    private static final Pattern PAT = Pattern.compile(
            "^\\e](.*?)(?:\\a|\\e\\\\)$", Pattern.DOTALL);

    public final String body;
    public final String[] args;

    public EscOsc(@NonNull final CharBuffer v) throws IllegalArgumentException {
        final Matcher m = PAT.matcher(v);
        if (!m.matches()) throw new IllegalArgumentException("len=" + v.length());
        body = m.group(1);
        args = body.split(";");
    }

    public int getIntArg(final int n, final int def) {
        if (args.length <= n) return def;
        try {
            return Integer.parseInt(args[n]);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

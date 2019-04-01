package green_green_avk.anotherterm;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.KeyEvent;

import java.util.regex.Pattern;

public class TermKeyMap implements TermKeyMapRules {
    public static final int APP_MODE_NONE = 0;
    public static final int APP_MODE_CURSOR = 1;
    public static final int APP_MODE_NUMPAD = 2;
    public static final int APP_MODE_DEFAULT = -1;

    public static final int MODIFIERS_SIZE = 8;

    public static final int KEYCODE_SCROLL_SCREEN_UP = KeyEvent.getMaxKeyCode() + 1;
    public static final int KEYCODE_SCROLL_SCREEN_DOWN = KeyEvent.getMaxKeyCode() + 2;
    public static final int KEYCODES_COUNT = KeyEvent.getMaxKeyCode() + 3;

    private static final class KeyMap {
        public int appMode;
        public String[] nm;
        public String[] am;

        public KeyMap(int appMode, String[] nm, String[] am) {
            this.appMode = appMode;
            this.nm = nm;
            this.am = am;
        }

        @NonNull
        public KeyMap copy() {
            return new KeyMap(appMode,
                    nm != null ? nm.clone() : null,
                    am != null ? am.clone() : null);
        }
    }

    private final KeyMap[] map = new KeyMap[KEYCODES_COUNT];

    public TermKeyMap() {
        this(true);
    }

    protected TermKeyMap(boolean doInit) {
        if (!doInit) return;
        reinit();
    }

    public TermKeyMap(@NonNull final TermKeyMap keyMap) {
        reinit(keyMap);
    }

    @NonNull
    public TermKeyMap copy() {
        return new TermKeyMap(this);
    }

    @NonNull
    public TermKeyMap reinit(@NonNull final TermKeyMap keyMap) {
        for (int k = 0; k < keyMap.map.length; ++k) {
            final KeyMap km = keyMap.map[k];
            if (km != null) map[k] = km.copy();
            else map[k] = null;
        }
        return this;
    }

    @NonNull
    public TermKeyMap reinit() {
        for (int k = 0; k < map.length; ++k) {
            if (TermKeyMapRulesDefault.contains(k)) {
                final int appMode = TermKeyMapRulesDefault.getAppMode(k);
                final String[] nm = new String[MODIFIERS_SIZE];
                String[] am = null;
                for (int m = 0; m < MODIFIERS_SIZE; ++m)
                    nm[m] = TermKeyMapRulesDefault.get(k, m, APP_MODE_NONE);
                if (appMode != APP_MODE_NONE) {
                    am = new String[MODIFIERS_SIZE];
                    for (int m = 0; m < MODIFIERS_SIZE; ++m)
                        am[m] = TermKeyMapRulesDefault.get(k, m, APP_MODE_DEFAULT);
                }
                map[k] = new KeyMap(appMode, nm, am);
            } else
                map[k] = null;
        }
        return this;
    }

    @NonNull
    public TermKeyMap append(@NonNull final TermKeyMapRules keyMap) {
        for (final int k : TermKeyMapRulesDefault.getSupportedKeys()) {
            final KeyMap km = map[k];
            final int appMode = keyMap.getAppMode(k);
            boolean setNM = false;
            String[] nm = km != null ? km.nm : null;
            if (nm == null) nm = new String[MODIFIERS_SIZE];
            boolean setAM = false;
            String[] am = km != null ? km.am : null;
            if (am == null) am = new String[MODIFIERS_SIZE];
            for (int m = 0; m < MODIFIERS_SIZE; ++m) {
                String r;
                r = keyMap.get(k, m, APP_MODE_NONE);
                setNM |= (nm[m] = r != null ? r : nm[m]) != null;
                r = keyMap.get(k, m, APP_MODE_DEFAULT);
                setAM |= (am[m] = r != null ? r : am[m]) != null;
            }
            if (km == null) {
                if (appMode != APP_MODE_DEFAULT || setNM || setAM)
                    map[k] = new KeyMap(appMode, setNM ? nm : null, setAM ? am : null);
            } else {
                if (appMode != APP_MODE_DEFAULT) km.appMode = appMode;
                km.nm = setNM ? nm : null;
                km.am = setAM ? am : null;
            }
        }
        return this;
    }

    @Override
    public int getAppMode(int code) {
        if (code >= map.length) return APP_MODE_DEFAULT;
        final KeyMap km = map[code];
        if (km == null) return APP_MODE_DEFAULT;
        return km.appMode;
    }

    @Nullable
    @Override
    public String get(int code, int modifiers, int appMode) {
        if (code >= map.length) return null;
        final KeyMap km = map[code];
        if (km == null) return null;
        else return (km.appMode & appMode) == 0
                ? (km.nm != null ? km.nm[modifiers % MODIFIERS_SIZE] : null)
                : (km.am != null ? km.am[modifiers % MODIFIERS_SIZE] : null);
    }

    private static final Pattern keyLabelsP = Pattern.compile("^KEYCODE_");
    private static final SparseArray<String> keyLabels = new SparseArray<>();

    static {
        keyLabels.put(KeyEvent.KEYCODE_DEL, "BACKSPACE");
        keyLabels.put(KeyEvent.KEYCODE_FORWARD_DEL, "DELETE");
        keyLabels.put(KEYCODE_SCROLL_SCREEN_UP, "Scroll screen up (alt. buffer only)");
        keyLabels.put(KEYCODE_SCROLL_SCREEN_DOWN, "Scroll screen down (alt. buffer only)");
    }

    @NonNull
    public static String keyCodeToString(int code) {
        String label = keyLabels.get(code);
        if (label == null) {
            label = keyLabelsP.matcher(KeyEvent.keyCodeToString(code)).replaceFirst("")
                    .replace('_', ' ');
        }
        return label;
    }
}

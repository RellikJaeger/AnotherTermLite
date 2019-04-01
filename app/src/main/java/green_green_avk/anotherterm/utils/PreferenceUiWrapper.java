package green_green_avk.anotherterm.utils;

import android.view.View;

import java.util.Map;

public interface PreferenceUiWrapper {

    class ParseException extends RuntimeException {
        final public View view;
        final public String key;
        final public Object value;

        public ParseException(final String message, final View view,
                              final String key, final Object value) {
            super(message);
            this.view = view;
            this.key = key;
            this.value = value;
        }
    }

    Object get(String key);

    void set(String key, Object value);

    Map<String, Object> getPreferences();

    void setPreferences(Map<String, ?> pp);
}

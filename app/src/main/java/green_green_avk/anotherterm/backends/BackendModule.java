package green_green_avk.anotherterm.backends;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class BackendModule {

    public static class Meta {
        protected final Set<String> schemes;

        public Meta() {
            this.schemes = Collections.emptySet();
        }

        public Meta(@NonNull final String defaultScheme) {
            this.schemes = Collections.singleton(defaultScheme);
        }

        public Meta(@NonNull final Set<String> defaultSchemes) {
            this.schemes = Collections.unmodifiableSet(defaultSchemes);
        }

        @Nullable
        public Map<String, String> checkParameters(@NonNull final Map<String, ?> params) {
            return null;
        }

        @NonNull
        public Set<String> getUriSchemes() {
            return schemes;
        }

        @NonNull
        public Uri toUri(@NonNull final Map<String, ?> params) {
            final Uri.Builder b = new Uri.Builder()
                    .scheme(getUriSchemes().iterator().next())
                    .path("opts");
            for (final String k : params.keySet()) {
                b.appendQueryParameter(k, params.get(k).toString());
            }
            return b.build();
        }

        @NonNull

        public Map<String, ?> fromUri(@NonNull final Uri uri) {
            if (uri.isOpaque()) throw new ParametersUriParseException();
            final Map<String, String> params = new HashMap<>();
            for (final String k : uri.getQueryParameterNames()) {
                // TODO: '+' decoding issue before Jelly Bean
                params.put(k, uri.getQueryParameter(k));
            }
            return params;
        }
    }

    static Meta getMeta(@NonNull final Class<?> klass, @NonNull final String defaultScheme) {
        try {
            final Field f = klass.getField("meta");
            return (Meta) f.get(null);
        } catch (final NoSuchFieldException e) {
            return new Meta(defaultScheme);
        } catch (final IllegalAccessException e) {
            return new Meta(defaultScheme);
        } catch (final NullPointerException e) {
            return new Meta(defaultScheme);
        } catch (final ClassCastException e) {
            return new Meta(defaultScheme);
        } catch (final SecurityException e) {
            Log.e("Backend module", "Different class loaders", e);
            return new Meta(defaultScheme);
        }
    }

    protected Context context = null;

    public static final class ParametersWrapper {
        private final Map<String, ?> map;

        public ParametersWrapper(final Map<String, ?> params) {
            map = params;
        }

        public String getString(final String key, final String def) {
            if (!map.containsKey(key)) return def;
            try {
                return map.get(key).toString();
            } catch (final RuntimeException e) {
                throw new BackendException(e);
            }
        }

        public int getInt(final String key, final int def) {
            if (!map.containsKey(key)) return def;
            final Object v = map.get(key);
            if (v instanceof Integer) return (int) v;
            if (v instanceof Long) return (int) (long) v;
            throw new BackendException("Incompatible type of the key `" + key + "': " + v.getClass().getSimpleName());
        }

        public boolean getBoolean(final String key, final boolean def) {
            if (!map.containsKey(key)) return def;
            final Object v = map.get(key);
            if (v instanceof Boolean) return (boolean) v;
            throw new BackendException("Incompatible type of the key `" + key + "': " + v.getClass().getSimpleName());
        }

        public <T> T getFromMap(final String key, final Map<String, T> optsMap, final T def) {
            if (!map.containsKey(key)) return def;
            final Object v = map.get(key);
            if (!(v instanceof String)) throw new BackendException("Bad option type: " + key);
            if (!optsMap.containsKey(v)) throw new BackendException("Bad option value: " + key);
            return optsMap.get(v);
        }
    }

    public static final class ParametersUriParseException extends RuntimeException {
    }

    public void setContext(@NonNull final Context context) {
        this.context = context.getApplicationContext();
    }

    public abstract void setParameters(@NonNull Map<String, ?> params);

    protected BackendUiInteraction ui;

    public abstract void setOutputStream(@NonNull OutputStream stream);

    @NonNull
    public abstract OutputStream getOutputStream();

    public interface OnMessageListener {
        void onMessage(@NonNull Object msg);
    }

    public abstract void setOnMessageListener(OnMessageListener l);

    public BackendUiInteraction getUi() {
        return ui;
    }

    public void setUi(final BackendUiInteraction ui) {
        this.ui = ui;
    }

    public abstract boolean isConnected();

    public abstract void connect();

    public abstract void disconnect();

    public abstract void resize(int col, int row, int wp, int hp);

    @NonNull
    public abstract String getConnDesc();
}

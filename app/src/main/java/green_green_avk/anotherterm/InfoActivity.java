package green_green_avk.anotherterm;

import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

import green_green_avk.anotherterm.ui.HtmlTextView;

public final class InfoActivity extends AppCompatActivity {

    private static final class Source {
        private enum Type {HTML, PLAIN}

        private final int id;
        private final Type type;

        private Source(@StringRes final int id, final Type type) {
            this.id = id;
            this.type = type;
        }
    }

    private static final Map<String, Source> res = new HashMap<>();

    static {
        res.put("keymap_escapes", new Source(R.string.desc_keymap_escapes, Source.Type.HTML));
        res.put("termsh_man", new Source(R.string.desc_termsh_help, Source.Type.PLAIN));
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        final HtmlTextView v = findViewById(R.id.desc);
        final Uri uri = getIntent().getData();
        if (uri != null) {
            if ("infores".equals(uri.getScheme())) {
                final Source source = res.get(uri.getSchemeSpecificPart());
                if (source == null) return;
                if (source.type == Source.Type.PLAIN) {
                    v.setTypeface(Typeface.MONOSPACE);
                    v.setText(source.id);
                } else
                    v.setHtmlText(getString(source.id));
            }
        }
    }
}

package ru.noties.markwon;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;

import ru.noties.debug.Debug;

public class MainActivity extends Activity {

    @Inject
    MarkdownLoader markdownLoader;

    @Inject
    MarkdownRenderer markdownRenderer;

    @Inject
    Themes themes;

    @Inject
    UriProcessor uriProcessor;

    @Inject
    GifProcessor gifProcessor;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.component(this)
                .mainActivitySubcomponent()
                .inject(this);

        themes.apply(this);

        // how can we obtain SpannableConfiguration after theme was applied?
        // as we inject `themes` we won't be able to inject configuration, as it requires theme set

        setContentView(R.layout.activity_main);

        // we process additionally github urls, as if url has in path `blob`, we won't receive
        // desired file, but instead rendered html
        checkUri();

        final AppBarItem.Renderer appBarRenderer
                = new AppBarItem.Renderer(findViewById(R.id.app_bar), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                themes.toggle();
                recreate();
            }
        });

        final TextView textView = Views.findView(this, R.id.text);
        final View progress = findViewById(R.id.progress);

        appBarRenderer.render(appBarState());

        markdownLoader.load(uri(), new MarkdownLoader.OnMarkdownTextLoaded() {
            @Override
            public void apply(final String text) {
                markdownRenderer.render(MainActivity.this, themes.isLight(), uri(), text, new MarkdownRenderer.MarkdownReadyListener() {
                    @Override
                    public void onMarkdownReady(CharSequence markdown) {

                        Markwon.setText(textView, markdown);

                        gifProcessor.process(textView);

                        Views.setVisible(progress, false);
                    }
                });
            }
        });
    }

    @NonNull
    private AppBarItem.State appBarState() {

        final String title;
        final String subtitle;

        // two possible states: just opened from launcher (no subtitle)
        // opened to display external resource (subtitle as a path/url/whatever)

        final Uri uri = uri();

        if (uri != null) {
            title = uri.getLastPathSegment();
            subtitle = uri.toString();
        } else {
            title = getString(R.string.app_name);
            subtitle = null;
        }

        return new AppBarItem.State(title, subtitle);
    }

    private void checkUri() {
        final Uri uri = uri();
        if (uri != null) {
            getIntent().setData(uriProcessor.process(uri));
        }
    }

    @Nullable
    private Uri uri() {
        final Intent intent = getIntent();
        return intent != null
                ? intent.getData()
                : null;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        try {
            super.onRestoreInstanceState(savedInstanceState);
        } catch (Throwable t) {
            // amazing stuff, we need this because on JB it will crash otherwise with NPE
            Debug.e(t);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        markdownLoader.cancel();
        markdownRenderer.cancel();
    }
}

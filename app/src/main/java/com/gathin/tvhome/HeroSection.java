package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Selected title plus a remote-controlled shelf; artwork never advances on a timer. */
final class HeroSection {
    interface Listener { void open(Movie movie); void options(Movie movie); void settings(); }
    final LinearLayout root;
    private final Context context;
    private final TvPalette palette;
    private final ArtworkCache artwork;
    private final Listener listener;
    private final ImageView image;
    private final TextView title, metadata, overview, open, kicker;
    private final LinearLayout posters;
    private Movie selected;

    HeroSection(Context context, ArtworkCache artwork, Listener listener) {
        this.context = context; this.artwork = artwork; this.listener = listener;
        palette = TvPalette.of(context); root = TvUi.row(context);
        FrameLayout outer = new FrameLayout(context);
        outer.setPadding(dp(2), dp(2), dp(2), dp(2));
        TvUi.surface(outer, palette, 24); TvUi.focus(outer, palette, 24, null);
        outer.setOnClickListener(view -> { if (selected != null) listener.open(selected); else listener.settings(); });
        outer.setOnLongClickListener(view -> { if (selected != null) listener.options(selected); return true; });
        FrameLayout panel = new FrameLayout(context);
        panel.setBackground(TvUi.shape(context, Color.rgb(21, 32, 48), 0, 22)); panel.setClipToOutline(true);
        image = new ImageView(context); image.setScaleType(ImageView.ScaleType.CENTER_CROP); panel.addView(image, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(context);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xF20A1220, 0xA90A1220, 0x080A1220})); panel.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout copy = TvUi.column(context); copy.setPadding(dp(24), dp(18), dp(20), dp(18));
        kicker = TvUi.title(context, TvUi.label(context, "TONIGHT, YOUR WAY", "TU PRÓXIMA HISTORIA"), 10, 0xFFD3E0F2);
        kicker.setLetterSpacing(.12f); copy.addView(kicker); TvUi.gap(copy, 9);
        title = TvUi.title(context, "", 30, Color.WHITE); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END); copy.addView(title);
        TvUi.gap(copy, 6); metadata = TvUi.text(context, "", 11, 0xFFCCE0FB); copy.addView(metadata);
        TvUi.gap(copy, 8); overview = TvUi.text(context, "", 13, 0xFFE2E8F1); overview.setMaxLines(2); overview.setEllipsize(TextUtils.TruncateAt.END); copy.addView(overview);
        TvUi.gap(copy, 12); open = TvUi.title(context, "▶  " + TvUi.label(context, "Open in Stremio", "Abrir en Stremio"), 14, Color.WHITE); copy.addView(open);
        panel.addView(copy, new FrameLayout.LayoutParams(dp(380), -1)); outer.addView(panel, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams lead = new LinearLayout.LayoutParams(0, dp(218), 2.1f); lead.setMarginEnd(dp(14)); root.addView(outer, lead);
        HorizontalScrollView shelf = new HorizontalScrollView(context); shelf.setHorizontalScrollBarEnabled(false); shelf.setClipToPadding(false);
        posters = TvUi.row(context); posters.setPadding(dp(2), dp(3), dp(8), dp(3)); shelf.addView(posters);
        root.addView(shelf, new LinearLayout.LayoutParams(0, dp(224), 1));
        message(TvUi.label(context, "A little inspiration", "Algo para descubrir"), TvUi.label(context,
                "Your movie and series recommendations will appear here.", "Tus recomendaciones de películas y series aparecerán acá."));
    }
    void show(List<Movie> movies) {
        posters.removeAllViews();
        if (movies.isEmpty()) {
            selected = null; image.setImageDrawable(null);
            message(TvUi.label(context, "Find your next favorite", "Descubrí tu próximo favorito"),
                    TvUi.label(context, "Adjust your recommendation choices in Settings.", "Ajustá tus preferencias de recomendaciones en Ajustes."));
            return;
        }
        select(movies.get(0));
        for (int index = 0; index < Math.min(10, movies.size()); index++) poster(movies.get(index));
    }
    private void poster(Movie movie) {
        LinearLayout tile = TvUi.column(context); tile.setPadding(dp(2), dp(2), dp(2), dp(6));
        TvUi.surface(tile, palette, 17); TvUi.focus(tile, palette, 17, () -> select(movie));
        ImageView art = new ImageView(context); art.setScaleType(ImageView.ScaleType.CENTER_CROP);
        art.setBackground(TvUi.shape(context, palette.surfaceAlt, 0, 15)); art.setClipToOutline(true);
        tile.addView(art, TvUi.params(context, 116, 171, 0)); artwork.load(art, movie.poster, false);
        TextView label = TvUi.title(context, movie.title, 12, palette.text); label.setSingleLine(); label.setEllipsize(TextUtils.TruncateAt.END);
        label.setPadding(dp(8), dp(8), dp(5), 0); tile.addView(label, TvUi.params(context, 116, 32, 0));
        tile.setContentDescription(movie.title + ". " + movie.metadata());
        tile.setOnClickListener(view -> listener.open(movie));
        tile.setOnLongClickListener(view -> { listener.options(movie); return true; });
        posters.addView(tile, TvUi.params(context, 120, 210, 10));
    }
    private void select(Movie movie) {
        selected = movie; title.setText(movie.title); metadata.setText(movie.metadata()); overview.setText(movie.overview);
        open.setText("▶  " + TvUi.label(context, "Open in Stremio", "Abrir en Stremio"));
        artwork.load(image, movie.backdrop.isEmpty() ? movie.poster : movie.backdrop, true);
    }
    void message(String heading, String detail) {
        if (selected != null) return;
        title.setText(heading); overview.setText(detail); metadata.setText("TMDB");
        open.setText(TvUi.label(context, "Open settings →", "Abrir ajustes →"));
    }
    void opening(boolean value) {
        open.setText(value ? TvUi.label(context, "Opening…", "Abriendo…") : "▶  " + TvUi.label(context, "Open in Stremio", "Abrir en Stremio"));
    }
    void status(String value) { kicker.setText(value); }
    private int dp(int value) { return TvUi.dp(context, value); }
}

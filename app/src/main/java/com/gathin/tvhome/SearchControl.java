package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import java.util.ArrayList;

/** The system IME and system voice UI handle input; there is no launcher keyboard. */
final class SearchControl {
    static final int VOICE_REQUEST=4206;
    final LinearLayout root;
    private final Activity activity;
    private final TextView field;
    private String query="";
    SearchControl(Activity activity) {
        this.activity=activity; TvPalette p=TvPalette.of(activity); root=TvUi.row(activity);
        root.setPadding(dp(7),dp(8),dp(7),dp(8)); root.setBackground(GlassMaterial.card(root,p,12));
        field=TvUi.text(activity,TvUi.label(activity,"Movies, music or apps","Películas, música o apps"),16,p.muted);
        field.setSingleLine(); field.setEllipsize(android.text.TextUtils.TruncateAt.END); field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(10),0,dp(4),0); TvUi.focus(field,p,8,1,0,null); field.setOnClickListener(v -> edit());
        root.addView(field,new LinearLayout.LayoutParams(0,dp(48),1));
        root.addView(TvUi.iconButton(activity,"mic",TvUi.label(activity,"Speak a command","Decir un comando"),false,this::voice),TvUi.params(activity,48,48,0));
        root.addView(TvUi.iconButton(activity,"search",TvUi.label(activity,"Search","Buscar"),false,this::submit),TvUi.params(activity,48,48,0));
    }
    private void edit() {
        TvTextDialog.show(activity,TvUi.label(activity,"Search","Buscar"),query,
                TvUi.label(activity,"Watch…, listen to… or install…","Ver…, escuchar… o instalar…"),false,240,value -> {
            if (value.isEmpty()) return TvUi.label(activity,"Enter a search","Escribí una búsqueda");
            set(value); submit(); return null;
        });
    }
    private void set(String value) { query=value; field.setText(value); field.setTextColor(TvPalette.of(activity).text); }
    private void submit() { if (query.trim().isEmpty()) edit(); else SearchRouter.open(activity,query); }
    private void voice() {
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE,new TvPreferences(activity).voiceLanguage())
                .putExtra(RecognizerIntent.EXTRA_PROMPT,TvUi.label(activity,"Watch, listen or install","Ver, escuchar o instalar"));
        try { activity.startActivityForResult(intent,VOICE_REQUEST); }
        catch (android.content.ActivityNotFoundException error) {
            android.widget.Toast.makeText(activity,TvUi.label(activity,"Voice is unavailable. You can type.","La voz no está disponible. Podés escribir."),android.widget.Toast.LENGTH_LONG).show();
        }
    }
    void result(int code,Intent data) {
        if (code!=Activity.RESULT_OK || data==null) return;
        ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (words==null || words.isEmpty()) return;
        String value=words.get(0); set(value.substring(0,Math.min(value.length(),240))); submit();
    }
    private int dp(float value) { return TvUi.dp(activity,value); }
}

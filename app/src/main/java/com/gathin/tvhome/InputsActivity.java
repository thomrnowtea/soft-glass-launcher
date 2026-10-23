package com.gathin.tvhome;

import android.content.Intent;
import android.media.tv.TvContract;
import android.provider.Settings;
import java.util.List;

public final class InputsActivity extends ControlPanelActivity {
    @Override protected void buildContent() { header(TvUi.label(this,"Inputs","Entradas")); status.setVisibility(android.view.View.GONE);
        List<TvInputs.Entry> inputs=TvInputs.list(this);
        int count=0;
        for (TvInputs.Entry input:inputs) {
            item(input.label,"",() -> open(new Intent(Intent.ACTION_VIEW).setDataAndType(TvContract.buildChannelUriForPassthroughInput(input.id),
                    "vnd.android.cursor.item/channel")));
            count++;
        }
        if (count==0) item(TvUi.label(this,"TV inputs","Entradas de la TV"),"",() -> open(new Intent(Settings.ACTION_SETTINGS)));
    }
    private void open(Intent intent) {
        try { startActivity(intent); finish(); }
        catch (android.content.ActivityNotFoundException error) { status.setText(R.string.hc_unavailable); }
    }
}

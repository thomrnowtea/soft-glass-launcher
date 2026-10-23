package com.gathin.tvhome;

import android.content.Context;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Stable presentation order only; the platform's input IDs and routing remain untouched. */
final class TvInputs {
    private static final Pattern HDMI=Pattern.compile("HDMI\\s*([0-9]{1,2})",Pattern.CASE_INSENSITIVE);
    static final class Entry {
        final String id,label;
        final int order;
        Entry(String id,String label) { this.id=id; this.label=label; order=order(label); }
    }
    static List<Entry> list(Context context) {
        List<Entry> result=new ArrayList<>();
        TvInputManager manager=(TvInputManager)context.getSystemService(Context.TV_INPUT_SERVICE);
        if (manager==null) return result;
        for (TvInputInfo input:manager.getTvInputList()) {
            if (!input.isPassthroughInput() || input.getParentId()!=null) continue;
            result.add(new Entry(input.getId(),input.loadLabel(context).toString()));
        }
        result.sort(Comparator.comparingInt((Entry input)->input.order).thenComparing(input->input.label,String.CASE_INSENSITIVE_ORDER));
        return result;
    }
    private static int order(String label) {
        Matcher match=HDMI.matcher(label);
        return match.find() ? 100+Integer.parseInt(match.group(1)) : 0;
    }
}

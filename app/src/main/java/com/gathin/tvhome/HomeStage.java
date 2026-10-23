package com.gathin.tvhome;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;

/** A stable first page; enlarged text can grow it without pushing controls over one another. */
final class HomeStage extends FrameLayout {
    private final View header,hero,dock;
    private int heroTop,dockTop;
    HomeStage(Context context,View header,View hero,View dock) {
        super(context); this.header=header; this.hero=hero; this.dock=dock;
        setClipChildren(false); setClipToPadding(false);
        addView(header); addView(hero); addView(dock);
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec) {
        int width=MeasureSpec.getSize(widthSpec),inset=dp(52);
        header.measure(exact(Math.max(1,width-inset*2)),unspecified());
        hero.measure(exact(Math.max(1,Math.min(dp(425),width-inset*2))),unspecified());
        dock.measure(exact(Math.max(1,width-dp(88))),unspecified());
        heroTop=Math.max(dp(96),dp(24)+header.getMeasuredHeight()+dp(12));
        int minimum=heroTop+hero.getMeasuredHeight()+dp(8)+dock.getMeasuredHeight()+dp(24);
        int height=Math.max(getResources().getDisplayMetrics().heightPixels,minimum);
        dockTop=Math.max(heroTop+hero.getMeasuredHeight()+dp(8),height-dp(40)-dock.getMeasuredHeight());
        setMeasuredDimension(width,height);
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom) {
        int width=right-left;
        header.layout(dp(52),dp(24),width-dp(52),dp(24)+header.getMeasuredHeight());
        hero.layout(dp(52),heroTop,dp(52)+hero.getMeasuredWidth(),heroTop+hero.getMeasuredHeight());
        dock.layout(dp(44),dockTop,width-dp(44),dockTop+dock.getMeasuredHeight());
    }
    private static int exact(int size) { return MeasureSpec.makeMeasureSpec(size,MeasureSpec.EXACTLY); }
    private static int unspecified() { return MeasureSpec.makeMeasureSpec(0,MeasureSpec.UNSPECIFIED); }
    private int dp(float value) { return TvUi.dp(getContext(),value); }
}

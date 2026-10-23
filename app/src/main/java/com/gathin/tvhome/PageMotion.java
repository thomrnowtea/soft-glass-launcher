package com.gathin.tvhome;

import android.widget.ScrollView;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Rect;
import androidx.dynamicanimation.animation.FloatPropertyCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/** One interruptible spring owns page movement; a new destination carries the current velocity. */
final class PageMotion {
    private final ScrollView scroll;
    private final SpringAnimation spring;
    private int target=-1;
    PageMotion(ScrollView scroll) {
        this.scroll=scroll;
        spring=new SpringAnimation(scroll,new FloatPropertyCompat<ScrollView>("homeScroll") {
            @Override public float getValue(ScrollView view) { return view.getScrollY(); }
            @Override public void setValue(ScrollView view,float value) { view.scrollTo(0,Math.round(value)); }
        },0);
        spring.setSpring(new SpringForce(0).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY).setStiffness(800f));
        spring.setMinimumVisibleChange(.5f);
    }
    void to(int y) {
        View child=scroll.getChildAt(0);
        int max=child==null ? 0 : Math.max(0,child.getHeight()-scroll.getHeight());
        int next=Math.max(0,Math.min(max,y));
        if (next==target && (spring.isRunning() || scroll.getScrollY()==next)) return;
        target=next;
        if (!TvMotion.enabled(scroll.getContext())) { spring.cancel(); scroll.scrollTo(0,next); return; }
        spring.animateToFinalPosition(next);
    }
    void reveal(View view,ViewGroup content,int topInset,int bottomInset) {
        to(visibleTarget(view,content,destination(),topInset,bottomInset));
    }
    int destination() { return target>=0 && spring.isRunning() ? target : scroll.getScrollY(); }
    boolean fits(View view,ViewGroup content,int topInset,int bottomInset) {
        int current=destination();
        return visibleTarget(view,content,current,topInset,bottomInset)==current;
    }
    void jumpTo(int y) {
        stop();
        View child=scroll.getChildAt(0);
        int max=child==null ? 0 : Math.max(0,child.getHeight()-scroll.getHeight());
        scroll.scrollTo(0,Math.max(0,Math.min(max,y)));
    }
    int visibleTarget(View view,ViewGroup content,int preferred,int topInset,int bottomInset) {
        Rect bounds=bounds(view,content);
        View child=scroll.getChildAt(0);
        int max=child==null ? 0 : Math.max(0,child.getHeight()-scroll.getHeight());
        int next=Math.max(0,Math.min(max,preferred));
        int height=scroll.getHeight();
        if (bounds.bottom+bottomInset>next+height) next=bounds.bottom+bottomInset-height;
        if (bounds.top-topInset<next) next=bounds.top-topInset;
        return Math.max(0,Math.min(max,next));
    }
    static int top(View view,ViewGroup content) { return bounds(view,content).top; }
    private static Rect bounds(View view,ViewGroup content) {
        Rect result=new Rect(); view.getDrawingRect(result); content.offsetDescendantRectToMyCoords(view,result); return result;
    }
    void stop() { spring.cancel(); target=-1; }
}

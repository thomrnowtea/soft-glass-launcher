package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Spatial arrows: rows identify sections for restoration, never impose traversal order. */
final class FocusNavigator {
    static final class Row {
        final String name; final List<View> views;
        Row(String name,List<View> views) { this.name=name; this.views=views; }
    }
    private final Activity activity;
    private final Supplier<List<Row>> source;
    private Predicate<View> reachable=view -> true;
    private String savedRow="dock";
    private int savedColumn;
    private View last;
    private boolean lastVertical;
    private float anchor;
    FocusNavigator(Activity activity,Supplier<List<Row>> source) { this.activity=activity; this.source=source; }
    void reachable(Predicate<View> predicate) { reachable=predicate; }
    static void add(List<Row> rows,String name,View... roots) {
        List<View> views=new ArrayList<>();
        for (View root:roots) collect(root,views);
        if (!views.isEmpty()) rows.add(new Row(name,views));
    }
    private static void collect(View view,List<View> views) {
        if (view==null || view.getVisibility()!=View.VISIBLE || !view.isEnabled()) return;
        if (view.isFocusable()) { views.add(view); return; }
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group=(ViewGroup)view;
        for (int i=0;i<group.getChildCount();i++) collect(group.getChildAt(i),views);
    }
    boolean key(KeyEvent event) {
        if (event.getAction()!=KeyEvent.ACTION_DOWN) return false;
        int direction=direction(event.getKeyCode());
        if (direction==0 && event.getKeyCode()!=KeyEvent.KEYCODE_TAB) return false;
        View current=activity.getCurrentFocus();
        if (current instanceof android.widget.EditText) return false;
        List<View> views=controls();
        if (views.isEmpty()) return false;
        if (event.getKeyCode()==KeyEvent.KEYCODE_TAB) tab(views,current,event.isShiftPressed());
        else {
            View target=nearest(views,current,direction);
            if (target!=null) target.requestFocus();
        }
        return true;
    }
    private List<View> controls() {
        List<View> result=new ArrayList<>();
        for (Row row:source.get()) result.addAll(row.views);
        return result;
    }
    private static int direction(int key) {
        switch (key) {
            case KeyEvent.KEYCODE_DPAD_LEFT: return View.FOCUS_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return View.FOCUS_RIGHT;
            case KeyEvent.KEYCODE_DPAD_UP: return View.FOCUS_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return View.FOCUS_DOWN;
            default: return 0;
        }
    }
    private View nearest(List<View> views,View current,int direction) {
        if (current==null || !views.contains(current)) return views.get(0);
        boolean vertical=direction==View.FOCUS_UP || direction==View.FOCUS_DOWN;
        Rect from=bounds(current);
        float cross=vertical ? from.exactCenterX() : from.exactCenterY();
        if (last!=current || lastVertical!=vertical) anchor=cross;
        Candidate best=nearestCandidate(views,current,from,direction);
        if (best==null) return null;
        last=best.view; lastVertical=vertical;
        return best.view;
    }
    private Candidate nearestCandidate(List<View> views,View current,Rect from,int direction) {
        Candidate best=null;
        for (View view:views) {
            if (view==current || !reachable.test(view)) continue;
            Candidate candidate=Candidate.of(view,from,bounds(view),direction,anchor);
            if (candidate!=null && candidate.before(best)) best=candidate;
        }
        return best;
    }
    private Rect bounds(View view) {
        Rect rect=new Rect(); view.getDrawingRect(rect);
        ((ViewGroup)activity.getWindow().getDecorView()).offsetDescendantRectToMyCoords(view,rect);
        return rect;
    }
    private static final class Candidate {
        final View view; final boolean aligned; final double distance,center;
        Candidate(View view,boolean aligned,double distance,double center) {
            this.view=view; this.aligned=aligned; this.distance=distance; this.center=center;
        }
        static Candidate of(View view,Rect from,Rect to,int direction,float anchor) {
            boolean vertical=direction==View.FOCUS_UP || direction==View.FOCUS_DOWN;
            float gap=gap(from,to,direction);
            if (gap < -1) return null;
            int start=vertical ? to.left : to.top, end=vertical ? to.right : to.bottom;
            int sourceStart=vertical ? from.left : from.top, sourceEnd=vertical ? from.right : from.bottom;
            boolean aligned=start<sourceEnd-1 && end>sourceStart+1;
            float cross=Math.max(0,Math.max(start-anchor,anchor-end));
            return new Candidate(view,aligned,Math.hypot(Math.max(0,gap),cross),Math.abs((start+end)/2f-anchor));
        }
        private static int gap(Rect from,Rect to,int direction) {
            switch (direction) {
                case View.FOCUS_LEFT: return from.left-to.right;
                case View.FOCUS_RIGHT: return to.left-from.right;
                case View.FOCUS_UP: return from.top-to.bottom;
                default: return to.top-from.bottom;
            }
        }
        boolean before(Candidate other) {
            if (other==null) return true;
            if (Math.abs(distance-other.distance)>.5) return distance<other.distance;
            if (aligned!=other.aligned) return aligned;
            return center<other.center;
        }
    }
    private void tab(List<View> views,View current,boolean backwards) {
        int next=views.indexOf(current)+(backwards ? -1 : 1);
        views.get((next+views.size())%views.size()).requestFocus(); reset();
    }
    void reset() { last=null; }
    void remember() {
        for (Row row:source.get()) {
            int index=row.views.indexOf(activity.getCurrentFocus());
            if (index>=0) { savedRow=row.name; savedColumn=index; return; }
        }
    }
    void home() { savedRow="dock"; savedColumn=0; reset(); restore(); }
    void restore() {
        List<Row> rows=source.get();
        for (Row row:rows) if (row.name.equals(savedRow)) {
            row.views.get(Math.min(savedColumn,row.views.size()-1)).requestFocus(); return;
        }
        if (!rows.isEmpty()) rows.get(0).views.get(0).requestFocus();
    }
    /** Sequential nextFocus IDs would override geometric native focus search. */
    void wire() {
        for (View view:controls()) if (view.getId()==View.NO_ID) view.setId(View.generateViewId());
    }
}

package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

/** Small remote focus feedback, with an immediate contour and continuously retargeted movement. */
final class TvMotion {
    static boolean enabled(Context context) {
        TvPreferences preferences=new TvPreferences(context);
        return preferences.motion() && !preferences.reduced() && ValueAnimator.areAnimatorsEnabled() && !TvAccessibility.exploring(context);
    }
    static final class Focus {
        private final View view;
        private final float scale, lift;
        private final boolean motion;
        private final SpringAnimation x,y;
        private boolean focused, pressed;
        Focus(View view,float scale,float lift) {
            this.view=view; this.scale=scale; this.lift=lift; motion=enabled(view.getContext());
            x=spring(view,DynamicAnimation.SCALE_X); y=spring(view,DynamicAnimation.SCALE_Y);
            view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View v) { }
                @Override public void onViewDetachedFromWindow(View v) { x.cancel(); y.cancel(); }
            });
        }
        private static SpringAnimation spring(View view,DynamicAnimation.ViewProperty property) {
            SpringAnimation animation=new SpringAnimation(view,property,1f);
            animation.setSpring(new SpringForce(1).setDampingRatio(SpringForce.DAMPING_RATIO_NO_BOUNCY)
                    .setStiffness(SpringForce.STIFFNESS_MEDIUM));
            animation.setMinimumVisibleChange(.001f); return animation;
        }
        void focus(boolean value) { focused=value; if (!value) pressed=false; update(); }
        boolean key(KeyEvent event) {
            int code=event.getKeyCode();
            if (code!=KeyEvent.KEYCODE_DPAD_CENTER && code!=KeyEvent.KEYCODE_ENTER && code!=KeyEvent.KEYCODE_NUMPAD_ENTER) return false;
            boolean next=event.getAction()==KeyEvent.ACTION_DOWN;
            if (next==pressed) return false;
            pressed=next; update(); return false;
        }
        private void update() {
            float target=focused ? scale : 1;
            if (pressed) target*=.975f;
            float depth=focused && !pressed ? lift : 0;
            if (!motion || !ValueAnimator.areAnimatorsEnabled() || TvAccessibility.exploring(view.getContext())) {
                x.cancel(); y.cancel();
                view.setScaleX(1); view.setScaleY(1); view.setTranslationZ(0); return;
            }
            view.setTranslationZ(depth);
            x.animateToFinalPosition(target); y.animateToFinalPosition(target);
        }
    }
}

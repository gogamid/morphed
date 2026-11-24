package app.gogamid.morphed.extension.meindm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Uses the existing React Native button, without changing the Hermes bundle. */
public final class MeinDmAutoActivate {
    private static final String SCREEN = "de.dm.meindm.android.reactnative.fragments.CouponCenterFragment";
    private static final String BUTTON = "activateButtonTestId";
    private static final String SUCCESS = "Dein Coupon wurde erfolgreich aktiviert";
    private static final String CHANNEL = "morphed_coupon_activation";

    private MeinDmAutoActivate() {}

    public static void attach(Object fragment, View root) {
        if (root != null && SCREEN.equals(fragment.getClass().getName())) {
            root.addOnAttachStateChangeListener(new Session(root));
        }
    }

    private static final class Session implements View.OnAttachStateChangeListener, Runnable {
        private final View root;
        private final Set<View> attempted = Collections.newSetFromMap(new WeakHashMap<>());
        private View pending;
        private long deadline;
        private boolean sawSuccess;
        private int count;

        Session(View root) {
            this.root = root;
            if (root.isAttachedToWindow()) onViewAttachedToWindow(root);
        }

        @Override public void onViewAttachedToWindow(View view) {
            root.removeCallbacks(this);
            root.postDelayed(this, 500);
        }

        @Override public void onViewDetachedFromWindow(View view) {
            root.removeCallbacks(this);
            pending = null;
        }

        @Override public void run() {
            if (!root.isAttachedToWindow()) return;
            long delay = 500;
            if (root.isShown() && root.hasWindowFocus() && root.getWindowVisibility() == View.VISIBLE) {
                long now = SystemClock.uptimeMillis();
                if (pending != null) {
                    sawSuccess |= hasLabel(root.getRootView(), SUCCESS);
                    if (sawSuccess && pending.isShown() && hasLabel(pending, "Deaktivieren")) {
                        notifyActivated(root.getContext(), ++count);
                        pending = null;
                        delay = 900;
                    } else if (now >= deadline) {
                        pending = null;
                    } else {
                        delay = 150;
                    }
                } else if (!hasLabel(root.getRootView(), SUCCESS)) {
                    View button = findButton(root);
                    if (button != null && attempted.add(button) && button.performClick()) {
                        pending = button;
                        sawSuccess = false;
                        deadline = now + 12_000;
                        delay = 150;
                    }
                }
            }
            root.postDelayed(this, delay);
        }

        private View findButton(View view) {
            if (!view.isShown()) return null;
            if (BUTTON.equals(view.getTag()) && view.isEnabled() && view.hasOnClickListeners()
                    && !attempted.contains(view) && view.getGlobalVisibleRect(new Rect())
                    && hasLabel(view, "Aktivieren")) return view;
            if (view instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    View match = findButton(group.getChildAt(i));
                    if (match != null) return match;
                }
            }
            return null;
        }
    }

    private static boolean hasLabel(View view, String label) {
        if (!view.isShown()) return false;
        if (view instanceof TextView && label.contentEquals(((TextView) view).getText())) return true;
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                if (hasLabel(group.getChildAt(i), label)) return true;
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static void notifyActivated(Context context, int count) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null || !manager.areNotificationsEnabled()) return;
        try {
            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= 26) {
                manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                        "Coupon-Aktivierung", NotificationManager.IMPORTANCE_DEFAULT));
                builder = new Notification.Builder(context, CHANNEL);
            } else {
                builder = new Notification.Builder(context);
            }
            manager.notify(0x444d, builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("dm-Coupons automatisch aktiviert")
                    .setContentText(count == 1 ? "1 Coupon aktiviert" : count + " Coupons aktiviert")
                    .setOnlyAlertOnce(true).setAutoCancel(true).build());
        } catch (SecurityException ignored) {
            // Notification permission can change during posting.
        }
    }
}

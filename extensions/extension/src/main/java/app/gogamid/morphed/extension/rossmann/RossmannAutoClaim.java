package app.gogamid.morphed.extension.rossmann;

import de.rossmann.app.android.business.coupon.ActivateCoupon;
import de.rossmann.app.android.models.coupon.Coupon;
import de.rossmann.app.android.models.coupon.Coupon.PreviewState.NotPreview;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.coroutines.jvm.internal.ContinuationImpl;

/**
 * Runtime of the "Auto claim coupons" patch for Rossmann (de.rossmann.app.android).
 *
 * The patch injects two calls into the app:
 *
 * - PageViewModelAdapterImpl's constructor hands over the Hilt-provided
 *   ActivateCoupon use case, so the extension drives the app's own activate
 *   action instead of a copy of it.
 * - CouponDisruptorState's factory hands over the coupon list it is about to
 *   turn into a disruptor state. The factory runs for the coupon nag helper,
 *   product search, the cart, order history and product details, so the
 *   coupon list is seen early and often.
 *
 * For every coupon that is not yet in the wallet and is available now, the
 * extension invokes the app's own suspend activate action. Exclusive
 * coupons, wallet limits and other domain refusals come back through the
 * Either result and stay silent, exactly like a refused manual tap.
 */
public final class RossmannAutoClaim {

    /** The app's own activate use case, captured when Hilt wires the pages adapter. */
    private static volatile ActivateCoupon activateCoupon;

    /** Coupon EANs this process already tried, so repeated calls stay silent. */
    private static final Set<String> CLAIMED = ConcurrentHashMap.newKeySet();

    /** Claims run off the main thread; Room and the wallet sync pick their own dispatchers. */
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private RossmannAutoClaim() {
    }

    /** Called by the code injected into PageViewModelAdapterImpl's constructor. */
    public static void register(ActivateCoupon activate) {
        if (activate != null) {
            activateCoupon = activate;
        }
    }

    /** Called by the code injected into CouponDisruptorState's factory. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onCoupons(List coupons) {
        ActivateCoupon activate = activateCoupon;
        if (activate == null || coupons == null || coupons.isEmpty()) {
            return;
        }
        NotPreview availableNow = NotPreview.a;
        for (Object element : coupons) {
            if (!(element instanceof Coupon)) {
                continue;
            }
            Coupon coupon = (Coupon) element;
            if (coupon.q) {
                // Already in the wallet.
                continue;
            }
            if (availableNow == null || coupon.R == null || !availableNow.equals(coupon.R)) {
                // The coupon is a preview and becomes available later. Skip it without
                // marking it attempted, so it is claimed once it becomes available.
                continue;
            }
            String ean = coupon.l;
            if (ean == null || ean.isEmpty()) {
                // No backend identifier, the activate action cannot sync this one.
                continue;
            }
            if (!CLAIMED.add(ean)) {
                // Already attempted in this process.
                continue;
            }
            EXECUTOR.execute(() -> claim(activate, coupon));
        }
    }

    private static void claim(ActivateCoupon activate, Coupon coupon) {
        try {
            // The app's own activate action. Completing the returned continuation is a
            // no-op, so the claim is fire and forget and refusals stay silent.
            activate.c(coupon, new ClaimContinuation());
        } catch (Throwable ignored) {
            // Silent refusals.
        }
    }

    /** Continuation for the app's suspend activate use case. */
    static final class ClaimContinuation extends ContinuationImpl {

        ClaimContinuation() {
            super(TerminalContinuation.INSTANCE);
        }

        @Override
        public Object invokeSuspend(Object result) {
            return Unit.INSTANCE;
        }

        @Override
        protected void releaseIntercepted() {
            // No interceptor to release.
        }
    }

    /** Terminal continuation that drops the result. */
    static final class TerminalContinuation implements Continuation<Object> {

        static final TerminalContinuation INSTANCE = new TerminalContinuation();

        @Override
        public CoroutineContext getContext() {
            return EmptyCoroutineContext.INSTANCE;
        }

        @Override
        public void resumeWith(Object result) {
            // Fire and forget.
        }
    }
}

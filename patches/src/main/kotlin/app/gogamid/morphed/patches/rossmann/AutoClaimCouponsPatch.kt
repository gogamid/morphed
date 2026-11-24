package app.gogamid.morphed.patches.rossmann

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.gogamid.morphed.patches.shared.Constants.COMPATIBILITY_ROSSMANN

private const val EXTENSION_CLASS = "Lapp/gogamid/morphed/extension/rossmann/RossmannAutoClaim;"

/**
 * Hands the Hilt-provided ActivateCoupon use case of the app to the extension.
 * Internal: required by [autoClaimCouponsPatch], not shown in Morphe Manager.
 */
internal val captureActivateCouponPatch = patch {
    extendWith("extensions/extension.mpe")

    execute {
        // The first constructor argument is the app's own ActivateCoupon use case.
        PageViewModelAdapterConstructorFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p1}, $EXTENSION_CLASS->register(Lde/rossmann/app/android/business/coupon/ActivateCoupon;)V
            """.trimIndent(),
        )
    }
}

/**
 * Activates every coupon that is available and not yet in the wallet when the
 * app presents coupons. Each activation is the app's own activate action, so
 * exclusive coupons and wallet limits keep their rules and refusals stay
 * silent. Coupons that become available later are claimed once they are.
 */
@Suppress("unused")
val autoClaimCouponsPatch = patch(
    name = "Auto claim coupons",
    description = "Activates every coupon that is available and not yet in the wallet when the app presents coupons. Each activation is the app's own activate action.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ROSSMANN)

    dependsOn(captureActivateCouponPatch)

    extendWith("extensions/extension.mpe")

    execute {
        // The factory receives the applicable coupon list for the current
        // context before turning it into a disruptor state. The extension
        // claims from that list through the app's own use case.
        CouponDisruptorCreateFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, $EXTENSION_CLASS->onCoupons(Ljava/util/List;)V
            """.trimIndent(),
        )
    }
}

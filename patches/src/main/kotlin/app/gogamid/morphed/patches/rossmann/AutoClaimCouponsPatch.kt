package app.gogamid.morphed.patches.rossmann

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.gogamid.morphed.patches.shared.Constants.COMPATIBILITY_ROSSMANN
import com.android.tools.smali.dexlib2.Opcode

private const val EXTENSION_CLASS = "Lapp/gogamid/morphed/extension/rossmann/RossmannAutoClaim;"
private const val COMPONENT = "Lde/rossmann/app/android/ui/DaggerRossmannApplication_HiltComponents_SingletonC\$SingletonCImpl;"
private const val ACTIVATE = "Lde/rossmann/app/android/business/coupon/ActivateCoupon;"

@Suppress("unused")
val autoClaimCouponsPatch = patch(
    name = "Auto claim coupons",
    description = "Activates eligible coupons as they load, including in the Coupons tab, and notifies you when Rossmann confirms activation.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_ROSSMANN)
    extendWith("extensions/extension.mpe")

    execute {
        val onCreate = RossmannApplicationOnCreateFingerprint.method
        val returns = onCreate.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_VOID }
        check(returns.size == 1) { "Expected one normal return in RossmannApplication.onCreate" }
        // v0/v1 are dead at the return. Hilt and application startup have completed.
        onCreate.addInstructions(
            returns.single().index,
            """
                invoke-virtual {p0}, Lde/rossmann/app/android/ui/Hilt_RossmannApplication;->C0()Ljava/lang/Object;
                move-result-object v0
                check-cast v0, $COMPONENT
                invoke-virtual {v0}, $COMPONENT->n()$ACTIVATE
                move-result-object v0
                sget-object v1, Lkotlin/coroutines/EmptyCoroutineContext;->a:Lkotlin/coroutines/EmptyCoroutineContext;
                invoke-static {p0, v0, v1}, $EXTENSION_CLASS->register(Landroid/content/Context;${ACTIVATE}Lkotlin/coroutines/CoroutineContext;)V
            """.trimIndent(),
        )

        CouponDisplayModelFingerprint.method.addInstructions(
            0,
            "invoke-static {p0}, $EXTENSION_CLASS->onCoupon(Lde/rossmann/app/android/models/coupon/Coupon;)V",
        )
        CouponDisruptorCreateFingerprint.method.addInstructions(
            0,
            "invoke-static {p0}, $EXTENSION_CLASS->onCoupons(Ljava/util/List;)V",
        )
    }
}

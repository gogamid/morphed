package app.gogamid.morphed.patches.meindm

import app.gogamid.morphed.patches.shared.Constants.COMPATIBILITY_MEIN_DM
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.rawResourcePatch

private object ReactFragmentViewCreatedFingerprint : Fingerprint(
    definingClass = "Lde/dm/meindm/android/reactnative/fragments/ReactFragment;",
    name = "onViewCreated",
    parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
    returnType = "V",
)

private val verifyMeinDmBundle = rawResourcePatch {
    execute {
        get(MeinDmHermesBundle.PATH).inputStream().use(MeinDmHermesBundle::requireSupported)
    }
}

@Suppress("unused")
val meinDmAutoActivateCouponsPatch = patch(
    name = "Auto activate dm coupons",
    description = "Experimental: activates visible German-language dm coupons in the foreground Coupons tab. Scroll to load more. A notification follows the app's successful activation message.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_MEIN_DM)
    dependsOn(verifyMeinDmBundle)
    extendWith("extensions/extension.mpe")

    execute {
        ReactFragmentViewCreatedFingerprint.method.addInstructions(
            0,
            "invoke-static {p0, p1}, Lapp/gogamid/morphed/extension/meindm/MeinDmAutoActivate;->attach(Ljava/lang/Object;Landroid/view/View;)V",
        )
    }
}

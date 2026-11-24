package app.gogamid.morphed.patches.meindm

import app.gogamid.morphed.patches.shared.Constants.COMPATIBILITY_MEIN_DM
import app.gogamid.morphed.patches.shared.pairip.pairipLicenseCheckPatch
import app.morphe.patcher.patch.patch

/**
 * PairIP blocks every repackaged build: the Play licensing service refuses the
 * install and the signature check throws. This patch disables both gates so the
 * rest of the app, including its virtualized code, keeps working.
 */
@Suppress("unused")
val disablePlayLicenseCheckPatch = patch(
    name = "Disable Play license check",
    description = "Stops the Google Play license and signature check from blocking the patched app with a \"Get this app from Play\" screen.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_MEIN_DM)
    dependsOn(pairipLicenseCheckPatch)
}

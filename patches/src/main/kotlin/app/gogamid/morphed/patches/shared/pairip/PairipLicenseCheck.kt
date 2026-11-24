package app.gogamid.morphed.patches.shared.pairip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch

/**
 * PairIP license check, adapted from amfakh/fakh-patches (GPL-3.0-only).
 *
 * `com.pairip.licensecheck.LicenseClient.checkLicense(Context)` asks the Play
 * licensing service about the install. A repackaged APK is never licensed, so
 * the service returns a failure and the app opens Play's "Get this app from
 * Play" screen and closes. Called from the PairIP application shell and from
 * LicenseContentProvider.
 */
private val checkLicenseFingerprint = Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    custom = { method, classDef ->
        classDef.type.startsWith("Lcom/pairip/licensecheck/") && method.name == "checkLicense"
    },
)

/**
 * `com.pairip.SignatureCheck.verifyIntegrity(Context)` compares the APK signing
 * certificate with PairIP's pinned hashes and throws when they differ, which
 * happens for every repackaged build. Mein dm calls it from the PairIP shell's
 * attachBaseContext before the license check.
 */
private val verifyIntegrityFingerprint = Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
)

/** Neutralizes PairIP's Play licensing and signature gates. Internal dependency. */
internal val pairipLicenseCheckPatch = patch {
    execute {
        checkLicenseFingerprint.method.addInstructions(0, "return-void")
        verifyIntegrityFingerprint.method.addInstructions(0, "return-void")
    }
}

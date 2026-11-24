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
 * happens for every repackaged build.
 */
private val verifyIntegrityFingerprint = Fingerprint(
    definingClass = "Lcom/pairip/SignatureCheck;",
    name = "verifyIntegrity",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
)

/**
 * `LicenseActivity.onStart` sends Play's paywall intent and then closes the app.
 * Returning at its start covers every path that reaches the activity, including
 * paths that never call [checkLicenseFingerprint].
 */
private val licenseActivityOnStartFingerprint = Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "onStart",
    returnType = "V",
)

/**
 * `LicenseClient.initializeLicenseCheck()` binds the Play licensing service. The
 * Java entry point is [checkLicenseFingerprint], but PairIP's VM can reach this
 * instance method directly, so it is neutralized as well.
 */
private val initializeLicenseCheckFingerprint = Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "initializeLicenseCheck",
    returnType = "V",
)

/**
 * `StartupLauncher.launch()` runs PairIP's native startup program from the static
 * initializer of the app component factory, before the Application exists. On
 * Mein dm it keeps triggering the Play gate even when every Java check above is
 * neutral. The rest of the app executes its own VM programs per call, so the
 * startup program is not needed for them.
 */
private val startupLauncherFingerprint = Fingerprint(
    definingClass = "Lcom/pairip/StartupLauncher;",
    name = "launch",
    returnType = "V",
)

/** Neutralizes PairIP's Play licensing and signature gates. Internal dependency. */
internal val pairipLicenseCheckPatch = patch {
    execute {
        checkLicenseFingerprint.method.addInstructions(0, "return-void")
        verifyIntegrityFingerprint.method.addInstructions(0, "return-void")
        licenseActivityOnStartFingerprint.method.addInstructions(0, "return-void")
        initializeLicenseCheckFingerprint.method.addInstructions(0, "return-void")
        startupLauncherFingerprint.method.addInstructions(0, "return-void")
    }
}

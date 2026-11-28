package app.gogamid.morphed.patches.projectivy

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

/*
 * Fingerprints for Projectivy Launcher 4.73 (version code 98).
 *
 * The app keeps its class names but renames its own members with R8, so every
 * declaration here is pinned to the supported build.
 */

internal const val PT_APPLICATION = "Lcom/spocky/projengmenu/PTApplication;"

/** The native licensing wrapper; `vs(Z)Z` is the JNI entry point. */
internal object PremiumNativeCheckFingerprint : Fingerprint(
    definingClass = PT_APPLICATION,
    name = "f",
    returnType = "Z",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

/** Creates the premium StateFlow; true means locked. */
internal object PremiumStateInitFingerprint : Fingerprint(
    definingClass = PT_APPLICATION,
    name = "<init>",
    returnType = "V",
    parameters = emptyList(),
)

/** Billing callback that updates the premium StateFlow. */
internal object PremiumStateSetFingerprint : Fingerprint(
    definingClass = PT_APPLICATION,
    name = "d",
    returnType = "V",
    parameters = listOf("Z"),
)

/**
 * Decrypts an appearance backup blob. The preset importer hands the whole
 * `.pltheme` ZIP to this method, which cannot decrypt a ZIP and fails with
 * `IllegalBlockSizeException`; the fix patch unwraps the archive first.
 */
internal object ThemeBackupDecryptFingerprint : Fingerprint(
    definingClass = "Ly71;",
    name = "b",
    parameters = listOf("[B"),
    returnType = "[B",
)

/**
 * Initialises `s0`, the extra key-material prefix length used by the appearance
 * decryptor. The arm32 distribution build sets it to 2, which shifts the IV and
 * makes every `.plbackup` fail with `WRONG_FINAL_BLOCK_LENGTH`.
 */
internal object AccessibilityClinitFingerprint : Fingerprint(
    definingClass = "Lcom/spocky/projengmenu/services/ProjectivyAccessibilityService;",
    name = "<clinit>",
    returnType = "V",
)

/*
 * Copyright (C) 2026 gogamid
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Projectivy imports appearance presets by handing the whole `.pltheme` ZIP to
 * its appearance decryptor (`y71.b([B)[B`). That method expects the decrypted
 * material layout `key material | IV | AES-CBC ciphertext` and therefore fails
 * on a ZIP with `IllegalBlockSizeException: WRONG_FINAL_BLOCK_LENGTH`; the UI
 * then reports "An error occurred while retrieving config file".
 *
 * The fix prepends an archive unwrap in that method: if the input is a ZIP, the
 * `appearance.plbackup` entry is extracted and the existing decryption runs on
 * its bytes. Plain appearance backups are passed through untouched.
 */
package app.gogamid.morphed.patches.projectivy

import app.gogamid.morphed.patches.shared.Constants.COMPATIBILITY_PROJECTIVY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val THEME_ARCHIVE = "Lapp/gogamid/morphed/extension/projectivy/ThemeArchive;"

@Suppress("unused")
val fixThemePresetImportPatch = bytecodePatch(
    name = "Fix theme preset import",
    description = "Fixes importing appearance presets (Settings > Appearance > Themes > Presets), which otherwise fails with an error because the .pltheme archive is not unwrapped before decryption.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PROJECTIVY)
    extendWith("extensions/extension.mpe")

    execute {
        ThemeBackupDecryptFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, $THEME_ARCHIVE->unwrap([B)[B
                move-result-object p0
            """.trimIndent(),
        )
    }
}

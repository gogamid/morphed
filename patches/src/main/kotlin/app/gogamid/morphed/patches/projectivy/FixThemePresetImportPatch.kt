/*
 * Copyright (C) 2026 gogamid
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Projectivy 4.73 (arm32 rebuild) cannot import appearance presets:
 *
 *  - `appearance.plbackup` is encrypted as `key material (16) | IV (16) | AES-CBC
 *    ciphertext`, but `y71.b` splits it using `ProjectivyAccessibilityService.s0 + 16`
 *    for the material length. The arm32 build initialises `s0` to 2, so it reads
 *    18 + 16 bytes and the remaining ciphertext is not block aligned:
 *    `IllegalBlockSizeException: WRONG_FINAL_BLOCK_LENGTH`.
 *
 *  - Importing a `.pltheme` archive from storage hands the whole ZIP to the same
 *    decryptor, which is also not a valid blob. The extension unwraps the
 *    `appearance.plbackup` entry first and passes plain blobs through.
 */
package app.gogamid.morphed.patches.projectivy

import app.gogamid.morphed.patches.shared.Constants.COMPATIBILITY_PROJECTIVY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val THEME_ARCHIVE = "Lapp/gogamid/morphed/extension/projectivy/ThemeArchive;"
private const val S0_FIELD = "Lcom/spocky/projengmenu/services/ProjectivyAccessibilityService;->s0:B"

@Suppress("unused")
val fixThemePresetImportPatch = bytecodePatch(
    name = "Fix theme preset import",
    description = "Fixes importing appearance presets (Settings > Appearance > Themes > Import), which otherwise fails because the appearance decryptor uses a wrong key-material offset in this build and does not unwrap .pltheme archives.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PROJECTIVY)
    extendWith("extensions/extension.mpe")

    execute {
        AccessibilityClinitFingerprint.method.apply {
            val sput = instructions.withIndex().single { (_, instruction) ->
                instruction.getReference<FieldReference>()?.toString() == S0_FIELD
            }.index
            check(sput > 0) { "Projectivy s0 initialiser changed" }
            val register = getInstruction<OneRegisterInstruction>(sput - 1).registerA
            replaceInstruction(sput - 1, "const/4 v$register, 0x0")
        }

        ThemeBackupDecryptFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0}, $THEME_ARCHIVE->unwrap([B)[B
                move-result-object p0
            """.trimIndent(),
        )
    }
}

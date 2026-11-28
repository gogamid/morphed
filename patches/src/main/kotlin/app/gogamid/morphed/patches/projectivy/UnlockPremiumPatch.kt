/*
 * Copyright (C) 2026 Saiesh Nitin Shirodkar
 * Copyright (C) 2026 johninnld
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted for Morphed from johninnld/nl-morphe-patches
 * (patches/src/main/kotlin/patches/projectivy/UnlockPremiumPatch.kt), which in
 * turn adapted saieshshirodkar/saiesh-morphe-patches at
 * bc8d7d9fb066f6eb5e1095c148b44df9899d381d for Projectivy 4.71.
 * Verified here against Projectivy 4.73 (version code 98).
 */
package app.gogamid.morphed.patches.projectivy

import app.gogamid.morphed.patches.shared.Constants.COMPATIBILITY_PROJECTIVY
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks premium features by bypassing the native check and keeping the app's premium feature gates unlocked.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_PROJECTIVY)

    execute {
        // This distribution build keeps its own licensing semantics; the wrapper
        // is left untouched because forcing it hid premium-only UI (Presets).

        // True means locked. Keep the original StateFlow and its read-only view.
        // Already-unlocked builds (re-applying the patch) already read FALSE.
        PremiumStateInitFingerprint.method.apply {
            val initial = instructions.withIndex().firstOrNull { (_, instruction) ->
                instruction.getReference<FieldReference>()?.toString() ==
                    "Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;"
            }
            val alreadyUnlocked = instructions.any { instruction ->
                instruction.getReference<FieldReference>()?.toString() ==
                    "Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;"
            }
            check(initial != null || alreadyUnlocked) { "Projectivy premium state initialiser changed" }
            if (initial != null) {
                val register = getInstruction<OneRegisterInstruction>(initial.index).registerA
                replaceInstruction(
                    initial.index,
                    "sget-object v$register, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;",
                )
            }
        }

        // Billing callbacks update the same flow. Preserve the native setter,
        // but prevent an unpaid or expired response from locking features again.
        PremiumStateSetFingerprint.method.apply {
            // Field types differ between Projectivy distribution builds, so the
            // setter is pinned by class, name and signature instead.
            addInstructions(0, "const/4 p1, 0x0")
        }
    }
}

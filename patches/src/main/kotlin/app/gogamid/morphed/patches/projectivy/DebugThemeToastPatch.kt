package app.gogamid.morphed.patches.projectivy

import app.gogamid.morphed.patches.shared.Constants.COMPATIBILITY_PROJECTIVY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

/** Temporary diagnostic: logs the call chain whenever Projectivy shows a toast. */
private object ToastFingerprint : Fingerprint(
    definingClass = "Ldz2;",
    name = "d",
    parameters = listOf("I", "Ljava/lang/String;"),
    returnType = "V",
)

@Suppress("unused")
val debugThemeToastPatch = bytecodePatch(
    name = "[debug] log toasts",
    description = "Temporary: logs the stack trace behind every Projectivy toast.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_PROJECTIVY)
    extendWith("extensions/extension.mpe")
    execute {
        ToastFingerprint.method.addInstructions(
            0,
            "invoke-static {}, Lapp/gogamid/morphed/extension/projectivy/ThemeDebug;->toast()V",
        )
    }
}

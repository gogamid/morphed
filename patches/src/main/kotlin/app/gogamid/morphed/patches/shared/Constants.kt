package app.gogamid.morphed.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_MEIN_DM = Compatibility(
        name = "Mein dm",
        packageName = "de.dm.meindm.android",
        apkFileType = ApkFileType.APKM,
        appIconColor = 0x00366C,
        targets = listOf(AppTarget(version = "6.23.0")),
    )

    val COMPATIBILITY_ROSSMANN = Compatibility(
        name = "Rossmann",
        packageName = "de.rossmann.app.android",
        apkFileType = ApkFileType.APKM,
        // Rossmann red.
        appIconColor = 0xD0082C,
        targets = listOf(
            AppTarget(version = "5.11.1"),
        ),
    )
}

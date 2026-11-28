package app.gogamid.morphed.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.FormFactor

object Constants {
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

    val COMPATIBILITY_PROJECTIVY = Compatibility(
        name = "Projectivy Launcher",
        packageName = "com.spocky.projengmenu",
        apkFileType = ApkFileType.APK,
        formFactor = FormFactor.TV,
        appIconColor = 0x1E88E5,
        // Publisher signing certificate, pinned by the upstream patch source.
        signatures = setOf("f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd"),
        targets = listOf(
            AppTarget(version = "4.73", versionCode = 98, minSdk = 23),
        ),
    )
}

package app.gogamid.morphed.patches.meindm

import app.morphe.patcher.patch.PatchException
import java.io.InputStream
import java.security.MessageDigest

/** Version guard for the Coupons hook; APK version alone does not pin Hermes function IDs. */
internal object MeinDmHermesBundle {
    const val PATH = "assets/index.android.bundle"
    private const val SHA256 = "b39647e24a5e8eeab89098378a212d575ee9f540428d5adbeb723af7dfd8024c"

    /** The caller owns [input] and must validate it before writing any edits. */
    fun requireSupported(input: InputStream) {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
        if (actual != SHA256) {
            throw PatchException("Unsupported Mein dm Hermes bundle; expected Germany 6.23.0 (194649)")
        }
    }
}

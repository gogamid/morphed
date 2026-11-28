package app.gogamid.morphed.extension.projectivy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Archive helper for Projectivy appearance presets.
 *
 * A `.pltheme` preset is a ZIP that contains the encrypted appearance backup as
 * `appearance.plbackup`. Projectivy 4.73 decrypts the raw stream, so importing a
 * bundled or downloaded preset fails unless the entry is extracted first.
 */
public final class ThemeArchive {

    private ThemeArchive() {
    }

    /** Returns the appearance backup from a `.pltheme` ZIP, or the input unchanged. */
    public static byte[] unwrap(byte[] data) {
        ThemeDebug.note("decrypt input: " + (data == null ? "null" : data.length + " bytes, head "
                + String.format("%02x%02x%02x%02x", data.length > 0 ? data[0] : 0,
                data.length > 1 ? data[1] : 0, data.length > 2 ? data[2] : 0,
                data.length > 3 ? data[3] : 0)));
        if (data == null || data.length < 4
                || data[0] != 'P' || data[1] != 'K' || data[2] != 3 || data[3] != 4) {
            return data;
        }
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name != null && name.endsWith(".plbackup")) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = zip.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                    byte[] extracted = out.toByteArray();
                    ThemeDebug.note("unwrapped " + name + ": " + extracted.length + " bytes");
                    return extracted;
                }
            }
        } catch (IOException ignored) {
            // Not a readable archive; keep the original bytes for the caller.
        }
        return data;
    }
}

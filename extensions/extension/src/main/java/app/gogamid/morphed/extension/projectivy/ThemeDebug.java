package app.gogamid.morphed.extension.projectivy;

import android.util.Log;

/** Temporary diagnostic helper: records the call chain that shows an error toast. */
public final class ThemeDebug {

    private static final String TAG = "MorphedProjectivy";

    private ThemeDebug() {
    }

    public static void toast() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        StringBuilder builder = new StringBuilder("toast shown");
        int limit = Math.min(stack.length, 12);
        for (int i = 1; i < limit; i++) {
            builder.append("\n  at ").append(stack[i]);
        }
        Log.e(TAG, builder.toString());
    }

    public static void note(String message) {
        Log.e(TAG, message);
    }
}

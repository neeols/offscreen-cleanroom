package examples.single.groovy

// Requires OffscreenCleanroom on Minecraft 1.12.2 Cleanroom.
// Add this file to your addon with its language runtime enabled.
// Call open() from a client key/button handler after joining a world; declarations open nothing.
// Call close() from your client-thread disconnect/unload hook.
// The server-action example additionally registers its panel during common startup on both sides.
import com.sbancuz.offscreen.api.script.OffscreenWindows as Windows
import com.sbancuz.offscreen.api.script.OffscreenFiles as Files
import com.sbancuz.offscreen.api.script.OffscreenPanel as Panel
import com.sbancuz.offscreen.api.script.OffscreenWindows.*
import com.sbancuz.offscreen.api.script.OffscreenFiles.*
import com.sbancuz.offscreen.api.script.OffscreenPanel.Context
import com.sbancuz.offscreen.api.script.OffscreenPanel.Player

@groovy.transform.CompileStatic
final class MonitorExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private static Window owned = null;
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    public static void header(ScreenEvent event) {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464);
        event.rect(20, 20, 3, 16, -10103043);
        event.text("Memory monitor", 32, 23, -1445387, false);
        event.text("Heap usage in MiB; 30 seconds of history.", 32, 42, -6372673, false);
        event.rect(20, 62, event.getWidth() - 40, 1, -13614255);
    }
    
    // Small local arithmetic helper; it is included rather than supplied by a hidden utility file.
    public static int minInt(double a, double b) {
        if (a < b) {
            return (int) (a);
        }
        return (int) (b);
    }
    
    // Clamp layout and scrolling without allocating objects during drawing.
    public static int maxInt(double a, double b) {
        if (a > b) {
            return (int) (a);
        }
        return (int) (b);
    }
    
    // Build fresh screen state, declare callbacks, then open once. A returned handle can precede
    // the first layout; readiness is separate from ownership. No window opens during file loading.
    public static void open() {
        close();
        Screen screen = Windows.screen("Memory monitor", 960, 640);
        screen.getDefaults().integer("ticks", 0);
        screen.getDefaults().integer("cursor", 0);
        screen.getDefaults().integer("count", 0);
        screen.on("draw", { ScreenEvent event -> draw(event) });
        screen.on("tick", { ScreenEvent event -> tick(event) });
        owned = screen.open();
    }
    
    // Take one sample every five client ticks, independent of frame rate. Exactly 120 keys are reused
    // as a ring buffer; running for hours cannot grow the history or its memory footprint.
    public static boolean tick(ScreenEvent event) {
        int ticks = (int) (event.getState().integer("ticks") + 1);
        event.getState().integer("ticks", ticks);
        if (ticks % 5 != 0) {
            return false;
        }
        int cursor = (int) (event.getState().integer("cursor"));
        event.getState().number("sample" + cursor, Windows.usedHeapMiB());
        event.getState().integer("cursor", (cursor + 1) % 120);
        event.getState().integer("count", minInt(120, event.getState().integer("count") + 1));
        return false;
    }
    
    // Calculate columns from current GUI units, not desktop pixels. Cards retain their internal padding.
    // The chart reads oldest-to-newest even after wraparound; drawing never creates samples.
    public static boolean draw(ScreenEvent event) {
        header(event);
        int columns = (int) (1);
        if (event.getWidth() >= 300) {
            columns = 2;
        }
        if (event.getWidth() >= 440) {
            columns = 3;
        }
        int cardWidth = (int) (maxInt(40, (event.getWidth() - 40 - (columns - 1) * 12) / columns));
        for (int i = 0; i < 3; i++) {
            int left = (int) (20 + (i % columns) * (cardWidth + 12));
            int top = (int) (82 + i.intdiv(columns) * 76);
            event.rect(left, top, cardWidth, 64, -15062988);
            String label = "Used: -- MiB";
            if (event.getState().integer("count") > 0) {
                label = "Used: " + (int) (event.getState().number("sample" + ((event.getState().integer("cursor") + 119) % 120))) + " MiB";
            }
            if (i == 1) {
                label = "Limit: " + (int) (Windows.heapLimitMiB()) + " MiB";
            }
            if (i == 2) {
                label = "Samples: " + event.getState().integer("count");
            }
            event.text(label, left + 12, top + 26, -1445387, false);
        }
        int top = (int) (94 + (3 + columns - 1).intdiv(columns) * 76);
        int bottom = (int) (event.getHeight() - 20);
        if (bottom - top < 25) {
            return false;
        }
        // Keep a fixed vertical scale for the entire session. Rescaling to the latest peak would
        // move every old bar whenever allocation or garbage collection changes that peak.
        int count = (int) (event.getState().integer("count"));
        double peak = Windows.heapLimitMiB();
        event.rect(20, top, event.getWidth() - 40, bottom - top, -15062988);
        for (int i = 0; i < count; i++) {
            int index = (int) ((event.getState().integer("cursor") - count + i + 120) % 120);
            int left = (int) (20 + i * (event.getWidth() - 40) / 120);
            int height = (int) ((int) ((bottom - top - 4) * event.getState().number("sample" + index) / peak));
            event.rect(left, bottom - height, maxInt(1, (event.getWidth() - 40) / 120), height, -10103043);
        }
        return false;
    }
    
    // Close only this file's owned window. Closing also emits the screen close callback.
    // Call on the client thread; repeated calls are harmless. Never use global closeAll for addon cleanup.
    public static void close() {
        if (owned != null) {
            owned.close();
        }
        owned = null;
    }
    
}

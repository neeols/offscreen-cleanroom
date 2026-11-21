package examples.single.java;

// Requires OffscreenCleanroom on Minecraft 1.12.2 Cleanroom.
// Add this file to your addon with its language runtime enabled.
// Call open() from a client key/button handler after joining a world; declarations open nothing.
// Call close() from your client-thread disconnect/unload hook.
// The server-action example additionally registers its panel during common startup on both sides.
import com.sbancuz.offscreen.api.script.OffscreenWindows;
import com.sbancuz.offscreen.api.script.OffscreenWindows.*;
import com.sbancuz.offscreen.api.script.OffscreenFiles;
import com.sbancuz.offscreen.api.script.OffscreenFiles.*;
import com.sbancuz.offscreen.api.script.OffscreenPanel;
import com.sbancuz.offscreen.api.script.OffscreenPanel.Context;
import com.sbancuz.offscreen.api.script.OffscreenPanel.Player;

public final class DashboardExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private static Window owned = null;
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    public static void header(ScreenEvent event) {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464);
        event.rect(20, 20, 3, 16, -10103043);
        event.text("Responsive dashboard", 32, 23, -1445387, false);
        event.text("Resize the window to rearrange the cards.", 32, 42, -6372673, false);
        event.rect(20, 62, event.getWidth() - 40, 1, -13614255);
    }
    
    // Small local arithmetic helper; it is included rather than supplied by a hidden utility file.
    public static int minInt(int a, int b) {
        if (a < b) {
            return a;
        }
        return b;
    }
    
    // Clamp layout and scrolling without allocating objects during drawing.
    public static int maxInt(int a, int b) {
        if (a > b) {
            return a;
        }
        return b;
    }
    
    // Build fresh screen state, declare callbacks, then open once. A returned handle can precede
    // the first layout; readiness is separate from ownership. No window opens during file loading.
    public static void open() {
        close();
        Screen screen = OffscreenWindows.screen("Responsive dashboard", 960, 640);
        screen.getDefaults().integer("ticks", 0);
        screen.getDefaults().integer("cursor", 0);
        screen.getDefaults().integer("count", 0);
        screen.on("draw", event -> draw(event));
        owned = screen.open();
    }
    
    // Calculate columns from current GUI units, not desktop pixels. Cards retain their internal padding.
    // The chart reads oldest-to-newest even after wraparound; drawing never creates samples.
    public static boolean draw(ScreenEvent event) {
        header(event);
        int columns = 1;
        if (event.getWidth() >= 300) {
            columns = 2;
        }
        if (event.getWidth() >= 440) {
            columns = 3;
        }
        int cardWidth = maxInt(40, (event.getWidth() - 40 - (columns - 1) * 12) / columns);
        for (int i = 0; i < 3; i++) {
            int left = 20 + (i % columns) * (cardWidth + 12);
            int top = 82 + (i / columns) * 76;
            event.rect(left, top, cardWidth, 64, -15062988);
            String label = "Columns: " + columns;
            if (i == 1) {
                label = "Width: " + event.getWidth();
            }
            if (i == 2) {
                label = "Height: " + event.getHeight();
            }
            event.text(label, left + 12, top + 26, -1445387, false);
        }
        event.text("Columns: " + columns, 24, event.getHeight() - 24, -6372673, false);
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

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

public final class SettingsExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private static Window owned = null;
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    public static void header(ScreenEvent event) {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464);
        event.rect(20, 20, 3, 16, -10103043);
        event.text("Settings", 32, 23, -1445387, false);
        event.text("Edit a draft, then apply it to disk.", 32, 42, -6372673, false);
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
        Screen screen = OffscreenWindows.screen("Settings", 960, 640);
        TextResult loaded = OffscreenFiles.read("config/offscreen-settings.txt", "Player");
        screen.textField("name", 36, 105, 400, 24, loaded.value()).maxLength(40);
        screen.getDefaults().text("message", "Edit the name, then apply your changes.");
        if (!loaded.success()) {
            screen.getDefaults().text("message", loaded.error());
        }
        screen.button("Apply changes", 36, 158, 120, 26, clicked -> apply(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
        screen.button("Cancel", 168, 158, 100, 26, clicked -> cancel(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
        screen.button("Reset", 280, 158, 100, 26, clicked -> reset(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
        screen.on("draw", event -> draw(event));
        owned = screen.open();
    }
    
    // The widget renders its own editable field; this callback draws labels and validation feedback.
    public static boolean draw(ScreenEvent event) {
        header(event);
        event.text("DISPLAY NAME", 36, 85, -6372673, false);
        event.text(event.getState().text("message"), 24, 215, -1445387, false);
        return false;
    }
    
    // Validate the draft before I/O. Saving is explicit; window state alone is not disk persistence.
    public static void apply(Window clicked) {
        String draft = clicked.widget("name").getText().trim();
        if (draft.equals("")) {
            clicked.getState().text("message", "Enter a name before saving.");
            return;
        }
        // This is deliberately a tiny local settings file. Large/network writes belong in a worker.
        // The API writes a temporary sibling before replacing the previous settings document.
        TextResult saved = OffscreenFiles.write("config/offscreen-settings.txt", draft);
        if (saved.success()) {
            clicked.getState().text("message", "Saved. Close and reopen to load this value.");
        }
        else {
            clicked.getState().text("message", saved.error());
        }
    }
    
    // Reset affects the visible draft only. It must never silently overwrite the saved file.
    public static void reset(Window clicked) {
        clicked.widget("name").text("Player");
        clicked.getState().text("message", "Default restored in the draft. Apply to save.");
    }
    
    // Discard the current draft by closing this exact owner; no write occurs.
    public static void cancel(Window clicked) {
        clicked.close();
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

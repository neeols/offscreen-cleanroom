package examples.single.scala

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

object SettingsExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private var owned: Window = null
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    def header(event: ScreenEvent): Unit = {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464)
        event.rect(20, 20, 3, 16, -10103043)
        event.text("Settings", 32, 23, -1445387, false)
        event.text("Edit a draft, then apply it to disk.", 32, 42, -6372673, false)
        event.rect(20, 62, event.getWidth() - 40, 1, -13614255)
    }
    
    // Small local arithmetic helper; it is included rather than supplied by a hidden utility file.
    def minInt(a: Int, b: Int): Int = {
        if (a < b) {
            return a
        }
        return b
    }
    
    // Clamp layout and scrolling without allocating objects during drawing.
    def maxInt(a: Int, b: Int): Int = {
        if (a > b) {
            return a
        }
        return b
    }
    
    // Build fresh screen state, declare callbacks, then open once. A returned handle can precede
    // the first layout; readiness is separate from ownership. No window opens during file loading.
    def open(): Unit = {
        close()
        val screen: Screen = Windows.screen("Settings", 960, 640)
        val loaded: TextResult = Files.read("config/offscreen-settings.txt", "Player")
        screen.textField("name", 36, 105, 400, 24, loaded.value()).maxLength(40)
        screen.getDefaults().text("message", "Edit the name, then apply your changes.")
        if (!loaded.success()) {
            screen.getDefaults().text("message", loaded.error())
        }
        screen.button("Apply changes", 36, 158, 120, 26, (clicked: Window) => apply(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1)
        screen.button("Cancel", 168, 158, 100, 26, (clicked: Window) => cancel(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1)
        screen.button("Reset", 280, 158, 100, 26, (clicked: Window) => reset(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1)
        screen.on("draw", (event: ScreenEvent) => draw(event))
        owned = screen.open()
    }
    
    // The widget renders its own editable field; this callback draws labels and validation feedback.
    def draw(event: ScreenEvent): Boolean = {
        header(event)
        event.text("DISPLAY NAME", 36, 85, -6372673, false)
        event.text(event.getState().text("message"), 24, 215, -1445387, false)
        return false
    }
    
    // Validate the draft before I/O. Saving is explicit; window state alone is not disk persistence.
    def apply(clicked: Window): Unit = {
        val draft: String = clicked.widget("name").getText().trim()
        if (draft == "") {
            clicked.getState().text("message", "Enter a name before saving.")
            return
        }
        // This is deliberately a tiny local settings file. Large/network writes belong in a worker.
        // The API writes a temporary sibling before replacing the previous settings document.
        val saved: TextResult = Files.write("config/offscreen-settings.txt", draft)
        if (saved.success()) {
            clicked.getState().text("message", "Saved. Close and reopen to load this value.")
        }
        else {
            clicked.getState().text("message", saved.error())
        }
    }
    
    // Reset affects the visible draft only. It must never silently overwrite the saved file.
    def reset(clicked: Window): Unit = {
        clicked.widget("name").text("Player")
        clicked.getState().text("message", "Default restored in the draft. Apply to save.")
    }
    
    // Discard the current draft by closing this exact owner; no write occurs.
    def cancel(clicked: Window): Unit = {
        clicked.close()
    }
    
    // Close only this file's owned window. Closing also emits the screen close callback.
    // Call on the client thread; repeated calls are harmless. Never use global closeAll for addon cleanup.
    def close(): Unit = {
        if (owned != null) {
            owned.close()
        }
        owned = null
    }
    
}

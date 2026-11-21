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

object ServerActionExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private var owned: Window = null
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    def header(event: ScreenEvent): Unit = {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464)
        event.rect(20, 20, 3, 16, -10103043)
        event.text("Server action", 32, 23, -1445387, false)
        event.text("Review locally; authorization belongs to the server.", 32, 42, -6372673, false)
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
    
    // Register once during common initialization on both physical sides. The same schema must be installed.
    // This example requires creative mode; replace that predicate with your own actual permission rule.
    private var registered: Boolean = false
    // Declare server-owned fields and actions before any window opens. Read-only prevents a client
    // from fabricating confirmation or a counter value through an ordinary value-update packet.
    def register(): Unit = {
        if (registered) {
            return
        }
        val panel: Panel = Windows.panel("single_confirmation_scala", "example", 300, 190)
        panel.colors(-15722464, -1445387, -15062988, -13612969)
        panel.text("summary", "Actions applied: 0", 80).readOnly("summary")
        panel.text("status", "Confirm the action first.", 120).readOnly("status")
        panel.flag("confirmed", false).readOnly("confirmed")
        panel.number("count", 0.0, 0.0, 1000000.0).readOnly("count")
        panel.permission((player: Player) => allowed(player))
        panel.action("confirm", (context: Context) => confirm(context))
        panel.action("apply", (context: Context) => apply(context))
        panel.control("label", "title", "Confirm server action", 20, 18, 260, 18)
        panel.control("value", "statusLabel", "status", 20, 50, 260, 18)
        panel.control("value", "countLabel", "summary", 20, 80, 260, 18)
        panel.control("button", "Confirm", "confirm", 20, 125, 116, 28)
        panel.control("button", "Apply", "apply", 148, 125, 132, 28)
        panel.register()
        registered = true
    }
    
    // This callback runs on the server. Never trust a client-local flag as permission.
    def allowed(player: Player): Boolean = {
        return player.isCreative()
    }
    
    // Record confirmation in the authoritative session. The borrowed context expires after this callback.
    def confirm(context: Context): Unit = {
        if (!allowed(context.getPlayer())) {
            return
        }
        context.flag("confirmed", true)
        context.text("status", "Confirmed. Apply to continue.")
    }
    
    // Recheck permission, then consume confirmation before changing server state. Repeated Apply
    // requests cannot reuse an old confirmation. Closing ends the session; this count is not disk persistence.
    def apply(context: Context): Unit = {
        if (!allowed(context.getPlayer())) {
            return
        }
        if (!context.flag("confirmed")) {
            context.text("status", "Confirm the action first.")
            return
        }
        context.flag("confirmed", false)
        context.number("count", context.number("count") + 1.0)
        context.text("summary", "Actions applied: " + (context.number("count")).toInt)
        context.text("status", "Applied successfully.")
    }
    
    // Build fresh screen state, declare callbacks, then open once. A returned handle can precede
    // the first layout; readiness is separate from ownership. No window opens during file loading.
    def open(): Unit = {
        close()
        val screen: Screen = Windows.screen("Server action", 960, 640)
        screen.button("Continue", 36, 150, 140, 26, (clicked: Window) => continueAction(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1)
        screen.button("Cancel", 188, 150, 100, 26, (clicked: Window) => cancel(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1)
        screen.on("draw", (event: ScreenEvent) => draw(event))
        owned = screen.open()
    }
    
    // The local question controls navigation only. It cannot authorize or perform the server action.
    def draw(event: ScreenEvent): Boolean = {
        header(event)
        event.text("Open the server action panel?", 36, 100, -1445387, false)
        return false
    }
    
    // Close the local prompt, then request a synchronized session from the registered factory.
    // The library preserves its waiting/readiness protocol; do not replace the server-backed root screen.
    def continueAction(clicked: Window): Unit = {
        clicked.close()
        owned = Windows.openSynchronized("single_confirmation_scala")
    }
    
    // Cancel stays local and performs no server action.
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

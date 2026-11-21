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
final class ServerActionExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private static Window owned = null;
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    public static void header(ScreenEvent event) {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464);
        event.rect(20, 20, 3, 16, -10103043);
        event.text("Server action", 32, 23, -1445387, false);
        event.text("Review locally; authorization belongs to the server.", 32, 42, -6372673, false);
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
    
    // Register once during common initialization on both physical sides. The same schema must be installed.
    // This example requires creative mode; replace that predicate with your own actual permission rule.
    private static boolean registered = false;
    // Declare server-owned fields and actions before any window opens. Read-only prevents a client
    // from fabricating confirmation or a counter value through an ordinary value-update packet.
    public static void register() {
        if (registered) {
            return;
        }
        Panel panel = Windows.panel("single_confirmation_groovy", "example", 300, 190);
        panel.colors(-15722464, -1445387, -15062988, -13612969);
        panel.text("summary", "Actions applied: 0", 80).readOnly("summary");
        panel.text("status", "Confirm the action first.", 120).readOnly("status");
        panel.flag("confirmed", false).readOnly("confirmed");
        panel.number("count", 0.0, 0.0, 1000000.0).readOnly("count");
        panel.permission({ Player player -> allowed(player) });
        panel.action("confirm", { Context context -> confirm(context) });
        panel.action("apply", { Context context -> apply(context) });
        panel.control("label", "title", "Confirm server action", 20, 18, 260, 18);
        panel.control("value", "statusLabel", "status", 20, 50, 260, 18);
        panel.control("value", "countLabel", "summary", 20, 80, 260, 18);
        panel.control("button", "Confirm", "confirm", 20, 125, 116, 28);
        panel.control("button", "Apply", "apply", 148, 125, 132, 28);
        panel.register();
        registered = true;
    }
    
    // This callback runs on the server. Never trust a client-local flag as permission.
    public static boolean allowed(Player player) {
        return player.isCreative();
    }
    
    // Record confirmation in the authoritative session. The borrowed context expires after this callback.
    public static void confirm(Context context) {
        if (!allowed(context.getPlayer())) {
            return;
        }
        context.flag("confirmed", true);
        context.text("status", "Confirmed. Apply to continue.");
    }
    
    // Recheck permission, then consume confirmation before changing server state. Repeated Apply
    // requests cannot reuse an old confirmation. Closing ends the session; this count is not disk persistence.
    public static void apply(Context context) {
        if (!allowed(context.getPlayer())) {
            return;
        }
        if (!context.flag("confirmed")) {
            context.text("status", "Confirm the action first.");
            return;
        }
        context.flag("confirmed", false);
        context.number("count", context.number("count") + 1.0);
        context.text("summary", "Actions applied: " + (int) (context.number("count")));
        context.text("status", "Applied successfully.");
    }
    
    // Build fresh screen state, declare callbacks, then open once. A returned handle can precede
    // the first layout; readiness is separate from ownership. No window opens during file loading.
    public static void open() {
        close();
        Screen screen = Windows.screen("Server action", 960, 640);
        screen.button("Continue", 36, 150, 140, 26, { Window clicked -> continueAction(clicked) }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
        screen.button("Cancel", 188, 150, 100, 26, { Window clicked -> cancel(clicked) }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
        screen.on("draw", { ScreenEvent event -> draw(event) });
        owned = screen.open();
    }
    
    // The local question controls navigation only. It cannot authorize or perform the server action.
    public static boolean draw(ScreenEvent event) {
        header(event);
        event.text("Open the server action panel?", 36, 100, -1445387, false);
        return false;
    }
    
    // Close the local prompt, then request a synchronized session from the registered factory.
    // The library preserves its waiting/readiness protocol; do not replace the server-backed root screen.
    public static void continueAction(Window clicked) {
        clicked.close();
        owned = Windows.openSynchronized("single_confirmation_groovy");
    }
    
    // Cancel stays local and performs no server action.
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

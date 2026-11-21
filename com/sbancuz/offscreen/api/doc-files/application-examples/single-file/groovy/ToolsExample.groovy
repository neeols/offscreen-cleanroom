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
final class ToolsExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private static Window owned = null;
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    public static void header(ScreenEvent event) {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464);
        event.rect(20, 20, 3, 16, -10103043);
        event.text("Addon tools", 32, 23, -1445387, false);
        event.text("Repeated opens reuse this same native window.", 32, 42, -6372673, false);
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
    
    // This counter belongs to the addon, so it survives a user closing and reopening its window.
    private static int requests = 0;
    // Check live state rather than null alone: the operating system may have closed the retained handle.
    // Reuse an open handle; create a replacement only after it has really closed.
    public static void open() {
        requests = requests + 1;
        if (owned != null && owned.isOpen()) {
            return;
        }
        Screen screen = Windows.screen("Addon tools", 960, 640);
        screen.button("Open tools again", 36, 170, 170, 26, { Window clicked -> again(clicked) }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
        screen.on("draw", { ScreenEvent event -> draw(event) });
        owned = screen.open();
    }
    
    // The button calls the same entry point as an addon key binding, exercising handle reuse.
    public static void again(Window clicked) {
        open();
    }
    
    // The count changes on repeated requests while the native window remains the same.
    public static boolean draw(ScreenEvent event) {
        header(event);
        event.rect(20, 82, event.getWidth() - 40, 132, -15062988);
        event.text("Open requests: " + requests, 36, 105, -1445387, false);
        event.text("Only this addon owns this handle.", 36, 134, -10103043, false);
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

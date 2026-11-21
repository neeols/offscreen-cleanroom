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

public final class ImageViewerExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private static Window owned = null;
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    public static void header(ScreenEvent event) {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464);
        event.rect(20, 20, 3, 16, -10103043);
        event.text("Image viewer", 32, 23, -1445387, false);
        event.text("Scroll to zoom, drag to pan, or fit the image.", 32, 42, -6372673, false);
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
        Screen screen = OffscreenWindows.screen("Image viewer", 960, 640);
        screen.getDefaults().number("zoom", 1.0);
        screen.getDefaults().number("left", 0.0);
        screen.getDefaults().number("top", 120.0);
        screen.getDefaults().integer("viewWidth", 480);
        screen.getDefaults().integer("viewHeight", 320);
        screen.button("Fit image", 20, 76, 110, 26, clicked -> fitButton(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
        screen.on("draw", event -> draw(event));
        screen.on("resize", event -> resized(event));
        screen.on("scroll", event -> zoom(event));
        screen.on("click", event -> press(event));
        screen.on("drag", event -> drag(event));
        screen.on("release", event -> release(event));
        owned = screen.open();
    }
    
    // Use the smaller width/height ratio to preserve the 500x500 source aspect ratio. Center in the
    // viewport, not in the whole window, so the toolbar remains clear.
    public static void fit(State state) {
        double scale = (state.integer("viewWidth") - 40) / 500.0;
        double vertical = (state.integer("viewHeight") - 140) / 500.0;
        if (vertical < scale) {
            scale = vertical;
        }
        if (scale < 0.01) {
            scale = 0.01;
        }
        state.number("zoom", scale);
        state.number("left", (state.integer("viewWidth") - 500 * scale) / 2.0);
        state.number("top", 120 + (state.integer("viewHeight") - 140 - 500 * scale) / 2.0);
    }
    
    // Fit reuses the most recent GUI dimensions retained by the resize callback.
    public static void fitButton(Window clicked) {
        fit(clicked.getState());
    }
    
    // Refit after resize so the image cannot remain stranded beyond the new viewport.
    public static boolean resized(ScreenEvent event) {
        event.getState().integer("viewWidth", event.getWidth());
        event.getState().integer("viewHeight", event.getHeight());
        fit(event.getState());
        return false;
    }
    
    // Clip only the image. The library restores the caller's scissor even when a drawing callback fails.
    public static boolean draw(ScreenEvent event) {
        header(event);
        event.clipped(20, 120, maxInt(0, event.getWidth() - 40), maxInt(0, event.getHeight() - 140), canvas -> drawImage(canvas));
        return false;
    }
    
    // Draw a shared packaged texture. Change the resource identifier and source dimensions together.
    // Minecraft owns the texture; this example closes its window but never deletes the shared resource.
    public static boolean drawImage(ScreenEvent event) {
        int size = maxInt(1, (int) (500 * event.getState().number("zoom")));
        event.image("offscreencleanroom:logo.png", (int) (event.getState().number("left")), (int) (event.getState().number("top")), size, size);
        return false;
    }
    
    // Use the same viewport for drawing and hit testing; toolbar clicks must not begin a drag.
    public static boolean inside(ScreenEvent event) {
        return event.getX() >= 20 && event.getX() < event.getWidth() - 20 && event.getY() >= 120 && event.getY() < event.getHeight() - 20;
    }
    
    // Keep the same source pixel under the pointer by rescaling the pointer-to-image-origin distance.
    // Clamping magnification prevents division by zero and impractically large geometry.
    public static boolean zoom(ScreenEvent event) {
        if (!inside(event)) {
            return false;
        }
        double old = event.getState().number("zoom");
        double next = old / 1.2;
        if (event.getCode() > 0) {
            next = old * 1.2;
        }
        if (next < 0.01) {
            next = 0.01;
        }
        if (next > 16.0) {
            next = 16.0;
        }
        event.getState().number("left", event.getX() - (event.getX() - event.getState().number("left")) * next / old);
        event.getState().number("top", event.getY() - (event.getY() - event.getState().number("top")) * next / old);
        event.getState().number("zoom", next);
        return true;
    }
    
    // Only a left-button press inside the image owns the following drag.
    public static boolean press(ScreenEvent event) {
        if (event.getCode() != 0 || !inside(event)) {
            return false;
        }
        event.getState().flag("dragging", true);
        event.getState().integer("lastX", event.getX());
        event.getState().integer("lastY", event.getY());
        return true;
    }
    
    // Apply incremental movement, not the entire distance since mouse-down on every frame.
    public static boolean drag(ScreenEvent event) {
        if (!event.getState().flag("dragging")) {
            return false;
        }
        event.getState().number("left", event.getState().number("left") + event.getX() - event.getState().integer("lastX"));
        event.getState().number("top", event.getState().number("top") + event.getY() - event.getState().integer("lastY"));
        event.getState().integer("lastX", event.getX());
        event.getState().integer("lastY", event.getY());
        return true;
    }
    
    // Release drag ownership even when the pointer leaves the image viewport.
    public static boolean release(ScreenEvent event) {
        event.getState().flag("dragging", false);
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

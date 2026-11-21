#side client
// Requires OffscreenCleanroom and CraftTweaker on Minecraft 1.12.2 Cleanroom.
// Add this file to your addon with its language runtime enabled.
// Call open() from a client key/button handler after joining a world; declarations open nothing.
// Call close() from your client-thread disconnect/unload hook.
// The server-action example additionally registers its panel during common startup on both sides.
import mods.offscreencleanroom.Windows;
import mods.offscreencleanroom.Window;
import mods.offscreencleanroom.Screen;
import mods.offscreencleanroom.ScreenEvent;
import mods.offscreencleanroom.Widget;
import mods.offscreencleanroom.Files;
import mods.offscreencleanroom.DirectoryTask;
import mods.offscreencleanroom.TextResult;
import mods.offscreencleanroom.Panel;
import mods.offscreencleanroom.PanelContext;
import mods.offscreencleanroom.State;
import mods.offscreencleanroom.ServerPlayer;

// Retain the exact native handle. A title is not unique enough to prove ownership.
static owned as Window = null;
// Draw the same compact header at the current GUI size. Rendering stays on the client thread.
function header(event as ScreenEvent) as void {
    event.rect(0, 0, event.width, event.height, -15722464);
    event.rect(20, 20, 3, 16, -10103043);
    event.text("Image viewer", 32, 23, -1445387, false);
    event.text("Scroll to zoom, drag to pan, or fit the image.", 32, 42, -6372673, false);
    event.rect(20, 62, event.width - 40, 1, -13614255);
}

// Small local arithmetic helper; it is included rather than supplied by a hidden utility file.
function minInt(a as int, b as int) as int {
    if (a < b) {
        return a;
    }
    return b;
}

// Clamp layout and scrolling without allocating objects during drawing.
function maxInt(a as int, b as int) as int {
    if (a > b) {
        return a;
    }
    return b;
}

// Build fresh screen state, declare callbacks, then open once. A returned handle can precede
// the first layout; readiness is separate from ownership. No window opens during file loading.
function open() as void {
    close();
    val screen as Screen = Windows.screen("Image viewer", 960, 640);
    screen.defaults.number("zoom", 1.0);
    screen.defaults.number("left", 0.0);
    screen.defaults.number("top", 120.0);
    screen.defaults.integer("viewWidth", 480);
    screen.defaults.integer("viewHeight", 320);
    screen.button("Fit image", 20, 76, 110, 26, function(clicked) { fitButton(clicked); }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
    screen.on("draw", function(event) { return draw(event); });
    screen.on("resize", function(event) { return resized(event); });
    screen.on("scroll", function(event) { return zoom(event); });
    screen.on("click", function(event) { return press(event); });
    screen.on("drag", function(event) { return drag(event); });
    screen.on("release", function(event) { return release(event); });
    owned = screen.open();
}

// Use the smaller width/height ratio to preserve the 500x500 source aspect ratio. Center in the
// viewport, not in the whole window, so the toolbar remains clear.
function fit(state as State) as void {
    var scale as double = ((state.integer("viewWidth") - 40) as double) / 500.0;
    val vertical as double = ((state.integer("viewHeight") - 140) as double) / 500.0;
    if (vertical < scale) {
        scale = vertical;
    }
    if (scale < 0.01) {
        scale = 0.01;
    }
    state.number("zoom", scale);
    state.number("left", ((state.integer("viewWidth") as double) - 500.0 * scale) / 2.0);
    state.number("top", 120.0 + ((state.integer("viewHeight") as double) - 140.0 - 500.0 * scale) / 2.0);
}

// Fit reuses the most recent GUI dimensions retained by the resize callback.
function fitButton(clicked as Window) as void {
    fit(clicked.state);
}

// Refit after resize so the image cannot remain stranded beyond the new viewport.
function resized(event as ScreenEvent) as bool {
    event.state.integer("viewWidth", event.width);
    event.state.integer("viewHeight", event.height);
    fit(event.state);
    return false;
}

// Clip only the image. The library restores the caller's scissor even when a drawing callback fails.
function draw(event as ScreenEvent) as bool {
    header(event);
    event.clipped(20, 120, maxInt(0, event.width - 40), maxInt(0, event.height - 140), function(canvas) { return drawImage(canvas); });
    return false;
}

// Draw a shared packaged texture. Change the resource identifier and source dimensions together.
// Minecraft owns the texture; this example closes its window but never deletes the shared resource.
function drawImage(event as ScreenEvent) as bool {
    val size as int = maxInt(1, ((500.0 * event.state.number("zoom")) as int));
    event.image("offscreencleanroom:logo.png", ((event.state.number("left")) as int), ((event.state.number("top")) as int), size, size);
    return false;
}

// Use the same viewport for drawing and hit testing; toolbar clicks must not begin a drag.
function inside(event as ScreenEvent) as bool {
    return event.x >= 20 && event.x < event.width - 20 && event.y >= 120 && event.y < event.height - 20;
}

// Keep the same source pixel under the pointer by rescaling the pointer-to-image-origin distance.
// Clamping magnification prevents division by zero and impractically large geometry.
function zoom(event as ScreenEvent) as bool {
    if (!inside(event)) {
        return false;
    }
    val old as double = event.state.number("zoom");
    var next as double = old / 1.2;
    if (event.code > 0) {
        next = old * 1.2;
    }
    if (next < 0.01) {
        next = 0.01;
    }
    if (next > 16.0) {
        next = 16.0;
    }
    event.state.number("left", (event.x as double) - ((event.x as double) - event.state.number("left")) * next / old);
    event.state.number("top", (event.y as double) - ((event.y as double) - event.state.number("top")) * next / old);
    event.state.number("zoom", next);
    return true;
}

// Only a left-button press inside the image owns the following drag.
function press(event as ScreenEvent) as bool {
    if (event.code != 0 || !inside(event)) {
        return false;
    }
    event.state.flag("dragging", true);
    event.state.integer("lastX", event.x);
    event.state.integer("lastY", event.y);
    return true;
}

// Apply incremental movement, not the entire distance since mouse-down on every frame.
function drag(event as ScreenEvent) as bool {
    if (!event.state.flag("dragging")) {
        return false;
    }
    event.state.number("left", event.state.number("left") + event.x - event.state.integer("lastX"));
    event.state.number("top", event.state.number("top") + event.y - event.state.integer("lastY"));
    event.state.integer("lastX", event.x);
    event.state.integer("lastY", event.y);
    return true;
}

// Release drag ownership even when the pointer leaves the image viewport.
function release(event as ScreenEvent) as bool {
    event.state.flag("dragging", false);
    return false;
}

// Close only this file's owned window. Closing also emits the screen close callback.
// Call on the client thread; repeated calls are harmless. Never use global closeAll for addon cleanup.
function close() as void {
    if (!isNull(owned)) {
        owned.close();
    }
    owned = null;
}


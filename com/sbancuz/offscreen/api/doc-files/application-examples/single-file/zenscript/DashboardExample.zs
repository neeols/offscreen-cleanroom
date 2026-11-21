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
    event.text("Responsive dashboard", 32, 23, -1445387, false);
    event.text("Resize the window to rearrange the cards.", 32, 42, -6372673, false);
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
    val screen as Screen = Windows.screen("Responsive dashboard", 960, 640);
    screen.defaults.integer("ticks", 0);
    screen.defaults.integer("cursor", 0);
    screen.defaults.integer("count", 0);
    screen.on("draw", function(event) { return draw(event); });
    owned = screen.open();
}

// Calculate columns from current GUI units, not desktop pixels. Cards retain their internal padding.
// The chart reads oldest-to-newest even after wraparound; drawing never creates samples.
function draw(event as ScreenEvent) as bool {
    header(event);
    var columns as int = 1;
    if (event.width >= 300) {
        columns = 2;
    }
    if (event.width >= 440) {
        columns = 3;
    }
    val cardWidth as int = maxInt(40, (event.width - 40 - (columns - 1) * 12) / columns);
    for i in 0 .. 3 {
        val left as int = 20 + (i % columns) * (cardWidth + 12);
        val top as int = 82 + (i / columns) * 76;
        event.rect(left, top, cardWidth, 64, -15062988);
        var label as string = "Columns: " + columns;
        if (i == 1) {
            label = "Width: " + event.width;
        }
        if (i == 2) {
            label = "Height: " + event.height;
        }
        event.text(label, left + 12, top + 26, -1445387, false);
    }
    event.text("Columns: " + columns, 24, event.height - 24, -6372673, false);
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


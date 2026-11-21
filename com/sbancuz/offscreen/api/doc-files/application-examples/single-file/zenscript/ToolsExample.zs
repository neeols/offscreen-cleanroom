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
    event.text("Addon tools", 32, 23, -1445387, false);
    event.text("Repeated opens reuse this same native window.", 32, 42, -6372673, false);
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

// This counter belongs to the addon, so it survives a user closing and reopening its window.
static requests as int = 0;
// Check live state rather than null alone: the operating system may have closed the retained handle.
// Reuse an open handle; create a replacement only after it has really closed.
function open() as void {
    requests = requests + 1;
    if (!isNull(owned) && owned.open) {
        return;
    }
    val screen as Screen = Windows.screen("Addon tools", 960, 640);
    screen.button("Open tools again", 36, 170, 170, 26, function(clicked) { again(clicked); }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
    screen.on("draw", function(event) { return draw(event); });
    owned = screen.open();
}

// The button calls the same entry point as an addon key binding, exercising handle reuse.
function again(clicked as Window) as void {
    open();
}

// The count changes on repeated requests while the native window remains the same.
function draw(event as ScreenEvent) as bool {
    header(event);
    event.rect(20, 82, event.width - 40, 132, -15062988);
    event.text("Open requests: " + requests, 36, 105, -1445387, false);
    event.text("Only this addon owns this handle.", 36, 134, -10103043, false);
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


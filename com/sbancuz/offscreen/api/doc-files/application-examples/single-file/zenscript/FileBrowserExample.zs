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
    event.text("File browser", 32, 23, -1445387, false);
    event.text("Background I/O; closing cancels the current request.", 32, 42, -6372673, false);
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

// One request and one immutable result array belong to this file's single window.
// The worker never calls these drawing functions or mutates GUI state.
static pending as DirectoryTask = null;
static names as string[] = [] as string[];
// Build fresh screen state, declare callbacks, then open once. A returned handle can precede
// the first layout; readiness is separate from ownership. No window opens during file loading.
function open() as void {
    close();
    val screen as Screen = Windows.screen("File browser", 960, 640);
    if (!isNull(pending)) {
        pending.close();
    }
    names = [] as string[];
    pending = Files.list("config", 200);
    screen.defaults.text("status", "Loading files...");
    screen.defaults.integer("first", 0);
    screen.button("Reload", 364, 76, 96, 26, function(clicked) { reload(clicked); }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
    screen.on("draw", function(event) { return draw(event); });
    screen.on("tick", function(event) { return tick(event); });
    screen.on("close", function(event) { return disposed(event); });
    screen.on("scroll", function(event) { return scroll(event); });
    owned = screen.open();
}

// Cancel the previous owner before submitting a replacement. A late old result cannot become current.
function reload(clicked as Window) as void {
    if (!isNull(pending)) {
        pending.close();
    }
    names = [] as string[];
    pending = Files.list("config", 200);
    clicked.state.text("status", "Loading files...");
    clicked.state.integer("first", 0);
}

// Poll without blocking. Adopt completed immutable data only during the client update callback,
// then release the request. Errors are shown rather than mistaken for an empty directory.
function tick(event as ScreenEvent) as bool {
    if (isNull(pending)) {
        return false;
    }
    val task as DirectoryTask = pending;
    if (task.status == "running") {
        return false;
    }
    if (task.status == "ready") {
        names = task.entries;
        event.state.text("status", "Files: " + names.length);
    }
    else {
        event.state.text("status", task.error);
    }
    task.close();
    pending = null;
    return false;
}

// Render a bounded page of the adopted data. No directory reads occur in this callback.
function draw(event as ScreenEvent) as bool {
    header(event);
    event.text(event.state.text("status"), 24, 84, -1445387, false);
    val rows as int = maxInt(1, (event.height - 150) / 22);
    val first as int = maxInt(0, minInt(event.state.integer("first"), names.length - rows));
    event.state.integer("first", first);
    for row in 0 .. rows {
        if (first + row < names.length) {
            event.rect(20, 115 + row * 22, event.width - 40, 21, -15062988);
            event.text(names[first + row], 30, 121 + row * 22, -1445387, false);
        }
    }
    event.text("Scroll to browse", 24, event.height - 20, -6372673, false);
    return false;
}

// Scroll only the viewport. The current immutable result is reused instead of reloading the directory.
function scroll(event as ScreenEvent) as bool {
    var step as int = 3;
    if (event.code > 0) {
        step = -3;
    }
    event.state.integer("first", maxInt(0, event.state.integer("first") + step));
    return true;
}

// OS close, explicit close and screen disposal all cancel the current request. Cancellation
// prevents a worker completion from reviving closed state; the bounded worker pool is library-owned.
function disposed(event as ScreenEvent) as bool {
    if (!isNull(pending)) {
        pending.close();
    }
    pending = null;
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


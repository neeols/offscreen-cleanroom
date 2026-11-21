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
    event.text("Memory monitor", 32, 23, -1445387, false);
    event.text("Heap usage in MiB; 30 seconds of history.", 32, 42, -6372673, false);
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
    val screen as Screen = Windows.screen("Memory monitor", 960, 640);
    screen.defaults.integer("ticks", 0);
    screen.defaults.integer("cursor", 0);
    screen.defaults.integer("count", 0);
    screen.on("draw", function(event) { return draw(event); });
    screen.on("tick", function(event) { return tick(event); });
    owned = screen.open();
}

// Take one sample every five client ticks, independent of frame rate. Exactly 120 keys are reused
// as a ring buffer; running for hours cannot grow the history or its memory footprint.
function tick(event as ScreenEvent) as bool {
    val ticks as int = event.state.integer("ticks") + 1;
    event.state.integer("ticks", ticks);
    if (ticks % 5 != 0) {
        return false;
    }
    val cursor as int = event.state.integer("cursor");
    event.state.number("sample" + cursor, Windows.usedHeapMiB());
    event.state.integer("cursor", (cursor + 1) % 120);
    event.state.integer("count", minInt(120, event.state.integer("count") + 1));
    return false;
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
        var label as string = "Used: -- MiB";
        if (event.state.integer("count") > 0) {
            label = "Used: " + ((event.state.number("sample" + ((event.state.integer("cursor") + 119) % 120))) as int) + " MiB";
        }
        if (i == 1) {
            label = "Limit: " + ((Windows.heapLimitMiB()) as int) + " MiB";
        }
        if (i == 2) {
            label = "Samples: " + event.state.integer("count");
        }
        event.text(label, left + 12, top + 26, -1445387, false);
    }
    val top as int = 94 + ((3 + columns - 1) / columns) * 76;
    val bottom as int = event.height - 20;
    if (bottom - top < 25) {
        return false;
    }
    // Keep a fixed vertical scale for the entire session. Rescaling to the latest peak would
    // move every old bar whenever allocation or garbage collection changes that peak.
    val count as int = event.state.integer("count");
    val peak as double = Windows.heapLimitMiB();
    event.rect(20, top, event.width - 40, bottom - top, -15062988);
    for i in 0 .. count {
        val index as int = (event.state.integer("cursor") - count + i + 120) % 120;
        val left as int = 20 + i * (event.width - 40) / 120;
        val height as int = ((((bottom - top - 4) as double) * event.state.number("sample" + index) / peak) as int);
        event.rect(left, bottom - height, maxInt(1, (event.width - 40) / 120), height, -10103043);
    }
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


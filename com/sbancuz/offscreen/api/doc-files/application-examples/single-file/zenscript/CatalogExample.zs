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
    event.text("Material browser", 32, 23, -1445387, false);
    event.text("Search, scroll, and select a material.", 32, 42, -6372673, false);
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

// The catalogue is immutable input. Filtering changes visibility, never the source list.
static materials as string[] = ["Copper", "Silver", "Gold", "Tin", "Iron", "Nickel", "Cobalt", "Zinc", "Lead", "Platinum", "Iridium", "Osmium", "Titanium", "Aluminium", "Antimony", "Beryllium", "Bismuth", "Boron", "Cadmium", "Calcium", "Carbon", "Chromium", "Germanium", "Lithium", "Magnesium", "Manganese", "Molybdenum", "Niobium", "Palladium", "Potassium", "Silicon", "Sodium", "Tantalum", "Tellurium", "Tungsten", "Vanadium", "Zirconium"] as string[];
// Count matching catalogue entries without retaining an ever-growing results history.
function countMatches(query as string) as int {
    var count as int = 0;
    val needle as string = query.toLowerCase();
    for i in 0 .. materials.length {
        if (materials[i].toLowerCase().contains(needle)) {
            count = count + 1;
        }
    }
    return count;
}

// Reserve the header and footer; only complete row slots are selectable.
function rows(height as int) as int {
    return maxInt(1, (height - 160) / 24);
}

// Build fresh screen state, declare callbacks, then open once. A returned handle can precede
// the first layout; readiness is separate from ownership. No window opens during file loading.
function open() as void {
    close();
    val screen as Screen = Windows.screen("Material browser", 960, 640);
    screen.textField("search", 28, 78, 420, 24, "").changed(function(widget) { searchChanged(widget); });
    screen.defaults.integer("first", 0);
    screen.defaults.text("selection", "Choose a material");
    screen.on("draw", function(event) { return draw(event); });
    screen.on("click", function(event) { return click(event); });
    screen.on("scroll", function(event) { return scroll(event); });
    owned = screen.open();
}

// Restart scrolling after a query change. The selected identity is intentionally retained.
function searchChanged(widget as Widget) as void {
    widget.state.integer("first", 0);
}

// Map filtered row indices into visible rectangles. Re-clamp after resize so the viewport never
// starts beyond the end of a shorter result list. Selection is independent of filtering.
function draw(event as ScreenEvent) as bool {
    header(event);
    val query as string = event.widget("search").text.toLowerCase();
    var first as int = maxInt(0, minInt(event.state.integer("first"), countMatches(query) - rows(event.height)));
    event.state.integer("first", first);
    var matched as int = 0;
    var visible as int = 0;
    for i in 0 .. materials.length {
        val value as string = materials[i];
        if (value.toLowerCase().contains(query)) {
            if (matched >= first && visible < rows(event.height)) {
                val top as int = 120 + visible * 24;
                event.rect(20, top, 250, 22, -15062988);
                if (value == event.state.text("selection")) {
                    event.rect(20, top, 3, 22, -10103043);
                }
                event.text(value, 30, top + 7, -1445387, false);
                visible = visible + 1;
            }
            matched = matched + 1;
        }
    }
    if (matched == 0) {
        event.text("No matching materials", 28, 130, -6372673, false);
    }
    event.text("SELECTION", 292, 125, -6372673, false);
    event.text(event.state.text("selection"), 292, 153, -10103043, false);
    event.text("Results: " + matched, 24, event.height - 24, -6372673, false);
    return false;
}

// Invert the row layout to find the clicked filtered entry. Ignore clicks in the search field,
// detail panel, footer and rows beyond the visible viewport.
function click(event as ScreenEvent) as bool {
    if (event.code != 0 || event.x < 20 || event.x >= 270 || event.y < 120) {
        return false;
    }
    val row as int = (event.y - 120) / 24;
    if (row >= rows(event.height)) {
        return false;
    }
    val target as int = event.state.integer("first") + row;
    var matched as int = 0;
    val query as string = event.widget("search").text.toLowerCase();
    for i in 0 .. materials.length {
        val value as string = materials[i];
        if (value.toLowerCase().contains(query)) {
            if (matched == target) {
                event.state.text("selection", value);
                return true;
            }
            matched = matched + 1;
        }
    }
    return false;
}

// Scroll a bounded number of rows. Drawing clamps the upper limit against the current query.
function scroll(event as ScreenEvent) as bool {
    var step as int = 1;
    if (event.code > 0) {
        step = -1;
    }
    event.state.integer("first", maxInt(0, event.state.integer("first") + step * 3));
    return true;
}

// Close only this file's owned window. Closing also emits the screen close callback.
// Call on the client thread; repeated calls are harmless. Never use global closeAll for addon cleanup.
function close() as void {
    if (!isNull(owned)) {
        owned.close();
    }
    owned = null;
}


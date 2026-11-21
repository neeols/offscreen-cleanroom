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
    event.text("Settings", 32, 23, -1445387, false);
    event.text("Edit a draft, then apply it to disk.", 32, 42, -6372673, false);
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
    val screen as Screen = Windows.screen("Settings", 960, 640);
    val loaded as TextResult = Files.read("config/offscreen-settings.txt", "Player");
    screen.textField("name", 36, 105, 400, 24, loaded.value).maxLength(40);
    screen.defaults.text("message", "Edit the name, then apply your changes.");
    if (!loaded.success) {
        screen.defaults.text("message", loaded.error);
    }
    screen.button("Apply changes", 36, 158, 120, 26, function(clicked) { apply(clicked); }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
    screen.button("Cancel", 168, 158, 100, 26, function(clicked) { cancel(clicked); }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
    screen.button("Reset", 280, 158, 100, 26, function(clicked) { reset(clicked); }).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1);
    screen.on("draw", function(event) { return draw(event); });
    owned = screen.open();
}

// The widget renders its own editable field; this callback draws labels and validation feedback.
function draw(event as ScreenEvent) as bool {
    header(event);
    event.text("DISPLAY NAME", 36, 85, -6372673, false);
    event.text(event.state.text("message"), 24, 215, -1445387, false);
    return false;
}

// Validate the draft before I/O. Saving is explicit; window state alone is not disk persistence.
function apply(clicked as Window) as void {
    val draft as string = clicked.widget("name").text.trim();
    if (draft == "") {
        clicked.state.text("message", "Enter a name before saving.");
        return;
    }
    // This is deliberately a tiny local settings file. Large/network writes belong in a worker.
    // The API writes a temporary sibling before replacing the previous settings document.
    val saved as TextResult = Files.write("config/offscreen-settings.txt", draft);
    if (saved.success) {
        clicked.state.text("message", "Saved. Close and reopen to load this value.");
    }
    else {
        clicked.state.text("message", saved.error);
    }
}

// Reset affects the visible draft only. It must never silently overwrite the saved file.
function reset(clicked as Window) as void {
    clicked.widget("name").text("Player");
    clicked.state.text("message", "Default restored in the draft. Apply to save.");
}

// Discard the current draft by closing this exact owner; no write occurs.
function cancel(clicked as Window) as void {
    clicked.close();
}

// Close only this file's owned window. Closing also emits the screen close callback.
// Call on the client thread; repeated calls are harmless. Never use global closeAll for addon cleanup.
function close() as void {
    if (!isNull(owned)) {
        owned.close();
    }
    owned = null;
}


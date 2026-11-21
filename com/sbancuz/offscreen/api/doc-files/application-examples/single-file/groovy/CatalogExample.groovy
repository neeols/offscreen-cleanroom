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
final class CatalogExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private static Window owned = null;
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    public static void header(ScreenEvent event) {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464);
        event.rect(20, 20, 3, 16, -10103043);
        event.text("Material browser", 32, 23, -1445387, false);
        event.text("Search, scroll, and select a material.", 32, 42, -6372673, false);
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
    
    // The catalogue is immutable input. Filtering changes visibility, never the source list.
    private static String[] materials = ["Copper", "Silver", "Gold", "Tin", "Iron", "Nickel", "Cobalt", "Zinc", "Lead", "Platinum", "Iridium", "Osmium", "Titanium", "Aluminium", "Antimony", "Beryllium", "Bismuth", "Boron", "Cadmium", "Calcium", "Carbon", "Chromium", "Germanium", "Lithium", "Magnesium", "Manganese", "Molybdenum", "Niobium", "Palladium", "Potassium", "Silicon", "Sodium", "Tantalum", "Tellurium", "Tungsten", "Vanadium", "Zirconium"] as String[];
    // Count matching catalogue entries without retaining an ever-growing results history.
    public static int countMatches(String query) {
        int count = (int) (0);
        String needle = query.toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < materials.length; i++) {
            if (materials[i].toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                count = count + 1;
            }
        }
        return (int) (count);
    }
    
    // Reserve the header and footer; only complete row slots are selectable.
    public static int rows(int height) {
        return (int) (maxInt(1, (height - 160) / 24));
    }
    
    // Build fresh screen state, declare callbacks, then open once. A returned handle can precede
    // the first layout; readiness is separate from ownership. No window opens during file loading.
    public static void open() {
        close();
        Screen screen = Windows.screen("Material browser", 960, 640);
        screen.textField("search", 28, 78, 420, 24, "").changed({ Widget widget -> searchChanged(widget) });
        screen.getDefaults().integer("first", 0);
        screen.getDefaults().text("selection", "Choose a material");
        screen.on("draw", { ScreenEvent event -> draw(event) });
        screen.on("click", { ScreenEvent event -> click(event) });
        screen.on("scroll", { ScreenEvent event -> scroll(event) });
        owned = screen.open();
    }
    
    // Restart scrolling after a query change. The selected identity is intentionally retained.
    public static void searchChanged(Widget widget) {
        widget.getState().integer("first", 0);
    }
    
    // Map filtered row indices into visible rectangles. Re-clamp after resize so the viewport never
    // starts beyond the end of a shorter result list. Selection is independent of filtering.
    public static boolean draw(ScreenEvent event) {
        header(event);
        String query = event.widget("search").getText().toLowerCase(java.util.Locale.ROOT);
        int first = (int) (maxInt(0, minInt(event.getState().integer("first"), countMatches(query) - rows(event.getHeight()))));
        event.getState().integer("first", first);
        int matched = (int) (0);
        int visible = (int) (0);
        for (int i = 0; i < materials.length; i++) {
            String value = materials[i];
            if (value.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                if (matched >= first && visible < rows(event.getHeight())) {
                    int top = (int) (120 + visible * 24);
                    event.rect(20, top, 250, 22, -15062988);
                    if (value.equals(event.getState().text("selection"))) {
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
        event.text(event.getState().text("selection"), 292, 153, -10103043, false);
        event.text("Results: " + matched, 24, event.getHeight() - 24, -6372673, false);
        return false;
    }
    
    // Invert the row layout to find the clicked filtered entry. Ignore clicks in the search field,
    // detail panel, footer and rows beyond the visible viewport.
    public static boolean click(ScreenEvent event) {
        if (event.getCode() != 0 || event.getX() < 20 || event.getX() >= 270 || event.getY() < 120) {
            return false;
        }
        int row = (int) ((event.getY() - 120) / 24);
        if (row >= rows(event.getHeight())) {
            return false;
        }
        int target = (int) (event.getState().integer("first") + row);
        int matched = (int) (0);
        String query = event.widget("search").getText().toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < materials.length; i++) {
            String value = materials[i];
            if (value.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                if (matched == target) {
                    event.getState().text("selection", value);
                    return true;
                }
                matched = matched + 1;
            }
        }
        return false;
    }
    
    // Scroll a bounded number of rows. Drawing clamps the upper limit against the current query.
    public static boolean scroll(ScreenEvent event) {
        int step = (int) (1);
        if (event.getCode() > 0) {
            step = -1;
        }
        event.getState().integer("first", maxInt(0, event.getState().integer("first") + step * 3));
        return true;
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

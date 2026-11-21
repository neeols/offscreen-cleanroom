package examples.single.scala

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

object ConnectedWindowsExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private var owned: Window = null
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    def header(event: ScreenEvent): Unit = {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464)
        event.rect(20, 20, 3, 16, -10103043)
        event.text("Material browser", 32, 23, -1445387, false)
        event.text("Search, scroll, and select a material.", 32, 42, -6372673, false)
        event.rect(20, 62, event.getWidth() - 40, 1, -13614255)
    }
    
    // Small local arithmetic helper; it is included rather than supplied by a hidden utility file.
    def minInt(a: Int, b: Int): Int = {
        if (a < b) {
            return a
        }
        return b
    }
    
    // Clamp layout and scrolling without allocating objects during drawing.
    def maxInt(a: Int, b: Int): Int = {
        if (a > b) {
            return a
        }
        return b
    }
    
    // The catalogue is immutable input. Filtering changes visibility, never the source list.
    private var materials: Array[String] = Array("Copper", "Silver", "Gold", "Tin", "Iron", "Nickel", "Cobalt", "Zinc", "Lead", "Platinum", "Iridium", "Osmium", "Titanium", "Aluminium", "Antimony", "Beryllium", "Bismuth", "Boron", "Cadmium", "Calcium", "Carbon", "Chromium", "Germanium", "Lithium", "Magnesium", "Manganese", "Molybdenum", "Niobium", "Palladium", "Potassium", "Silicon", "Sodium", "Tantalum", "Tellurium", "Tungsten", "Vanadium", "Zirconium")
    // Both windows deliberately share this one value; query/scroll remain in the list window.
    private var selection: String = "Choose a material"
    private var preview: Window = null
    // Count matching catalogue entries without retaining an ever-growing results history.
    def countMatches(query: String): Int = {
        var count: Int = 0
        val needle: String = query.toLowerCase(java.util.Locale.ROOT)
        var i: Int = 0
        while (i < materials.length) {
            if (materials(i).toLowerCase(java.util.Locale.ROOT).contains(needle)) {
                count = count + 1
            }
            i += 1
        }
        return count
    }
    
    // Reserve the header and footer; only complete row slots are selectable.
    def rows(height: Int): Int = {
        return maxInt(1, (height - 160) / 24)
    }
    
    // Open only missing windows. Closing either half leaves its sibling and shared selection usable.
    def open(): Unit = {
        if (owned == null || !owned.isOpen()) {
            val screen: Screen = Windows.screen("Materials", 960, 640)
            screen.textField("search", 28, 78, 420, 24, "").changed((widget: Widget) => searchChanged(widget))
            screen.getDefaults().integer("first", 0)
            screen.getDefaults().text("selection", "Choose a material")
            screen.on("draw", (event: ScreenEvent) => draw(event))
            screen.on("click", (event: ScreenEvent) => click(event))
            screen.on("scroll", (event: ScreenEvent) => scroll(event))
            owned = screen.open()
        }
        if (preview == null || !preview.isOpen()) {
            val details: Screen = Windows.screen("Selection preview", 960, 640)
            details.on("draw", (event: ScreenEvent) => drawPreview(event))
            preview = details.open()
        }
    }
    
    // Read the shared value on each draw rather than copying a stale selection when the preview opens.
    def drawPreview(event: ScreenEvent): Boolean = {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464)
        event.text("Selected material", 32, 36, -6372673, false)
        event.text(selection, 32, 84, -10103043, false)
        event.text("Selection is shared; search and scroll are independent.", 32, 132, -1445387, false)
        return false
    }
    
    // Restart scrolling after a query change. The selected identity is intentionally retained.
    def searchChanged(widget: Widget): Unit = {
        widget.getState().integer("first", 0)
    }
    
    // Map filtered row indices into visible rectangles. Re-clamp after resize so the viewport never
    // starts beyond the end of a shorter result list. Selection is independent of filtering.
    def draw(event: ScreenEvent): Boolean = {
        header(event)
        val query: String = event.widget("search").getText().toLowerCase(java.util.Locale.ROOT)
        var first: Int = maxInt(0, minInt(event.getState().integer("first"), countMatches(query) - rows(event.getHeight())))
        event.getState().integer("first", first)
        var matched: Int = 0
        var visible: Int = 0
        var i: Int = 0
        while (i < materials.length) {
            val value: String = materials(i)
            if (value.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                if (matched >= first && visible < rows(event.getHeight())) {
                    val top: Int = 120 + visible * 24
                    event.rect(20, top, 250, 22, -15062988)
                    if (value == selection) {
                        event.rect(20, top, 3, 22, -10103043)
                    }
                    event.text(value, 30, top + 7, -1445387, false)
                    visible = visible + 1
                }
                matched = matched + 1
            }
            i += 1
        }
        if (matched == 0) {
            event.text("No matching materials", 28, 130, -6372673, false)
        }
        event.text("SELECTION", 292, 125, -6372673, false)
        event.text(selection, 292, 153, -10103043, false)
        event.text("Results: " + matched, 24, event.getHeight() - 24, -6372673, false)
        return false
    }
    
    // Invert the row layout to find the clicked filtered entry. Ignore clicks in the search field,
    // detail panel, footer and rows beyond the visible viewport.
    def click(event: ScreenEvent): Boolean = {
        if (event.getCode() != 0 || event.getX() < 20 || event.getX() >= 270 || event.getY() < 120) {
            return false
        }
        val row: Int = (event.getY() - 120) / 24
        if (row >= rows(event.getHeight())) {
            return false
        }
        val target: Int = event.getState().integer("first") + row
        var matched: Int = 0
        val query: String = event.widget("search").getText().toLowerCase(java.util.Locale.ROOT)
        var i: Int = 0
        while (i < materials.length) {
            val value: String = materials(i)
            if (value.toLowerCase(java.util.Locale.ROOT).contains(query)) {
                if (matched == target) {
                    selection = value
                    return true
                }
                matched = matched + 1
            }
            i += 1
        }
        return false
    }
    
    // Scroll a bounded number of rows. Drawing clamps the upper limit against the current query.
    def scroll(event: ScreenEvent): Boolean = {
        var step: Int = 1
        if (event.getCode() > 0) {
            step = -1
        }
        event.getState().integer("first", maxInt(0, event.getState().integer("first") + step * 3))
        return true
    }
    
    // Close only this file's owned window. Closing also emits the screen close callback.
    // Call on the client thread; repeated calls are harmless. Never use global closeAll for addon cleanup.
    def close(): Unit = {
        if (preview != null) {
            preview.close()
        }
        preview = null
        if (owned != null) {
            owned.close()
        }
        owned = null
    }
    
}

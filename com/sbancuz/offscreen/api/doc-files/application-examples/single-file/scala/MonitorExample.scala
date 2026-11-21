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

object MonitorExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private var owned: Window = null
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    def header(event: ScreenEvent): Unit = {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464)
        event.rect(20, 20, 3, 16, -10103043)
        event.text("Memory monitor", 32, 23, -1445387, false)
        event.text("Heap usage in MiB; 30 seconds of history.", 32, 42, -6372673, false)
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
    
    // Build fresh screen state, declare callbacks, then open once. A returned handle can precede
    // the first layout; readiness is separate from ownership. No window opens during file loading.
    def open(): Unit = {
        close()
        val screen: Screen = Windows.screen("Memory monitor", 960, 640)
        screen.getDefaults().integer("ticks", 0)
        screen.getDefaults().integer("cursor", 0)
        screen.getDefaults().integer("count", 0)
        screen.on("draw", (event: ScreenEvent) => draw(event))
        screen.on("tick", (event: ScreenEvent) => tick(event))
        owned = screen.open()
    }
    
    // Take one sample every five client ticks, independent of frame rate. Exactly 120 keys are reused
    // as a ring buffer; running for hours cannot grow the history or its memory footprint.
    def tick(event: ScreenEvent): Boolean = {
        val ticks: Int = event.getState().integer("ticks") + 1
        event.getState().integer("ticks", ticks)
        if (ticks % 5 != 0) {
            return false
        }
        val cursor: Int = event.getState().integer("cursor")
        event.getState().number("sample" + cursor, Windows.usedHeapMiB())
        event.getState().integer("cursor", (cursor + 1) % 120)
        event.getState().integer("count", minInt(120, event.getState().integer("count") + 1))
        return false
    }
    
    // Calculate columns from current GUI units, not desktop pixels. Cards retain their internal padding.
    // The chart reads oldest-to-newest even after wraparound; drawing never creates samples.
    def draw(event: ScreenEvent): Boolean = {
        header(event)
        var columns: Int = 1
        if (event.getWidth() >= 300) {
            columns = 2
        }
        if (event.getWidth() >= 440) {
            columns = 3
        }
        val cardWidth: Int = maxInt(40, (event.getWidth() - 40 - (columns - 1) * 12) / columns)
        var i: Int = 0
        while (i < 3) {
            val left: Int = 20 + (i % columns) * (cardWidth + 12)
            val top: Int = 82 + (i / columns) * 76
            event.rect(left, top, cardWidth, 64, -15062988)
            var label: String = "Used: -- MiB"
            if (event.getState().integer("count") > 0) {
                label = "Used: " + (event.getState().number("sample" + ((event.getState().integer("cursor") + 119) % 120))).toInt + " MiB"
            }
            if (i == 1) {
                label = "Limit: " + (Windows.heapLimitMiB()).toInt + " MiB"
            }
            if (i == 2) {
                label = "Samples: " + event.getState().integer("count")
            }
            event.text(label, left + 12, top + 26, -1445387, false)
            i += 1
        }
        val top: Int = 94 + ((3 + columns - 1) / columns) * 76
        val bottom: Int = event.getHeight() - 20
        if (bottom - top < 25) {
            return false
        }
        // Keep a fixed vertical scale for the entire session. Rescaling to the latest peak would
        // move every old bar whenever allocation or garbage collection changes that peak.
        val count: Int = event.getState().integer("count")
        val peak: Double = Windows.heapLimitMiB()
        event.rect(20, top, event.getWidth() - 40, bottom - top, -15062988)
        i = 0
        while (i < count) {
            val index: Int = (event.getState().integer("cursor") - count + i + 120) % 120
            val left: Int = 20 + i * (event.getWidth() - 40) / 120
            val height: Int = ((bottom - top - 4) * event.getState().number("sample" + index) / peak).toInt
            event.rect(left, bottom - height, maxInt(1, (event.getWidth() - 40) / 120), height, -10103043)
            i += 1
        }
        return false
    }
    
    // Close only this file's owned window. Closing also emits the screen close callback.
    // Call on the client thread; repeated calls are harmless. Never use global closeAll for addon cleanup.
    def close(): Unit = {
        if (owned != null) {
            owned.close()
        }
        owned = null
    }
    
}

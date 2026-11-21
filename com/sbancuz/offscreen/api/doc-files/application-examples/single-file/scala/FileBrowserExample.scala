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

object FileBrowserExample {
    // Retain the exact native handle. A title is not unique enough to prove ownership.
    private var owned: Window = null
    // Draw the same compact header at the current GUI size. Rendering stays on the client thread.
    def header(event: ScreenEvent): Unit = {
        event.rect(0, 0, event.getWidth(), event.getHeight(), -15722464)
        event.rect(20, 20, 3, 16, -10103043)
        event.text("File browser", 32, 23, -1445387, false)
        event.text("Background I/O; closing cancels the current request.", 32, 42, -6372673, false)
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
    
    // One request and one immutable result array belong to this file's single window.
    // The worker never calls these drawing functions or mutates GUI state.
    private var pending: DirectoryTask = null
    private var names: Array[String] = Array()
    // Build fresh screen state, declare callbacks, then open once. A returned handle can precede
    // the first layout; readiness is separate from ownership. No window opens during file loading.
    def open(): Unit = {
        close()
        val screen: Screen = Windows.screen("File browser", 960, 640)
        if (pending != null) {
            pending.close()
        }
        names = Array()
        pending = Files.list("config", 200)
        screen.getDefaults().text("status", "Loading files...")
        screen.getDefaults().integer("first", 0)
        screen.button("Reload", 364, 76, 96, 26, (clicked: Window) => reload(clicked)).flat().shadow(false).skin("normal", -15062988, -13614255, -1445387, 1).skin("hovered", -13612969, -10037571, -1445387, 1)
        screen.on("draw", (event: ScreenEvent) => draw(event))
        screen.on("tick", (event: ScreenEvent) => tick(event))
        screen.on("close", (event: ScreenEvent) => disposed(event))
        screen.on("scroll", (event: ScreenEvent) => scroll(event))
        owned = screen.open()
    }
    
    // Cancel the previous owner before submitting a replacement. A late old result cannot become current.
    def reload(clicked: Window): Unit = {
        if (pending != null) {
            pending.close()
        }
        names = Array()
        pending = Files.list("config", 200)
        clicked.getState().text("status", "Loading files...")
        clicked.getState().integer("first", 0)
    }
    
    // Poll without blocking. Adopt completed immutable data only during the client update callback,
    // then release the request. Errors are shown rather than mistaken for an empty directory.
    def tick(event: ScreenEvent): Boolean = {
        if (pending == null) {
            return false
        }
        val task: DirectoryTask = pending
        if (task.status() == "running") {
            return false
        }
        if (task.status() == "ready") {
            names = task.entries()
            event.getState().text("status", "Files: " + names.length)
        }
        else {
            event.getState().text("status", task.error())
        }
        task.close()
        pending = null
        return false
    }
    
    // Render a bounded page of the adopted data. No directory reads occur in this callback.
    def draw(event: ScreenEvent): Boolean = {
        header(event)
        event.text(event.getState().text("status"), 24, 84, -1445387, false)
        val rows: Int = maxInt(1, (event.getHeight() - 150) / 22)
        val first: Int = maxInt(0, minInt(event.getState().integer("first"), names.length - rows))
        event.getState().integer("first", first)
        var row: Int = 0
        while (row < rows) {
            if (first + row < names.length) {
                event.rect(20, 115 + row * 22, event.getWidth() - 40, 21, -15062988)
                event.text(names(first + row), 30, 121 + row * 22, -1445387, false)
            }
            row += 1
        }
        event.text("Scroll to browse", 24, event.getHeight() - 20, -6372673, false)
        return false
    }
    
    // Scroll only the viewport. The current immutable result is reused instead of reloading the directory.
    def scroll(event: ScreenEvent): Boolean = {
        var step: Int = 3
        if (event.getCode() > 0) {
            step = -3
        }
        event.getState().integer("first", maxInt(0, event.getState().integer("first") + step))
        return true
    }
    
    // OS close, explicit close and screen disposal all cancel the current request. Cancellation
    // prevents a worker completion from reviving closed state; the bounded worker pool is library-owned.
    def disposed(event: ScreenEvent): Boolean = {
        if (pending != null) {
            pending.close()
        }
        pending = null
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

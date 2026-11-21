package com.sbancuz.offscreen.examples;

import com.sbancuz.offscreen.api.OffscreenAPI;
import com.sbancuz.offscreen.window.Window;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client-only screen that displays a greeting in a separate desktop window.
 *
 * <p>Add this file to your addon's client Java sources and adjust the package declaration.
 * Call {@link #open()} from a client button or key handler after initialization; this
 * example does not register a keybinding. Each call creates another window titled
 * "Hello window", with a dark background and a centered greeting.
 *
 * <p>Keep the returned window if you need to close it later with
 * {@link OffscreenAPI#close(Window)} from a client action.
 */
@SideOnly(Side.CLIENT)
public final class HelloScreen extends GuiScreen {
    /**
     * Opens a local screen from a client-thread action after initialization.
     *
     * @return created window, or null if no adapter handles this screen
     * @throws IllegalStateException if native window or OpenGL context creation fails
     */
    public static Window open() {
        Window window = OffscreenAPI.open(HelloScreen::new);
        if (OffscreenAPI.isOpen(window)) {
            OffscreenAPI.setTitle(window, "Hello window");
            OffscreenAPI.resize(window, 960, 640);
        }
        return window;
    }

    /** {@inheritDoc} Uses scaled GUI units supplied by the hosted vanilla adapter. */
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xFF17232D);
        drawCenteredString(fontRenderer, "Hello from a separate window!",
                width / 2, height / 2, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}

package com.tajmoti.tubediscs.gui;

import com.tajmoti.tubediscs.net.TubeSaveMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@SideOnly(Side.CLIENT)
public class TubeDiscGui extends GuiScreen {
    public static final int ID = 0;

    private final Logger logger;
    private final SimpleNetworkWrapper network;
    private GuiTextField textField;


    public TubeDiscGui(Logger logger, SimpleNetworkWrapper network) {
        this.logger = logger;
        this.network = network;
    }

    @Override
    public void initGui() {
        addButton(new GuiButton(0, 5, 20, "Submit"));
        textField = new GuiTextField(1, fontRenderer, 5, 5, 512, 16);
        textField.setMaxStringLength(512);
        textField.setFocused(true);
        super.initGui();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
        switch (button.id) {
            case 0:
                try {
                    URL url = new URL(textField.getText());
                    // Close the screen
                    Minecraft.getMinecraft().player.closeScreen();
                    // Send the update item message to the server
                    TubeSaveMessage msg = new TubeSaveMessage(url);
                    network.sendToServer(msg);
                } catch (MalformedURLException e) {
                    logger.warn(e);
                }
                break;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        textField.updateCursorCounter();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        textField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        textField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        textField.textboxKeyTyped(typedChar, keyCode);
    }
}

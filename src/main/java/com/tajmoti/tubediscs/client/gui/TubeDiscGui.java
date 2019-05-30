package com.tajmoti.tubediscs.client.gui;

import com.tajmoti.tubediscs.item.TubeDisc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@SideOnly(Side.CLIENT)
public class TubeDiscGui extends GuiScreen {
    public static final int ID = 0;
    private ItemStack disc;
    private GuiTextField textField;


    /**
     * ItemStack MUST BE of TubeDisc!
     */
    public TubeDiscGui(ItemStack disc) {
        this.disc = disc;
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
                    TubeDisc.setUrl(disc, url);
                    Minecraft.getMinecraft().player.closeScreen();
                } catch (MalformedURLException e) {
                    // TODO
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

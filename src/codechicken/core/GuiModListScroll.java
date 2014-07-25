package codechicken.core;

import codechicken.core.launch.CodeChickenCorePlugin;
import codechicken.lib.gui.GuiDraw;
import codechicken.lib.vec.Rectangle4i;
import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.ModMetadata;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.*;

import java.util.LinkedList;
import java.util.List;

public class GuiModListScroll
{
    private static List<ModContainer> scrollMods = new LinkedList<ModContainer>();

    public static void register(Object mod) {
        register(FMLCommonHandler.instance().findContainerFor(mod));
    }

    private static void register(ModContainer mod) {
        if (MinecraftForgeClient.getStencilBits() == 0)
            CodeChickenCorePlugin.logger.error("Unable to do mod description scrolling due to lack of stencil buffer");
        else
            scrollMods.add(mod);
    }

    private static ModContainer lastMod;
    private static double scroll;
    private static double lastFrameTime;
    private static double timeStart;
    public static void draw(GuiModList gui, int mouseX, int mouseY) {
        ModContainer selectedMod = ReflectionManager.getField(GuiModList.class, ModContainer.class, gui, "selectedMod");
        if(selectedMod != lastMod) {
            lastMod = selectedMod;
            scroll = 0;
            timeStart = ClientUtils.getRenderTime();
        }
        if(!scrollMods.contains(selectedMod) || selectedMod.getMetadata().autogenerated)
            return;

        int y1 = calcDescY(gui, selectedMod);
        int y1draw = y1 + 10;
        int y2 = gui.height-38;
        int x1 = ReflectionManager.getField(GuiModList.class, Integer.class, gui, "listWidth") + 20;
        int x2 = gui.width - 20;
        if(x2 - x1 <= 20)
            return;

        glEnable(GL_STENCIL_TEST);
        glStencilFunc(GL_ALWAYS, 1, 1);
        glColorMask(false, false, false, false);
        glStencilOp(GL_ZERO, GL_ZERO, GL_ZERO);
        GuiDraw.drawRect(0, 0, gui.width, gui.height, -1);//clear stencil buffer

        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
        GuiDraw.drawRect(x1, y1, gui.width-x1, gui.height-y1, -1);//add description area (even below button)
        glStencilOp(GL_ZERO, GL_ZERO, GL_ZERO);
        GuiDraw.drawRect(gui.width / 2 - 75, y2, 200, 20, -1);//subtract done button

        glColorMask(true, true, true, true);
        glStencilFunc(GL_EQUAL, 1, 1);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

        gui.drawDefaultBackground();//fill stencil with background

        glColorMask(false, false, false, false);
        glStencilOp(GL_ZERO, GL_ZERO, GL_ZERO);
        GuiDraw.drawRect(0, 0, gui.width, gui.height, -1);
        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
        GuiDraw.drawRect(x1, y1draw, x2-x1, y2-y1draw, -1);

        glColorMask(true, true, true, true);
        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

        String description = selectedMod.getMetadata().description;
        int height = GuiDraw.fontRenderer.listFormattedStringToWidth(description, x2-x1).size()*GuiDraw.fontRenderer.FONT_HEIGHT;

        boolean needsScroll = height > y2-y1draw;
        if((ClientUtils.getRenderTime() - timeStart) > 40 && needsScroll) {
            double dt = ClientUtils.getRenderTime() - lastFrameTime;
            if (new Rectangle4i(x1, y1draw, x2 - x1, y2 - y1draw).contains(mouseX, mouseY)) {
                double d = Keyboard.isKeyDown(Keyboard.KEY_UP) ? -1 : Keyboard.isKeyDown(Keyboard.KEY_DOWN) ? 1 : 0;
                scroll += d * dt * 1.5;
            } else {
                scroll += dt * 0.2;
            }
        }
        lastFrameTime = ClientUtils.getRenderTime();

        //draw description

        double dy = scroll % (height + 20);
        GL11.glPushMatrix();
            GL11.glTranslated(0, -dy, 0);
            GuiDraw.fontRenderer.drawSplitString(description, x1, y1draw, x2-x1, 0xDDDDDD);
            if(needsScroll) {
                GL11.glTranslated(0, height + 20, 0);
                GuiDraw.fontRenderer.drawSplitString(description, x1, y1draw, x2 - x1, 0xDDDDDD);
            }
        GL11.glPopMatrix();

        glDisable(GL_STENCIL_TEST);
    }

    /**
     * Does not add the last 10 px space before the description normally starts
     * Ignores empty child mods expecting a background draw overwrite
     */
    private static int calcDescY(GuiModList gui, ModContainer mod) {
        ModMetadata meta = mod.getMetadata();
        int y = 35;
        if(!!meta.logoFile.isEmpty() && ReflectionManager.getField(GuiModList.class, ResourceLocation.class, gui, "cachedLogo") != null)
            y += 65;
        y += 12; // title
        y += 40; // necessary lines
        if(!meta.credits.isEmpty())
            y += 10;
        if(!meta.childMods.isEmpty())
            y += 10;
        return y;
    }
}

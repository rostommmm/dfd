package net.minecraftforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.mojang.blaze3d.matrix.MatrixStack;

@FunctionalInterface
public interface ISkyRenderHandler {
    void render(int var1, float var2, MatrixStack var3, ClientWorld var4, Minecraft var5);
}

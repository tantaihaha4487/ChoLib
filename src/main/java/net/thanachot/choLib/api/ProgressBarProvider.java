package net.thanachot.choLib.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

@FunctionalInterface
public interface ProgressBarProvider {
    Text buildBar(ServerPlayerEntity player, int current, int max);
}

package com.brainage.fabric_macros;

import com.brainage.fabric_macros.event.KeyInputHandler;
import net.fabricmc.api.ClientModInitializer;

public class FabricMacrosClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyInputHandler.register();
    }
}

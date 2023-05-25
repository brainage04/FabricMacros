package com.brainage.fabric_macros;

import com.brainage.fabric_macros.config.MacroOptions;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricMacros implements ModInitializer {
    public static final String MOD_ID = "fabric_macros";
    public static final String MOD_NAME = "Fabric Macros";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static MacroOptions macroConfig;

    @Override
    public void onInitialize() {
        AutoConfig.register(MacroOptions.class, JanksonConfigSerializer::new);

        macroConfig = AutoConfig.getConfigHolder(MacroOptions.class).getConfig();

        LOGGER.info(MOD_NAME + " initialized successfully.");
    }
}
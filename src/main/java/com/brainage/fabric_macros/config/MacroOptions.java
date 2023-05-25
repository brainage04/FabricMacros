package com.brainage.fabric_macros.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import static com.brainage.fabric_macros.FabricMacros.MOD_ID;

@Config(name = MOD_ID)
public class MacroOptions implements ConfigData {
    @ConfigEntry.Gui.CollapsibleObject
    public MacroOptionsContainer macroOptionsContainer = new MacroOptionsContainer();

    public static class MacroOptionsContainer {
        @ConfigEntry.Gui.Tooltip()
        public int currentMacroIndex = 1;
        @ConfigEntry.Gui.Tooltip()
        public int minReactionDuration = 15;
        @ConfigEntry.Gui.Tooltip()
        public int maxReactionDuration = 30;

        public MacroEntry[] macroList = new MacroEntry[1];
    }

    public static class MacroEntry {
        public String macroName = "My First Macro";
        @ConfigEntry.Gui.Tooltip()
        public boolean verticalMovementFailsafe = true;
        @ConfigEntry.Gui.Tooltip()
        public boolean horizontalMovementFailsafe = true;
        @ConfigEntry.Gui.Tooltip()
        public boolean calibrateOnNextUse = true;

        public StepEntry[] stepEntries = new StepEntry[1];
    }

    public static class StepEntry {
        @ConfigEntry.Gui.Tooltip()
        public int minStepDuration = 0;
        @ConfigEntry.Gui.Tooltip()
        public int stepDuration = 6000;

        public KeyEntry[] keyEntries = new KeyEntry[1];
    }

    public static class KeyEntry {
        public enum KeyType {
            W,
            A,
            S,
            D,
            LEFT_CLICK,
            RIGHT_CLICK,
            JUMP,
            SNEAK,
            SPRINT
        }

        public KeyType key = KeyType.LEFT_CLICK;
        public boolean pressed = true;
    }
}
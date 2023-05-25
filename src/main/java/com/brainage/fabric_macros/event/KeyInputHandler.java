package com.brainage.fabric_macros.event;

import com.brainage.fabric_macros.config.MacroOptions;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import static com.brainage.fabric_macros.FabricMacros.LOGGER;
import static com.brainage.fabric_macros.FabricMacros.macroConfig;

public class KeyInputHandler {
    public static final String KEY_MACRO_CATEGORY = "key.category.fabric_macros.macros";

    public static KeyBinding toggleMacroKey;

    // NOTE: YAW = X-AXIS, PITCH = Y-AXIS

    public static int currentStep = 0;
    public static int actualStep = 0;

    public static int ticksElapsed;

    public static Boolean macroRunning = false;

    public static int finalTick = -1;
    public static int reactionTimeTicks = 15;

    public static String disableMessage;

    public static MacroOptions.MacroEntry currentMacro;
    public static int currentMacroIndex;
    
    public static Boolean isMovingHorizontally(MinecraftClient client) { // returns false if velocity is virtually 0 when it shouldn't be (when player is holding movement keys)
        Vec3d velocity = client.player.getVelocity();

        return (Math.abs(velocity.x) > 0.001d || Math.abs(velocity.z) > 0.001d) && (client.options.forwardKey.isPressed() || client.options.leftKey.isPressed() || client.options.backKey.isPressed() || client.options.rightKey.isPressed());
    }

    public static Boolean isMovingVertically(MinecraftClient client) { // returns true if vertical velocity is more than virtually 0 (accounting for gravity) when it shouldn't be (when player is not holding the jump key)
        Vec3d velocity = client.player.getVelocity();
        double gravity = -0.08d;

        return Math.abs(velocity.y) + gravity > 0.01d && !client.options.jumpKey.isPressed();
    }

    public static void printLogs(MinecraftClient client, String logMessage, boolean playSound) {
        client.player.sendMessage(Text.literal(logMessage));
        LOGGER.info(logMessage);

        if (playSound) {
            client.player.playSound(SoundEvents.ENTITY_ARROW_HIT_PLAYER, SoundCategory.MASTER, 1, 0);
        }
    }

    public static void delayDisableMacro(MinecraftClient client, String message) {
        if (macroRunning) {
            reactionTimeTicks = macroConfig.macroOptionsContainer.minReactionDuration + (int)(Math.random() * (macroConfig.macroOptionsContainer.maxReactionDuration - macroConfig.macroOptionsContainer.minReactionDuration)); // assigns new value to reactionTimeTicks between min/max range

            finalTick = ticksElapsed + reactionTimeTicks;
            disableMessage = message;

            printLogs(client, "Stopping in " + reactionTimeTicks + " ticks...", true);
        }

        macroRunning = false;
    }

    public static void disableMacro(MinecraftClient client) {
        client.options.forwardKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        client.options.sprintKey.setPressed(false);

        macroRunning = false;
        finalTick = -1;

        printLogs(client, disableMessage + " Disabled.", true);
    }

    public static void enableMacro(MinecraftClient client) {
        ticksElapsed = 0;

        currentStep = 0;
        macroRunning = true;

        printLogs(client, "Enabled.", true);
    }

    /*
    Fabric Macros Notes:
reintroduce min/max step duration for individual steps
reintroduce calibration of min/max step duration based on average step duration for individual steps
find a better way to detect keybinds than the 2 quintillion "else if" statements in my key input handler class
     */

    public static void registerKeyInputs() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (toggleMacroKey.wasPressed()) {
                if (macroRunning) {
                    disableMessage = "Toggle Key pressed.";
                    disableMacro(client);
                } else {
                    enableMacro(client);
                }
            }

            if (ticksElapsed == finalTick) {
                disableMacro(client);
            }

            if (macroRunning) {
                if (macroConfig.macroOptionsContainer.macroList.length < 1) { // stop execution if no macros in config:
                    macroRunning = false;
                    printLogs(client, "No macros detected. Please add a macro to continue.", true);
                    return;
                }

                if (macroConfig.macroOptionsContainer.currentMacroIndex < 1 || macroConfig.macroOptionsContainer.currentMacroIndex > macroConfig.macroOptionsContainer.macroList.length) { // if current macro index is negative or out of bounds:
                    macroConfig.macroOptionsContainer.currentMacroIndex = 1;
                    printLogs(client, "Index less than 1 or out of bounds. Defaulted back to 1.", true);
                    AutoConfig.getConfigHolder(MacroOptions.class).save();
                    return;
                }

                currentMacroIndex = macroConfig.macroOptionsContainer.currentMacroIndex - 1;
                currentMacro = macroConfig.macroOptionsContainer.macroList[currentMacroIndex];

                actualStep = currentStep % currentMacro.stepEntries.length;

                if (currentMacro.verticalMovementFailsafe && isMovingVertically(client)) {
                    delayDisableMacro(client, "Vertical movement detected.");
                }

                if (currentMacro.horizontalMovementFailsafe && !isMovingHorizontally(client) && ticksElapsed <= currentMacro.stepEntries[actualStep].minStepDuration && currentStep != 0) { // if horizontal obstruction is detected before minimum step duration:
                    delayDisableMacro(client, "Horizontal obstruction detected before " + currentMacro.stepEntries[actualStep].minStepDuration + " ticks.");
                }

                if (currentMacro.horizontalMovementFailsafe && !isMovingHorizontally(client) || ticksElapsed >= currentMacro.stepEntries[actualStep].stepDuration || currentStep == 0) { // if specified number of ticks have elapsed (or if macro was just enabled):
                    printLogs(client, "Step: " + (actualStep + 1) + "/" + currentMacro.stepEntries.length, false);

                    if (currentMacro.calibrateOnNextUse && currentStep != 0) {
                        currentMacro.stepEntries[currentStep - 1].stepDuration = ticksElapsed + 10;
                        currentMacro.stepEntries[currentStep - 1].minStepDuration = ticksElapsed - 10;
                        AutoConfig.getConfigHolder(MacroOptions.class).save();

                        printLogs(client, "Step " + (currentStep) + " calibrated. Duration: " + currentMacro.stepEntries[currentStep - 1].stepDuration + ", Minimum Duration: " + currentMacro.stepEntries[currentStep - 1].minStepDuration, true);

                        if (currentStep >= currentMacro.stepEntries.length) {
                            currentMacro.calibrateOnNextUse = false;
                            AutoConfig.getConfigHolder(MacroOptions.class).save();
                            printLogs(client, "All steps calibrated.", true);
                        }
                    }

                    for (int j = 0; j < currentMacro.stepEntries[actualStep].keyEntries.length; j++) {
                        MacroOptions.KeyEntry keyEntry = currentMacro.stepEntries[actualStep].keyEntries[j];

                        printLogs(client, "Key: " + keyEntry.key + ", Pressed: " + keyEntry.pressed, false);

                        if (keyEntry.key == MacroOptions.KeyEntry.KeyType.W) {
                            client.options.forwardKey.setPressed(keyEntry.pressed);
                        } else if (keyEntry.key == MacroOptions.KeyEntry.KeyType.A) {
                            client.options.leftKey.setPressed(keyEntry.pressed);
                        } else if (keyEntry.key == MacroOptions.KeyEntry.KeyType.S) {
                            client.options.backKey.setPressed(keyEntry.pressed);
                        } else if (keyEntry.key == MacroOptions.KeyEntry.KeyType.D) {
                            client.options.rightKey.setPressed(keyEntry.pressed);
                        } else if (keyEntry.key == MacroOptions.KeyEntry.KeyType.LEFT_CLICK) {
                            client.options.attackKey.setPressed(keyEntry.pressed);
                        } else if (keyEntry.key == MacroOptions.KeyEntry.KeyType.RIGHT_CLICK) {
                            client.options.useKey.setPressed(keyEntry.pressed);
                        } else if (keyEntry.key == MacroOptions.KeyEntry.KeyType.JUMP) {
                            client.options.jumpKey.setPressed(keyEntry.pressed);
                        } else if (keyEntry.key == MacroOptions.KeyEntry.KeyType.SNEAK) {
                            client.options.sneakKey.setPressed(keyEntry.pressed);
                        } else if (keyEntry.key == MacroOptions.KeyEntry.KeyType.SPRINT) {
                            client.options.sprintKey.setPressed(keyEntry.pressed);
                        }
                    }

                    currentStep++;
                    ticksElapsed = 0;
                }
            }

            ticksElapsed++;
        });
    }

    public static void register() {
        toggleMacroKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.fabric_macros.toggle_macro",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                KEY_MACRO_CATEGORY
        ));

        registerKeyInputs();
    }
}
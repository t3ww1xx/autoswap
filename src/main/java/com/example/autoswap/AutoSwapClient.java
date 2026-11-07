package com.example.autoswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class AutoSwapClient implements ClientModInitializer {

    private static final int OFFHAND_SWAP_BUTTON = 40;

    // С 1.21.9 категория клавиш — объект. Название берётся из lang: key.category.autoswap.main
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("autoswap", "main"));

    private static KeyBinding toggleKey;
    private static KeyBinding menuKey;
    private static KeyBinding ballKey;
    private static int cooldown = 0;

    @Override
    public void onInitializeClient() {
        AutoSwapConfig.load();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoswap.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                CATEGORY
        ));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoswap.menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                CATEGORY
        ));

        ballKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoswap.ballswap",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        AutoSwapConfig cfg = AutoSwapConfig.get();

        while (menuKey.wasPressed()) {
            client.setScreen(new AutoSwapScreen(null));
        }

        while (ballKey.wasPressed()) {
            swapBall(player, client, cfg);
        }

        while (toggleKey.wasPressed()) {
            cfg.enabled = !cfg.enabled;
            AutoSwapConfig.save();
            player.sendMessage(Text.literal("AutoSwap: " + (cfg.enabled ? "§aON" : "§cOFF")), true);
        }

        if (!cfg.enabled) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }

        Item item = cfg.getItem();
        ItemStack offhand = player.getOffHandStack();
        if (offhand.isOf(item)) return;
        if (cfg.onlyIfEmpty && !offhand.isEmpty()) return;

        int slotId = findSlot(player, item);
        if (slotId == -1) return;

        client.interactionManager.clickSlot(
                player.playerScreenHandler.syncId,
                slotId,
                OFFHAND_SWAP_BUTTON,
                SlotActionType.SWAP,
                player
        );
        cooldown = cfg.delayTicks;
    }

    /** Меняет предмет в левой руке на "шар" из хотбара (поиск по части названия). */
    private void swapBall(ClientPlayerEntity player, MinecraftClient client, AutoSwapConfig cfg) {
        String filter = cfg.ballFilter == null ? "" : cfg.ballFilter.trim().toLowerCase(Locale.ROOT);
        if (filter.isEmpty()) {
            player.sendMessage(Text.literal("AutoSwap: §cукажите название шара в настройках"), true);
            return;
        }

        ItemStack offhand = player.getOffHandStack();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (!stack.getName().getString().toLowerCase(Locale.ROOT).contains(filter)) continue;
            if (sameStack(stack, offhand)) continue;

            client.interactionManager.clickSlot(
                    player.playerScreenHandler.syncId,
                    36 + i,
                    OFFHAND_SWAP_BUTTON,
                    SlotActionType.SWAP,
                    player
            );
            player.sendMessage(Text.literal("AutoSwap: §aшар заменён"), true);
            return;
        }
        player.sendMessage(Text.literal("AutoSwap: §cшар в хотбаре не найден"), true);
    }

    private static boolean sameStack(ItemStack a, ItemStack b) {
        return a.getItem() == b.getItem()
                && a.getName().getString().equals(b.getName().getString());
    }

    /** Номер слота в playerScreenHandler или -1, если предмета нет. */
    private int findSlot(ClientPlayerEntity player, Item item) {
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).isOf(item)) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }
}

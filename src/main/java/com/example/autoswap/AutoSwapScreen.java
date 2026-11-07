package com.example.autoswap;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class AutoSwapScreen extends Screen {

    private static final String[] ITEMS = {
            "minecraft:totem_of_undying",
            "minecraft:shield",
            "minecraft:golden_apple",
            "minecraft:enchanted_golden_apple",
            "minecraft:end_crystal",
            "minecraft:firework_rocket",
            "minecraft:experience_bottle",
            "minecraft:ender_pearl"
    };

    private final Screen parent;
    private int labelX;
    private int labelY;
    private int titleY;

    public AutoSwapScreen(Screen parent) {
        super(Text.literal("AutoSwap"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AutoSwapConfig cfg = AutoSwapConfig.get();
        int w = 220;
        int h = 20;
        int x = this.width / 2 - w / 2;
        int y = this.height / 2 - 80;
        titleY = y - 16;
        int step = 26;

        // 1. Включить / выключить
        addDrawableChild(ButtonWidget.builder(enabledText(cfg), button -> {
            cfg.enabled = !cfg.enabled;
            button.setMessage(enabledText(cfg));
        }).dimensions(x, y, w, h).build());

        // 2. Выбор предмета (по кругу)
        addDrawableChild(ButtonWidget.builder(itemText(cfg), button -> {
            int index = 0;
            for (int i = 0; i < ITEMS.length; i++) {
                if (ITEMS[i].equals(cfg.item)) {
                    index = i;
                    break;
                }
            }
            cfg.item = ITEMS[(index + 1) % ITEMS.length];
            button.setMessage(itemText(cfg));
        }).dimensions(x, y + step, w, h).build());

        // 3. Только если рука пуста
        addDrawableChild(ButtonWidget.builder(emptyText(cfg), button -> {
            cfg.onlyIfEmpty = !cfg.onlyIfEmpty;
            button.setMessage(emptyText(cfg));
        }).dimensions(x, y + step * 2, w, h).build());

        // 4. Задержка
        addDrawableChild(new DelaySlider(x, y + step * 3, w, h, cfg));

        // 5. Название шара для свапа по клавише
        labelX = x;
        labelY = y + step * 4 + 2;
        TextFieldWidget field = new TextFieldWidget(this.textRenderer, x, y + step * 4 + 14, w, h, Text.literal("Название шара"));
        field.setMaxLength(64);
        field.setText(cfg.ballFilter == null ? "" : cfg.ballFilter);
        field.setChangedListener(text -> cfg.ballFilter = text);
        addDrawableChild(field);

        // 6. Готово
        addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), button -> close())
                .dimensions(x, y + step * 4 + 14 + 32, w, h).build());
    }

    private static Text enabledText(AutoSwapConfig cfg) {
        return Text.literal("Мод: " + (cfg.enabled ? "§aВключён" : "§cВыключен"));
    }

    private static Text itemText(AutoSwapConfig cfg) {
        return Text.literal("Предмет: ").append(new ItemStack(cfg.getItem()).getName());
    }

    private static Text emptyText(AutoSwapConfig cfg) {
        return Text.literal("Только если рука пуста: " + (cfg.onlyIfEmpty ? "§aДа" : "§cНет"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, titleY, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.literal("Шар (часть названия) для свапа по клавише:"), labelX, labelY, 0xAAAAAA);
    }

    @Override
    public void close() {
        AutoSwapConfig.save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private static class DelaySlider extends SliderWidget {
        private final AutoSwapConfig cfg;

        DelaySlider(int x, int y, int width, int height, AutoSwapConfig cfg) {
            super(x, y, width, height, Text.empty(), cfg.delayTicks / 20.0);
            this.cfg = cfg;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            int ticks = (int) Math.round(this.value * 20);
            setMessage(Text.literal("Задержка: " + ticks + " тик. (" + (ticks * 50) + " мс)"));
        }

        @Override
        protected void applyValue() {
            cfg.delayTicks = (int) Math.round(this.value * 20);
        }
    }
}

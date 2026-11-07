package com.example.autoswap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class AutoSwapConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("autoswap.json");
    private static AutoSwapConfig instance = new AutoSwapConfig();

    public boolean enabled = true;
    public String item = "minecraft:totem_of_undying";
    public boolean onlyIfEmpty = true;
    public int delayTicks = 3;
    /** Часть названия предмета, по которой ищется "шар" в хотбаре. */
    public String ballFilter = "шар";

    public static AutoSwapConfig get() {
        return instance;
    }

    public Item getItem() {
        Identifier id = Identifier.tryParse(item);
        if (id == null) return Items.TOTEM_OF_UNDYING;
        Item result = Registries.ITEM.get(id);
        return result == Items.AIR ? Items.TOTEM_OF_UNDYING : result;
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(PATH)) {
            AutoSwapConfig loaded = GSON.fromJson(reader, AutoSwapConfig.class);
            if (loaded != null) {
                loaded.delayTicks = Math.max(0, Math.min(20, loaded.delayTicks));
                if (loaded.ballFilter == null) loaded.ballFilter = "шар";
                instance = loaded;
            }
        } catch (Exception e) {
            System.err.println("[AutoSwap] Не удалось прочитать конфиг, используются значения по умолчанию: " + e);
        }
    }

    public static void save() {
        try (Writer writer = Files.newBufferedWriter(PATH)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            System.err.println("[AutoSwap] Не удалось сохранить конфиг: " + e);
        }
    }
}

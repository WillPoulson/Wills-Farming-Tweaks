package uk.co.willpoulson.willsfarmingtweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("wills-farming-tweaks.json");

    private static ModConfig config = new ModConfig();

    public static ModConfig get() {
        return config;
    }

    public static void load() {
        if (!Files.exists(PATH)) {
            config = new ModConfig();
            save();
            return;
        }

        try (Reader r = Files.newBufferedReader(PATH)) {
            ModConfig loaded = GSON.fromJson(r, ModConfig.class);
            config = (loaded != null) ? loaded : new ModConfig();
        } catch (IOException e) {
            config = new ModConfig();
        }

        sanitize();
        save(); // optional: writes back missing defaults / sanitized values
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(PATH)) {
            GSON.toJson(config, w);
        } catch (IOException ignored) {
        }
    }

    private static void sanitize() {
        if (config == null) config = new ModConfig();

        if (config.harvest == null) config.harvest = new ModConfig.Harvest();
        if (config.bonemeal == null) config.bonemeal = new ModConfig.Bonemeal();

        if (config.harvest.radiusByItemId == null) {
            config.harvest.radiusByItemId = new ModConfig.Harvest().radiusByItemId;
        }

        for (var e : new ModConfig.Harvest().radiusByItemId.entrySet()) {
            config.harvest.radiusByItemId.putIfAbsent(e.getKey(), e.getValue());
        }

        config.harvest.cooldownTicks = clampInt(config.harvest.cooldownTicks, 0, 100);
        config.harvest.defaultRadius = clampInt(config.harvest.defaultRadius, 0, 16);

        config.bonemeal.radius = clampInt(config.bonemeal.radius, 0, 16);
        config.bonemeal.baseChance = clampDouble(config.bonemeal.baseChance, 0.0, 1.0);

        // Normalize + clamp map entries (remove null keys, fix null values)
        config.harvest.radiusByItemId.entrySet().removeIf(e -> e.getKey() == null || e.getKey().isBlank());

        for (var e : config.harvest.radiusByItemId.entrySet()) {
            Integer v = e.getValue();
            e.setValue(clampInt(v == null ? config.harvest.defaultRadius : v, 0, 16));
        }
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private ConfigManager() {}
}

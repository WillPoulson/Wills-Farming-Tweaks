package uk.co.willpoulson.willsfarmingtweaks.config;

import java.util.HashMap;
import java.util.Map;

public class ModConfig {
    public boolean allowTrample = false;
    public Harvest harvest = new Harvest();
    public Bonemeal bonemeal = new Bonemeal();

    public static class Harvest {
        public int cooldownTicks = 8;
        public int defaultRadius = 0;
        public Map<String, Integer> radiusByItemId = defaultRadiusByItemId();
    }

    public static class Bonemeal {
        public int radius = 2;
        public double baseChance = 1.0;
    }

    private static Map<String, Integer> defaultRadiusByItemId() {
        Map<String, Integer> m = new HashMap<>();
        m.put("minecraft:wooden_hoe", 0);
        m.put("minecraft:stone_hoe", 1);
        m.put("minecraft:iron_hoe", 2);
        m.put("minecraft:golden_hoe", 2);
        m.put("minecraft:diamond_hoe", 3);
        m.put("minecraft:netherite_hoe", 4);
        return m;
    }
}

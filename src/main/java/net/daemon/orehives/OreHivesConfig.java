package net.daemon.orehives;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Simple JSON config at config/orehives.json. */
public class OreHivesConfig {
    /** Ticks between production checks (1200 = 1 min). */
    public int checkIntervalTicks = 1200;
    /**
     * Ticks between bee scans. Natural bees spend long stretches inside their vanilla hive
     * (not in the world), so we sample often and remember every bee seen during the cycle.
     */
    public int beeSampleIntervalTicks = 100;
    /** Chance per qualifying bee per check to add a comb. */
    public double combChancePerBee = 0.25;
    /** Max distinct bees counted per hive per production cycle. */
    public int maxBeesCounted = 3;
    /** Search radius for bees around the hive. */
    public int beeRadius = 8;
    /** Max combs stored in a hive. */
    public int maxStoredCombs = 8;
    /** Comb chance multiplier for precious ores (diamond, emerald) so they stay rare. */
    public double preciousChanceMultiplier = 0.2;
    /** Allow generic flowers (#orehives:ore_flowers) next to/under ore blocks to work (fallback without OrePlants). */
    public boolean allowFlowerOnOreFallback = true;
    /**
     * How many blocks beneath the flower's ground block to search for #c:ores/&lt;ore&gt;.
     * The ground block itself is always checked, then this many further down
     * (through dirt/grass/farmland). Default 2 covers flower-on-grass with ore
     * under the grass, or under grass+dirt — Survival flowers cannot sit on ore.
     */
    public int oreSearchDepth = 2;
    /** Centrifuge processing time in ticks. */
    public int centrifugeTicks = 100;

    public static OreHivesConfig INSTANCE = new OreHivesConfig();

    public static void load() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("orehives.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(p)) {
                OreHivesConfig c = gson.fromJson(Files.readString(p), OreHivesConfig.class);
                if (c != null) INSTANCE = c;
            }
            Files.writeString(p, gson.toJson(INSTANCE));
        } catch (Exception e) {
            OreHives.LOGGER.warn("Could not read/write orehives.json, using defaults", e);
        }
    }
}

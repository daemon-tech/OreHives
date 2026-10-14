package net.daemon.orehives;

import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/** Ore types a hive can produce. Flower->ore mapping is fully tag driven. */
public enum OreType {
    IRON, COPPER, GOLD, DIAMOND;

    public final String id = name().toLowerCase();
    /** Flowers that directly count as this ore (e.g. oreplant:iron_crop_block). */
    public final TagKey<Block> flowerTag = TagKey.of(RegistryKeys.BLOCK, OreHives.id("ore_flowers/" + id));
    /** Ore blocks that "infuse" a generic ore flower standing on/next to them. */
    public final TagKey<Block> oreTag = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores/" + id));

    public static OreType byId(String s) {
        for (OreType t : values()) if (t.id.equals(s)) return t;
        return null;
    }
}

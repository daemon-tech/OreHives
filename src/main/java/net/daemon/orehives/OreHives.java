package net.daemon.orehives;

import net.daemon.orehives.block.CentrifugeBlock;
import net.daemon.orehives.block.CentrifugeBlockEntity;
import net.daemon.orehives.block.OreBeehiveBlock;
import net.daemon.orehives.block.OreBeehiveBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.registry.FabricBrewingRecipeRegistryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

public class OreHives implements ModInitializer {
    public static final String MOD_ID = "orehives";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String p) { return Identifier.of(MOD_ID, p); }

    public static final Block ORE_BEEHIVE = new OreBeehiveBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.STONE_GRAY).strength(1.5f).sounds(BlockSoundGroup.WOOD));
    public static final Block CENTRIFUGE = new CentrifugeBlock(AbstractBlock.Settings.create()
            .mapColor(MapColor.IRON_GRAY).strength(3.0f).requiresTool().sounds(BlockSoundGroup.METAL));

    public static BlockEntityType<OreBeehiveBlockEntity> ORE_BEEHIVE_BE;
    public static BlockEntityType<CentrifugeBlockEntity> CENTRIFUGE_BE;

    public static final Map<OreType, Item> COMBS = new EnumMap<>(OreType.class);
    public static final Item ORE_HONEY = new HoneyBottleItem(new Item.Settings().maxCount(16)
            .recipeRemainder(Items.GLASS_BOTTLE)
            .food(new FoodComponent.Builder().nutrition(4).saturationModifier(0.3f).alwaysEdible()
                    .statusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20 * 60), 1.0f).build()));

    public static boolean orePlantsLoaded;

    @Override
    public void onInitialize() {
        OreHivesConfig.load();
        orePlantsLoaded = FabricLoader.getInstance().isModLoaded("oreplant");

        Registry.register(Registries.BLOCK, id("ore_beehive"), ORE_BEEHIVE);
        Registry.register(Registries.BLOCK, id("centrifuge"), CENTRIFUGE);
        Registry.register(Registries.ITEM, id("ore_beehive"), new BlockItem(ORE_BEEHIVE, new Item.Settings()));
        Registry.register(Registries.ITEM, id("centrifuge"), new BlockItem(CENTRIFUGE, new Item.Settings()));
        for (OreType t : OreType.values()) {
            COMBS.put(t, Registry.register(Registries.ITEM, id(t.id + "_comb"), new Item(new Item.Settings())));
        }
        Registry.register(Registries.ITEM, id("ore_honey_bottle"), ORE_HONEY);

        ORE_BEEHIVE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("ore_beehive"),
                BlockEntityType.Builder.create(OreBeehiveBlockEntity::new, ORE_BEEHIVE).build(null));
        CENTRIFUGE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("centrifuge"),
                BlockEntityType.Builder.create(CentrifugeBlockEntity::new, CENTRIFUGE).build(null));

        Registry.register(Registries.ITEM_GROUP, id("main"), FabricItemGroup.builder()
                .icon(() -> new ItemStack(COMBS.get(OreType.IRON)))
                .displayName(Text.translatable("itemGroup.orehives.main"))
                .entries((ctx, e) -> {
                    e.add(ORE_BEEHIVE); e.add(CENTRIFUGE);
                    COMBS.values().forEach(e::add);
                    e.add(ORE_HONEY);
                }).build());

        // Ore honey as potion ingredient: awkward + ore honey -> strength
        FabricBrewingRecipeRegistryBuilder.BUILD.register(b -> b.registerPotionRecipe(Potions.AWKWARD, ORE_HONEY, Potions.STRENGTH));

        LOGGER.info("Ore Hives loaded (OrePlants present: {})", orePlantsLoaded);
    }
}

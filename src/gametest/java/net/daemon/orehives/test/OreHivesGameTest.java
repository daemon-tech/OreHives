package net.daemon.orehives.test;

import net.daemon.orehives.OreHives;
import net.daemon.orehives.OreHivesConfig;
import net.daemon.orehives.OreType;
import net.daemon.orehives.block.CentrifugeBlockEntity;
import net.daemon.orehives.block.OreBeehiveBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.HopperBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public class OreHivesGameTest implements FabricGameTest {
    /**
     * Survival-friendly fallback: flower on grass/dirt counts when ore is buried
     * 1–2 blocks under it; adjacent ore also counts; plain dirt does not.
     */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void flowerOnOreResolvesType(TestContext ctx) {
        // Flower on grass, iron ore directly under the grass (depth 1).
        BlockPos a = new BlockPos(1, 1, 1);
        ctx.setBlockState(a, Blocks.IRON_ORE);
        ctx.setBlockState(a.up(), Blocks.GRASS_BLOCK);
        ctx.setBlockState(a.up(2), Blocks.POPPY);
        OreType t = OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(a.up(2)));
        ctx.assertTrue(t == OreType.IRON, "depth-1 under grass: expected IRON, got " + t);

        // Flower on grass, iron ore two blocks under (depth 2).
        BlockPos b = new BlockPos(3, 1, 1);
        ctx.setBlockState(b, Blocks.IRON_ORE);
        ctx.setBlockState(b.up(), Blocks.DIRT);
        ctx.setBlockState(b.up(2), Blocks.GRASS_BLOCK);
        ctx.setBlockState(b.up(3), Blocks.POPPY);
        t = OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(b.up(3)));
        ctx.assertTrue(t == OreType.IRON, "depth-2 under grass: expected IRON, got " + t);

        // Ore next to the ground block (existing adjacent check).
        BlockPos ground = new BlockPos(5, 1, 1);
        ctx.setBlockState(ground.east(), Blocks.LAPIS_ORE);
        ctx.setBlockState(ground, Blocks.DIRT);
        ctx.setBlockState(ground.up(), Blocks.POPPY);
        t = OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(ground.up()));
        ctx.assertTrue(t == OreType.LAPIS, "adjacent: expected LAPIS, got " + t);

        // Flower on dirt with no ore nearby must not count.
        BlockPos plain = new BlockPos(1, 1, 4);
        ctx.setBlockState(plain, Blocks.DIRT);
        ctx.setBlockState(plain.up(), Blocks.POPPY);
        ctx.assertTrue(OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(plain.up())) == null,
                "plain flower on dirt must not count");
        ctx.complete();
    }

    @GameTest(templateName = EMPTY_STRUCTURE)
    public void hiveHarvestAndCentrifugeLoot(TestContext ctx) {
        BlockPos p = new BlockPos(1, 1, 1);
        ctx.setBlockState(p, OreHives.ORE_BEEHIVE);
        OreBeehiveBlockEntity be = (OreBeehiveBlockEntity) ctx.getBlockEntity(p);
        be.add(OreType.GOLD, 2);
        ctx.assertTrue(be.totalCombs() == 2, "combs stored");
        ctx.assertTrue(be.harvest(ctx.getWorld(), ctx.getAbsolutePos(p)), "harvest");
        ctx.assertTrue(be.totalCombs() == 0, "emptied");
        List<ItemStack> out = CentrifugeBlockEntity.roll(ctx.getWorld(), ctx.getAbsolutePos(p), new ItemStack(OreHives.COMBS.get(OreType.REDSTONE)));
        ctx.assertTrue(out.stream().anyMatch(s -> s.isOf(Items.REDSTONE)), "redstone comb -> redstone, got " + out);
        ctx.complete();
    }

    /** Hopper above feeds combs, hopper below pulls iron nuggets out of the buffer. */
    @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 1200)
    public void hopperAutomation(TestContext ctx) {
        BlockPos below = new BlockPos(1, 1, 1), c = below.up(), above = c.up();
        ctx.setBlockState(below, Blocks.HOPPER.getDefaultState().with(HopperBlock.FACING, Direction.DOWN));
        ctx.setBlockState(c, OreHives.CENTRIFUGE);
        ctx.setBlockState(above, Blocks.HOPPER.getDefaultState().with(HopperBlock.FACING, Direction.DOWN));
        var top = (net.minecraft.inventory.Inventory) ctx.getBlockEntity(above);
        top.setStack(0, new ItemStack(OreHives.COMBS.get(OreType.IRON), 3));
        ctx.assertTrue(OreHivesConfig.INSTANCE.centrifugeTicks < 1000, "centrifuge time fits the test");
        ctx.waitAndRun(OreHivesConfig.INSTANCE.centrifugeTicks + 80, () -> {
            var bottom = (net.minecraft.inventory.Inventory) ctx.getBlockEntity(below);
            boolean got = false;
            for (int i = 0; i < bottom.size(); i++) got |= bottom.getStack(i).isOf(Items.IRON_NUGGET);
            ctx.assertTrue(got, "bottom hopper should contain iron nuggets");
            ctx.complete();
        });
    }

    /** A bee whose flower is an ore flower is sampled once (deduplicated) and yields combs. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void beeIsSampledAndCounted(TestContext ctx) {
        BlockPos ore = new BlockPos(1, 1, 1), hive = new BlockPos(4, 2, 4);
        ctx.setBlockState(ore, Blocks.GOLD_ORE);
        ctx.setBlockState(ore.up(), Blocks.GRASS_BLOCK);
        ctx.setBlockState(ore.up(2), Blocks.POPPY);
        ctx.setBlockState(hive, OreHives.ORE_BEEHIVE);
        BeeEntity bee = ctx.spawnEntity(EntityType.BEE, new BlockPos(3, 3, 3));
        bee.setAiDisabled(true);
        bee.setFlowerPos(ctx.getAbsolutePos(ore.up(2)));
        OreBeehiveBlockEntity be = (OreBeehiveBlockEntity) ctx.getBlockEntity(hive);
        be.sampleBees(ctx.getWorld(), ctx.getAbsolutePos(hive));
        be.sampleBees(ctx.getWorld(), ctx.getAbsolutePos(hive));
        ctx.assertTrue(be.seenBeeCount() == 1, "same bee must be counted once, got " + be.seenBeeCount());
        double old = OreHivesConfig.INSTANCE.combChancePerBee;
        OreHivesConfig.INSTANCE.combChancePerBee = 1.0;
        try { be.produce(ctx.getWorld(), ctx.getAbsolutePos(hive)); }
        finally { OreHivesConfig.INSTANCE.combChancePerBee = old; }
        ctx.assertTrue(be.count(OreType.GOLD) == 1, "expected 1 gold comb, got " + be.describe());
        ctx.assertTrue(be.seenBeeCount() == 0, "cycle resets seen bees");
        ctx.complete();
    }
}

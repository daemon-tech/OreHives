package net.daemon.orehives.test;

import net.daemon.orehives.OreHives;
import net.daemon.orehives.OreHivesConfig;
import net.daemon.orehives.OreType;
import net.daemon.orehives.block.CentrifugeBlockEntity;
import net.daemon.orehives.block.OreBeehiveBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.HopperBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.List;

public class OreHivesGameTest implements FabricGameTest {
    /** Fallback path: generic flower on an ore block counts as that ore. */
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void flowerOnOreResolvesType(TestContext ctx) {
        BlockPos ore = new BlockPos(1, 1, 1);
        ctx.setBlockState(ore, Blocks.IRON_ORE);
        ctx.setBlockState(ore.up(), Blocks.POPPY);
        OreType t = OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(ore.up()));
        ctx.assertTrue(t == OreType.IRON, "expected IRON, got " + t);
        // Ore next to (not under) the flower's ground block also counts.
        BlockPos ground = new BlockPos(3, 1, 1);
        ctx.setBlockState(ground.east(), Blocks.LAPIS_ORE);
        ctx.setBlockState(ground, Blocks.DIRT);
        ctx.setBlockState(ground.up(), Blocks.POPPY);
        t = OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(ground.up()));
        ctx.assertTrue(t == OreType.LAPIS, "expected LAPIS, got " + t);
        BlockPos plain = new BlockPos(1, 1, 4);
        ctx.setBlockState(plain, Blocks.DIRT);
        ctx.setBlockState(plain.up(), Blocks.POPPY);
        ctx.assertTrue(OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(plain.up())) == null, "plain flower must not count");
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
}

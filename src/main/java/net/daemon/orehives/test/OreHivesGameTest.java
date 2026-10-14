package net.daemon.orehives.test;

import net.daemon.orehives.OreHives;
import net.daemon.orehives.OreType;
import net.daemon.orehives.block.CentrifugeBlockEntity;
import net.daemon.orehives.block.OreBeehiveBlockEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class OreHivesGameTest implements FabricGameTest {
    @GameTest(templateName = EMPTY_STRUCTURE)
    public void flowerOnIronOreGivesIron(TestContext ctx) {
        BlockPos ore = new BlockPos(1, 1, 1);
        ctx.setBlockState(ore, Blocks.IRON_ORE);
        ctx.setBlockState(ore.up(), Blocks.POPPY);
        OreType t = OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(ore.up()));
        ctx.assertTrue(t == OreType.IRON, "expected IRON, got " + t);
        ctx.setBlockState(ore, Blocks.DIRT);
        ctx.assertTrue(OreBeehiveBlockEntity.oreTypeOf(ctx.getWorld(), ctx.getAbsolutePos(ore.up())) == null, "plain flower must not count");
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
        List<ItemStack> out = CentrifugeBlockEntity.roll(ctx.getWorld(), ctx.getAbsolutePos(p), OreHives.COMBS.get(OreType.GOLD));
        ctx.assertTrue(out.stream().anyMatch(s -> s.isOf(Items.GOLD_NUGGET)), "gold comb -> gold nuggets, got " + out);
        ctx.complete();
    }
}

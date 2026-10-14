package net.daemon.orehives.block;

import net.daemon.orehives.OreHives;
import net.daemon.orehives.OreHivesConfig;
import net.daemon.orehives.OreType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class OreBeehiveBlockEntity extends BlockEntity {
    /** Generic fallback flowers: work when planted on/next to an ore block. */
    public static final TagKey<Block> ORE_FLOWERS = TagKey.of(RegistryKeys.BLOCK, OreHives.id("ore_flowers"));

    private final Map<OreType, Integer> combs = new EnumMap<>(OreType.class);
    private int timer;

    public OreBeehiveBlockEntity(BlockPos pos, BlockState state) { super(OreHives.ORE_BEEHIVE_BE, pos, state); }

    public static void serverTick(World world, BlockPos pos, BlockState state, OreBeehiveBlockEntity be) {
        OreHivesConfig c = OreHivesConfig.INSTANCE;
        if (++be.timer < c.checkIntervalTicks) return;
        be.timer = 0;
        be.produce(world, pos);
    }

    /** One production check; public for gametests. */
    public void produce(World world, BlockPos pos) {
        OreHivesConfig c = OreHivesConfig.INSTANCE;
        if (totalCombs() >= c.maxStoredCombs) return;
        List<BeeEntity> bees = world.getEntitiesByClass(BeeEntity.class, new Box(pos).expand(c.beeRadius),
                b -> b.getFlowerPos() != null);
        int counted = 0;
        for (BeeEntity bee : bees) {
            if (counted >= c.maxBeesCounted || totalCombs() >= c.maxStoredCombs) break;
            OreType t = oreTypeOf(world, bee.getFlowerPos());
            if (t == null) continue;
            counted++;
            double chance = c.combChancePerBee * (t == OreType.DIAMOND ? c.diamondChanceMultiplier : 1.0);
            if (world.random.nextDouble() < chance) combs.merge(t, 1, Integer::sum);
        }
        updateState(world, pos);
    }

    /** Resolve the ore "pollen" type of a flower. Tag driven; OrePlants entries are optional tag entries. */
    public static OreType oreTypeOf(World world, BlockPos flower) {
        BlockState fs = world.getBlockState(flower);
        for (OreType t : OreType.values()) if (fs.isIn(t.flowerTag)) return t;
        if (!OreHivesConfig.INSTANCE.allowFlowerOnOreFallback || !fs.isIn(ORE_FLOWERS)) return null;
        BlockPos below = flower.down();
        for (OreType t : OreType.values()) {
            if (world.getBlockState(below).isIn(t.oreTag)) return t;
            for (Direction d : Direction.Type.HORIZONTAL)
                if (world.getBlockState(below.offset(d)).isIn(t.oreTag)) return t;
        }
        return null;
    }

    public int totalCombs() { return combs.values().stream().mapToInt(Integer::intValue).sum(); }
    public int count(OreType t) { return combs.getOrDefault(t, 0); }
    public void add(OreType t, int n) { combs.merge(t, n, Integer::sum); if (world != null) updateState(world, pos); }

    public String describe() {
        StringBuilder sb = new StringBuilder();
        combs.forEach((t, n) -> { if (n > 0) sb.append(t.id).append('=').append(n).append(' '); });
        return sb.toString().trim();
    }

    public boolean harvest(World world, BlockPos pos) {
        if (totalCombs() == 0) return false;
        combs.forEach((t, n) -> ItemScatterer.spawn(world, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5,
                new ItemStack(OreHives.COMBS.get(t), n)));
        combs.clear();
        updateState(world, pos);
        return true;
    }

    private void updateState(World world, BlockPos pos) {
        int total = totalCombs();
        int fill = total == 0 ? 0 : total >= OreHivesConfig.INSTANCE.maxStoredCombs ? 2 : 1;
        BlockState s = world.getBlockState(pos);
        if (s.contains(OreBeehiveBlock.FILL) && s.get(OreBeehiveBlock.FILL) != fill)
            world.setBlockState(pos, s.with(OreBeehiveBlock.FILL, fill));
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup reg) {
        NbtCompound c = new NbtCompound();
        combs.forEach((t, n) -> c.putInt(t.id, n));
        nbt.put("Combs", c);
        nbt.putInt("Timer", timer);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup reg) {
        combs.clear();
        NbtCompound c = nbt.getCompound("Combs");
        for (String k : c.getKeys()) { OreType t = OreType.byId(k); if (t != null) combs.put(t, c.getInt(k)); }
        timer = nbt.getInt("Timer");
    }
}

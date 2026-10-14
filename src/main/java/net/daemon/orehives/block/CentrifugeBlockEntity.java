package net.daemon.orehives.block;

import net.daemon.orehives.OreHives;
import net.daemon.orehives.OreHivesConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/** Outputs are data-driven: loot table orehives:centrifuge/<comb item path>. */
public class CentrifugeBlockEntity extends BlockEntity {
    private Item input;
    private int progress;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) { super(OreHives.CENTRIFUGE_BE, pos, state); }

    public boolean insert(Item comb) {
        if (input != null) return false;
        input = comb; progress = 0;
        if (world != null) world.setBlockState(pos, getCachedState().with(CentrifugeBlock.RUNNING, true));
        markDirty();
        return true;
    }

    public boolean isBusy() { return input != null; }

    public static void serverTick(World world, BlockPos pos, BlockState state, CentrifugeBlockEntity be) {
        if (be.input == null) return;
        if (++be.progress < OreHivesConfig.INSTANCE.centrifugeTicks) return;
        for (ItemStack out : roll((ServerWorld) world, pos, be.input))
            ItemScatterer.spawn(world, pos.getX() + .5, pos.getY() + 1.1, pos.getZ() + .5, out);
        be.input = null; be.progress = 0;
        world.setBlockState(pos, state.with(CentrifugeBlock.RUNNING, false));
        be.markDirty();
    }

    public static List<ItemStack> roll(ServerWorld world, BlockPos pos, Item comb) {
        Identifier itemId = Registries.ITEM.getId(comb);
        RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE,
                Identifier.of(itemId.getNamespace(), "centrifuge/" + itemId.getPath()));
        LootTable table = world.getServer().getReloadableRegistries().getLootTable(key);
        return table.generateLoot(new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos)).build(LootContextTypes.CHEST));
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup reg) {
        if (input != null) nbt.putString("Input", Registries.ITEM.getId(input).toString());
        nbt.putInt("Progress", progress);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup reg) {
        input = nbt.contains("Input") ? Registries.ITEM.get(Identifier.of(nbt.getString("Input"))) : null;
        progress = nbt.getInt("Progress");
    }
}

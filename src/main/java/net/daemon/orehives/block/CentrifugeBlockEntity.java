package net.daemon.orehives.block;

import net.daemon.orehives.OreHives;
import net.daemon.orehives.OreHivesConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
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
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Spins one comb at a time into an internal output buffer.
 * Hoppers: insert combs from top/sides (slot 0), extract results from the bottom (slots 1..N).
 * Outputs are data-driven: loot table {@code <ns>:centrifuge/<comb item path>}.
 */
public class CentrifugeBlockEntity extends BlockEntity implements SidedInventory {
    public static final int INPUT = 0;
    public static final int OUTPUT_SLOTS = 6;
    private static final int[] INPUT_SLOTS = {INPUT};
    private static final int[] OUT_SLOTS = {1, 2, 3, 4, 5, 6};

    private final DefaultedList<ItemStack> items = DefaultedList.ofSize(1 + OUTPUT_SLOTS, ItemStack.EMPTY);
    private int progress;

    public CentrifugeBlockEntity(BlockPos pos, BlockState state) { super(OreHives.CENTRIFUGE_BE, pos, state); }

    public static boolean isComb(ItemStack s) { return s.isIn(OreHives.ORE_COMBS_TAG); }

    public static void serverTick(World world, BlockPos pos, BlockState state, CentrifugeBlockEntity be) {
        boolean running = !be.items.get(INPUT).isEmpty() && be.hasFreeOutputSlot();
        if (state.get(CentrifugeBlock.RUNNING) != running) world.setBlockState(pos, state.with(CentrifugeBlock.RUNNING, running));
        if (!running) { be.progress = 0; return; }
        if (++be.progress < OreHivesConfig.INSTANCE.centrifugeTicks) return;
        be.progress = 0;
        ItemStack comb = be.items.get(INPUT).split(1);
        // Overflow beyond the buffer is lost; we only start when at least one slot is free.
        for (ItemStack out : roll((ServerWorld) world, pos, comb)) be.addOutput(out);
        be.markDirty();
    }

    /** Rolls the centrifuge loot table for a comb. Public for gametests. */
    public static List<ItemStack> roll(ServerWorld world, BlockPos pos, ItemStack comb) {
        Identifier itemId = Registries.ITEM.getId(comb.getItem());
        RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE,
                Identifier.of(itemId.getNamespace(), "centrifuge/" + itemId.getPath()));
        LootTable table = world.getServer().getReloadableRegistries().getLootTable(key);
        return table.generateLoot(new LootContextParameterSet.Builder(world)
                .add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos)).build(LootContextTypes.CHEST));
    }

    private boolean hasFreeOutputSlot() {
        for (int s : OUT_SLOTS) if (items.get(s).isEmpty()) return true;
        return false;
    }

    private void addOutput(ItemStack stack) {
        for (int s : OUT_SLOTS) {
            ItemStack cur = items.get(s);
            if (cur.isEmpty()) { items.set(s, stack); return; }
            if (ItemStack.areItemsAndComponentsEqual(cur, stack)) {
                int move = Math.min(stack.getCount(), cur.getMaxCount() - cur.getCount());
                cur.increment(move); stack.decrement(move);
                if (stack.isEmpty()) return;
            }
        }
    }

    /** Player helper: moves all outputs to the player (drops what doesn't fit). */
    public boolean giveOutputs(PlayerEntity player) {
        boolean any = false;
        for (int s : OUT_SLOTS) {
            ItemStack st = items.get(s);
            if (st.isEmpty()) continue;
            any = true;
            if (!player.giveItemStack(st)) player.dropItem(st, false);
            items.set(s, ItemStack.EMPTY);
        }
        if (any) markDirty();
        return any;
    }

    // --- SidedInventory ---
    @Override public int[] getAvailableSlots(Direction side) { return side == Direction.DOWN ? OUT_SLOTS : INPUT_SLOTS; }
    @Override public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) { return slot == INPUT && isComb(stack); }
    @Override public boolean canExtract(int slot, ItemStack stack, Direction dir) { return slot != INPUT && dir == Direction.DOWN; }
    @Override public boolean isValid(int slot, ItemStack stack) { return slot == INPUT && isComb(stack); }
    @Override public int size() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getStack(int slot) { return items.get(slot); }
    @Override public ItemStack removeStack(int slot, int amount) { ItemStack r = Inventories.splitStack(items, slot, amount); markDirty(); return r; }
    @Override public ItemStack removeStack(int slot) { return Inventories.removeStack(items, slot); }
    @Override public void setStack(int slot, ItemStack stack) { items.set(slot, stack); markDirty(); }
    @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
    @Override public void clear() { items.clear(); }

    // --- Persistence ---
    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup reg) {
        Inventories.writeNbt(nbt, items, reg);
        nbt.putInt("Progress", progress);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup reg) {
        items.clear();
        Inventories.readNbt(nbt, items, reg);
        progress = nbt.getInt("Progress");
    }
}

package net.daemon.orehives.block;

import com.mojang.serialization.MapCodec;
import net.daemon.orehives.OreHives;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Right-click with combs to load them, empty-handed to take results. Hopper-compatible, see {@link CentrifugeBlockEntity}. */
public class CentrifugeBlock extends BlockWithEntity {
    public static final MapCodec<CentrifugeBlock> CODEC = createCodec(CentrifugeBlock::new);
    public static final BooleanProperty RUNNING = BooleanProperty.of("running");

    public CentrifugeBlock(Settings s) { super(s); setDefaultState(getDefaultState().with(RUNNING, false)); }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(RUNNING); }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new CentrifugeBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, OreHives.CENTRIFUGE_BE, CentrifugeBlockEntity::serverTick);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!CentrifugeBlockEntity.isComb(stack)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (world.isClient) return ItemActionResult.SUCCESS;
        if (world.getBlockEntity(pos) instanceof CentrifugeBlockEntity be) {
            ItemStack in = be.getStack(CentrifugeBlockEntity.INPUT);
            if (in.isEmpty()) be.setStack(CentrifugeBlockEntity.INPUT, stack.copyAndEmpty());
            else if (ItemStack.areItemsAndComponentsEqual(in, stack)) {
                int move = Math.min(stack.getCount(), in.getMaxCount() - in.getCount());
                in.increment(move); stack.decrement(move); be.markDirty();
            }
        }
        return ItemActionResult.SUCCESS;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof CentrifugeBlockEntity be) be.giveOutputs(player);
        return ActionResult.SUCCESS;
    }

    /** Drop buffered items when broken. */
    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        ItemScatterer.onStateReplaced(state, newState, world, pos);
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override protected boolean hasComparatorOutput(BlockState state) { return true; }
    @Override protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return ScreenHandler.calculateComparatorOutput(world.getBlockEntity(pos));
    }
}

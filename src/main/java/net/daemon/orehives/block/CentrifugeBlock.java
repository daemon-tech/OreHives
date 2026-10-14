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
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Right-click with an ore comb to insert it; outputs pop out on top when done. Hopper automation: not yet. */
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
        if (!OreHives.COMBS.containsValue(stack.getItem())) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (world.isClient) return ItemActionResult.SUCCESS;
        if (world.getBlockEntity(pos) instanceof CentrifugeBlockEntity be && be.insert(stack.getItem())) {
            stack.decrementUnlessCreative(1, player);
            return ItemActionResult.SUCCESS;
        }
        return ItemActionResult.CONSUME;
    }
}

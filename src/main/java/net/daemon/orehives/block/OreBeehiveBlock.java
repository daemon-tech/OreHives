package net.daemon.orehives.block;

import com.mojang.serialization.MapCodec;
import net.daemon.orehives.OreHives;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class OreBeehiveBlock extends BlockWithEntity {
    public static final MapCodec<OreBeehiveBlock> CODEC = createCodec(OreBeehiveBlock::new);
    /** 0 = empty, 1 = has combs, 2 = full */
    public static final IntProperty FILL = IntProperty.of("fill", 0, 2);

    public OreBeehiveBlock(Settings s) {
        super(s);
        setDefaultState(getDefaultState().with(FILL, 0));
    }

    @Override protected MapCodec<? extends BlockWithEntity> getCodec() { return CODEC; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> b) { b.add(FILL); }
    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Nullable @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new OreBeehiveBlockEntity(pos, state); }

    @Nullable @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : validateTicker(type, OreHives.ORE_BEEHIVE_BE, OreBeehiveBlockEntity::serverTick);
    }

    @Override
    protected ItemActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!stack.isOf(Items.SHEARS)) return ItemActionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (world.isClient) return ItemActionResult.SUCCESS;
        if (world.getBlockEntity(pos) instanceof OreBeehiveBlockEntity be && be.harvest(world, pos)) {
            stack.damage(1, player, net.minecraft.entity.LivingEntity.getSlotForHand(hand));
            world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_SHEAR, SoundCategory.BLOCKS, 1f, 1f);
            return ItemActionResult.SUCCESS;
        }
        return ItemActionResult.CONSUME;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof OreBeehiveBlockEntity be) {
            player.sendMessage(Text.translatable("message.orehives.hive_status", be.totalCombs(), be.describe()), true);
        }
        return ActionResult.SUCCESS;
    }
}

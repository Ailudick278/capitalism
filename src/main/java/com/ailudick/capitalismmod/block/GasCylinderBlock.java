package com.ailudick.capitalismmod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

/** Two-block decorative compressed-gas cylinder; no inventory or gas economy yet. */
public final class GasCylinderBlock extends Block {
    public static final MapCodec<GasCylinderBlock> CODEC = simpleCodec(GasCylinderBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape BODY = Shapes.or(box(3,0,4,13,16,12), box(4,0,3,12,16,13));
    private static final VoxelShape TOP = Shapes.or(box(3,0,4,13,8,12), box(4,0,3,12,8,13),
            box(4,8,4,12,10,12), box(6,10,6,10,12,10), box(4,12,4,12,16,12));

    public GasCylinderBlock(Properties properties) {
        super(properties.noOcclusion().sound(SoundType.METAL).pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override public MapCodec<GasCylinderBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        builder.add(HALF, FACING, OPEN);
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? BODY : TOP;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight()-1 || !level.getBlockState(pos.above()).canBeReplaced(context)
                || !level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) return null;
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        return state.canSurvive(level, pos) ? state : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? below.isFaceSturdy(level,pos.below(),Direction.UP)
                : below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
    }
    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                               LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
        if (direction == (lower ? Direction.UP : Direction.DOWN)) {
            return neighbor.is(this) && neighbor.getValue(HALF) != state.getValue(HALF)
                    ? state.setValue(OPEN,neighbor.getValue(OPEN)).setValue(FACING,neighbor.getValue(FACING))
                    : Blocks.AIR.defaultBlockState();
        }
        if (lower && direction == Direction.DOWN && !state.canSurvive(level,pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            if (below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER) level.setBlock(pos.below(),Blocks.AIR.defaultBlockState(),35);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    private boolean hitsValve(BlockState state, BlockPos pos, BlockHitResult hit) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER && hit.getLocation().y-pos.getY() >= 0.65;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        if (!hitsValve(state,pos,hit)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        useWithoutItem(state,level,pos,player,hit);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!hitsValve(state,pos,hit)) return InteractionResult.PASS;
        if (!player.mayBuild()) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            boolean open = !state.getValue(OPEN);
            level.setBlock(pos,state.setValue(OPEN,open),3);
            level.playSound(null,pos,SoundEvents.LEVER_CLICK,SoundSource.BLOCKS,0.45f,open ? 0.7f : 0.5f);
            player.displayClientMessage(Component.translatable(open ? "message.capitalismmod.cylinder_open" : "message.capitalismmod.cylinder_closed"),true);
            if (open) level.scheduleTick(pos,this,1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) != DoubleBlockHalf.UPPER || !state.getValue(OPEN)) return;
        Direction facing = state.getValue(FACING);
        double x=pos.getX()+0.5+facing.getStepX()*0.25;
        double y=pos.getY()+0.805;
        double z=pos.getZ()+0.5+facing.getStepZ()*0.25;
        for(int i=0;i<3;i++) level.sendParticles(ParticleTypes.CLOUD,x,y,z,0,
                facing.getStepX()+random.nextGaussian()*0.08,random.nextGaussian()*0.04,
                facing.getStepZ()+random.nextGaussian()*0.08,0.17+random.nextDouble()*0.08);
        if (level.getGameTime()%21 == 0) level.playSound(null,pos,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,0.12f,1.8f);
        level.scheduleTick(pos,this,3);
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING,rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
}

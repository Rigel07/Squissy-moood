package com.squissy.block;

import com.squissy.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rastro de baba: una capa finita y brillante que dejan Squissy y las botas babosas.
 * No bloquea el paso y se seca sola pasados unos segundos. El color (0-4) coincide con el de Squissy.
 */
public class SlimeTrailBlock extends Block {
    public static final IntegerProperty COLOR = IntegerProperty.create("color", 0, 4);
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public SlimeTrailBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportRigidBlock(level, pos.below());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 500 + level.random.nextInt(500));
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.sendParticles(ParticleTypes.ITEM_SLIME, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D,
                3, 0.3D, 0.05D, 0.3D, 0.0D);
        level.removeBlock(pos, false);
    }

    /** Deja una mancha de baba en {@code pos} si ahí cabe (aire o plantas bajas) y hay suelo firme debajo. */
    public static boolean tryPlace(Level level, BlockPos pos, int color) {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() instanceof SlimeTrailBlock) {
            return false;
        }
        if (!current.isAir() && !(current.canBeReplaced() && current.getFluidState().isEmpty())) {
            return false;
        }
        if (!Block.canSupportRigidBlock(level, pos.below())) {
            return false;
        }
        BlockState trail = ModRegistry.SLIME_TRAIL.get().defaultBlockState().setValue(COLOR, Math.floorMod(color, 5));
        return level.setBlock(pos, trail, 3);
    }
}

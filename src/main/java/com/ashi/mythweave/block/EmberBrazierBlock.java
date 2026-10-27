package com.ashi.mythweave.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jspecify.annotations.Nullable;

/**
 * Ember Brazier: an iron bowl of coals that only a bonded Cindercub can light (see
 * {@link com.ashi.mythweave.ability.EmberSparkAbility}). Players can put it out with an empty hand,
 * like a candle.
 *
 * <p>Safe by design: the flame is part of the block's model, not a fire block, so a lit
 * brazier can't spread fire, burn neighbours, or hurt entities (unlike a campfire).
 * It just glows (light level 15) and gives off ember particles.
 */
public class EmberBrazierBlock extends Block {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	// Matches the block model: base plate, post, bowl (in pixels, 16 = one block).
	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(4.0, 0.0, 4.0, 12.0, 2.0, 12.0),
			Block.box(6.0, 2.0, 6.0, 10.0, 6.0, 10.0),
			Block.box(2.0, 6.0, 2.0, 14.0, 10.0, 14.0));

	public EmberBrazierBlock(BlockBehaviour.Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	/** Empty hand on a lit brazier puts it out (same rule as vanilla candles). */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.isEmpty() && player.getAbilities().mayBuild && state.getValue(LIT)) {
			if (!level.isClientSide()) {
				setLit(level, pos, state, false, player);
			}
			return InteractionResult.SUCCESS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	/**
	 * Server-side. Lights or puts out the brazier at {@code pos} and plays the matching sound.
	 * Flag 11 = update neighbours + send to clients + re-render, as vanilla candles use.
	 */
	public static void setLit(Level level, BlockPos pos, BlockState state, boolean lit, @Nullable Entity cause) {
		level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL_IMMEDIATE);
		level.playSound(null, pos, lit ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH,
				SoundSource.BLOCKS, 0.8F, 1.0F);
		level.gameEvent(cause, GameEvent.BLOCK_CHANGE, pos);
	}

	/** Client-side visuals only: rising flames, the odd ember pop, and a quiet crackle. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(LIT)) {
			return;
		}
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.7;
		double z = pos.getZ() + 0.5;
		if (random.nextInt(10) == 0) {
			level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
					0.5F + random.nextFloat() * 0.5F, random.nextFloat() * 0.7F + 0.6F, false);
		}
		if (random.nextInt(4) == 0) {
			level.addParticle(ParticleTypes.LAVA, x, y, z, 0.0, 0.0, 0.0);
		}
		level.addParticle(ParticleTypes.FLAME,
				x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.02, 0.0);
		if (random.nextInt(3) == 0) {
			level.addParticle(ParticleTypes.SMOKE, x, y + 0.4, z, 0.0, 0.03, 0.0);
		}
	}
}

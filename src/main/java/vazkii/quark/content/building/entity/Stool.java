package vazkii.quark.content.building.entity;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import vazkii.quark.content.building.block.StoolBlock;

public class Stool extends Entity {

	public Stool(EntityType<?> entityTypeIn, Level worldIn) {
		super(entityTypeIn, worldIn);
	}

	@Override
	public void tick() {
		super.tick();

		List<Entity> passengers = getPassengers();
		boolean dead = passengers.isEmpty();

		BlockPos pos = blockPosition();
		BlockState state = level.getBlockState(pos);

		if(!dead) {
			if(!(state.getBlock() instanceof StoolBlock)) {
				PistonMovingBlockEntity piston = null;
				boolean didOffset = false;

				BlockEntity tile = level.getBlockEntity(pos);
				if(tile instanceof PistonMovingBlockEntity && ((PistonMovingBlockEntity) tile).getMovedState().getBlock() instanceof StoolBlock)
					piston = (PistonMovingBlockEntity) tile;
				else for(Direction d : Direction.values()) {
					BlockPos offPos = pos.relative(d);
					tile = level.getBlockEntity(offPos);

					if(tile instanceof PistonMovingBlockEntity && ((PistonMovingBlockEntity) tile).getMovedState().getBlock() instanceof StoolBlock) {
						piston = (PistonMovingBlockEntity) tile;
						break;
					}
				}

				if(piston != null) {
					Direction dir = piston.getMovementDirection();
					move(MoverType.PISTON, new Vec3((float) dir.getStepX() * 0.33, (float) dir.getStepY() * 0.33, (float) dir.getStepZ() * 0.33));

					didOffset = true;
				}

				dead = !didOffset;
			}
		}

		if(dead && !level.isClientSide) {
			removeAfterChangingDimensions();

			if(state.getBlock() instanceof StoolBlock)
				level.setBlockAndUpdate(pos, state.setValue(StoolBlock.SAT_IN, false));
		}
	}

	@Override
	public double getPassengersRidingOffset() {
		return -0.3;
	}

	@Override
	protected void defineSynchedData() {
		// NO-OP
	}

	@Override
	protected void readAdditionalSaveData(@Nonnull CompoundTag compound) {
		// NO-OP
	}

	@Override
	protected void addAdditionalSaveData(@Nonnull CompoundTag compound) {
		// NO-OP
	}

	@Nonnull
	@Override
	public Packet<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

}

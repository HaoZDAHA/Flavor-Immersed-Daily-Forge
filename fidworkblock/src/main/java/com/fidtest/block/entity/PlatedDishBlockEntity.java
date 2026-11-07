package com.fidtest.block.entity;

import com.fidtest.registration.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PlatedDishBlockEntity extends BlockEntity {
    private ItemStack dish = ItemStack.EMPTY;

    public PlatedDishBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLATED_DISH.get(), pos, state);
    }

    public ItemStack getDish() { return dish; }

    public void setDish(ItemStack stack) {
        dish = stack.copyWithCount(1);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Dish", dish.save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        dish = ItemStack.of(tag.getCompound("Dish"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}

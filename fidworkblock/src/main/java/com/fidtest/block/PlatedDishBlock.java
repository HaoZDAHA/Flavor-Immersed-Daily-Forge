package com.fidtest.block;

import com.fidtest.block.entity.PlatedDishBlockEntity;
import com.fidtest.item.ChopsticksItem;
import com.fidtest.item.DishFragments;
import com.fidtest.network.ChopsticksEatPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;

public class PlatedDishBlock extends BaseEntityBlock {
    private static final VoxelShape SHAPE = box(2, 0, 2, 14, 3, 14);

    public PlatedDishBlock(Properties properties) { super(properties); }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlatedDishBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!TablewareBlock.canInteract(level, pos, player)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof PlatedDishBlockEntity plate)) return InteractionResult.PASS;
        ItemStack dish = plate.getDish();
        var food = dish.getItem().getFoodProperties(dish, player);
        if (dish.isEmpty() || food == null || !player.canEat(food.canAlwaysEat())) return InteractionResult.CONSUME;
        if (!level.isClientSide) {
            boolean notifyChopsticks = hand == InteractionHand.MAIN_HAND
                    && player.getMainHandItem().getItem() instanceof ChopsticksItem;
            CompoundTag bite = notifyChopsticks ? DishFragments.snapshotBite(dish) : null;
            ItemStack before = dish.copy();
            ItemStack result = dish.finishUsingItem(level, player);
            result = ForgeEventFactory.onItemUseFinish(player, before, 0, result);
            if (notifyChopsticks) {
                ChopsticksEatPayload.send((ServerPlayer) player,
                        new ChopsticksEatPayload(player.getInventory().selected, bite, ItemStack.EMPTY));
            }
            if (result.isEmpty()) level.removeBlock(pos, false);
            else plate.setDish(result);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PlatedDishBlockEntity plate) {
            ItemStack dish = plate.getDish();
            if (!dish.isEmpty() && DishFragments.data(dish).getInt("Bites") == 0) return List.of(dish.copy());
        }
        return List.of();
    }
}

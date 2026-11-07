package com.fidtest.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DishItem extends Item {

    public DishItem(Properties p) { super(p.stacksTo(1).food(new FoodProperties.Builder().nutrition(4).saturationMod(0.2F).build())); }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            private final com.fidtest.client.DishItemRenderer renderer = new com.fidtest.client.DishItemRenderer();
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    @Override public UseAnim getUseAnimation(ItemStack s) { return UseAnim.EAT; }
    @Override public int getUseDuration(ItemStack s) { return 32; }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && eatBite(stack, level, entity)) stack.shrink(1);
        return stack;
    }

    @Override
    public FoodProperties getFoodProperties(ItemStack stack, LivingEntity entity) {
        if (stack.hasTag() && !stack.getTag().contains("seasoning") && stack.getTag().contains("seasoningeffect")) {
            setSeasoningEffect(stack, stack.getTag().getString("seasoningeffect"));
        }
        int bites = Math.max(0, Math.min(3, DishFragments.data(stack).getInt("Bites")));
        int total = Math.max(0, getHungerValue(stack));
        int nutrition = bites >= 3 ? 0 : total / 3 + (bites < total % 3 ? 1 : 0);
        return new FoodProperties.Builder().nutrition(nutrition).saturationMod(0.2F).build();
    }

    public static boolean eatBite(ItemStack stack, Level level, LivingEntity entity) {
        if (level.isClientSide) return false;
        if (entity instanceof Player player) {
            FoodProperties food = stack.getItem().getFoodProperties(stack, player);
            player.getFoodData().eat(food.getNutrition(), food.getSaturationModifier());
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(stack.getItem()));
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
        }
        entity.gameEvent(net.minecraft.world.level.gameevent.GameEvent.EAT);
        return DishFragments.takeBite(stack);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
// 命名页面由客户端输入事件打开，这里直接返回成功，避免同时开始食用
            return InteractionResultHolder.success(held);
        }
        return super.use(level, player, hand);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return net.minecraft.world.InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            use(context.getLevel(), player, context.getHand());
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        var placement = new net.minecraft.world.item.context.BlockPlaceContext(context);
        Level level = context.getLevel();
        var pos = placement.getClickedPos();
        var state = com.fidtest.registration.ModBlocks.PLATED_DISH.get().defaultBlockState();
        if (!placement.canPlace() || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, placement.getClickedFace(), context.getItemInHand())
                || !level.getWorldBorder().isWithinBounds(pos) || level.isOutsideBuildHeight(pos)
                || !state.canSurvive(level, pos)
                || !level.isUnobstructed(state, pos, net.minecraft.world.phys.shapes.CollisionContext.of(player))) {
            return net.minecraft.world.InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            if (!level.setBlock(pos, state, 3)) return net.minecraft.world.InteractionResult.FAIL;
            if (level.getBlockEntity(pos) instanceof com.fidtest.block.entity.PlatedDishBlockEntity plate) {
                plate.setDish(context.getItemInHand());
                if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
            }
            level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE, pos);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tips, TooltipFlag flag) {
        int hunger = getHungerValue(stack);
        tips.add(Component.translatable("tooltip.fidworkblock.dish_hunger", hunger));
        int sc = getStirCount(stack);
        tips.add(Component.translatable("tooltip.fidworkblock.dish_cooking_level", Component.translatable(getCookingLevelKey(sc))));
        String seasoning = getSeasoningEffect(stack);
        if (!seasoning.isEmpty()) try {
            tips.add(Component.translatable("tooltip.fidworkblock.dish_seasoning", new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(seasoning))).getHoverName()));
        } catch (Exception e) { tips.add(Component.literal(seasoning)); }
        String desc = getDishDescription(stack);
        if (!desc.isEmpty()) tips.add(Component.literal(desc).withStyle(ChatFormatting.GRAY));
        String chef = getChefName(stack);
        if (!chef.isEmpty()) tips.add(Component.translatable("tooltip.fidworkblock.dish_chef", chef));
        tips.add(Component.translatable("tooltip.fidworkblock.dish_placeable").withStyle(ChatFormatting.GRAY));
        tips.add(Component.translatable("tooltip.fidworkblock.dish_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static String getCookingLevelKey(int sc) {
        if (sc <= 2) return "cooking_level.fidworkblock.raw"; if (sc <= 4) return "cooking_level.fidworkblock.undercooked";
        if (sc <= 9) return "cooking_level.fidworkblock.perfect"; if (sc <= 12) return "cooking_level.fidworkblock.overcooked";
        return "cooking_level.fidworkblock.burnt";
    }

    private static CompoundTag getWokData(ItemStack dish) { CompoundTag t = dish.getTag(); return t != null && t.contains("WokDish") ? t.getCompound("WokDish") : null; }
    private static CompoundTag getOrCreateWokData(ItemStack dish) { CompoundTag t = dish.getOrCreateTag(); if (!t.contains("WokDish")) t.put("WokDish", new CompoundTag()); return t.getCompound("WokDish"); }

    public static ItemStack createProceduralDish(List<ItemStack> inputs) {
        ItemStack dish = new ItemStack(com.fidtest.registration.ModItems.DISH.get());
        CompoundTag wok = new CompoundTag();
        ListTag colorList = new ListTag(), nameList = new ListTag();
        int total = 0;
        for (ItemStack in : inputs) {
            String id = in.getItem().builtInRegistryHolder().key().location().toString();
            nameList.add(StringTag.valueOf(id));
            colorList.add(IntTag.valueOf(hashToColor(id)));
            FoodProperties fp = in.getItem().getFoodProperties(in, null);
            if (fp != null) total += fp.getNutrition();
        }
        wok.put("Colors", colorList); wok.put("Items", nameList); wok.putInt("Hunger", Math.max(1, total / 2));
        dish.getOrCreateTag().put("WokDish", wok);
        dish.setHoverName(getDefaultDishName(dish));
        return dish;
    }

    public static Component getDefaultDishName(ItemStack dish) {
        List<String> itemNames = getItemNames(dish);
        if (itemNames.isEmpty()) return Component.translatable("item.fidworkblock.dish");
        MutableComponent result = Component.empty();
        for (int i = 0; i < itemNames.size(); i++) {
            if (i > 0) result = result.copy().append("炒");
            try {
                var item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemNames.get(i)));
                if (item != null) result = result.copy().append(new ItemStack(item).getHoverName());
                else result = result.copy().append(itemNames.get(i));
            } catch (Exception e) { result = result.copy().append(itemNames.get(i)); }
        }
        return result;
    }

    public static void setDishName(ItemStack dish, String name, int color) {
        getOrCreateWokData(dish).putString("Name", name);
        getOrCreateWokData(dish).putInt("NameColor", color);
        dish.setHoverName(Component.literal(name).withStyle(s -> s.withColor(color)));
    }

    public static void setDishDescription(ItemStack dish, String desc) { getOrCreateWokData(dish).putString("Description", desc); }
    public static void setChefName(ItemStack dish, String chef) { getOrCreateWokData(dish).putString("Chef", chef); }

    public static String getDishDescription(ItemStack dish) { CompoundTag w = getWokData(dish); return w != null && w.contains("Description") ? w.getString("Description") : ""; }
    public static String getChefName(ItemStack dish) { CompoundTag w = getWokData(dish); return w != null && w.contains("Chef") ? w.getString("Chef") : ""; }

    public static void setFoodProperties(ItemStack dish, List<ItemStack> inputs, int sc) {
        int h = 0;
        for (ItemStack in : inputs) { FoodProperties fp = in.getItem().getFoodProperties(in, null); if (fp != null) h += fp.getNutrition(); }
        getOrCreateWokData(dish).putInt("Hunger", Math.max(1, (int)(h * getHungerMultiplier(sc))));
    }

    public static float getHungerMultiplier(int sc) { if (sc <= 2) return 0.25F; if (sc <= 4) return 0.5F; if (sc <= 9) return 1.0F; if (sc <= 12) return 0.5F; return 0.25F; }

    public static void setStirCount(ItemStack dish, int sc) { getOrCreateWokData(dish).putInt("StirCount", sc); }
    public static int getStirCount(ItemStack dish) { CompoundTag w = getWokData(dish); return w != null && w.contains("StirCount") ? w.getInt("StirCount") : 0; }
    public static void setSeasoningEffect(ItemStack dish, String s) {
        dish.getOrCreateTag().putString("seasoning", s);
        dish.getOrCreateTag().remove("seasoningeffect");
    }
    public static String getSeasoningEffect(ItemStack dish) {
        CompoundTag t = dish.getTag();
        if (t == null) return "";
        return t.contains("seasoning") ? t.getString("seasoning") : t.getString("seasoningeffect");
    }
    public static int getHungerValue(ItemStack dish) { CompoundTag w = getWokData(dish); return w != null && w.contains("Hunger") ? w.getInt("Hunger") : 4; }

    public static List<String> getItemNames(ItemStack dish) {
        List<String> names = new ArrayList<>();
        CompoundTag w = getWokData(dish);
        if (w != null && w.contains("Items")) { ListTag l = w.getList("Items", 8); for (int i = 0; i < l.size(); i++) names.add(l.getString(i)); }
        return names;
    }

    private static int hashToColor(String name) { int h = name.hashCode(); int r = Math.min(((h>>16)&0xFF)|0x40, 220); int g = Math.min(((h>>8)&0xFF)|0x40, 220); int b = Math.min((h&0xFF)|0x40, 220); return (r<<16)|(g<<8)|b; }
}

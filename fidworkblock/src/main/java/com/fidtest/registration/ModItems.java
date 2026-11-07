package com.fidtest.registration;

import com.fidtest.FidworkMod;
import com.fidtest.item.DishItem;
import com.fidtest.item.TablewareItem;
import com.fidtest.item.ChopsticksItem;
import com.fidtest.block.entity.TablewareBlockEntity.Kind;
import com.fidtest.item.FryingpanItem;
import com.fidtest.item.JarItem;
import com.fidtest.item.MeatmincerItem;
import com.fidtest.item.StartBookItem;
import com.fidtest.item.SteamboxItem;
import com.fidtest.item.TeapotItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FidworkMod.MODID);

    public static final RegistryObject<DishItem> DISH =
            ITEMS.register("dish", () -> new DishItem(new Item.Properties()));

    public static final RegistryObject<TablewareItem> PLATE =
            ITEMS.register("plate", () -> new TablewareItem(new Item.Properties(), Kind.PLATE));
    public static final RegistryObject<TablewareItem> LONG_PLATE =
            ITEMS.register("long_plate", () -> new TablewareItem(new Item.Properties(), Kind.LONG_PLATE));
    public static final RegistryObject<TablewareItem> SMALL_PLATE =
            ITEMS.register("small_plate", () -> new TablewareItem(new Item.Properties(), Kind.SAUCER));
    public static final RegistryObject<ChopsticksItem> CHOPSTICKS =
            ITEMS.register("chopsticks", () -> new ChopsticksItem(new Item.Properties().stacksTo(1)));

// 烟火-教程卷轴（帕秋莉手册）
    public static final RegistryObject<StartBookItem> STARTBOOK =
            ITEMS.register("startbook", StartBookItem::new);

    static {
        for (var entry : ModBlocks.BLOCKS.getEntries()) {
            String name = entry.getId().getPath();
            switch (name) {
                case "tableware":
                case "plated_dish":
                    break;
                case "steambox":
                    ITEMS.register(name, () -> new SteamboxItem(entry.get(), new Item.Properties()));
                    break;
                case "teapot":
                    ITEMS.register(name, () -> new TeapotItem(entry.get(), new Item.Properties()));
                    break;
                case "meatmincer":
                    ITEMS.register(name, () -> new MeatmincerItem(entry.get(), new Item.Properties()));
                    break;
                case "jar":
                    ITEMS.register(name, () -> new JarItem(entry.get(), new Item.Properties()));
                    break;
                case "fryingpan":
                    ITEMS.register(name, () -> new FryingpanItem(entry.get(), new Item.Properties()));
                    break;
                default:
                    ITEMS.register(name, () -> new BlockItem(entry.get(), new Item.Properties()));
                    break;
            }
        }
    }
}

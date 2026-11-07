package com.fidtest;

import com.fidtest.client.ChoppingboardRenderer;
import com.fidtest.client.FryingpanRenderer;
import com.fidtest.client.JarRenderer;
import com.fidtest.client.MeatmincerRenderer;
import com.fidtest.client.SqueezingmachineRenderer;
import com.fidtest.client.SteamboxRenderer;
import com.fidtest.client.TeapotRenderer;
import com.fidtest.client.WokRenderer;
import com.fidtest.client.model.DishModelLoader;
import com.fidtest.registration.ModBlockEntities;
import com.fidtest.registration.ModBlocks;
import com.fidtest.registration.ModMenuTypes;
import com.fidtest.screen.CookingScreen;
import com.fidtest.screen.DishNamingScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = FidworkMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class FidworkModClient {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.COOKING.get(), CookingScreen::new);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MEATMINCER.get(), RenderType.cutout());
            MinecraftForge.EVENT_BUS.addListener(FidworkModClient::onInteractionKeyMappingTriggered);
            MinecraftForge.EVENT_BUS.addListener(com.fidtest.client.ChopsticksHandRenderer::onClientTick);
            MinecraftForge.EVENT_BUS.addListener(com.fidtest.client.ChopsticksHandRenderer::onRenderHand);
        });
    }

// 潜行右键菜品时在客户端直接拦截「使用物品」输入，避免同时发送使用包导致服务端开始食用
    private static void onInteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || player.isSpectator() || !player.isShiftKeyDown()) return;
        var held = player.getItemInHand(event.getHand());
        if (!(held.getItem() instanceof com.fidtest.item.DishItem)) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        openDishNamingScreen(event.getHand());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CHOPPINGBOARD.get(), ChoppingboardRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FRYINGPAN.get(), FryingpanRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.JAR.get(), JarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SQUEEZINGMACHINE.get(), SqueezingmachineRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.STEAMBOX.get(), SteamboxRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TEAPOT.get(), TeapotRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.MEATMINCER.get(), MeatmincerRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WOK.get(), WokRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.TABLEWARE.get(), com.fidtest.client.TablewareRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PLATED_DISH.get(), com.fidtest.client.PlatedDishRenderer::new);
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register("dish", DishModelLoader.INSTANCE);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(com.fidtest.client.TablewareRenderer.PLATE);
        event.register(com.fidtest.client.TablewareRenderer.LONG_PLATE);
        event.register(com.fidtest.client.TablewareRenderer.SAUCER);
        event.register(com.fidtest.client.TablewareRenderer.SAUCER_DRY_SEASONING);
        event.register(com.fidtest.client.TablewareRenderer.SAUCER_WET_SEASONING);
        event.register(com.fidtest.client.model.DishModelGeometry.PLATE);
        event.register(com.fidtest.client.ChopsticksHandRenderer.CHOPSTICK_A);
        event.register(com.fidtest.client.ChopsticksHandRenderer.CHOPSTICK_B);
    }

    @SubscribeEvent
    public static void registerReloadListeners(net.minecraftforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                manager -> com.fidtest.client.DishRenderCache.clear());
    }

    public static void openDishNamingScreen(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new DishNamingScreen(hand));
    }
}

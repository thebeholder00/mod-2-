package com.skinshift;

import com.skinshift.client.ClientInit;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(SkinShiftMod.MODID)
public class SkinShiftMod {
    public static final String MODID = "skinshift";

    public SkinShiftMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModParticles.PARTICLES.register(modBus);
        modBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.addListener(ServerLogic::onStartTracking);
        MinecraftForge.EVENT_BUS.addListener(ServerLogic::onLogout);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init(modBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Net::init);
    }
}

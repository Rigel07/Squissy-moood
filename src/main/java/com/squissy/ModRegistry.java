package com.squissy;

import com.squissy.block.SlimeTrailBlock;
import com.squissy.entity.SquissyEntity;
import com.squissy.item.SlimeBootsItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRegistry {
    private ModRegistry() {}

    private static final String ID = SquissyMod.MOD_ID;

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    // ---- Bloque: el rastro de baba (una capa fina, sin colisión, que se seca sola) ----
    public static final RegistryObject<Block> SLIME_TRAIL = BLOCKS.register("slime_trail",
            () -> new SlimeTrailBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .noCollission()
                    .noOcclusion()
                    .instabreak()
                    .replaceable()
                    .pushReaction(PushReaction.DESTROY)
                    .sound(SoundType.SLIME_BLOCK)
                    .lightLevel(state -> 6)));

    // ---- Entidad ----
    public static final RegistryObject<EntityType<SquissyEntity>> SQUISSY = ENTITIES.register("squissy",
            () -> EntityType.Builder.<SquissyEntity>of(SquissyEntity::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(10)
                    .build(ID + ":squissy"));

    // ---- Objetos ----
    public static final RegistryObject<Item> MAGIC_SLIME = ITEMS.register("magic_slime",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SLIME_BOOTS = ITEMS.register("slime_boots",
            () -> new SlimeBootsItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SQUISSY_EGG = ITEMS.register("squissy_spawn_egg",
            () -> new ForgeSpawnEggItem(SQUISSY, 0x6BEA8E, 0xFF8AD0, new Item.Properties()));

    // ---- Pestaña creativa ----
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("squissy_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.squissy"))
                    .icon(() -> new ItemStack(MAGIC_SLIME.get()))
                    .displayItems((params, output) -> {
                        output.accept(SQUISSY_EGG.get());
                        output.accept(MAGIC_SLIME.get());
                        output.accept(SLIME_BOOTS.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        TABS.register(bus);
    }
}

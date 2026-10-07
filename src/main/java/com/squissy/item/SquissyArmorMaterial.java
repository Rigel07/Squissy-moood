package com.squissy.item;

import com.squissy.ModRegistry;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** Material de las botas babosas (textura: textures/models/armor/slime_layer_1.png). */
public enum SquissyArmorMaterial implements ArmorMaterial {
    SLIME;

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return 300;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return 2;
    }

    @Override
    public int getEnchantmentValue() {
        return 18;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.SLIME_BLOCK_PLACE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModRegistry.MAGIC_SLIME.get());
    }

    @Override
    public String getName() {
        return "squissy:slime";
    }

    @Override
    public float getToughness() {
        return 0.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.0F;
    }
}

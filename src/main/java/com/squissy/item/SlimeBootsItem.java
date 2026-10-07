package com.squissy.item;

import com.squissy.block.SlimeTrailBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Botas babosas: dejan un rastro de baba de colores al andar y, sobre la baba, te hacen resbalar
 * ganando velocidad (igual que Squissy).
 */
public class SlimeBootsItem extends ArmorItem {
    public SlimeBootsItem(Properties properties) {
        super(SquissyArmorMaterial.SLIME, ArmorItem.Type.BOOTS, properties);
    }

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (!player.onGround() || player.isInWater() || player.isSpectator()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        double horizontal = motion.horizontalDistanceSqr();
        boolean moving = horizontal > 0.0009D;
        if (!moving) {
            return;
        }
        BlockPos pos = player.blockPosition();
        if (!level.isClientSide && player.tickCount % 3 == 0) {
            SlimeTrailBlock.tryPlace(level, pos, level.random.nextInt(5));
        }
        boolean onTrail = level.getBlockState(pos).getBlock() instanceof SlimeTrailBlock;
        if (onTrail) {
            // Resbalón: un empujón extra mientras no vayas ya demasiado rápido.
            if (horizontal < 0.2D) {
                player.setDeltaMovement(motion.x * 1.08D, motion.y, motion.z * 1.08D);
            }
            if (!level.isClientSide) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12, 1, true, false, false));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.squissy.slime_boots").withStyle(ChatFormatting.GRAY));
    }
}

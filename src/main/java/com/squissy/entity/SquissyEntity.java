package com.squissy.entity;

import com.squissy.ModRegistry;
import com.squissy.block.SlimeTrailBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Squissy: una babosa redondita y achuchable que brilla en cinco colores.
 * Deja un rastro de baba sobre el que se desliza (más rápida), trepa paredes con su baba,
 * suelta babas mágicas de vez en cuando y, una vez domesticada, absorbe objetos en su barriguita.
 */
public class SquissyEntity extends TamableAnimal {
    public static final int MODE_FOLLOW = 0;
    public static final int MODE_STAY = 1;
    public static final int MODE_WANDER = 2;
    public static final int VARIANTS = 5;
    private static final int WANDER_RADIUS = 8;

    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(SquissyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MODE =
            SynchedEntityData.defineId(SquissyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING =
            SynchedEntityData.defineId(SquissyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> DATA_SHOWN =
            SynchedEntityData.defineId(SquissyEntity.class, EntityDataSerializers.ITEM_STACK);

    private final SimpleContainer belly = new SimpleContainer(9);
    private int dropTimer;
    @Nullable
    private BlockPos home;

    public SquissyEntity(EntityType<? extends SquissyEntity> type, Level level) {
        super(type, level);
        this.entityData.set(DATA_VARIANT, this.random.nextInt(VARIANTS));
        this.dropTimer = 6000 + this.random.nextInt(6000);
        this.belly.addListener(container -> refreshShownItem());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D);
    }

    // ------------------------------------------------------------------ IA

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.5D) {
            @Override
            public boolean canUse() {
                return !SquissyEntity.this.isTame() && super.canUse();
            }
        });
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.2D, 5.0F, 2.0F, false) {
            @Override
            public boolean canUse() {
                return SquissyEntity.this.getMode() == MODE_FOLLOW && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return SquissyEntity.this.getMode() == MODE_FOLLOW && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1D, Ingredient.of(Items.SWEET_BERRIES, Items.GLOW_BERRIES,
                Items.APPLE, Items.CARROT, Items.BREAD, Items.COOKIE, Items.MELON_SLICE, Items.GOLDEN_CARROT), false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Navegación de araña: puede subir por las paredes. */
    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ------------------------------------------------------------- Datos

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_VARIANT, 0);
        this.entityData.define(DATA_MODE, MODE_FOLLOW);
        this.entityData.define(DATA_CLIMBING, false);
        this.entityData.define(DATA_SHOWN, ItemStack.EMPTY);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, Math.floorMod(variant, VARIANTS));
    }

    public int getMode() {
        return this.entityData.get(DATA_MODE);
    }

    public boolean isClimbing() {
        return this.entityData.get(DATA_CLIMBING);
    }

    /** Objeto que lleva dentro (para dibujarlo flotando sobre ella). */
    public ItemStack getShownItem() {
        return this.entityData.get(DATA_SHOWN);
    }

    private void refreshShownItem() {
        ItemStack first = ItemStack.EMPTY;
        for (int i = 0; i < belly.getContainerSize(); i++) {
            ItemStack stack = belly.getItem(i);
            if (!stack.isEmpty()) {
                first = stack.copyWithCount(1);
                break;
            }
        }
        this.entityData.set(DATA_SHOWN, first);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("Mode", getMode());
        tag.putInt("DropTimer", dropTimer);
        ListTag list = new ListTag();
        for (int i = 0; i < belly.getContainerSize(); i++) {
            ItemStack stack = belly.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag slot = new CompoundTag();
                slot.putByte("Slot", (byte) i);
                stack.save(slot);
                list.add(slot);
            }
        }
        tag.put("Belly", list);
        if (home != null) {
            tag.putInt("HomeX", home.getX());
            tag.putInt("HomeY", home.getY());
            tag.putInt("HomeZ", home.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Variant")) {
            setVariant(tag.getInt("Variant"));
        }
        if (tag.contains("DropTimer")) {
            dropTimer = tag.getInt("DropTimer");
        }
        this.entityData.set(DATA_MODE, tag.getInt("Mode"));
        belly.clearContent();
        ListTag list = tag.getList("Belly", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag slot = list.getCompound(i);
            int index = slot.getByte("Slot") & 255;
            if (index < belly.getContainerSize()) {
                belly.setItem(index, ItemStack.of(slot));
            }
        }
        refreshShownItem();
        if (tag.contains("HomeX")) {
            home = new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ"));
            if (getMode() == MODE_WANDER) {
                restrictTo(home, WANDER_RADIUS);
            }
        }
    }

    // ------------------------------------------------------ Comida / tinte

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.isEdible();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return null;
    }

    private static int dyeVariant(ItemStack stack) {
        if (stack.is(Items.RED_DYE)) {
            return 0;
        }
        if (stack.is(Items.GREEN_DYE) || stack.is(Items.LIME_DYE)) {
            return 1;
        }
        if (stack.is(Items.YELLOW_DYE)) {
            return 2;
        }
        if (stack.is(Items.PINK_DYE)) {
            return 3;
        }
        if (stack.is(Items.BLUE_DYE) || stack.is(Items.LIGHT_BLUE_DYE)) {
            return 4;
        }
        return -1;
    }

    // ------------------------------------------------------ Interacción

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Salvaje: cualquier comida puede domesticarla.
        if (!isTame()) {
            if (stack.isEdible()) {
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    if (this.random.nextInt(3) == 0) {
                        tame(player);
                        this.navigation.stop();
                        setTarget(null);
                        applyMode(MODE_FOLLOW);
                        getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0D);
                        setHealth(20.0F);
                        level().broadcastEntityEvent(this, (byte) 7);
                        playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.6F);
                    } else {
                        level().broadcastEntityEvent(this, (byte) 6);
                        playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.0F, 1.2F);
                    }
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return InteractionResult.PASS;
        }

        if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }

        // Tinte: cambia de color.
        int dye = dyeVariant(stack);
        if (dye >= 0) {
            if (!level().isClientSide && dye != getVariant()) {
                setVariant(dye);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.8F);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY() + 0.7D, getZ(), 8, 0.4D, 0.4D, 0.4D, 0.0D);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Comida con la vida baja: se cura.
        if (stack.isEdible() && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) {
                FoodProperties food = stack.getItem().getFoodProperties();
                float amount = food != null ? Math.max(2.0F, food.getNutrition()) : 2.0F;
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                heal(amount);
                level().broadcastEntityEvent(this, (byte) 7);
                playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.5F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Agachado + mano vacía: abre su barriguita.
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            if (!level().isClientSide) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, belly, 1),
                        Component.translatable("container.squissy.belly")));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Mano vacía: cambia el modo (sígueme -> quieta -> deambula).
        if (stack.isEmpty()) {
            if (!level().isClientSide) {
                int next = (getMode() + 1) % 3;
                applyMode(next);
                String key = next == MODE_FOLLOW ? "message.squissy.follow"
                        : next == MODE_STAY ? "message.squissy.stay" : "message.squissy.wander";
                player.displayClientMessage(Component.translatable(key), true);
                playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.0F, 1.4F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        // Cualquier otro objeto: lo absorbe.
        if (!level().isClientSide) {
            ItemStack remaining = belly.addItem(stack.copy());
            int absorbed = stack.getCount() - remaining.getCount();
            if (absorbed > 0) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(absorbed);
                }
                playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.2F);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.5D, getZ(), 8, 0.3D, 0.3D, 0.3D, 0.0D);
                }
            } else {
                player.displayClientMessage(Component.translatable("message.squissy.belly_full"), true);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void applyMode(int mode) {
        this.entityData.set(DATA_MODE, mode);
        this.jumping = false;
        switch (mode) {
            case MODE_STAY -> {
                setOrderedToSit(true);
                clearRestriction();
                this.navigation.stop();
            }
            case MODE_WANDER -> {
                setOrderedToSit(false);
                home = blockPosition();
                restrictTo(home, WANDER_RADIUS);
            }
            default -> {
                setOrderedToSit(false);
                clearRestriction();
            }
        }
    }

    // ------------------------------------------------------------- Tick

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            if (this.random.nextInt(12) == 0) {
                level().addParticle(ParticleTypes.ITEM_SLIME, getRandomX(0.5D), getY() + 0.1D, getRandomZ(0.5D),
                        0.0D, 0.0D, 0.0D);
            }
            if (isClimbing() && this.random.nextInt(4) == 0) {
                level().addParticle(ParticleTypes.ITEM_SLIME, getRandomX(0.6D), getRandomY(), getRandomZ(0.6D),
                        0.0D, 0.0D, 0.0D);
            }
            return;
        }

        // Pegada a la pared: sube con su baba.
        this.entityData.set(DATA_CLIMBING, this.horizontalCollision);

        // Rastro de baba y resbalón.
        Vec3 motion = getDeltaMovement();
        double horizontal = motion.horizontalDistanceSqr();
        boolean moving = horizontal > 0.0004D;
        if (onGround() && moving) {
            if (this.tickCount % 2 == 0) {
                SlimeTrailBlock.tryPlace(level(), blockPosition(), getVariant());
            }
            if (level().getBlockState(blockPosition()).getBlock() instanceof SlimeTrailBlock) {
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 12, 1, true, false, false));
                if (horizontal < 0.16D) {
                    setDeltaMovement(motion.x * 1.07D, motion.y, motion.z * 1.07D);
                }
            }
        }

        // Babas mágicas cada cierto tiempo (5-10 minutos).
        if (--dropTimer <= 0) {
            dropTimer = 6000 + this.random.nextInt(6000);
            spawnAtLocation(new ItemStack(ModRegistry.MAGIC_SLIME.get()));
            playSound(SoundEvents.SLIME_SQUISH, 1.0F, 1.3F);
            if (level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.GLOW, getX(), getY() + 0.6D, getZ(), 10, 0.3D, 0.3D, 0.3D, 0.02D);
            }
        }

        // Domesticada: absorbe lo que haya tirado cerca.
        if (isTame() && this.tickCount % 10 == 0) {
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class,
                    getBoundingBox().inflate(1.0D, 0.5D, 1.0D), item -> item.isAlive() && !item.hasPickUpDelay());
            for (ItemEntity item : items) {
                ItemStack remaining = belly.addItem(item.getItem().copy());
                if (remaining.isEmpty()) {
                    item.discard();
                } else {
                    item.setItem(remaining);
                }
                playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.8F, 1.6F);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide) {
            for (int i = 0; i < belly.getContainerSize(); i++) {
                ItemStack stack = belly.getItem(i);
                if (!stack.isEmpty()) {
                    spawnAtLocation(stack);
                }
            }
            belly.clearContent();
        }
        super.die(source);
    }

    // ------------------------------------------------------------ Spawn

    public static boolean checkSquissySpawnRules(EntityType<SquissyEntity> type, LevelAccessor level,
                                                 MobSpawnType reason, BlockPos pos, RandomSource random) {
        return Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    // ---------------------------------------------------------- Sonidos

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.15F, 1.5F);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }
}

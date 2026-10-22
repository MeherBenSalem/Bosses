package com.nightbeam.remnants.mixin;

import com.nightbeam.remnants.journal.BossHunts;
import com.nightbeam.remnants.journal.EncounterCredits;
import com.nightbeam.remnants.journal.HealthWriteTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class JournalLivingEntityMixin {
    // Accessors resolve through the refmap; Forge 1.20.1 jars keep shadowed names unmapped (MixinPolicyRegression).
    @Accessor("dead") protected abstract boolean remnant$dead();
    @Unique private boolean remnant$huntProcessed;
    @Unique private HealthWriteTracker remnant$healthWrites;
    @Unique private static final String REMNANT_HUNT_PROCESSED = "RemnantBossHuntProcessed";

    // Measure the write itself: cancellable pre-events and reentrant post-events lie outside this pair.
    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V", shift = At.Shift.BEFORE))
    private void remnant$beforeHealthWrite(DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide || BossHunts.boss(entity) == null) return;
        if (remnant$healthWrites == null) remnant$healthWrites = new HealthWriteTracker();
        remnant$healthWrites.beforeWrite(entity.getHealth());
    }
    @Inject(method = "actuallyHurt", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V", shift = At.Shift.AFTER))
    private void remnant$afterHealthWrite(DamageSource source, float amount, CallbackInfo ci) {
        if (remnant$healthWrites == null) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        float healthLost = remnant$healthWrites.afterWrite(entity.getHealth());
        BossHunts.damage(entity, source, healthLost);
    }
    @Inject(method = "die", at = @At("RETURN"))
    private void remnant$acceptedDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // isDeadOrDying() is also true at zero health after a Forge/NeoForge cancellation.
        if (!entity.level().isClientSide && BossHunts.boss(entity) != null
                && EncounterCredits.acceptedDeath(remnant$dead(), remnant$huntProcessed)) {
            remnant$huntProcessed = true;
            BossHunts.died(entity);
        }
    }
    @Inject(method = "tick", at = @At("TAIL"))
    private void remnant$discover(CallbackInfo ci) { BossHunts.discover((LivingEntity) (Object) this); }
    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void remnant$saveJournalMarker(CompoundTag tag, CallbackInfo ci) {
        if (remnant$huntProcessed) tag.putBoolean(REMNANT_HUNT_PROCESSED, true);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void remnant$loadJournalMarker(CompoundTag tag, CallbackInfo ci) {
        remnant$huntProcessed = tag.getBoolean(REMNANT_HUNT_PROCESSED);
    }
}

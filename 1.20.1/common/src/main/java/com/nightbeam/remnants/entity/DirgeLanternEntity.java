package com.nightbeam.remnants.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class DirgeLanternEntity extends BoneMonsterEntity {
    public DirgeLanternEntity(EntityType<? extends DirgeLanternEntity> type,Level level) { super(type,level); setNoGravity(true); moveControl=new net.minecraft.world.entity.ai.control.FlyingMoveControl(this,20,true); }
    @Override public String monsterId() { return "dirge_lantern"; }

    @Override public boolean floating() { return true; }
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        var nav=new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this,level);
        nav.setCanOpenDoors(false);nav.setCanFloat(true);nav.setCanPassDoors(true);return nav;
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source) { return false; }
}

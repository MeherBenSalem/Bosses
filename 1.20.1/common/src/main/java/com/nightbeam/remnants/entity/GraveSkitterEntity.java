package com.nightbeam.remnants.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class GraveSkitterEntity extends BoneMonsterEntity {
    public GraveSkitterEntity(EntityType<? extends GraveSkitterEntity> type,Level level) { super(type,level);  }
    @Override public String monsterId() { return "grave_skitter"; }
}

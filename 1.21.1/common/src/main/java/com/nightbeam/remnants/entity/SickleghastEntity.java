package com.nightbeam.remnants.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SickleghastEntity extends BoneMonsterEntity {
    public SickleghastEntity(EntityType<? extends SickleghastEntity> type,Level level) { super(type,level);  }
    @Override public String monsterId() { return "sickleghast"; }
}

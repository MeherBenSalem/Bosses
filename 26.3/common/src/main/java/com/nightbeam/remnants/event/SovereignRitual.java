package com.nightbeam.remnants.event;

import com.nightbeam.remnants.config.*;
import net.minecraft.world.entity.EntitySpawnReason;
import com.nightbeam.remnants.entity.HollowSovereignEntity;
import com.nightbeam.remnants.init.*;
import com.nightbeam.remnants.journal.RitualInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Validate everything before committing the offering. The spawned entity owns the awakening. */
public final class SovereignRitual {
    private SovereignRitual() {}
    public static boolean tryActivate(Player player,Level level,BlockPos altar) {
        RitualInfo ritual=RitualInfo.resolve("hollow_sovereign_summon");
        String item=ritual.activationItem();
        if(!BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString().equals(item)) return false;
        if(!(level instanceof ServerLevel server)) return true;
        if(player.isSpectator() || SovereignConfig.value("summoning_enabled",0,1)==0 || level.getDifficulty()==Difficulty.PEACEFUL) {
            message(player,"disabled"); return true;
        }
        if(RealmConfig.ritualsRealmOnly() && !com.nightbeam.remnants.realm.EclipsedRealm.isRealm(level)) { message(player,"realm"); return true; }
        BlockPos[] pedestals={altar.offset(3,0,0),altar.offset(-3,0,0),altar.offset(0,0,3),altar.offset(0,0,-3)};
        for(int i=0;i<4;i++) {
            Identifier id=Identifier.tryParse(ritual.offerings().get(i));
            if(!ritual.valid() || id==null || !BuiltInRegistries.BLOCK.containsKey(id) || !level.getBlockState(pedestals[i]).is(ModBlocks.ANCIENT_PEDESTAL.get())
                || !level.getBlockState(pedestals[i].above()).is(BuiltInRegistries.BLOCK.getValue(id))) { message(player,"pattern"); return true; }
        }
        double radius=SovereignConfig.value("duplicate_radius",16,128);
        if(!level.getEntitiesOfClass(HollowSovereignEntity.class,new AABB(altar).inflate(radius),e->true).isEmpty()) { message(player,"nearby"); return true; }
        HollowSovereignEntity boss=ModEntities.HOLLOW_SOVEREIGN.get().create(server, EntitySpawnReason.MOB_SUMMONED);
        if(boss==null) return true;
        boss.snapTo(altar.getX()+.5,altar.getY()+1,altar.getZ()+.5,player.getYRot()+180,0);
        if(!level.getWorldBorder().isWithinBounds(boss.getBoundingBox()) || !level.noCollision(boss,boss.getBoundingBox())) { message(player,"space"); return true; }
        boss.setPersistenceRequired();
        if(!server.addFreshEntity(boss)) { message(player,"space"); return true; }
        // No delayed global task, no per-observer consumption, no griefing explosion.
        for(BlockPos pedestal:pedestals) server.setBlock(pedestal.above(),Blocks.AIR.defaultBlockState(),3);
        if(!player.getAbilities().instabuild) player.getMainHandItem().shrink(1);
        message(player,"summoned");
        return true;
    }
    private static void message(Player player,String suffix) { player.sendOverlayMessage(Component.translatable("message.remnant_bosses.sovereign."+suffix)); }
}

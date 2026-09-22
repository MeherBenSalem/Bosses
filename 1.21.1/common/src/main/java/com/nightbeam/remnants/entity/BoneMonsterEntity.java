package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.BoneMonsterConfig;
import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.EnumSet;

/** Regular enemies with server-owned windups and client-only, bounded particle tells. */
public abstract class BoneMonsterEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<String> ACTION = SynchedEntityData.defineId(BoneMonsterEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(BoneMonsterEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> AIM_X = SynchedEntityData.defineId(BoneMonsterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_Y = SynchedEntityData.defineId(BoneMonsterEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_Z = SynchedEntityData.defineId(BoneMonsterEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean configured, loaded, pounceHit;
    private int cooldown = 30;
    private Vec3 castOrigin = Vec3.ZERO;

    protected BoneMonsterEntity(EntityType<? extends BoneMonsterEntity> type, Level level) { super(type, level); xpReward=8; }
    public abstract String monsterId();
    public boolean floating() { return false; }
    private boolean skitter() { return monsterId().equals("grave_skitter"); }
    private double config(String key,double min,double max) { return BoneMonsterConfig.value(monsterId(),key,min,max); }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,5)
            .add(Attributes.ARMOR,3).add(Attributes.MOVEMENT_SPEED,.26).add(Attributes.FOLLOW_RANGE,24)
            .add(Attributes.FLYING_SPEED,.3);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTION,""); builder.define(AGE,0);
        builder.define(AIM_X,0f); builder.define(AIM_Y,0f); builder.define(AIM_Z,0f);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
            @Override public boolean canUse() { return getTarget()!=null && getTarget().isAlive(); }
            @Override public void stop() { getNavigation().stop(); }
            @Override public void tick() {
                LivingEntity t=getTarget();
                if(t==null || !action().isEmpty()) { getNavigation().stop(); return; }
                getLookControl().setLookAt(t,30,30);
                if(distanceTo(t)>(floating()?6:2)) getNavigation().moveTo(t,1);
                else getNavigation().stop();
            }
        });
        goalSelector.addGoal(3, floating()?new WaterAvoidingRandomFlyingGoal(this,.7):new WaterAvoidingRandomStrollGoal(this,.7));
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,16));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public String action() { return entityData.get(ACTION); }
    private Vec3 aim() { return new Vec3(entityData.get(AIM_X),entityData.get(AIM_Y),entityData.get(AIM_Z)); }
    private void configure() {
        if(configured) return;
        configured=true;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(config("max_health",1,1024));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(config("attack_damage",0,100));
        getAttribute(Attributes.ARMOR).setBaseValue(config("armor",0,30));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(config("movement_speed",.05,.6));
        getAttribute(Attributes.FLYING_SPEED).setBaseValue(config("movement_speed",.05,.6));
        xpReward=(int)config("xp_reward",0,100);
        setHealth(loaded?Math.min(getHealth(),getMaxHealth()):getMaxHealth());
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) { visuals(); return; }
        configure();
        if(!isAlive() || isNoAi()) return;
        if(!action().isEmpty()) {
            getNavigation().stop();
            int age=entityData.get(AGE)+1;entityData.set(AGE,age);
            resolve(age);
            if(age>=(action().equals("special")?32:20)) {
                entityData.set(ACTION,"");cooldown=(int)config("attack_cooldown_ticks",10,200);
            }
            return;
        }
        if(cooldown>0) { cooldown--; return; }
        LivingEntity target=getTarget();
        if(target==null || !target.isAlive() || !hasLineOfSight(target)) return;
        double distance=distanceTo(target);
        if(distance>config("special_range",3,16)) return;
        if(!floating() && distance>3 && !skitter()) return;
        if(skitter() && !onGround()) return;
        boolean special=floating() || distance>3 || random.nextInt(3)==0;
        begin(special?"special":"attack",target);
    }
    private void begin(String action,LivingEntity target) {
        castOrigin=position();pounceHit=false;
        entityData.set(AIM_X,(float)target.getX());entityData.set(AIM_Y,(float)target.getY());entityData.set(AIM_Z,(float)target.getZ());
        entityData.set(AGE,0);entityData.set(ACTION,action);
        getNavigation().stop();
        playSound(floating()?SoundEvents.SOUL_ESCAPE.value():SoundEvents.SKELETON_AMBIENT,.8f,skitter()?1.4f:.65f);
    }
    private boolean victim(LivingEntity e) {
        return e!=this && e.isAlive() && !(e instanceof BoneMonsterEntity) && !isAlliedTo(e)
            && (!(e instanceof Player p) || (!p.isCreative() && !p.isSpectator()));
    }
    private void hit(LivingEntity e,float damage) {
        if(e.hurt(damageSources().mobAttack(this),damage)) {
            e.knockback(.35,getX()-e.getX(),getZ()-e.getZ());
        }
    }
    private void resolve(int age) {
        boolean special=action().equals("special");
        if(skitter() && special) {
            if(age==12) {
                Vec3 d=aim().subtract(position()).multiply(1,0,1).normalize();
                setDeltaMovement(d.scale(.65).add(0,.34,0));hasImpulse=true;
            }
            LivingEntity t=getTarget();
            if(age>=17 && age<=25 && !pounceHit && t!=null && victim(t) && distanceTo(t)<2.4 && hasLineOfSight(t)) {
                hit(t,(float)config("special_damage",0,100));pounceHit=true;
            }
            return;
        }
        if(age!=(special?17:10)) return;
        float damage=special?(float)config("special_damage",0,100):(float)getAttributeValue(Attributes.ATTACK_DAMAGE);
        if(floating() && special) {
            // The location is locked at windup start so players can dodge the burst.
            Vec3 center=aim();
            if(center.distanceTo(castOrigin)>config("special_range",3,16)+1) return;
            for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(2.25),this::victim)) {
                if(e.position().distanceToSqr(center)<=2.25*2.25 && hasLineOfSight(e))hit(e,damage);
            }
        } else {
            Vec3 forward=aim().subtract(castOrigin).multiply(1,0,1).normalize();
            for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(special?3:2.2),this::victim)) {
                Vec3 d=e.position().subtract(position());
                if(d.length()<=(special?3.4:2.6) && d.multiply(1,0,1).normalize().dot(forward)>.25 && hasLineOfSight(e))hit(e,damage);
            }
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP,.8f,.8f);
    }
    private void visuals() {
        if(!isAlive() || SovereignConfig.visualDensity()<=0) return;
        double density=SovereignConfig.visualDensity();
        if(floating() && tickCount%4==0 && random.nextDouble()<density)
            level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,getX(),getY()+1.35,getZ(),0,.015,0);
        if(action().isEmpty()) return;
        int age=entityData.get(AGE);
        if(age>22 || tickCount%2!=0) return;
        Vec3 center=floating()?aim():position();
        double radius=floating()?2.25:(skitter()?1.1:2.8);
        int count=Math.max(1,(int)(12*density));
        for(int i=0;i<count;i++) {
            double angle=i*Math.PI*2/count+age*.08;
            level().addParticle(age<17?ParticleTypes.SOUL:ParticleTypes.SOUL_FIRE_FLAME,
                center.x+Math.cos(angle)*radius,center.y+.12+(age>=17?(age-17)*.12:0),center.z+Math.sin(angle)*radius,
                0,age>=17?.025:0,0);
        }
    }
    @Override protected void tickDeath() {
        deathTime++;getNavigation().stop();
        if(deathTime>=30 && !level().isClientSide) { level().broadcastEntityEvent(this,(byte)60);remove(RemovalReason.KILLED); }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);loaded=true;configured=false;entityData.set(ACTION,"");entityData.set(AGE,0);cooldown=30;
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this,"monster",2,state->{
            String prefix="animation."+monsterId()+".";
            if(!isAlive())return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(prefix+"death"));
            if(!action().isEmpty())return state.setAndContinue(RawAnimation.begin().thenPlay(prefix+action()));
            if(hurtTime>0)return state.setAndContinue(RawAnimation.begin().thenPlay(prefix+"hurt"));
            return state.setAndContinue(RawAnimation.begin().thenLoop(prefix+(state.isMoving()?"walk":"idle")));
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

package com.nightbeam.remnants.entity;

import com.nightbeam.remnants.config.SovereignConfig;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import java.util.*;

/** Server owns attack timing and damage; clients derive bounded VFX from synchronized state. */
public class HollowSovereignEntity extends Monster implements GeoEntity {
    private static final EntityDataAccessor<String> ATTACK = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> AGE = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> AIM_X = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_Y = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> AIM_Z = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> WAVE_RADIUS = SynchedEntityData.defineId(HollowSovereignEntity.class, EntityDataSerializers.FLOAT);
    private static final String PREFIX = "animation.hollow_sovereign.";
    private static final DustParticleOptions SOUL_LIGHT = new DustParticleOptions(new Vector3f(.28f,1f,.58f),1.3f);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.remnant_bosses.hollow_sovereign"), BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> waveHits = new HashSet<>();
    private boolean configured, loaded;
    private int cooldown = 40, duration;
    private float lockedYaw;
    private String lastAttack = "";

    public HollowSovereignEntity(EntityType<? extends HollowSovereignEntity> type, Level level) { super(type, level); xpReward=150; }
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,950).add(Attributes.ATTACK_DAMAGE,16)
            .add(Attributes.ARMOR,8).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.FOLLOW_RANGE,48)
            .add(Attributes.KNOCKBACK_RESISTANCE,1);
    }
    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ATTACK, ""); entityData.define(AGE,0); entityData.define(ENRAGED,false);
        entityData.define(AIM_X,0f); entityData.define(AIM_Y,0f); entityData.define(AIM_Z,0f);
        entityData.define(WAVE_RADIUS,12f);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new Goal() {
            { setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK)); }
            @Override public boolean canUse() { return getTarget()!=null && getTarget().isAlive(); }
            @Override public void tick() {
                LivingEntity target=getTarget();
                if (target==null || !attack().isEmpty()) { getNavigation().stop(); return; }
                getLookControl().setLookAt(target,25,25);
                if (distanceTo(target)>3.5) getNavigation().moveTo(target,enraged()?1.2:1);
                else getNavigation().stop();
            }
        });
        goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,24));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public String attack() { return entityData.get(ATTACK); }
    public boolean enraged() { return entityData.get(ENRAGED); }
    private Vec3 aim() { return position().add(entityData.get(AIM_X),entityData.get(AIM_Y),entityData.get(AIM_Z)); }
    private Vec3 core() { return position().add(0,2.35,0); }
    private void configure() {
        if(configured) return;
        configured=true;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(SovereignConfig.value("max_health",1,100000));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(SovereignConfig.value("attack_damage",0,1000));
        getAttribute(Attributes.ARMOR).setBaseValue(SovereignConfig.value("armor",0,30));
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(SovereignConfig.value("movement_speed",.05,.6));
        xpReward=(int)SovereignConfig.value("xp_reward",0,10000);
        setHealth(loaded ? Math.min(getHealth(),getMaxHealth()) : getMaxHealth());
        if(!loaded) begin("awaken",60,position());
    }
    @Override public void tick() {
        super.tick();
        if(level().isClientSide) { visuals(); return; }
        configure(); bar.setProgress(Math.max(0,getHealth()/getMaxHealth()));
        if(!isAlive()) return;
        if(!attack().isEmpty()) {
            getNavigation().stop(); setDeltaMovement(0,getDeltaMovement().y,0);
            setYRot(lockedYaw); setYBodyRot(lockedYaw); setYHeadRot(lockedYaw);
            int t=entityData.get(AGE)+1; entityData.set(AGE,t);
            resolve(t);
            if(t>=duration) { entityData.set(ATTACK,""); cooldown=(int)SovereignConfig.value("attack_cooldown_ticks",10,200); }
            return;
        }
        if(!enraged() && getHealth()/getMaxHealth()*100<=SovereignConfig.value("phase_two_percent",1,90)) {
            entityData.set(ENRAGED,true); begin("enrage",60,position()); return;
        }
        if(cooldown>0) { cooldown--; return; }
        LivingEntity target=getTarget();
        if(target==null || !target.isAlive() || !hasLineOfSight(target) || distanceTo(target)>SovereignConfig.value("spell_range",8,40)) return;
        String choice=distanceTo(target)<5 ? (random.nextBoolean()?"claw_combo":"ground_slam") : (random.nextBoolean()?"core_blast":"roar");
        if(choice.equals(lastAttack)) choice=distanceTo(target)<6?"ground_slam":"roar";
        begin(choice,switch(choice){case "claw_combo"->36;case "ground_slam"->44;case "core_blast"->52;default->40;},target.position());
    }
    private void begin(String name,int ticks,Vec3 target) {
        if(!level().isClientSide && getTarget()!=null) {
            Vec3 d=getTarget().position().subtract(position());
            setYRot((float)(Math.atan2(d.z,d.x)*180/Math.PI)-90);
        }
        lockedYaw=getYRot(); duration=ticks; lastAttack=name; waveHits.clear();
        Vec3 d=target.subtract(position());
        entityData.set(AIM_X,(float)d.x); entityData.set(AIM_Y,(float)d.y); entityData.set(AIM_Z,(float)d.z);
        entityData.set(AGE,0); entityData.set(ATTACK,name);
        entityData.set(WAVE_RADIUS,(float)SovereignConfig.value("wave_radius",4,18));
        playSound(name.equals("core_blast")?SoundEvents.BEACON_ACTIVATE:SoundEvents.WARDEN_ROAR,.9f,enraged()?.65f:.8f);
    }
    private boolean victim(LivingEntity e) { return e!=this && e.isAlive() && !(e instanceof HollowSovereignEntity) && !isAlliedTo(e) && (!(e instanceof Player p)||(!p.isCreative()&&!p.isSpectator())); }
    private void hit(LivingEntity e,double damage) {
        if(victim(e) && hasLineOfSight(e) && e.hurt(damageSources().mobAttack(this),(float)(damage*(enraged()?SovereignConfig.value("phase_two_damage_multiplier",1,3):1)))) {
            Vec3 d=e.position().subtract(position()); e.knockback(.6,-d.x,-d.z);
        }
    }
    private void area(Vec3 center,double radius,double damage) {
        for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius,3,radius),this::victim))
            if(e.position().subtract(center).horizontalDistanceSqr()<=radius*radius) hit(e,damage);
    }
    private Vec3 beamEnd() {
        Vec3 end=aim().add(0,1,0);
        return level().clip(new ClipContext(core(),end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this)).getLocation();
    }
    private void resolve(int t) {
        switch(attack()) {
            case "claw_combo" -> { if(t==11||t==23) { area(position().add(getLookAngle().scale(2)),3,getAttributeValue(Attributes.ATTACK_DAMAGE)); playSound(SoundEvents.PLAYER_ATTACK_SWEEP,1,.65f); } }
            case "ground_slam" -> {
                if(t==21) playSound(SoundEvents.GENERIC_EXPLODE,1.4f,.6f);
                if(t>=21 && t<=36) {
                    double r=(t-20)*entityData.get(WAVE_RADIUS)/16;
                    for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(r+1,2,r+1),this::victim)) {
                        double d=e.position().subtract(position()).horizontalDistance();
                        if(Math.abs(d-r)<1.2 && Math.abs(e.getY()-getY())<2.5 && waveHits.add(e.getUUID())) hit(e,SovereignConfig.value("slam_damage",0,1000));
                    }
                }
            }
            case "core_blast" -> { if(t==29) {
                Vec3 a=core(),b=beamEnd(),ab=b.subtract(a); double length=ab.lengthSqr();
                if(length>0) for(LivingEntity e:level().getEntitiesOfClass(LivingEntity.class,new AABB(a,b).inflate(1.3),this::victim)) {
                    Vec3 p=e.getBoundingBox().getCenter(); double u=Math.max(0,Math.min(1,p.subtract(a).dot(ab)/length));
                    if(p.distanceTo(a.add(ab.scale(u)))<1.3+e.getBbWidth()/2) hit(e,SovereignConfig.value("beam_damage",0,1000));
                }
                playSound(SoundEvents.WARDEN_SONIC_BOOM,1.5f,.7f);
            } }
            case "roar" -> { if(t==28) { area(aim(),enraged()?5:3.5,SovereignConfig.value("eruption_damage",0,1000)); playSound(SoundEvents.EVOKER_PREPARE_ATTACK,1,.6f); } }
            case "enrage" -> { if(t==40) { area(position(),6,SovereignConfig.value("slam_damage",0,1000)); playSound(SoundEvents.WITHER_SPAWN,1,.7f); } }
        }
    }
    private void particle(Vec3 p,boolean bright) { level().addParticle(bright?(tickCount%2==0?SOUL_LIGHT:ParticleTypes.END_ROD):ParticleTypes.SOUL_FIRE_FLAME,p.x,p.y,p.z,0,bright?.015:.035,0); }
    private void ring(Vec3 c,double radius,int count,double twist) {
        for(int i=0;i<count;i++) { double a=i*Math.PI*2/count+twist; particle(c.add(Math.cos(a)*radius,0,Math.sin(a)*radius),i%6==0); }
    }
    private void seal(Vec3 c,double radius,int count,double rotation) {
        ring(c,radius,count,rotation);
        for(int edge=0;edge<5;edge++) {
            double a=edge*Math.PI*2/5+rotation,b=a+Math.PI*4/5;
            Vec3 from=c.add(Math.cos(a)*radius,0,Math.sin(a)*radius),to=c.add(Math.cos(b)*radius,0,Math.sin(b)*radius);
            for(int j=0;j<count/5;j++) particle(from.lerp(to,j/(double)(count/5)),true);
        }
    }
    private void visuals() {
        double density=SovereignConfig.visualDensity();
        if(density<=0) return;
        int n=Math.max(8,(int)(48*density)); int t=entityData.get(AGE);
        if(!isAlive()) { if(deathTime<65) ring(core(),Math.max(.1,3-deathTime*.045),n,tickCount*.15); return; }
        if(tickCount%3==0) {
            for(int i=0;i<5;i++) { double a=tickCount*.04+i*Math.PI*2/5; particle(position().add(Math.cos(a)*.7,4.2+Math.sin(a*2)*.15,Math.sin(a)*.7),true); }
        }
        switch(attack()) {
            case "ground_slam" -> {
                if(t<21) { ring(position().add(0,.1,0),4,n,-tickCount*.02); ring(core(),(21-t)*.12,n/2,t*.2); }
                else if(t<=36) { double r=(t-20)*entityData.get(WAVE_RADIUS)/16; ring(position().add(0,.15,0),r,n*2,t*.1); ring(position().add(0,.7,0),r-.3,n,t*.1); }
            }
            case "core_blast" -> {
                Vec3 end=beamEnd(),d=end.subtract(core());
                if(t<29) {
                    ring(core(),.45+t*.025,n,t*.2);
                    for(int i=0;i<12;i++) particle(core().add(d.scale(i/12.0)),true);
                } else if(t<39) {
                    Vec3 axis=d.normalize(),side=axis.cross(new Vec3(0,1,0));
                    if(side.lengthSqr()<.01) side=new Vec3(1,0,0); else side=side.normalize();
                    Vec3 up=side.cross(axis).normalize();
                    for(int i=0;i<n*3;i++) { double u=i/(double)(n*3),a=u*32-t*.6; Vec3 p=core().add(d.scale(u)); particle(p,true); particle(p.add(side.scale(Math.cos(a)*.55)).add(up.scale(Math.sin(a)*.55)),false); }
                }
            }
            case "roar" -> {
                double radius=enraged()?5:3.5;
                if(t<28) { seal(aim().add(0,.1,0),radius,n,t*.02); ring(aim().add(0,.12,0),radius*(28-t)/28.0,n/2,-t*.08); }
                else for(int i=0;i<n;i++) { double a=i*2*Math.PI/n+t*.2; particle(aim().add(Math.cos(a)*radius,(t-28)*.45,Math.sin(a)*radius),i%4==0); }
            }
            case "awaken", "enrage" -> {
                for(int h=0;h<4;h++) ring(position().add(0,h+Math.sin(t*.08)*.2,0),2.5+h*.25,n,t*.09+h*.6);
                if(tickCount%2==0) seal(position().add(0,.08,0),6,n,-t*.025);
            }
            case "claw_combo" -> { if((t>=8&&t<=14)||(t>=20&&t<=26)) {
                for(int i=0;i<n;i++) { double a=Math.toRadians(getYRot())+(i/(double)n-.5)*2; particle(position().add(-Math.sin(a)*3,1.6+(i/(double)n),Math.cos(a)*3),i%4==0); }
            } }
        }
    }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public void startSeenByPlayer(ServerPlayer p) { super.startSeenByPlayer(p); bar.addPlayer(p); }
    @Override public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }
    @Override public void remove(RemovalReason reason) { bar.removeAllPlayers(); super.remove(reason); }
    @Override protected void tickDeath() { deathTime++; getNavigation().stop(); if(deathTime>=70&&!level().isClientSide) { level().broadcastEntityEvent(this,(byte)60); remove(RemovalReason.KILLED); } }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("SovereignEnraged",enraged()); }
    @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); loaded=true; configured=false; entityData.set(ENRAGED,tag.getBoolean("SovereignEnraged")); entityData.set(ATTACK,""); cooldown=40; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this,"sovereign",2,state -> {
            if(!isAlive()) return state.setAndContinue(RawAnimation.begin().thenPlayAndHold(PREFIX+"death"));
            if(!attack().isEmpty()) return state.setAndContinue(RawAnimation.begin().thenPlay(PREFIX+attack()));
            return state.setAndContinue(RawAnimation.begin().thenLoop(PREFIX+(state.isMoving()?"walk":"idle")));
        }));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}

package com.nightbeam.remnants.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * remnant/monsters/&lt;id&gt;: regular enemies, including the Hollow Legion, the Cryptborn and the Forgotten Souls.
 * The dirge_lantern file from earlier versions stays on disk unused.
 */
public final class BoneMonsterConfig {
    private BoneMonsterConfig() {}
    public static final Map<String,Map<String,Double>> DEFAULTS=Map.ofEntries(
        Map.entry("sickleghast",stats(40,6,5,.24,38,9,4,10)),
        Map.entry("hollow_sentinel",legion(30,8,7,.23,8,30,
            "bash_damage",4,"strike_damage",9,"shield_reduction",.6,"shield_disable_ticks",100)),
        Map.entry("spectral_royal_guard",legion(20,4,4,.25,0,30,
            "bash_damage",3,"strike_damage",4,"shield_reduction",.6,"shield_disable_ticks",100,
            "boss_life_ticks",600,"sigil_life_ticks",400)),
        Map.entry("hollow_arbalist",legion(24,4,3,.25,9,20,
            "bolt_damage",8,"kick_damage",3,"hold_min_distance",8,"hold_max_distance",14,"retreat_distance",6)),
        Map.entry("fallen_standard_bearer",legion(34,6,6,.24,14,30,
            "aura_radius",10,"rally_cooldown_ticks",300)),
        Map.entry("soulbound_knight",legion(60,12,10,.27,25,30,
            "lunge_damage",12,"counter_damage",12,"counter_chance",.25,"knockback_resistance",.6)),
        // The Cryptborn. legion(hp, armor, damage, speed, xp, cooldown, extras...).
        Map.entry("catacomb_rat",legion(8,0,2,.32,1,16,
            "flee_light_level",10,"swarm_min",3,"swarm_max",6,"swarm_radius",12,"anger_ticks",400)),
        Map.entry("crypt_lurker",legion(22,3,5,.30,6,24,
            "pounce_damage",6,"pounce_slow_ticks",40,"ambush_radius",6,"pounce_cooldown_ticks",80,"rebury_ticks",400)),
        Map.entry("gravebound_soldier",legion(24,5,5,.26,7,26,
            "thrust_reach",3.2,"sweep_pull_blocks",1,"share_radius",8)),
        Map.entry("burial_guardian",legion(140,14,14,.18,60,36,
            "slam_damage",14,"slam_radius",4,"sweep_damage",14,"sweep_knockback",1.4,"unseal_damage",10,
            "unseal_wither_ticks",60,"unseal_cooldown_ticks",200,"turn_degrees",6,"phase_two_percent",50)),
        // The Forgotten Souls.
        Map.entry("lost_pilgrim",legion(20,2,4,.20,3,30,
            "strike_reach",2.6,"weakness_ticks",60,"alert_radius",12,"anger_ticks",400)),
        Map.entry("memory_wraith",legion(26,0,6,.30,8,40,
            "bolt_damage",6,"bolt_speed",.7,"bolt_range",24,"hold_min_distance",6,"hold_max_distance",12,
            "blink_chance",.5,"blink_trigger_distance",3,"blink_cooldown_ticks",60)),
        Map.entry("soul_collector",legion(70,6,8,.18,22,30,
            "harvest_damage",8,"harvest_reach",3,"harvest_heal",4,"mark_damage",8,"mark_radius",2,"mark_delay_ticks",30,
            "mark_slow_ticks",40,"mark_range",14,"mark_cooldown_ticks",60,"summon_count",2,"max_wisps",4,
            "summon_cooldown_ticks",300,"soul_heal",6,"aura_radius",10,"aura_ticks",80,"knockback_resistance",.5)),
        Map.entry("soul_wisp",Map.of("attack_damage",4.0,"movement_speed",.32,"life_ticks",200.0)));
    private static Map<String,Double> stats(double hp,double damage,double armor,double speed,double cooldown,double special,double range,double xp) {
        return Map.of("max_health",hp,"attack_damage",damage,"armor",armor,"movement_speed",speed,
            "attack_cooldown_ticks",cooldown,"special_damage",special,"special_range",range,"xp_reward",xp);
    }
    private static Map<String,Double> legion(double hp,double armor,double damage,double speed,double xp,double cooldown,Object... extra) {
        Map<String,Double> map=new LinkedHashMap<>();
        map.put("max_health",hp); map.put("armor",armor); map.put("attack_damage",damage);
        map.put("movement_speed",speed); map.put("xp_reward",xp); map.put("attack_cooldown_ticks",cooldown);
        for(int i=0;i<extra.length;i+=2) map.put((String)extra[i],((Number)extra[i+1]).doubleValue());
        return Map.copyOf(map);
    }
    public static double value(String id,String key,double min,double max) {
        double value=JaumlConfigLib.getNumberValue("remnant/monsters",id,key);
        double fallback=DEFAULTS.getOrDefault(id,Map.of()).getOrDefault(key,min);
        return Math.max(min,Math.min(max,Double.isFinite(value)?value:fallback));
    }
}

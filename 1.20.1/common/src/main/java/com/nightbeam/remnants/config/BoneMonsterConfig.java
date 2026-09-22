package com.nightbeam.remnants.config;

import java.util.Map;

public final class BoneMonsterConfig {
    private BoneMonsterConfig() {}
    public static final Map<String,Map<String,Double>> DEFAULTS=Map.of(
        "grave_skitter",stats(24,4,4,.29,32,7,7,6),
        "sickleghast",stats(40,6,5,.24,38,9,4,10),
        "dirge_lantern",stats(28,4,2,.23,55,7,12,8));
    private static Map<String,Double> stats(double hp,double damage,double armor,double speed,double cooldown,double special,double range,double xp) {
        return Map.of("max_health",hp,"attack_damage",damage,"armor",armor,"movement_speed",speed,
            "attack_cooldown_ticks",cooldown,"special_damage",special,"special_range",range,"xp_reward",xp);
    }
    public static double value(String id,String key,double min,double max) {
        double value=JaumlConfigLib.getNumberValue("remnant/monsters",id,key);
        return Math.max(min,Math.min(max,Double.isFinite(value)?value:DEFAULTS.get(id).get(key)));
    }
}

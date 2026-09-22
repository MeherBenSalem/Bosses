"""Wire the MCP-authored trio into both existing loader layouts."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
SPECS=[('grave_skitter','GraveSkitter','Grave Skitter',1.6,1.15),('sickleghast','Sickleghast','Sickleghast',.9,2.65),('dirge_lantern','DirgeLantern','Dirge Lantern',.85,2.7)]

def change(p,old,new):
    s=p.read_text()
    if new in s:return
    assert old in s,(p,old)
    p.write_text(s.replace(old,new))

for version in ['1.21.1','1.20.1']:
    base=ROOT/version;common=base/'common/src/main';java=common/'java/com/nightbeam/remnants'
    if version=='1.20.1':
        for f in ['entity/BoneMonsterEntity.java','config/BoneMonsterConfig.java']:
            source=(ROOT/'1.21.1/common/src/main/java/com/nightbeam/remnants'/f).read_text()
            source=source.replace('defineSynchedData(SynchedEntityData.Builder builder)','defineSynchedData()').replace('super.defineSynchedData(builder);','super.defineSynchedData();').replace('builder.define(','entityData.define(')
            source=source.replace('geckolib.animatable.instance','geckolib.core.animatable.instance').replace('geckolib.animation.*','geckolib.core.animation.*').replace('SoundEvents.SOUL_ESCAPE.value()','SoundEvents.SOUL_ESCAPE')
            (java/f).write_text(source)
    for id,cls,title,width,height in SPECS:
        extra=''
        init=''
        if id=='dirge_lantern':
            init='setNoGravity(true); moveControl=new net.minecraft.world.entity.ai.control.FlyingMoveControl(this,20,true);'
            extra='''
    @Override public boolean floating() { return true; }
    @Override protected net.minecraft.world.entity.ai.navigation.PathNavigation createNavigation(Level level) {
        var nav=new net.minecraft.world.entity.ai.navigation.FlyingPathNavigation(this,level);
        nav.setCanOpenDoors(false);nav.setCanFloat(true);nav.setCanPassDoors(true);return nav;
    }
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source) { return false; }
'''
        (java/f'entity/{cls}Entity.java').write_text(f'''package com.nightbeam.remnants.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class {cls}Entity extends BoneMonsterEntity {{
    public {cls}Entity(EntityType<? extends {cls}Entity> type,Level level) {{ super(type,level); {init} }}
    @Override public String monsterId() {{ return "{id}"; }}
{extra}}}
''')
        for kind in ['model','renderer']:
            s=(java/f'client/{kind}/HollowSovereign{kind.title()}.java').read_text()
            s=s.replace('HollowSovereign',cls).replace('hollow_sovereign',id).replace('shadowRadius = 1.2f',f'shadowRadius = {width*.4:.2f}f')
            (java/f'client/{kind}/{cls}{kind.title()}.java').write_text(s)
        p=java/'init/ModEntities.java'
        anchor='import com.nightbeam.remnants.entity.HollowSovereignEntity;'
        change(p,anchor,anchor+f'\nimport com.nightbeam.remnants.entity.{cls}Entity;')
        declaration=f'public static final RegistryHolder<EntityType<{cls}Entity>> {id.upper()} = RegistryHolder.entity("{id}"'
        builder=f'EntityType.Builder.<{cls}Entity>of({cls}Entity::new, MobCategory.MONSTER).sized({width}f,{height}f).clientTrackingRange(64).updateInterval(2)'
        if version=='1.21.1':declaration+=f', () -> {builder}.build("{id}"));'
        else:declaration+=f');\n    public static EntityType.Builder<{cls}Entity> create{cls}() {{ return {builder}; }}'
        declaration+=f'\n    public static final RegistryHolder<Item> {id.upper()}_SPAWN_EGG = new RegistryHolder<>("{id}_spawn_egg");'
        anchor='public final class ModEntities {'
        change(p,anchor,anchor+'\n    '+declaration)
        for p in base.glob('*/src/main/java/**/*.java'):
            if '/common/' in p.as_posix():continue
            lines=p.read_text().splitlines();out=[]
            for line in lines:
                out.append(line)
                if 'ModEntities.HOLLOW_SOVEREIGN' in line or line.strip() in ['import com.nightbeam.remnants.entity.HollowSovereignEntity;','import com.nightbeam.remnants.client.renderer.HollowSovereignRenderer;']:
                    added=line.replace('HOLLOW_SOVEREIGN',id.upper()).replace('HollowSovereign',cls)
                    if added not in lines:out.append(added)
            if out!=lines:p.write_text('\n'.join(out)+'\n')
        p=java/'init/ModTabs.java';anchor='tabData.accept(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG.get());'
        change(p,anchor,anchor+f'\n                    tabData.accept(ModEntities.{id.upper()}_SPAWN_EGG.get());')
        p=common/'resources/assets/remnant_bosses/lang/en_us.json';lang=json.loads(p.read_text())
        lang.update({f'entity.remnants.{id}':title,f'entity.remnant_bosses.{id}':title,f'item.remnant_bosses.{id}_spawn_egg':title+' Spawn Egg'})
        p.write_text(json.dumps(lang,indent=2,ensure_ascii=False)+'\n')
        (common/f'resources/assets/remnant_bosses/models/item/{id}_spawn_egg.json').write_text('{"parent":"minecraft:item/template_spawn_egg"}\n')
        folder='loot_table' if version=='1.21.1' else 'loot_tables'
        p=common/f'resources/data/remnants/{folder}/entities/{id}.json';p.parent.mkdir(parents=True,exist_ok=True)
        p.write_text(json.dumps({'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:bone','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':3}}]}]}]},indent=2))
    p=java/'config/JaumlConfigLib.java';anchor='private static double getDefaultNumber(String category, String file, String key) {'
    change(p,anchor,anchor+'\n        if ("remnant/monsters".equals(category) && BoneMonsterConfig.DEFAULTS.containsKey(file)) return BoneMonsterConfig.DEFAULTS.get(file).getOrDefault(key,1.0);')
    p=java/'config/JaumlConfigBootstrap.java';anchor='api.createConfigFile("remnant/bosses", "hollow_sovereign");'
    change(p,anchor,'''BoneMonsterConfig.DEFAULTS.forEach((file,defaults) -> {
            api.createConfigFile("remnant/monsters",file);
            defaults.forEach((key,value) -> {
                if(!api.arrayKeyExists("remnant/monsters",file,key)) api.setNumberValue("remnant/monsters",file,key,value);
            });
        });
        '''+anchor)
print('Integrated three ordinary mobs into four loaders.')

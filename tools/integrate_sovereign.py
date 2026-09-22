"""One-time integration of the Sovereign across the existing loader layouts."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
def change(p,old,new):
    s=p.read_text()
    if new in s:return
    assert old in s,(p,old)
    p.write_text(s.replace(old,new),encoding='utf-8')

for version in ['1.21.1','1.20.1']:
    base=ROOT/version; common=base/'common/src/main'; java=common/'java/com/nightbeam/remnants'
    if version=='1.20.1':
        for name in ['config/SovereignConfig.java','entity/HollowSovereignEntity.java','event/SovereignRitual.java']:
            source=(ROOT/'1.21.1/common/src/main/java/com/nightbeam/remnants'/name).read_text()
            if name.startswith('entity/'):
                source=source.replace('defineSynchedData(SynchedEntityData.Builder builder)','defineSynchedData()').replace('super.defineSynchedData(builder);','super.defineSynchedData();').replace('builder.define(','entityData.define(')
                source=source.replace('geckolib.animatable.instance','geckolib.core.animatable.instance').replace('geckolib.animation.*','geckolib.core.animation.*').replace('SoundEvents.GENERIC_EXPLODE.value()','SoundEvents.GENERIC_EXPLODE')
            (java/name).write_text(source)
    for cls in ['model','renderer']:
        p=java/f'client/{cls}/Umbrakar{cls.title()}.java'
        s=p.read_text().replace('Umbrakar','HollowSovereign').replace('umbrakar','hollow_sovereign').replace('shadowRadius = 3.6f','shadowRadius = 1.2f')
        (java/f'client/{cls}/HollowSovereign{cls.title()}.java').write_text(s)
    entities=java/'init/ModEntities.java'
    change(entities,'import com.nightbeam.remnants.entity.UmbrakarEntity;','import com.nightbeam.remnants.entity.UmbrakarEntity;\nimport com.nightbeam.remnants.entity.HollowSovereignEntity;')
    field='public static final RegistryHolder<Item> UMBRAKAR_SPAWN_EGG'
    change(entities,field,'public static final RegistryHolder<Item> HOLLOW_SOVEREIGN_SPAWN_EGG = new RegistryHolder<>("hollow_sovereign_spawn_egg");\n\t'+field)
    marker='public final class ModEntities {'
    if version=='1.21.1':
        declaration='public static final RegistryHolder<EntityType<HollowSovereignEntity>> HOLLOW_SOVEREIGN = RegistryHolder.entity("hollow_sovereign", () -> EntityType.Builder.<HollowSovereignEntity>of(HollowSovereignEntity::new, MobCategory.MONSTER).sized(1.8f,4.0f).clientTrackingRange(96).updateInterval(2).build("hollow_sovereign"));'
    else:
        declaration='public static final RegistryHolder<EntityType<HollowSovereignEntity>> HOLLOW_SOVEREIGN = RegistryHolder.entity("hollow_sovereign");\n\tpublic static EntityType.Builder<HollowSovereignEntity> createHollowSovereign() { return EntityType.Builder.<HollowSovereignEntity>of(HollowSovereignEntity::new, MobCategory.MONSTER).sized(1.8f,4.0f).clientTrackingRange(96).updateInterval(2); }'
    change(entities,marker,marker+'\n\t'+declaration)
    # Duplicate the established entity/egg/attribute/renderer bindings in each loader.
    for p in base.glob('*/src/main/java/**/*.java'):
        if '/common/' in p.as_posix():continue
        s=p.read_text(); lines=s.splitlines(); out=[]
        for line in lines:
            out.append(line)
            if ('ModEntities.UMBRAKAR' in line and 'UMBRAKAR_ORB' not in line) or line.strip() in ['import com.nightbeam.remnants.entity.UmbrakarEntity;','import com.nightbeam.remnants.client.renderer.UmbrakarRenderer;']:
                added=line.replace('UMBRAKAR','HOLLOW_SOVEREIGN').replace('Umbrakar','HollowSovereign').replace('0x3A1A4A, 0xC48CFF','0x947360, 0x64CE8A')
                if added not in lines:out.append(added)
        if out!=lines:p.write_text('\n'.join(out)+'\n')
    p=java/'init/ModTabs.java'
    s=p.read_text(); anchor='tabData.accept(ModEntities.UMBRAKAR_SPAWN_EGG.get());'
    change(p,anchor,anchor+'\n\t\t\t\t\ttabData.accept(ModEntities.HOLLOW_SOVEREIGN_SPAWN_EGG.get());')
    p=java/('event/BlockInteractionEvents.java' if version=='1.21.1' else 'event/GameEvents.java')
    change(p,'String heldKey =','if (SovereignRitual.tryActivate(player, level, pos)) return;\n\t\tString heldKey =')
    # Jauml defaults and bootstrap use one shared catalog for consistency.
    p=java/'config/JaumlConfigLib.java'
    anchor='private static double getDefaultNumber(String category, String file, String key) {'
    change(p,anchor,anchor+'\n\t\tif ("hollow_sovereign".equals(file)) return SovereignConfig.DEFAULTS.getOrDefault(key, 1.0);\n\t\tif ("presentation".equals(file)) return 1.0;')
    anchor='private static String getDefaultString(String category, String file, String key) {'
    change(p,anchor,anchor+'\n\t\tif ("hollow_sovereign_summon".equals(file)) return SovereignConfig.RITUAL.getOrDefault(key, "");')
    p=java/'config/JaumlConfigBootstrap.java'
    anchor='if (api.createConfigFile("remnant/bosses", "ossukage_summon")) {'
    extra='''api.createConfigFile("remnant/bosses", "hollow_sovereign");
        SovereignConfig.DEFAULTS.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/bosses", "hollow_sovereign", key)) api.setNumberValue("remnant/bosses", "hollow_sovereign", key, value);
        });
        api.createConfigFile("remnant/bosses", "hollow_sovereign_summon");
        SovereignConfig.RITUAL.forEach((key, value) -> {
            if (!api.arrayKeyExists("remnant/bosses", "hollow_sovereign_summon", key)) api.setStringValue("remnant/bosses", "hollow_sovereign_summon", key, value);
        });
        api.createConfigFile("remnant/client", "presentation");
        for (String key : new String[]{"custom_boss_bars", "vfx_density"}) {
            if (!api.arrayKeyExists("remnant/client", "presentation", key)) api.setNumberValue("remnant/client", "presentation", key, 1);
        }
        '''
    change(p,anchor,extra+anchor)
    lang=common/'resources/assets/remnant_bosses/lang/en_us.json'; data=json.loads(lang.read_text())
    data.update({'entity.remnant_bosses.hollow_sovereign':'Hollow Sovereign','item.remnant_bosses.hollow_sovereign_spawn_egg':'Hollow Sovereign Spawn Egg',
        'entity.remnants.hollow_sovereign':'Hollow Sovereign',
        'message.remnant_bosses.sovereign.disabled':'The ritual is dormant (disabled or Peaceful difficulty).',
        'message.remnant_bosses.sovereign.pattern':'Place four Ancient Pedestals 3 blocks from the altar: bone blocks east/west, skeleton skulls north/south.',
        'message.remnant_bosses.sovereign.nearby':'A Hollow Sovereign already haunts this area.',
        'message.remnant_bosses.sovereign.space':'The Sovereign needs clear space above the altar.',
        'message.remnant_bosses.sovereign.summoned':'The crown awakens. The Hollow Sovereign has risen!'
    }); lang.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
    p=common/'resources/assets/remnant_bosses/models/item/hollow_sovereign_spawn_egg.json';p.write_text('{"parent":"minecraft:item/template_spawn_egg"}\n')
    folder='loot_table' if version=='1.21.1' else 'loot_tables'
    p=common/f'resources/data/remnant_bosses/{folder}/entities/hollow_sovereign.json';p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps({'type':'minecraft:entity','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:echo_shard','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':4,'max':8}}]}]},{'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:bone','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':12,'max':24}}]}]}]},indent=2))
    canonical=common/f'resources/data/remnants/{folder}/entities/hollow_sovereign.json';canonical.parent.mkdir(parents=True,exist_ok=True);canonical.write_text(p.read_text())
print('Integrated both versions')

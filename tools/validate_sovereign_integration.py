"""Cross-version resources, loader wiring, defaults, and packaged Mixin checks."""
from pathlib import Path
import json
import re
import zipfile

root=Path(__file__).resolve().parents[1]
assets=[]
for version,loaders in [('1.20.1',['fabric','forge']),('1.21.1',['fabric','neoforge'])]:
    base=root/version
    common=base/'common/src/main'
    entity=(common/'java/com/nightbeam/remnants/entity/HollowSovereignEntity.java').read_text()
    anim=json.loads((common/'resources/assets/remnant_bosses/animations/entity/hollow_sovereign.animation.json').read_text())['animations']
    geo=json.loads((common/'resources/assets/remnant_bosses/geo/entity/hollow_sovereign.geo.json').read_text())['minecraft:geometry'][0]
    bone_names={b['name'] for b in geo['bones']}
    for name,a in anim.items():
        assert set(a['bones'])<=bone_names,name
        for channels in a['bones'].values():
            for frames in channels.values():
                # Clips are keyed at 24 fps: a closing key may sit under one game tick (0.05 s) past
                # animation_length (e.g. 2.4167 s in a 48-tick clip); GeckoLib simply ends the clip there.
                assert all(0<=float(t)<=a['animation_length']+0.05 for t in frames),(name,frames.keys())
    # Every clip RemnantClips.HOLLOW_SOVEREIGN lists must exist in the animation file with the same length in ticks.
    clips_java=(common/'java/com/nightbeam/remnants/entity/anim/RemnantClips.java').read_text()
    sheet=re.search(r'HOLLOW_SOVEREIGN = new Sheet\("hollow_sovereign"\)(.*?);',clips_java,re.S).group(1)
    durations={name:int(ticks) for _,name,ticks in re.findall(r'\.(loop|once|hold)\("([a-z_]+)", (\d+)\)',sheet)}
    assert {'idle','walk','run','awaken','combo','throne_slam','guard','riposte','rift_cleave','summon','spectral_slash',
            'soul_lunge','last_command','phase_shatter','phase_soul','stagger','death'}==set(durations),durations
    for clip,ticks in durations.items():assert abs(anim['animation.hollow_sovereign.'+clip]['animation_length']*20-ticks)<.01,clip
    assert 'setBlock' not in entity and 'level().explode' not in entity
    model=(common/'java/com/nightbeam/remnants/client/model/HollowSovereignModel.java').read_text()
    for path in re.findall(r'"((?:geo|textures|animations)/[^"]+)"',model):
        assert (common/'resources/assets/remnant_bosses'/path).exists(),path
    lang=json.loads((common/'resources/assets/remnant_bosses/lang/en_us.json').read_text())
    assert lang['entity.remnants.hollow_sovereign']=='Hollow Sovereign'
    configs=(common/'java/com/nightbeam/remnants/config/SovereignConfig.java').read_text()
    pauldron=(common/'java/com/nightbeam/remnants/entity/SovereignPauldronEntity.java').read_text()
    used=set(re.findall(r'SovereignConfig.value\("([^"]+)"',entity+pauldron+(common/'java/com/nightbeam/remnants/event/SovereignRitual.java').read_text()))
    used|=set(re.findall(r'\bcfg\("([^"]+)"',entity))
    defaults=set(re.findall(r'Map.entry\("([^"]+)"',configs))
    assert used<=defaults,(used-defaults)
    for loader in loaders:
        code='\n'.join(p.read_text() for p in (base/loader/'src/main/java').rglob('*.java'))
        assert 'HollowSovereignEntity.createAttributes()' in code
        assert 'HollowSovereignRenderer::new' in code
        assert 'HOLLOW_SOVEREIGN_SPAWN_EGG' in code
        candidates=[p for p in (base/loader/'build/libs').glob('*.jar') if not any(s in p.name for s in ['-sources','-javadoc','-dev','-shadow'])]
        jar=max(candidates,key=lambda p:p.stat().st_mtime)
        with zipfile.ZipFile(jar) as z:
            for suffix in ['entity/HollowSovereignEntity.class','event/SovereignRitual.class','client/RemnantBossBars.class','mixin/BossHealthOverlayMixin.class']:
                assert 'com/nightbeam/remnants/'+suffix in z.namelist(),(jar,suffix)
            mixins_cfg=json.loads(z.read('remnant_bosses.mixins.json'))
            assert 'BossHealthOverlayMixin' in mixins_cfg['client']
            folder='loot_tables' if version=='1.20.1' else 'loot_table'
            assert f'data/remnants/{folder}/entities/hollow_sovereign.json' in z.namelist()
            if loader=='fabric':
                assert mixins_cfg.get('refmap')=='remnant_bosses.refmap.json',(jar,mixins_cfg)
                assert 'remnant_bosses.refmap.json' in z.namelist(),jar
                ref=json.loads(z.read('remnant_bosses.refmap.json'))['mappings']['com/nightbeam/remnants/mixin/BossHealthOverlayMixin']
                assert any('method_1796' in v for v in ref.values()),ref
            if loader=='forge':
                ref=json.loads(z.read('remnant_bosses.refmap.json'))['mappings']['com/nightbeam/remnants/mixin/BossHealthOverlayMixin']
                assert any('f_93699_' in v for v in ref.values()),ref
                assert any('m_280106_' in v for v in ref.values()),ref
            # The save-migration alias mixins must resolve in production names.
            assert {'EntityTypeAliasMixin','ItemStackAliasMixin'}<=set(mixins_cfg['mixins']),(jar,mixins_cfg)
            if loader in ('fabric','forge'):
                refs=json.loads(z.read('remnant_bosses.refmap.json'))['mappings']
                fabric=loader=='fabric'
                targets={'com/nightbeam/remnants/mixin/EntityTypeAliasMixin':['method_17684' if fabric else 'm_20637_'],
                         'com/nightbeam/remnants/mixin/ItemStackAliasMixin':(['method_7915'] if version=='1.20.1' else ['method_57360','method_57359']) if fabric else ['m_41712_']}
                if fabric:
                    assert 'MappedRegistryAliasMixin' in json.loads(z.read('remnant_bosses.fabric.mixins.json'))['mixins'],jar
                    targets['com/nightbeam/remnants/fabric/mixin/MappedRegistryAliasMixin']=['method_10223','method_29107','method_40264']+([] if version=='1.20.1' else ['method_55841'])
                for mixin,names in targets.items():
                    for name in names: assert any(name in v for v in refs.get(mixin,{}).values()),(jar,mixin,name)
            print('PASS',jar.relative_to(root))
    assets.append({p.name:p.read_bytes() for p in (common/'resources/assets/remnant_bosses').rglob('hollow_sovereign*')})
assert assets[0]==assets[1],'Cross-version asset drift'
print('PASS animation contracts, config coverage, resource references, all loader packages, and identical assets')

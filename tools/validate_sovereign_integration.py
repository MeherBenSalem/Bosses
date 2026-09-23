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
                assert all(0<=float(t)<=a['animation_length'] for t in frames)
    for clip in ['awaken','enrage','core_blast','ground_slam','claw_combo','roar','walk','idle','death']:
        assert 'animation.hollow_sovereign.'+clip in anim
    durations={'awaken':60,'enrage':60,'core_blast':52,'ground_slam':44,'claw_combo':36,'roar':40,'death':70}
    for clip,ticks in durations.items():assert abs(anim['animation.hollow_sovereign.'+clip]['animation_length']*20-ticks)<.01
    assert 'setBlock' not in entity and 'level().explode' not in entity
    model=(common/'java/com/nightbeam/remnants/client/model/HollowSovereignModel.java').read_text()
    for path in re.findall(r'"((?:geo|textures|animations)/[^"]+)"',model):
        assert (common/'resources/assets/remnant_bosses'/path).exists(),path
    lang=json.loads((common/'resources/assets/remnant_bosses/lang/en_us.json').read_text())
    assert lang['entity.remnants.hollow_sovereign']=='Hollow Sovereign'
    configs=(common/'java/com/nightbeam/remnants/config/SovereignConfig.java').read_text()
    used=set(re.findall(r'SovereignConfig.value\("([^"]+)"',entity+(common/'java/com/nightbeam/remnants/event/SovereignRitual.java').read_text()))
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
            print('PASS',jar.relative_to(root))
    assets.append({p.name:p.read_bytes() for p in (common/'resources/assets/remnant_bosses').rglob('hollow_sovereign*')})
assert assets[0]==assets[1],'Cross-version asset drift'
print('PASS animation contracts, config coverage, resource references, all loader packages, and identical assets')

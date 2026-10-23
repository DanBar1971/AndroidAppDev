from pathlib import Path
root=Path('build-src/MediAgenda_Android_v1')
src=root/'app/src/main/java/com/mediagenda/app'
patch=Path('MediAgenda/v61_overrides')
for rel in ('MainActivity.kt','data/ProfileStore.kt',
            'data/ProfileDeletionStore.kt','sync/NearbySyncActivity.kt'):
    output=src/rel
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text((patch/rel).read_text(encoding='utf-8'),encoding='utf-8')
gradle=root/'app/build.gradle.kts'
s=gradle.read_text(encoding='utf-8')
assert s.count('versionCode = 51')==1 and s.count('versionName = "1.9.41"')==1
s=s.replace('versionCode = 51','versionCode = 52',1)
s=s.replace('versionName = "1.9.41"','versionName = "1.9.42"',1)
gradle.write_text(s,encoding='utf-8')
print('v1.9.42: individual profile/data deletion; polished bidirectional sync UI; FINE permission and refresh')
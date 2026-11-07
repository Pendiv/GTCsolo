from pathlib import Path
import hashlib
import json
import shutil
import zipfile
from verify_resources import verify

project = Path(__file__).resolve().parents[1]
root = project.parents[1]
dist = root / 'dist/tiered-torcherino'
dist.mkdir(parents=True, exist_ok=True)
jar = project / 'build/libs/gtcsolo-tiered-torcherino-0.1.0.jar'
dev = project / 'build/libs/gtcsolo-tiered-torcherino-0.1.0-dev.jar'
resources = project / 'src/main/resources'
verify(resources)
dependency = project / 'libs/Torcherino-forge-1.20.1-20.0.0.jar'
with zipfile.ZipFile(jar) as archive, zipfile.ZipFile(dependency) as native:
    names = archive.namelist()
    assert len(names) == len(set(names))
    assert not any(name.startswith('torcherino/') or '/smoke/' in name for name in names)
    assert not any(name.endswith('.jar') for name in names)
    for asset in resources.rglob('*'):
        if asset.is_file():
            relative = asset.relative_to(resources).as_posix()
            assert archive.read(relative) == asset.read_bytes(), relative
            if relative.startswith('assets/') and asset.suffix == '.json':
                data = json.loads(asset.read_text(encoding='utf-8'))
                parent = data.get('parent', '')
                if parent.startswith('torcherino:'):
                    assert ('assets/torcherino/models/' + parent.split(':')[1] + '.json') in native.namelist(), parent
    refmap = json.loads(archive.read('gtcsolo_torcherino.refmap.json'))
    assert 'BlockEntityTypeAccess' in str(refmap) and 'SliderMixin' in str(refmap)
for artifact in [jar, dev, project / 'README-ja.md', project / 'LICENSE']:
    shutil.copy2(artifact, dist / artifact.name)
source_zip = dist / 'gtcsolo-tiered-torcherino-0.1.0-source.zip'
with zipfile.ZipFile(source_zip, 'w', zipfile.ZIP_DEFLATED) as archive:
    files = [project / name for name in ['build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat', 'LICENSE', 'README-ja.md', '.gitignore']]
    files += [path for base in ['src', 'tools', 'gradle/wrapper'] for path in (project / base).rglob('*')
              if path.is_file() and '__pycache__' not in path.parts]
    for path in sorted(files): archive.write(path, 'tiered-torcherino/' + path.relative_to(project).as_posix())
    assert not any('/libs/' in name or '/run/' in name or '/build/' in name for name in archive.namelist())
hashes = {path.name: {'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'bytes': path.stat().st_size}
          for path in [dist / jar.name, dist / dev.name, source_zip]}
(dist / 'SHA256.json').write_text(json.dumps(hashes, indent=2) + '\n', encoding='utf-8')
print(json.dumps(hashes, indent=2))

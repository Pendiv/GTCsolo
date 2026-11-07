"""Check all added forms, own resource references, and exact user-artwork hashes."""
from pathlib import Path
import hashlib
import json
import struct

NS = 'gtcsolo_torcherino'
TIERS = ['lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv', 'uev', 'uiv', 'uxv', 'opv', 'max']


def verify(resources):
    project = Path(__file__).resolve().parents[1]
    root = resources / 'assets' / NS
    manifest = json.loads((project / 'tools/artwork-manifest.json').read_text(encoding='utf-8'))
    pngs = {p.relative_to(root).as_posix() for p in root.rglob('*.png')}
    assert pngs == {name for name in manifest if name.endswith('.png')}
    for name, expected in manifest.items():
        path = root / name
        assert hashlib.sha256(path.read_bytes()).hexdigest() == expected, name
        if name.endswith('.png.mcmeta'):
            png = path.with_suffix('').read_bytes()
            width, height = struct.unpack('>II', png[16:24])
            animation = json.loads(path.read_text(encoding='utf-8'))['animation']
            w, h = animation.get('width', width), animation.get('height', width)
            assert width % w == height % h == 0, name
            count = width // w * (height // h)
            assert all(0 <= (frame['index'] if isinstance(frame, dict) else frame) < count for frame in animation.get('frames', [])), name
    locales = [json.loads((root / 'lang' / (lang + '.json')).read_text(encoding='utf-8')) for lang in ['ja_jp', 'en_us']]
    for tier in TIERS:
        for shape in ['torcherino', 'lanterino', 'jack_o_lanterino']:
            name = tier + '_' + shape
            assert all('block.' + NS + '.' + name in locale for locale in locales), name
            for relative in ['blockstates/' + name, 'models/item/' + name, 'models/block/' + name]:
                assert (root / (relative + '.json')).is_file(), relative
            loot = json.loads((resources / 'data' / NS / 'loot_tables/blocks' / (name + '.json')).read_text(encoding='utf-8'))
            assert loot['pools'][0]['entries'][0]['name'] == NS + ':' + name
    for path in root.rglob('*.json'):
        data = json.loads(path.read_text(encoding='utf-8'))
        if 'models' in path.relative_to(root).parts:
            for key, value in [('model', data.get('parent', ''))] + [('texture', v) for v in data.get('textures', {}).values()]:
                if value.startswith(NS + ':'):
                    base, extension = ('models', '.json') if key == 'model' else ('textures', '.png')
                    assert (root / base / (value.split(':', 1)[1] + extension)).is_file(), (path, value)
        if path.parent.name == 'blockstates':
            def visit(value):
                if isinstance(value, dict):
                    model = value.get('model', '')
                    if model.startswith(NS + ':'):
                        assert (root / 'models' / (model.split(':', 1)[1] + '.json')).is_file(), (path, model)
                    for child in value.values(): visit(child)
                elif isinstance(value, list):
                    for child in value: visit(child)
            visit(data)
    print('Resource verification PASS: 42 items, models/loot/languages, 84 user PNG hashes, 14 animations')


if __name__ == '__main__':
    verify(Path(__file__).resolve().parents[1] / 'src/main/resources')

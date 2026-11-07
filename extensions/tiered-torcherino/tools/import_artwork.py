"""Import the user's voltage artwork; keep the acceleration add-on self-contained."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil

PROJECT = Path(__file__).resolve().parents[1]
NAMESPACE = 'gtcsolo_torcherino'
TIERS = ['lv', 'mv', 'hv', 'ev', 'iv', 'luv', 'zpm', 'uv', 'uhv', 'uev', 'uiv', 'uxv', 'opv', 'max']


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('torch_assets', type=Path, help='Artism torch-output or combined output directory')
    parser.add_argument('lamp_assets', type=Path, help='Artism combined lantern/pumpkin output directory')
    args = parser.parse_args()
    target = PROJECT / 'src/main/resources/assets' / NAMESPACE
    manifest = {}

    def write(relative, data):
        path = target / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

    def model(source, relative):
        data = json.loads(source.read_text(encoding='utf-8').replace('gtcsolo:', NAMESPACE + ':'))
        write(relative, data)

    for source_dir, kinds in [(args.torch_assets, ['block/voltage_torch']),
                              (args.lamp_assets, ['block/voltage_lantern', 'item/voltage_lantern', 'block/voltage_jack_o_lantern'])]:
        for kind in kinds:
            source = source_dir / 'assets/gtcsolo/textures' / kind
            files = sorted(path for path in source.rglob('*') if path.is_file() and (path.suffix == '.png' or path.name.endswith('.png.mcmeta')))
            if not files:
                raise ValueError('Missing artwork: ' + str(source))
            for path in files:
                relative = Path('textures') / kind / path.relative_to(source)
                dest = target / relative
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(path, dest)
                manifest[relative.as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()

    for tier in TIERS:
        texture = NAMESPACE + ':block/voltage_torch/' + tier
        write('models/block/' + tier + '_torcherino.json', {
            'parent': 'minecraft:block/template_torch', 'textures': {'torch': texture}, 'render_type': 'minecraft:cutout'})
        write('models/block/wall_' + tier + '_torcherino.json', {
            'parent': 'minecraft:block/template_torch_wall', 'textures': {'torch': texture}, 'render_type': 'minecraft:cutout'})
        # Generated item transforms hold the handle; block models use a different origin.
        write('models/item/' + tier + '_torcherino.json', {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': texture}})
        for shape, stem in [('lanterino', 'voltage_lantern'), ('jack_o_lanterino', 'voltage_jack_o_lantern')]:
            name = tier + '_' + shape
            source = args.lamp_assets / 'assets/gtcsolo'
            for model_type in ['block', 'item']:
                relative = 'models/' + model_type + '/' + name + '.json'
                data = json.loads((source / ('models/' + model_type + '/' + stem + '_' + tier + '.json')).read_text(encoding='utf-8'))
                text = json.dumps(data).replace('gtcsolo:', NAMESPACE + ':').replace('block/' + stem + '_' + tier, 'block/' + name)
                write(relative, json.loads(text))
            if shape == 'lanterino':
                model(source / ('models/block/' + stem + '_' + tier + '_hanging.json'), 'models/block/' + name + '_hanging.json')
            data = (source / ('blockstates/' + stem + '_' + tier + '.json')).read_text(encoding='utf-8')
            data = data.replace('gtcsolo:', NAMESPACE + ':').replace('block/' + stem + '_' + tier, 'block/' + name)
            write('blockstates/' + name + '.json', json.loads(data))
            loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': NAMESPACE + ':' + name}],
                                                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}]}
            path = PROJECT / 'src/main/resources/data' / NAMESPACE / 'loot_tables/blocks' / (name + '.json')
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(loot, indent=2) + '\n', encoding='utf-8')

    assert len([path for path in manifest if path.endswith('.png')]) == 84
    assert len([path for path in manifest if path.endswith('.png.mcmeta')]) == 14
    (PROJECT / 'tools/artwork-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf-8')
    print('Imported 84 user PNGs and 14 animation metadata files; generated models for 42 items.')


if __name__ == '__main__':
    main()

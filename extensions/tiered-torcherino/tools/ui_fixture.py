"""Drive only the private smoke world's normal GUI; no developer commands."""
from pathlib import Path
from urllib.request import Request, urlopen
import json
import re
import uuid
import base64

PROJECT = Path(__file__).resolve().parents[1]
description = (PROJECT / 'run/synapse/AGENT.md').read_text(encoding='utf-8')
BASE = re.search(r'Base URL: `(http://127\.0\.0\.1:\d+)`', description)[1]
credential = Path(re.search(r'instance credential file ([^\r\n]+?\.token)', description)[1])
assert credential.resolve().is_relative_to((Path.home() / '.synapse/credentials').resolve())
TOKEN = credential.read_text(encoding='utf-8').strip()

def call(path, body=None):
    headers = {'X-Synapse-Token': TOKEN}
    if body is not None:
        headers.update({'Content-Type': 'application/json', 'X-Synapse-Request-Id': str(uuid.uuid4())})
    with urlopen(Request(BASE + path, data=None if body is None else json.dumps(body).encode(), headers=headers), timeout=15) as response:
        result = json.load(response)
    if not result['ok']: raise RuntimeError(result.get('error'))
    return result['data']

def gui(): return call('/gui')

def action(body):
    snapshot = gui()
    return call('/gui', dict(body, expectedScreenRevision=snapshot['screenRevision']))

def screenshot(name):
    data = call('/screenshot')
    path = PROJECT / 'run/synapse' / name
    path.write_bytes(base64.b64decode(data['image'].split(',')[-1]))
    return str(path)

def click_slider(widget, fraction):
    x = widget['x'] + 4 + fraction * (widget['w'] - 8)
    y = widget['y'] + widget['h'] / 2
    action({'action': 'click', 'x': x, 'y': y, 'button': 0})
    action({'action': 'mouseReleased', 'x': x, 'y': y, 'button': 0})

if __name__ == '__main__':
    import sys
    call('/manifest')
    current = gui()
    if len(sys.argv) > 1 and sys.argv[1] == 'enter':
        if current['screen'] == 'TitleScreen':
            widget = next(w for w in current['widgets'] if w['label'] in ['Singleplayer', '\u30b7\u30f3\u30b0\u30eb\u30d7\u30ec\u30a4'])
            action({'action': 'clickButton', 'index': widget['index']})
            current = gui()
        if current['screen'] == 'SelectWorldScreen':
            matches = [r for r in current['lists'][0]['rows'] if 'Tiered Torcherino UI test' in r['label']]
            if matches: action({'action': 'activateRow', 'listIndex': 0, 'row': matches[0]['index'], 'async': True})
    print(json.dumps(gui(), ensure_ascii=True)[:6000])

import datetime
import json
import os
import subprocess
import sys
import tempfile
import urllib.error
import urllib.request
import zipfile

REPO = 'shiro-ka/kagari'
TAG = 'firefox-kagari'
TITLE = 'Firefox'
ADDON_ID = 'kagari@lab49'
WEB = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
API = 'https://api.github.com/repos/' + REPO
DOWNLOAD = 'https://github.com/' + REPO + '/releases/download/' + TAG + '/'
NODE_BIN = os.path.expanduser('~/App/node/bin')


def api(method, url, data=None, content_type='application/json'):
    req = urllib.request.Request(url, data=data, method=method, headers={
        'Authorization': 'Bearer ' + os.environ['GH_TOKEN'],
        'Accept': 'application/vnd.github+json',
        'Content-Type': content_type,
    })
    with urllib.request.urlopen(req) as res:
        body = res.read()
        return json.loads(body) if body else None


def node_env():
    return dict(os.environ, NODE_USE_ENV_PROXY='1', PATH=NODE_BIN + ':' + os.environ.get('PATH', ''))


def stamp(base):
    now = datetime.datetime.now()
    return '%s.%s.%d' % (base, now.strftime('%Y%m%d'), int(now.strftime('%H%M')))


def build(version):
    subprocess.run(['node', 'scripts/build-extension.mjs'], cwd=WEB, env=node_env(), check=True, stdout=sys.stderr)
    work = tempfile.mkdtemp()
    src = os.path.join(work, 'src')
    build_dir = os.path.join(WEB, 'build')
    for root, _dirs, files in os.walk(build_dir):
        for name in files:
            rel = os.path.relpath(os.path.join(root, name), build_dir)
            dest = os.path.join(src, rel)
            os.makedirs(os.path.dirname(dest), exist_ok=True)
            with open(os.path.join(root, name), 'rb') as f:
                data = f.read()
            if rel == 'manifest.json':
                manifest = json.loads(data)
                manifest['version'] = version
                data = (json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode()
            with open(dest, 'wb') as f:
                f.write(data)
    return src


def sign(src):
    out = tempfile.mkdtemp()
    subprocess.run(['npx', '-y', 'web-ext@8', 'sign', '--channel=unlisted', '--source-dir', src, '--artifacts-dir', out,
                    '--approval-timeout', '900000'], check=True, env=node_env(), stdout=sys.stderr)
    signed = [f for f in os.listdir(out) if f.endswith('.xpi')]
    if not signed:
        raise SystemExit('署名した xpi が見つからない')
    return os.path.join(out, signed[0])


def release():
    try:
        return api('GET', API + '/releases/tags/' + TAG)
    except urllib.error.HTTPError as e:
        if e.code != 404:
            raise
    return api('POST', API + '/releases', json.dumps({
        'tag_name': TAG, 'target_commitish': 'dev', 'name': TITLE, 'prerelease': True, 'body': ''
    }).encode())


def upload(rel, name, data, content_type):
    url = rel['upload_url'].split('{')[0] + '?name=' + name
    api('POST', url, data, content_type)


def main():
    with open(os.path.join(WEB, 'extension', 'manifest.json')) as f:
        version = stamp(json.load(f)['version'])
    xpi = sign(build(version))
    name = 'kagari-%s.xpi' % version
    updates = {'addons': {ADDON_ID: {'updates': [{'version': version, 'update_link': DOWNLOAD + name}]}}}

    rel = release()
    for asset in rel.get('assets', []):
        api('DELETE', API + '/releases/assets/%d' % asset['id'])
    with open(xpi, 'rb') as f:
        upload(rel, name, f.read(), 'application/x-xpinstall')
    upload(rel, 'updates.json', json.dumps(updates, indent=2).encode(), 'application/json')
    print(version, DOWNLOAD + name)


if __name__ == '__main__':
    sys.exit(main())

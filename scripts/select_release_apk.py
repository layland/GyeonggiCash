"""Select only GyeonggiCash APKs from the shared signing build repository."""
import json
import os
import re
import subprocess

BUILD_REPOSITORY = 'layland/MyParkingAndroid'
TAG = re.compile(r'gyeonggicash-v0\.1\.(\d+)')


def api(path):
    return json.loads(subprocess.check_output(['gh', 'api', path], text=True))


def releases(repo):
    page = 1
    while True:
        batch = api(f'repos/{repo}/releases?per_page=100&page={page}')
        yield from batch
        if len(batch) < 100:
            return
        page += 1


def select(builds, published):
    candidates = [r for r in builds if not r['draft'] and TAG.fullmatch(r['tag_name'])]
    if not candidates:
        return {'skip': 'true'}
    release = max(candidates, key=lambda r: int(TAG.fullmatch(r['tag_name'])[1]))
    version = release['tag_name'].removeprefix('gyeonggicash-v')
    name = f'GyeonggiCash-{version}.apk'
    tag = f'v{version}'
    for existing in published:
        if existing['tag_name'] == tag and not existing['draft'] and any(
                a['name'] == name and a['state'] == 'uploaded' for a in existing['assets']):
            return {'skip': 'true'}
    asset = next(a for a in release['assets'] if a['name'] == name and a['state'] == 'uploaded')
    digest = asset.get('digest') or ''
    if not re.fullmatch(r'sha256:[0-9a-f]{64}', digest):
        raise ValueError('Missing or invalid APK SHA-256 digest')
    if not asset['browser_download_url'].startswith(f'https://github.com/{BUILD_REPOSITORY}/releases/download/'):
        raise ValueError('Unexpected APK source')
    return {'skip': 'false', 'version': version, 'tag': tag, 'name': name,
            'url': asset['browser_download_url'], 'digest': digest,
            'release_url': release['html_url']}


if __name__ == '__main__':
    result = select(releases(BUILD_REPOSITORY), list(releases(os.environ['GITHUB_REPOSITORY'])))
    for key, value in result.items():
        if '\n' in value or '\r' in value:
            raise ValueError('Invalid workflow output')
        print(f'{key}={value}')

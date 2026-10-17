import unittest
from select_release_apk import select


def release(number):
    name = f'GyeonggiCash-0.1.{number}.apk'
    return {'draft': False, 'tag_name': f'gyeonggicash-v0.1.{number}',
            'html_url': 'https://github.com/layland/MyParkingAndroid/releases/test',
            'assets': [{'name': name, 'state': 'uploaded', 'digest': 'sha256:' + 'a' * 64,
                        'browser_download_url': 'https://github.com/layland/MyParkingAndroid/releases/download/test/' + name}]}


class SelectionTest(unittest.TestCase):
    def test_numeric_order_and_other_apps(self):
        other = dict(release(99), tag_name='myexercise-v0.1.99')
        self.assertEqual(select([release(2), other, release(10)], [])['version'], '0.1.10')

    def test_already_published(self):
        existing = dict(release(1), tag_name='v0.1.1')
        self.assertEqual(select([release(1)], [existing]), {'skip': 'true'})

    def test_missing_digest_rejected(self):
        r = release(1)
        r['assets'][0]['digest'] = None
        with self.assertRaises(ValueError):
            select([r], [])

    def test_no_builds(self):
        self.assertEqual(select([], []), {'skip': 'true'})

    def test_wrong_source_rejected(self):
        r = release(1)
        r['assets'][0]['browser_download_url'] = 'https://example.com/app.apk'
        with self.assertRaises(ValueError):
            select([r], [])


if __name__ == '__main__':
    unittest.main()

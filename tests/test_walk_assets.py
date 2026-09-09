"""Offline visual invariants for the grounded walk (Pillow/NumPy, steel)."""
from pathlib import Path
import json
import unittest
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]


class WalkAssetsTest(unittest.TestCase):
    def test_common_transform(self):
        manifest = json.loads((ROOT / 'assets/skins/dottore_fox/walk_v2/manifest.json').read_text())
        self.assertEqual(len(manifest['frames']), 9)
        self.assertEqual(len({f['scale'] for f in manifest['frames']}), 1)
        self.assertTrue(all(f['foot_y'] == 128 for f in manifest['frames']))

    def test_contact_and_unique_frames(self):
        names = ['fox_stand.png', 'shime11.png'] + [f'fox_walk_{i:02d}.png' for i in range(1, 9)]
        frames = []
        for name in names:
            with self.subTest(name=name):
                a = np.array(Image.open(ROOT / 'img' / name).convert('RGBA'))
                self.assertEqual(a.shape, (128, 128, 4))
                # Dark paw contacts must reach within two pixels of the actual anchor.
                dark = (a[:, :, :3].max(axis=2) < 115) & (a[:, :, 3] > 180)
                # Tail occupies rightmost region; only inspect the paw-bearing body.
                self.assertGreaterEqual(int(dark[-2:, :85].sum()), 4, name + ' paws floating')
                self.assertEqual((ROOT/'img'/name).read_bytes(), (ROOT/'dist/img'/name).read_bytes())
                frames.append(a.tobytes())
        self.assertEqual(len(set(frames[2:])), 8)


if __name__ == '__main__':
    unittest.main()

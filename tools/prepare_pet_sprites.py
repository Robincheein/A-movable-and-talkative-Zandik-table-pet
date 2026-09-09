"""Normalize approved head-pat artwork; preserve originals, align ground paws.

Run with conda run -n steel python tools/prepare_pet_sprites.py.
Local background removal was explicitly approved by the user.
"""
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw
from prepare_animal_sprites import cutout, ROOT

SOURCE = ROOT / 'assets/concepts/dottore_pet_moods_v2'
DEST = ROOT / 'assets/skins/dottore_fox/pet_moods'
NAMES = ('approach', 'normal_refuse', 'angry_block', 'good_mood_allow', 'withdraw')
EXTRA = ('rest', 'near', 'half_raise', 'mid_push', 'lower_paw', 'stroke_left', 'stroke_right')


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    manifest = {'canvas': [128, 128], 'anchor': [64, 128], 'scale': .085, 'poses': {}}
    sprites = {}
    for name in NAMES + EXTRA:
        folder = ROOT / 'assets/concepts/dottore_pet_moods_v3' if name in EXTRA else SOURCE
        source = folder / (name + '.png')
        image, bounds = cutout(source)
        rgba = np.array(image)
        yy, xx = np.indices(rgba.shape[:2])
        # Only the grounded paws under the torso, never the hanging tail tip.
        paws = ((rgba[:, :, :3].max(axis=2) < 115) & (rgba[:, :, 3] > 240)
                & (yy > image.height * .8) & (xx > image.width * .24)
                & (xx < image.width * .61))
        if not paws.any():
            raise ValueError('Ground paw anchor missing: ' + name)
        foot = int(yy[paws].max()) + 1
        foot_x = xx[paws & (yy >= foot - 18)]
        pivot_x = (int(foot_x.min()) + int(foot_x.max())) / 2
        scale = manifest['scale']
        size = (round(image.width * scale), round(image.height * scale))
        small = image.resize(size, Image.Resampling.LANCZOS)
        sprite = Image.new('RGBA', (128, 128))
        # Match the seated sprite's torso/paw center, not the tail-inclusive bbox.
        sprite.alpha_composite(small, (56 - round(pivot_x * scale), 128 - round(foot * scale)))
        image.save(DEST / (name + '_cutout.png'))
        sprite.save(DEST / (name + '.png'))
        for folder in (ROOT / 'img', ROOT / 'dist/img'):
            folder.mkdir(parents=True, exist_ok=True)
            sprite.save(folder / ('fox_pet_' + name + '.png'))
        sprites[name] = sprite
        manifest['poses'][name] = {'source': source.relative_to(ROOT).as_posix(),
                                  'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                                  'source_foot_y': foot, 'source_paw_center_x': pivot_x,
                                  'foreground_bounds': bounds}
    (DEST / 'manifest.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    active_path = ROOT / 'assets/skins/dottore_fox/manifest.json'
    if active_path.exists():
        active = json.loads(active_path.read_text(encoding='utf-8'))
        for name in NAMES + EXTRA:
            active['runtime_mapping']['fox_pet_' + name + '.png'] = 'pet_moods/' + name
        active['head_pat'] = {'drawn_interaction_poses': 12, 'idle_sprite': 'fox_pet_rest.png',
                              'reactions': ['refuse', 'angry', 'allow'], 'local_patience': True}
        active_path.write_text(json.dumps(active, ensure_ascii=False, indent=2), encoding='utf-8')
    sheet = Image.new('RGB', (4 * 256, 3 * 290), '#23394d')
    draw = ImageDraw.Draw(sheet)
    for index, (name, sprite) in enumerate(sprites.items()):
        x, y = index % 4 * 256, index // 4 * 290
        sheet.paste(sprite.resize((256, 256), Image.Resampling.NEAREST), (x, y),
                    sprite.resize((256, 256), Image.Resampling.NEAREST))
        draw.line((x, y + 255, x + 256, y + 255), fill='#d29572')
        draw.text((x + 8, y + 266), name, fill='white')
    sheet.save(DEST / 'preview.png')
    print('PET_SPRITES_OK: twelve transparent poses, shared scale and ground anchors')


if __name__ == '__main__':
    main()

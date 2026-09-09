"""Extract the new, jointly drawn standing + eight-frame atlas. Offline; steel env."""
import json
import hashlib
import shutil
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage
from prepare_animal_sprites import cutout, ROOT

DEST = ROOT / 'assets/skins/dottore_fox/walk_v2'


def main():
    source = ROOT / 'assets/concepts/dottore_walk_v2/atlas.png'
    atlas, _ = cutout(source)
    alpha = np.array(atlas.getchannel('A'))
    labels, _ = ndimage.label(alpha > 128)
    components = ndimage.find_objects(labels)
    counts = np.bincount(labels.ravel())
    boxes = []
    for i, slices in enumerate(components, 1):
        if slices and counts[i] > 6000:
            y, x = slices
            boxes.append((x.start, y.start, x.stop, y.stop))
    if len(boxes) != 9:
        raise ValueError(f'Expected 9 complete connected characters, got {len(boxes)}')
    boxes.sort(key=lambda b: (b[1] + b[3]) / 2)
    boxes = sum([sorted(boxes[i:i+3], key=lambda b: b[0]) for i in range(0, 9, 3)], [])
    scale = min(118 / max(b[2]-b[0] for b in boxes), 118 / max(b[3]-b[1] for b in boxes))
    DEST.mkdir(parents=True, exist_ok=True)
    frames = []
    records = []
    for i, box in enumerate(boxes):
        cropped = atlas.crop(box)
        size = (round(cropped.width*scale), round(cropped.height*scale))
        small = cropped.resize(size, Image.Resampling.LANCZOS)
        # Feet are the lowest points of this atlas. Shared scale, no independent fit.
        image = Image.new('RGBA', (128, 128))
        image.alpha_composite(small, ((128-size[0])//2, 128-size[1]))
        name = 'fox_stand.png' if i == 0 else f'fox_walk_{i:02d}.png'
        image.save(DEST / name)
        for directory in ('img', 'dist/img'):
            shutil.copyfile(DEST / name, ROOT / directory / name)
        frames.append(image)
        records.append({'file': name, 'source_bounds': list(box), 'scale': scale, 'foot_y': 128})
    for directory in ('img', 'dist/img'):
        # Standing and walking now share head/body proportions instead of switching
        # from a giant seated head to a much smaller walking head.
        for slot, frame in {1: 0, 2: 1, 3: 3, 34: 1, 35: 3, 36: 5}.items():
            frames[frame].save(ROOT / directory / f'shime{slot}.png')
    seated, seat_box = cutout(ROOT / 'assets/concepts/dottore_walk_v2/sit_grounded.png')
    seated = seated.crop(seat_box)
    pixels = np.array(seated)
    yy, xx = np.indices(pixels.shape[:2])
    paws = ((pixels[:, :, :3].max(axis=2) < 115) & (pixels[:, :, 3] > 240)
            & (yy > seated.height*.75) & (xx < seated.width*.62))
    foot = int(np.where(paws)[0].max()) + 1
    seat_scale = 100 / foot
    seated = seated.resize((round(seated.width*seat_scale), round(seated.height*seat_scale)), Image.Resampling.LANCZOS)
    seat = Image.new('RGBA', (128, 128))
    seat.alpha_composite(seated, ((128-seated.width)//2, 28))
    seat.save(DEST / 'fox_sit.png')
    for directory in ('img', 'dist/img'):
        for slot in (11, 31):
            seat.save(ROOT / directory / f'shime{slot}.png')
    (DEST / 'manifest.json').write_text(json.dumps({'frames': records, 'frame_ms': 160,
        'movement_px_per_tick': 1, 'tick_ms': 40, 'speed_px_per_second': 25}, indent=2), encoding='utf8')
    active_manifest_path = DEST.parent / 'manifest.json'
    active = json.loads(active_manifest_path.read_text(encoding='utf8'))
    active['version'] = '2.0.0-animal.2'
    for i in range(1, 9):
        active['runtime_mapping'][f'fox_walk_{i:02d}.png'] = f'walk_v2/fox_walk_{i:02d}'
    active['runtime_mapping']['fox_stand.png'] = 'walk_v2/fox_stand'
    for slot, name in {1:'fox_stand', 2:'fox_walk_01', 3:'fox_walk_03', 34:'fox_walk_01',
                       35:'fox_walk_03', 36:'fox_walk_05', 11:'fox_sit', 31:'fox_sit'}.items():
        active['runtime_mapping'][f'shime{slot}.png'] = 'walk_v2/' + name
    active['walk'] = {'frame_count': 8, 'frame_ms': 160, 'speed_px_per_second': 25}
    for name in ('atlas', 'sit_grounded'):
        path = ROOT / 'assets/concepts/dottore_walk_v2' / (name + '.png')
        active['sources']['walk_v2_' + name] = {'path': path.relative_to(ROOT).as_posix(),
                                             'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}
    active_manifest_path.write_text(json.dumps(active, indent=2, ensure_ascii=False), encoding='utf8')
    preview = Image.new('RGB', (3*256, 3*288), '#182a3e')
    draw = ImageDraw.Draw(preview)
    for i, frame in enumerate(frames):
        x, y = (i%3)*256, (i//3)*288
        enlarged = frame.resize((256, 256), Image.Resampling.NEAREST)
        preview.paste(enlarged, (x, y), enlarged)
        draw.line((x, y+255, x+255, y+255), fill='#83c1ae')
        draw.text((x+12, y+265), 'STAND' if i == 0 else f'WALK {i}', fill='white')
    preview.save(DEST / 'contact_sheet.png')
    gif_frames = []
    for frame in frames[1:]:
        bg = Image.new('RGB', (384, 384), '#182a3e')
        enlarged = frame.resize((384, 384), Image.Resampling.LANCZOS)
        bg.paste(enlarged, (0, 0), enlarged)
        ImageDraw.Draw(bg).line((0, 383, 383, 383), fill='#83c1ae')
        gif_frames.append(bg)
    gif_frames[0].save(DEST / 'walk_preview.gif', save_all=True, append_images=gif_frames[1:], duration=160, loop=0)
    print('WALK_V2_OK: neutral stand + 8 newly drawn walk frames, common scale and paw baseline')


if __name__ == '__main__':
    main()

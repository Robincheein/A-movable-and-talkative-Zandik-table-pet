"""Prepare the user-approved white-background animal artwork, offline.

Run: conda run -n steel python tools/prepare_animal_sprites.py
The user explicitly approved local background removal. Original concepts stay intact.
"""
from pathlib import Path
import hashlib
import json
import shutil

import numpy as np
from PIL import Image, ImageDraw
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
CONCEPTS = ROOT / "assets/concepts"
DEST = ROOT / "assets/skins/dottore_fox"
SOURCES = {
    "idle": CONCEPTS / "dottore_fox_idle_20260908_white.png",
    **{name: CONCEPTS / "dottore_actions" / (source + ".png") for name, source in {
        "walk_01": "walk_01_contact", "walk_02": "walk_02_passing",
        "walk_03": "walk_03_opposite_contact", "walk_04": "walk_04_recovery",
        "paw": "interact_paw", "head_tilt": "interact_head_tilt",
        "sleep": "sleep_curled", "dragged": "dragged", "falling": "falling",
    }.items()},
}


def cutout(path):
    """Flood only near-white background connected to the canvas edge.

    Dark closed outlines protect pale/white fur. No global white color key.
    A one-source-pixel shrink removes the remaining background fringe before resizing.
    """
    rgb = np.array(Image.open(path).convert("RGB"))
    values = rgb.astype(np.int16)
    bg = np.median(np.concatenate([values[0], values[-1], values[:, 0], values[:, -1]]), axis=0)
    neutral = (np.max(np.abs(values - bg), axis=2) < 35)
    labels, _ = ndimage.label(neutral)
    exterior = np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))
    exterior = exterior[exterior != 0]
    remove = np.isin(labels, exterior)
    # Background gaps enclosed by curls: match their warm paper color very strictly.
    # Blue-white fur and the cool-white mask panel do not match this warm RGB test.
    warm_paper = ((values[:, :, 0] - values[:, :, 2] >= 2)
                  & (values[:, :, 0] >= values[:, :, 1])
                  & (values.min(axis=2) > 244)
                  & (np.max(np.abs(values - bg), axis=2) < 7))
    inner_labels, count = ndimage.label(neutral & ~remove)
    areas = np.bincount(inner_labels.ravel(), minlength=count + 1)
    warm_counts = np.bincount(inner_labels.ravel(), weights=warm_paper.ravel(), minlength=count + 1)
    discard = (areas > 50) & (areas < 18000) & (warm_counts / np.maximum(areas, 1) > .92)
    discard[0] = False
    remove |= discard[inner_labels]
    keep = ndimage.binary_erosion(~remove, iterations=1, border_value=0)
    alpha = (keep * 255).astype(np.uint8)
    rgba = np.dstack([rgb, alpha])
    rgba[~keep, :3] = 0
    result = Image.fromarray(rgba)
    bounds = result.getbbox()
    if not bounds or keep.mean() < .12 or keep.mean() > .85:
        raise ValueError(f"Unexpected foreground coverage: {path.name}")
    return result, bounds


def fit(image, bounds, scale=None):
    cropped = image.crop(bounds)
    if scale is None:
        scale = min(120 / cropped.width, 122 / cropped.height)
    size = (round(cropped.width * scale), round(cropped.height * scale))
    small = cropped.resize(size, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (128, 128))
    # All ground poses share the same bottom; no old seated-pose vertical offsets.
    canvas.alpha_composite(small, ((128 - size[0]) // 2, 127 - size[1]))
    return canvas


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    extracted = {name: cutout(path) for name, path in SOURCES.items()}
    walking = [extracted[f"walk_0{i}"][1] for i in range(1, 5)]
    walk_scale = min(120 / max(b[2] - b[0] for b in walking),
                     122 / max(b[3] - b[1] for b in walking))
    sprites = {}
    for name, (image, bounds) in extracted.items():
        image.save(DEST / (name + "_cutout.png"))
        sprites[name] = fit(image, bounds, walk_scale if name.startswith("walk_") else None)
        sprites[name].save(DEST / (name + ".png"))
    # Every legacy slot is animal artwork, including currently inactive slots.
    mapping = {i: "idle" for i in range(1, 47)}
    groups = {
        "walk_01": [2, 34], "walk_02": [3, 35], "walk_03": [36],
        "falling": [4, 22], "dragged": list(range(5, 11)) + [12, 13, 14, 23, 24, 25],
        "head_tilt": [15, 16, 17, 26, 27, 28, 29],
        "sleep": [18, 19, 20, 21, 30], "paw": [32, 33, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46],
    }
    for name, slots in groups.items():
        for slot in slots:
            mapping[slot] = name
    runtime = {f"shime{i}.png": name for i, name in mapping.items()}
    runtime.update({f"fox_walk_0{i}.png": f"walk_0{i}" for i in range(1, 5)})
    runtime["icon.png"] = "idle"
    for directory in (ROOT / "img", ROOT / "dist/img"):
        directory.mkdir(parents=True, exist_ok=True)
        for filename, name in runtime.items():
            shutil.copyfile(DEST / (name + ".png"), directory / filename)
    manifest = {
        "version": "2.0.0-animal.1", "size": [128, 128], "anchor": [64, 128],
        "climbing_enabled": False, "runtime_mapping": runtime,
        "sources": {name: {"path": path.relative_to(ROOT).as_posix(),
                            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                            "bounds": list(extracted[name][1])} for name, path in SOURCES.items()},
    }
    (DEST / "manifest.json").write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")
    # Inspection contact sheet on dark/colored backgrounds makes white remnants visible.
    sheet = Image.new("RGB", (800, 400), "#17283a")
    draw = ImageDraw.Draw(sheet)
    for i, (name, sprite) in enumerate(sprites.items()):
        x, y = (i % 5) * 160 + 16, (i // 5) * 200 + 15
        sheet.paste(sprite, (x, y), sprite)
        draw.text((x, y + 139), name, fill="#def5ef")
    sheet.save(DEST / "preview.png")
    print(f"ANIMAL_SPRITES_OK: {len(sprites)} poses, {len(runtime)} runtime images, preserved all source files")
    # Apply the newer shared-proportion gait last so this entry point cannot
    # accidentally restore the old four-frame walk after an asset rebuild.
    if (CONCEPTS / 'dottore_walk_v2/atlas.png').exists():
        from prepare_walk_v2 import main as prepare_walk
        prepare_walk()
    if (CONCEPTS / 'dottore_pet_moods_v2/withdraw.png').exists():
        from prepare_pet_sprites import main as prepare_pet
        prepare_pet()


if __name__ == "__main__":
    main()

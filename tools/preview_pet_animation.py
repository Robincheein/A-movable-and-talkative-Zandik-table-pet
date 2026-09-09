"""Assemble review GIF from the actual Java timelines and prepared sprites."""
import re
from PIL import Image, ImageDraw
from prepare_animal_sprites import ROOT

source = (ROOT / 'src/com/group_finity/mascot/action/FoxInteraction.java').read_text(encoding='utf-8')
names = re.findall(r'"([^"]+)"', re.search(r'SPRITES = \{(.*?)\};', source, re.S).group(1))
plans = [list((int(a), int(b)) for a, b in re.findall(r'\{(\d+),\s*(\d+)\}', block))
         for block in re.findall(r'sequence = new int\[\]\[\]\{(.*?)\};', source, re.S)]
assert len(plans) == 3
timelines = [[pose for pose, hold in plan for _ in range(hold)] for plan in plans]
sprites = [Image.open(ROOT / 'img' / (name + '.png')).convert('RGBA').resize((256,256), Image.Resampling.NEAREST)
           for name in names]
frames = []
for tick in range(max(map(len, timelines)) + 25):
    frame = Image.new('RGB', (768,290), '#23394d')
    draw = ImageDraw.Draw(frame)
    for i, (label, timeline) in enumerate(zip(('ANGRY', 'ALLOW', 'REFUSE'), timelines)):
        sprite = sprites[timeline[min(tick, len(timeline)-1)]]
        frame.paste(sprite, (i*256,0), sprite)
        draw.line((i*256,255,(i+1)*256,255), fill='#d29572')
        draw.text((i*256+10,268), label, fill='white')
    frames.append(frame)
dest = ROOT / 'assets/skins/dottore_fox/pet_moods/animation_preview.gif'
frames[0].save(dest, save_all=True, append_images=frames[1:], duration=40, loop=0, optimize=False)
print('PET_PREVIEW_OK:', [(len(plan), len(set(timeline)), len(timeline)*40)
                           for plan, timeline in zip(plans,timelines)])

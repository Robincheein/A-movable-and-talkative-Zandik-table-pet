# 摸头补帧 v3

使用内置 imagegen 逐张编辑生成，共七张新增图片。保留 v2 原图。

## rest

参考：../dottore_pet_moods_v2/good_mood_allow.png

Use case: precise-object-edit. Image 1 is the edit target. Create ONE intermediate animation frame, NOT a sheet. Preserve fox size, position, camera, mint-white fur, navy opaque star mask, pendant, tail, lower body and grounded paw positions exactly. Same square canvas and plain warm-white background. No text, no props. Exactly FOUR total fox limbs; a lifted foreleg REPLACES its ground leg, never adds a fifth. Human hand must have exactly FIVE readable digits: four fingers plus thumb. Remove ONLY the visitor's entire hand, sleeve and arm; reconstruct a little crown fur. Keep the entire fox unchanged with FOUR paws on the floor. No human remains.

## near

参考：../dottore_pet_moods_v2/good_mood_allow.png

Use case: precise-object-edit. Image 1 is the edit target. Create ONE intermediate animation frame, NOT a sheet. Preserve fox size, position, camera, mint-white fur, navy opaque star mask, pendant, tail, lower body and grounded paw positions exactly. Same square canvas and plain warm-white background. No text, no props. Exactly FOUR total fox limbs; a lifted foreleg REPLACES its ground leg, never adds a fifth. Human hand must have exactly FIVE readable digits: four fingers plus thumb. Move ONLY the visitor hand 60 source pixels upward, leaving a small visible gap above crown. Same palm-down angle and five fingers. Fox entirely unchanged with FOUR paws on floor. This is the frame just before contact, closer than the earlier hovering approach.

## half_raise

参考：../dottore_pet_moods_v2/normal_refuse.png

Use case: precise-object-edit. Image 1 is the edit target. Create ONE intermediate animation frame, NOT a sheet. Preserve fox size, position, camera, mint-white fur, navy opaque star mask, pendant, tail, lower body and grounded paw positions exactly. Same square canvas and plain warm-white background. No text, no props. Exactly FOUR total fox limbs; a lifted foreleg REPLACES its ground leg, never adds a fifth. Human hand must have exactly FIVE readable digits: four fingers plus thumb. Lower the existing raised black forepaw from face height to CHEST height, elbow bent, just beginning to lift. Exactly THREE original ground paws remain, ONE black paw now at chest level. Hand above/right crown, palm angled downward about to approach, five fingers. Head halfway less tilted than target, both ears still alert, mildly wary. Keep lower body and tail exactly unchanged. No motion strokes.

## mid_push

参考：../dottore_pet_moods_v2/normal_refuse.png

Use case: precise-object-edit. Image 1 edit target. ONE animation in-between frame, not sheet. Preserve entire fox size, position, fixed lower body, grounded paws, tail, palette, mask and camera. Same square canvas and plain warm-white background. Exactly FOUR fox limbs total, never an extra ground foreleg below lifted one. Exactly FIVE readable human digits, FOUR long fingers plus ONE thumb. No words, no extra props. Bend raised arm halfway: lifted black paw at CHEEK height, around 120 pixels lower and 90 pixels left of current target, contacting visitor palm as push begins. Human palm stays close to head, fingers fanned visibly with all five tips clear. Keep three grounded paws, one elevated forepaw. Mildly annoyed expression. Remove action strokes.

## lower_paw

参考：../dottore_pet_moods_v2/normal_refuse.png

Use case: precise-object-edit. Image 1 edit target. ONE animation in-between frame, not sheet. Preserve entire fox size, position, fixed lower body, grounded paws, tail, palette, mask and camera. Same square canvas and plain warm-white background. Exactly FOUR fox limbs total, never an extra ground foreleg below lifted one. Exactly FIVE readable human digits, FOUR long fingers plus ONE thumb. No words, no extra props. Recovery after refusal: LOWER raised forearm until black paw hangs near lower belly, about 180 source pixels ABOVE its missing front footprint. Exactly THREE ground paws plus ONE paw low in air attached at shoulder by natural bent foreleg. Hand has recoiled far upper-right away from head, five distinct spread digits. Head is almost upright again, ears wary not friendly. Keep feet and tail fixed, no motion strokes.

## stroke_left

参考：../dottore_pet_moods_v2/good_mood_allow.png

Use case: precise-object-edit. Image 1 edit target. ONE animation in-between frame, not sheet. Preserve entire fox size, position, fixed lower body, grounded paws, tail, palette, mask and camera. Same square canvas and plain warm-white background. Exactly FOUR fox limbs total, never an extra ground foreleg below lifted one. Exactly FIVE readable human digits, FOUR long fingers plus ONE thumb. No words, no extra props. Only change the human hand and a tiny patch of crown fur under it. Slide same palm-down hand 55 source pixels LEFT along crown, gently petting WITHOUT lifting or grabbing ears. Keep all five distinct digits readable in same dorsal hand view. Fox body entirely unchanged, four ground paws. No expression change, no hearts. This is one small stroking in-between.

## stroke_right

参考：../dottore_pet_moods_v2/good_mood_allow.png

Use case: precise-object-edit. Image 1 edit target. ONE animation in-between frame, not sheet. Preserve entire fox size, position, fixed lower body, grounded paws, tail, palette, mask and camera. Same square canvas and plain warm-white background. Exactly FOUR fox limbs total, never an extra ground foreleg below lifted one. Exactly FIVE readable human digits, FOUR long fingers plus ONE thumb. No words, no extra props. Only change the human hand and tiny compressed fur below it. Slide same palm-down hand 45 source pixels RIGHT along crown, palm and fingertips still touching fur between ears. All five separate digits clear, same dorsal view. Fox remains completely unchanged, four ground paws. No smile, no hearts. This is an opposite small stroking in-between.

## half_raise 五指修正

Precise-object-edit. Fix ONLY the visitor hand in this frame: it has only THREE long fingers and a thumb. Redraw FOUR distinct long fingers plus ONE thumb, total FIVE clearly visible separate tips, gently fan the fingers to remove occlusion. Same approaching hand position, sleeve and wrist. Do NOT alter fox at all: three ground paws and one raised chest-height paw, exactly four limbs, same head/body/tail/location, plain warm-white background. No other changes.

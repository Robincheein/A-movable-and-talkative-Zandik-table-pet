package com.group_finity.mascot;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** 离线回归：不初始化 ChatManager，不读取记忆、不调用 API、不显示测试桌宠。 */
public class AnimalSkinSelfTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Logger.getLogger("").setLevel(Level.SEVERE);
        File root = new File(args.length > 0 ? args[0] : ".");
        Set<String> images = new HashSet<String>();
        for (int i = 1; i <= 46; i++) images.add("shime" + i + ".png");
        for (int i = 1; i <= 8; i++) images.add("fox_walk_0" + i + ".png");
        images.add("fox_stand.png");
        images.add("icon.png");
        for (String pose : new String[]{"approach", "normal_refuse", "angry_block", "good_mood_allow", "withdraw",
                "rest", "near", "half_raise", "mid_push", "lower_paw", "stroke_left", "stroke_right"})
            images.add("fox_pet_" + pose + ".png");
        for (String name : images) {
            File path = new File(root, "img/" + name);
            BufferedImage im = ImageIO.read(path);
            check(im != null && im.getWidth() == 128 && im.getHeight() == 128, name + " dimensions");
            check(im.getColorModel().hasAlpha(), name + " alpha channel");
            int transparent = 0, opaque = 0;
            for (int y = 0; y < 128; y++) for (int x = 0; x < 128; x++) {
                int alpha = im.getRGB(x, y) >>> 24;
                if (alpha == 0) transparent++;
                if (alpha > 240) opaque++;
            }
            check(transparent > 128 * 128 / 4 && opaque > 300, name + " foreground/background");
            check((im.getRGB(0, 0) >>> 24) == 0 && (im.getRGB(127, 127) >>> 24) == 0, name + " corners");
            File packaged = new File(root, "dist/img/" + name);
            if (packaged.exists()) check(Arrays.equals(Files.readAllBytes(path.toPath()),
                    Files.readAllBytes(packaged.toPath())), name + " packaged sync");
        }
        Configuration config = new Configuration();
        Document actions = null;
        for (String name : new String[]{"Behavior.xml", "Actions.xml"}) {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new File(root, "conf/" + name));
            config.load(new Entry(doc.getDocumentElement()));
            if (name.equals("Actions.xml")) actions = doc;
        }
        config.validate();
        for (String name : config.getBehaviorBuilders().keySet()) {
            check(!name.matches(".*(壁|登る|よじのぼる|掴まって|上辺を伝う|下辺を伝う|飛びつく).*"),
                    "Climbing still registered: " + name);
        }
        NodeList poses = actions.getElementsByTagName("ポーズ");
        for (int i = 0; i < poses.getLength(); i++) {
            Element pose = (Element) poses.item(i);
            check(images.contains(pose.getAttribute("画像").substring(1)), "Unmapped pose " + pose.getAttribute("画像"));
        }
        NodeList actionNodes = actions.getElementsByTagName("動作");
        for (int i = 0; i < actionNodes.getLength(); i++) {
            Element action = (Element) actionNodes.item(i);
            if (!action.getAttribute("名前").matches("歩く|走る|猛ダッシュ|IEを持って歩く|IEを持って走る")) continue;
            NodeList frames = action.getElementsByTagName("ポーズ");
            check(frames.getLength() == 8, "Walking frame count");
            for (int j = 0; j < 8; j++) {
                Element pose = (Element)frames.item(j);
                check(pose.getAttribute("画像").equals("/fox_walk_0" + (j + 1) + ".png"), "Walking frame order");
                check(pose.getAttribute("移動速度").equals("-1,0"), "Walking is not constant 25px/s");
                check(pose.getAttribute("長さ").equals("4"), "Walking cadence is not 160ms/frame");
                check(pose.getAttribute("基準座標").equals("64,128"), "Walking anchor drift");
            }
        }
        Document behaviorDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new File(root, "conf/Behavior.xml"));
        for (String tag : new String[]{"行動", "行動参照"}) {
            NodeList behaviors = behaviorDoc.getElementsByTagName(tag);
            for (int i = 0; i < behaviors.getLength(); i++) {
                Element b = (Element)behaviors.item(i);
                if (b.getAttribute("名前").matches(".*(走る|走って|首が回った|足をぶらぶら|ずりずり).*"))
                    check(b.getAttribute("頻度").equals("0"), "Fast/jittery auto-behavior still enabled");
            }
        }
        Mascot mascot = new Mascot();
        mascot.setManager(new Manager() {
            @Override public int getCount() { return 1; }
        });
        try {
            int left = mascot.getEnvironment().getWorkArea().getLeft();
            int top = mascot.getEnvironment().getWorkArea().getTop();
            int bottom = mascot.getEnvironment().getWorkArea().getBottom();
            int right = mascot.getEnvironment().getWorkArea().getRight();
            for (int edge : new int[]{left, right - 1}) {
                mascot.setAnchor(new Point(edge, top + 100));
                mascot.setLookRight(edge != left);
                Point before = new Point(mascot.getAnchor());
                config.buildBehavior(null, mascot);
                check(before.equals(mascot.getAnchor()), "Airborne fallback teleported pet");
                Action fall = config.buildAction("落ちる", Collections.<String, String>emptyMap());
                fall.init(mascot);
                check(fall.hasNext(), "Fall stopped at wall");
                int ticks = 0;
                while (fall.hasNext() && ticks++ < 1000) {
                    fall.next();
                }
                check(ticks < 1000 && mascot.getEnvironment().getFloor().isOn(mascot.getAnchor()), "Did not land");
                check(mascot.getAnchor().y > before.y && mascot.getAnchor().y <= bottom, "Fall direction");
            }
            // Exercise a complete eight-frame ground walk without showing a window.
            mascot.setAnchor(new Point(left + 64, bottom));
            Action walk = config.buildAction("歩く", Collections.singletonMap("目的地X", String.valueOf(left + 300)));
            mascot.setBehavior(new com.group_finity.mascot.behavior.UserBehavior("test-walk", walk, config));
            Set<Object> visitedFrames = new HashSet<Object>();
            int initialX = mascot.getAnchor().x;
            for (int i = 0; i < 32; i++) {
                int previousX = mascot.getAnchor().x;
                mascot.tick();
                check(mascot.getAnchor().x == previousX + 1 && mascot.getAnchor().y == bottom,
                        "Ground speed or ground height changed between ticks");
                visitedFrames.add(mascot.getImage());
            }
            check(visitedFrames.size() == 8 && mascot.getAnchor().x == initialX + 32, "Eight-frame walk did not animate/move");
            // Real drag action (not just event dispatch), held past the old escape timeout.
            com.group_finity.mascot.environment.Location cursor = mascot.getEnvironment().getCursor();
            cursor.setX(left + 200); cursor.setY(top + 150); cursor.setDx(0); cursor.setDy(0);
            mascot.setAnchor(new Point(left + 200, top + 230));
            mascot.setLookRight(true);
            mascot.setAnimatingPublic(false);
            mascot.setBehavior(config.buildBehavior(com.group_finity.mascot.behavior.UserBehavior.BEHAVIORNAME_DRAGGED));
            Behavior held = mascot.getBehavior();
            Point pickupAnchor = new Point(mascot.getAnchor());
            visitedFrames.clear();
            int dragDirection = 6;
            for (int i = 0; i < 400; i++) {
                if (i == 0) { mascot.tick(); check(mascot.getAnchor().equals(pickupAnchor), "Pickup snapped to fixed offset"); }
                else {
                    if (cursor.getX() > right - 160) dragDirection = -6;
                    if (cursor.getX() < left + 160) dragDirection = 6;
                    cursor.setX(cursor.getX() + dragDirection); cursor.setDx(dragDirection); mascot.tick();
                }
                check(mascot.getBehavior() == held, "Drag escaped/teleported while held");
                check(mascot.isLookRight(), "Drag forcibly flipped facing");
                check(mascot.getAnchor().x == cursor.getX(), "Drag lost mouse control");
                visitedFrames.add(mascot.getImage());
            }
            check(visitedFrames.size() > 2, "Dragging is visually static");
            // Neutral and tilted frames must not resize/recenter the native window.
            com.group_finity.mascot.image.MascotImage dragSource =
                    com.group_finity.mascot.image.ImagePairLoader.load("/shime5.png", new Point(64, 128)).getImage(false);
            for (double scale : new double[]{0.75, 1.0, 1.5, 2.0}) {
                com.group_finity.mascot.image.MascotImage neutral = dragSource.motionFrame(0, 100);
                for (int angle = -6; angle <= 6; angle++) {
                    com.group_finity.mascot.image.MascotImage tilted = dragSource.motionFrame(angle, 100);
                    check(neutral.getCenter(scale).equals(tilted.getCenter(scale)), "Drag pivot jumps at neutral angle");
                    check(neutral.getSize(scale).equals(tilted.getSize(scale)), "Drag window resizes at neutral angle");
                    check(tilted == dragSource.motionFrame(angle, 100), "Motion frame not cached");
                }
            }
            held.mouseReleased(new MouseEvent(mascot.getWindow().asJWindow(), MouseEvent.MOUSE_RELEASED,
                    0, 0, 0, 0, 1, false, MouseEvent.BUTTON1));
            check(mascot.getBehavior() != held, "Release did not leave dragging");
            int fallTicks = 0;
            while (!mascot.getEnvironment().getFloor().isOn(mascot.getAnchor()) && fallTicks++ < 500) mascot.tick();
            check(fallTicks < 500, "Released pet never reached floor");
            for (int i = 0; i < 16; i++) mascot.tick();
            check(mascot.getImage().getVisibleBounds(1).height > 90, "Release chain ended in prone/sleeping image");
            mascot.setAnchor(new Point(left + 300, bottom));
            Action landing = config.buildAction("跳ねる", Collections.<String, String>emptyMap());
            check(landing instanceof com.group_finity.mascot.action.Landing, "Landing still maps to sleeping");
            mascot.setBehavior(new Behavior() {
                public void init(Mascot pet) throws com.group_finity.mascot.exception.CantBeAliveException {
                    try { landing.init(pet); } catch (Exception e) { throw new AssertionError(e); }
                }
                public void next() { try { landing.next(); } catch (Exception e) { throw new AssertionError(e); } }
                public void mousePressed(MouseEvent e) {} public void mouseReleased(MouseEvent e) {}
            });
            visitedFrames.clear();
            for (int i = 0; i < 8; i++) {
                check(landing.hasNext(), "Landing ended early");
                mascot.tick();
                check(mascot.getAnchor().y == bottom, "Landing feet left the floor");
                visitedFrames.add(mascot.getImage());
            }
            check(!landing.hasNext() && visitedFrames.size() >= 4, "Landing settle missing or loops forever");
            java.awt.Rectangle visible = mascot.getImage().getVisibleBounds(1);
            check(visible.y > 0 && visible.height < 128, "Transparent padding not excluded");
            java.awt.Rectangle pet = new java.awt.Rectangle(400, 800, 100, 100);
            java.awt.Rectangle work = new java.awt.Rectangle(0, 0, 1920, 1040);
            Point dialog = com.group_finity.mascot.chat.DialogPlacement.locate(pet, work, 400, 300);
            check(dialog.y + 300 == pet.y - 6, "Dialog gap is not six pixels");
            dialog = com.group_finity.mascot.chat.DialogPlacement.locate(new java.awt.Rectangle(-1800, 0, 100, 100),
                    new java.awt.Rectangle(-1920, 0, 1920, 1040), 400, 300);
            check(dialog.y == 106 && dialog.x >= -1920, "Top edge/negative monitor placement");
            final int[] events = new int[2];
            // Exercise each drawn outcome in both directions, independently of random mood.
            check(com.group_finity.mascot.action.FoxInteraction.Kind.values().length == 1, "Removed interactions remain");
            for (PetMood.Reaction reaction : PetMood.Reaction.values()) for (boolean facing : new boolean[]{false, true}) {
                mascot.setAnchor(new Point(left + 200, bottom));
                mascot.setLookRight(facing);
                cursor.setX(right + 300); cursor.setY(bottom - 60);
                Action petAction = new com.group_finity.mascot.action.FoxInteraction(reaction);
                mascot.setBehavior(new com.group_finity.mascot.behavior.UserBehavior(null, petAction, config));
                Behavior interaction = mascot.getBehavior();
                Set<Object> interactionFrames = new HashSet<Object>();
                Object lastInteractionFrame = null;
                int changes = 0, heldTicks = 0, longestHold = 0;
                int interactionTicks = 0;
                for (; mascot.getBehavior() == interaction && interactionTicks < 120; interactionTicks++) {
                    mascot.tick();
                    check(mascot.getAnchor().y == bottom, "Interaction feet left ground");
                    check(mascot.getAnchor().x == left + 200, "Head pat moved the pet");
                    check(mascot.getImage().getSize().equals(new java.awt.Dimension(128, 128)), "Interaction canvas changed");
                    check(mascot.getImage().getCenter().equals(new Point(64, 128)), "Interaction pivot changed");
                    interactionFrames.add(mascot.getImage());
                    if (lastInteractionFrame != mascot.getImage()) {
                        changes++; heldTicks = 0; lastInteractionFrame = mascot.getImage();
                    }
                    longestHold = Math.max(longestHold, ++heldTicks);
                }
                int expectedUnique = reaction == PetMood.Reaction.ALLOW ? 7 : reaction == PetMood.Reaction.ANGRY ? 9 : 8;
                check(interactionFrames.size() == expectedUnique, "Drawn in-between frames missing");
                check(changes >= 12 && longestHold <= 6, "Head pat still freezes on key poses");
                check(interactionTicks >= 45 && interactionTicks <= 55, "Interaction timing changed");
                check(mascot.getBehavior() != interaction, "Interaction did not finish");
                check(com.group_finity.mascot.action.FoxInteraction.start(mascot,
                        com.group_finity.mascot.action.FoxInteraction.Kind.PET, config), "Repeat interaction failed");
                interaction = mascot.getBehavior();
                interaction.mousePressed(new MouseEvent(mascot.getWindow().asJWindow(), MouseEvent.MOUSE_PRESSED,
                        0, MouseEvent.BUTTON1_DOWN_MASK, 0, 0, 1, false, MouseEvent.BUTTON1));
                check(mascot.getBehavior() != interaction, "Interaction blocks dragging");
            }
            mascot.setAnchor(new Point(left + 200, top + 100));
            Behavior airborne = mascot.getBehavior();
            check(!com.group_finity.mascot.action.FoxInteraction.start(mascot,
                    com.group_finity.mascot.action.FoxInteraction.Kind.PET, config), "Head pat while airborne");
            check(mascot.getBehavior() == airborne, "Rejected interaction changed behavior");
            // Top/side guards keep control; bottom is intentionally unrestricted.
            cursor.setX(left + 300); cursor.setY(top + 150); cursor.setDx(0);
            mascot.setAnchor(new Point(left + 300, top + 230));
            mascot.setBehavior(config.buildBehavior(com.group_finity.mascot.behavior.UserBehavior.BEHAVIORNAME_DRAGGED));
            Behavior edgeDrag = mascot.getBehavior();
            for (Point outside : new Point[]{new Point(left-500,top-500), new Point(right+500,top-500),
                    new Point(left-500,bottom+500), new Point(right+500,bottom+500)}) {
                cursor.setX(outside.x); cursor.setY(outside.y);
                for (int tick = 0; tick < 30; tick++) {
                    mascot.tick();
                    check(mascot.getBehavior() == edgeDrag, "Edge drag released the mouse");
                    assertInside(mascot);
                    if (outside.y > bottom) check(mascot.getAnchor().y > bottom, "Bottom guard still enabled");
                }
            }
            cursor.setX(left + 300); cursor.setY(top + 150); mascot.tick();
            check(mascot.getAnchor().x == cursor.getX(), "Mouse return snapped/lost grip");
            edgeDrag.mouseReleased(new MouseEvent(mascot.getWindow().asJWindow(), MouseEvent.MOUSE_RELEASED,
                    0, 0, 0, 0, 1, false, MouseEvent.BUTTON1));
            check(mascot.getBehavior() != edgeDrag, "Edge drag cannot release");
            // Grounded frame changes must not lift the foot anchor, including
            // artwork whose tail/padding reaches below its nominal pivot.
            for (String sprite : images) for (boolean facing : new boolean[]{false,true}) {
                mascot.setImage(com.group_finity.mascot.image.ImagePairLoader.load("/"+sprite,new Point(64,128)).getImage(facing));
                mascot.setAnchor(new Point((left+right)/2,bottom));
                for (int repeat=0; repeat<3; repeat++) {
                    mascot.constrainToWorkArea();
                    check(mascot.getAnchor().y == bottom, "Frame change lifted feet: " + sprite);
                    check(mascot.getEnvironment().getFloor().isOn(mascot.getAnchor()), "Frame change lost ground");
                }
            }
            // Explicit unreachable walk targets must terminate at the visible edge.
            for (int target : new int[]{left-500, right+500}) {
                mascot.setAnchor(new Point((left+right)/2, bottom));
                Action edgeWalk = config.buildAction("歩く", Collections.singletonMap("目的地X", String.valueOf(target)));
                edgeWalk.init(mascot);
                int steps = 0;
                while (edgeWalk.hasNext() && steps++ < 5000) { edgeWalk.next(); assertInside(mascot); }
                check(steps < 5000, "Walk keeps pushing into screen edge");
                check(mascot.isLookRight() == (target < left), "Edge walk did not turn inward");
            }
            mascot.setBehavior(new Behavior() {
                public void init(Mascot pet) {}
                public void next() {}
                public void mousePressed(MouseEvent event) { events[0]++; }
                public void mouseReleased(MouseEvent event) { events[1]++; }
            });
            MascotEventHandler handler = new MascotEventHandler(mascot);
            java.awt.Component window = mascot.getWindow().asJWindow();
            handler.mousePressed(new MouseEvent(window, MouseEvent.MOUSE_PRESSED, 0,
                    MouseEvent.BUTTON1_DOWN_MASK, 0, 0, 100, 100, 1, false, MouseEvent.BUTTON1));
            handler.mouseDragged(new MouseEvent(window, MouseEvent.MOUSE_DRAGGED, 1,
                    MouseEvent.BUTTON1_DOWN_MASK, 0, 0, 107, 100, 0, false, MouseEvent.NOBUTTON));
            check(events[0] == 0, "Small click jitter started dragging");
            handler.mouseDragged(new MouseEvent(window, MouseEvent.MOUSE_DRAGGED, 2,
                    MouseEvent.BUTTON1_DOWN_MASK, 0, 0, 109, 100, 0, false, MouseEvent.NOBUTTON));
            handler.mouseReleased(new MouseEvent(window, MouseEvent.MOUSE_RELEASED, 3,
                    0, 0, 0, 109, 100, 1, false, MouseEvent.BUTTON1));
            check(events[0] == 1 && events[1] == 1, "Drag/release dispatch regression");
        } finally {
            mascot.dispose();
        }
        System.out.println("ANIMAL_SKIN_OK: " + images.size() + " sprites, XML, no climbing, edge landing, eight-frame walk, real held/release/settle, dialog placement, drag threshold");
        System.exit(0);
    }

    private static void assertInside(Mascot mascot) {
        java.awt.Rectangle visible = mascot.getImage().getVisibleBounds(SettingsManager.getInstance().getCharacterScale());
        java.awt.Rectangle window = mascot.getBounds();
        visible.translate(window.x, window.y);
        java.awt.Rectangle area = mascot.getEnvironment().getWorkArea().toRectangle();
        check(visible.x >= area.x && visible.x + visible.width <= area.x + area.width && visible.y >= area.y,
                "Visible artwork outside top/side limits: " + visible);
    }
}

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
        for (int i = 1; i <= 4; i++) images.add("fox_walk_0" + i + ".png");
        images.add("icon.png");
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
            if (!action.getAttribute("名前").matches("歩く|走る|猛ダッシュ")) continue;
            NodeList frames = action.getElementsByTagName("ポーズ");
            check(frames.getLength() == 4, "Walking frame count");
            for (int j = 0; j < 4; j++) check(((Element)frames.item(j)).getAttribute("画像")
                    .equals("/fox_walk_0" + (j + 1) + ".png"), "Walking frame order");
        }
        Mascot mascot = new Mascot();
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
            // Exercise a real four-frame ground walk without showing a window.
            mascot.setAnchor(new Point(left + 64, bottom));
            Action walk = config.buildAction("歩く", Collections.singletonMap("目的地X", String.valueOf(left + 300)));
            mascot.setBehavior(new com.group_finity.mascot.behavior.UserBehavior("test-walk", walk, config));
            Set<Object> visitedFrames = new HashSet<Object>();
            int initialX = mascot.getAnchor().x;
            for (int i = 0; i < 24; i++) {
                mascot.tick();
                visitedFrames.add(mascot.getImage());
            }
            check(visitedFrames.size() >= 4 && mascot.getAnchor().x != initialX, "Four-frame walk did not animate/move");
            final int[] events = new int[2];
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
        System.out.println("ANIMAL_SKIN_OK: " + images.size() + " sprites, XML, no climbing, both-edge landing, four-frame walk, drag threshold");
        System.exit(0);
    }
}

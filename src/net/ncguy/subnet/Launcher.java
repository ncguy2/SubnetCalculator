package net.ncguy.subnet;

import com.bulenkov.darcula.DarculaLaf;
import com.bulenkov.darcula.DarculaLookAndFeelInfo;
import net.ncguy.subnet.data.IPAddress;
import net.ncguy.subnet.display.SubnetFrame;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static net.ncguy.subnet.Launcher.PreferenceData.lookAndFeel;

/**
 * Created by Guy on 23/03/2016.
 */
public class Launcher {

    static String prefsPath = "net.ncguy.prefs";
    static Preferences prefs = Preferences.userRoot().node(prefsPath);
    static Thread externalThread;
    static List<Runnable> runnables;
    static boolean alive = true;

    public static void kill() {
        alive = false;
        externalThread.interrupt();
        System.exit(0);
    }

    public static void main(String[] args) {
        loadProps();
        initShutdownHook(args);
        start(args);
        installThemes(args);
        applyTheme(args);
        openFrame(args);

        IPAddress addr1 = new IPAddress(0, 0, 0, 0);
        System.out.println(addr1.add(769));
    }

    public static void installThemes(String[] args) {
        UIManager.installLookAndFeel(new DarculaLookAndFeelInfo(){
            @Override public String getClassName() {
                return DarculaLaf.class.getCanonicalName();
            }
        });
    }

    public static void applyTheme(String[] args) {
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static void initShutdownHook(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Launcher::saveProps));
    }

    public static void openFrame(String[] args) {
        SwingUtilities.invokeLater(() -> SubnetFrame.getFrame(args));
    }

    public static void postRunnable(Runnable r) {
        runnables.add(r);
    }

    public static void start(String[] args) {
        runnables = new ArrayList<>();
        externalThread = new Thread(() -> {
            while(alive) {
                if(runnables.size() == 0) {
                    try { Thread.sleep(500); } catch (InterruptedException e) {
                        e.printStackTrace();
                        alive = false;
                    }
                }else{
                    runnables.stream().collect(Collectors.toList()).forEach(r -> {
                        r.run();
                        runnables.remove(r);
                    });
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        alive = false;

                    }
                }
            }
        });
        externalThread.start();
    }

    public static void loadProps() {
        lookAndFeel = prefs.get("LookAndFeel", DarculaLaf.class.getCanonicalName());
    }

    public static void saveProps() {
        prefs.put("LookAndFeel", UIManager.getLookAndFeel().getClass().getCanonicalName());
    }

    public static class PreferenceData {
        static String lookAndFeel;
    }

}

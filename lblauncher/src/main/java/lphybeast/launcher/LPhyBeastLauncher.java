package lphybeast.launcher;

import lphystudio.app.LPhyAppConfig;
import lphystudio.app.manager.DependencyUtils;
import lphystudio.core.awt.AboutMenuHelper;
import lphystudio.core.swing.io.TextPaneOutputStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.PrintStream;

/**
 * LPhyBeast GUI.
 * @author Walter Xie
 */
public class LPhyBeastLauncher extends JFrame {

    private static final String APP_NAME = "LPhy BEAST";
    static {
        LPhyAppConfig.setupEcoSys(APP_NAME);
    }

    private final String VERSION;

    private final int MASK = LPhyAppConfig.MASK;

    public LPhyBeastLauncher() {
        // use MANIFEST.MF to store version in jar, or use system property in development,
        // otherwise VERSION = "DEVELOPMENT"
        VERSION = DependencyUtils.getVersion(LPhyBeastLauncher.class, "lphy.beast.launcher.version");

        setTitle(APP_NAME + " version " + VERSION);
        // DISPOSE_ON_CLOSE will not close lphy studio frame
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        final int MAX_WIDTH = 800;
        final int MAX_HEIGHT = 900;
        LPhyAppConfig.setFrameLocation(this, MAX_WIDTH, MAX_HEIGHT, 0);

        // setOut before LauncherPanel to catch more System.out
        final JTextPane soutPane = new JTextPane();
        TextPaneOutputStream out = new TextPaneOutputStream(soutPane, false);
        PrintStream printStream = new PrintStream(out);
        System.setOut(printStream);

        LauncherPanel launcherPanel = new LauncherPanel();

        JTabbedPane tabbedPane = new JTabbedPane();
        //The following line enables to use scrolling tabs.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        tabbedPane.addTab("Launcher", launcherPanel);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        tabbedPane.addTab("Verbose", new JScrollPane(soutPane));
        tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

//        final JTextPane serrPane = new JTextPane();
//        TextPaneOutputStream err = new TextPaneOutputStream(serrPane, true);
//        printStream = new PrintStream(err);
//        System.setErr(printStream);
//
//        tabbedPane.addTab("Warnings", new JScrollPane(serrPane));
//        tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);

        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();

        AboutMenuHelper aboutMenuHelper =
                new AboutMenuHelper(this, APP_NAME + " v " + VERSION,
                        getHTMLCredits(), menuBar);

        setVisible(true);

    }

    private String getHTMLCredits() {
        return "<html><body width='%1s'><h3>Created by Walter Xie, Alexei Drummond, and Kylie Chen</h3>"+
                "<p>The Centre for Computational Evolution<br>"+
                "University of Auckland<br></p>"+
                "<p>Homepage :<br>"+
                "<a href=\""+LPhyAppConfig.LPHY_WEB+"\">"+LPhyAppConfig.LPHY_WEB+"</a></p>"+
                "<p>Source code distributed under the GNU Lesser General Public License Version 3</p>"+
                "<p>Require Java 17, current Java version " + System.getProperty("java.version") + "</p></html>";
    }

    public static void main(String[] args) {
        LPhyBeastLauncher launcher = new LPhyBeastLauncher();
    }

}

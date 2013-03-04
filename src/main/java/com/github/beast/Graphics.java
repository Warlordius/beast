package com.github.beast;

import java.awt.*;
import javax.swing.*;

public class Graphics {

    static BeastFrame beastFrame;

    public static void output(String text) {

	beastFrame.pOutput.output(text);
    }

    public static void init() {

	beastFrame = new BeastFrame();
    }

    public static String getURL() {

	return beastFrame.tURL.getText();
    }
}

class BeastFrame extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1;
    JPanel pMain;
    BeastOutput pOutput;
    JButton bRun;
    JTextField tURL;

    BeastFrame() {

	pMain = new JPanel();
	pMain.setBounds(getBounds());
	pMain.setLayout(null);

	// main windows elements setup

	pOutput = new BeastOutput();
	pMain.add(pOutput);

	tURL = new JTextField();
	tURL.setBounds(10, 10, 400, 25);
	tURL.setText("www.sme.sk");
	pMain.add(tURL);

	bRun = new JButton();
	bRun.setBounds(50, 520, 100, 30);
	bRun.setText("Run");
	pMain.add(bRun);

	// main window setup
	setSize(800, 600);
	setTitle("Bee Assisted Searching for Topics");
	setContentPane(pMain);
	setVisible(true);
    }
}

class BeastOutput extends JPanel {

    private static final long serialVersionUID = 1;
    private static Rectangle defaultCoords = new Rectangle(10, 60, 400, 450);
    private JTextArea tOutput;

    BeastOutput(int x, int y, int x2, int y2) {

	setBounds(x, y, x2, y2);
	setLayout(new BorderLayout());
	tOutput = new JTextArea();
	tOutput.setEditable(false);
	tOutput.setLineWrap(true);
	tOutput.setWrapStyleWord(false);
	JScrollPane textScroll = new JScrollPane(tOutput);
	add(textScroll, BorderLayout.CENTER);
    }

    BeastOutput(Rectangle rect) {

	this(rect.x, rect.y, rect.height, rect.width);
    }

    BeastOutput() {

	this(defaultCoords);
    }

    public void output(String text) {

	tOutput.setText(text);
    }
}
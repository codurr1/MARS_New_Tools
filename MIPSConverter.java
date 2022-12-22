package mars.tools;
import mars.*;
import mars.util.*;
import mars.assembler.*;
import mars.mips.instructions.*;
import mars.mips.hardware.*;
import java.util.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.text.html.*;


public class MIPSConverter extends AbstractMarsToolAndApplication {
	private static String version = "Version 1.0";
	private static String heading =  "Converts from Instruction to Hex and Vice Versa";
	private static final String title = "MIPS Converter, ";
	Converter c = new Converter();

	private MIPSConverter thisConverterTool;
	private static final int maxLengthHex = 8;
	private static final String defaultHex = "00000000";
	private static final String defaultCode = "Enter MIPS code";

	private JTextField hexDisplay, codeDisplay;
	private static final Font hexDisplayFont = new Font("Courier", Font.PLAIN, 20);
	private static final Font codeDisplayFont = new Font("Courier", Font.PLAIN, 20);
	private static final Color hexDisplayColor = Color.black;
	private static final Color codeDisplayColor = Color.black;
	private static final int maxLengthcode = 18;

	private InstructionsPane instructions;
	private String defaultInstructions = "<html>Enter HEX or CODE then press the Enter key for conversion<br> Ex:- add $t1 $t1 $t2<br>addi $t1 $t1 0x1<br>j 0x100<br>beq $s1 $zero 0x101<br>lw $t1 $s2 0x1<br>sw $s2 $s3 0x0</html>";
	private static final Font instructionsFont = new Font("Arial", Font.PLAIN, 14);

	private static final String expansionFontTag = "<font size=\"+1\" face=\"Courier\" color=\"#000000\">";


	public MIPSConverter(String title, String heading) {
		super(title, heading);
		thisConverterTool = this;
	}

	/**
	 *  Simple constructor, likely used by the MARS Tools menu mechanism
	 */
	public MIPSConverter() {
		this(title + version, heading);
	}

	public static void main(String[] args) {
		new MIPSConverter(title + version, heading).go();
	}
	public String getName() {
		return "MIPS Converter";
	}

	protected void reset() {
		instructions.setText(defaultInstructions);
		updateDisplays(new AllChanges());
	}


	protected JComponent buildMainDisplayArea() {
		// Editable display for hex code version of the float value
		Box mainPanel = Box.createVerticalBox();
		JPanel leftPanel = new JPanel(new GridLayout(2, 1, 0, 0));
		JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 0));
		Box subMainPanel = Box.createHorizontalBox();
		subMainPanel.add(leftPanel);
		subMainPanel.add(rightPanel);
		mainPanel.add(subMainPanel);


		hexDisplay = new JTextField(defaultHex, 19);
		hexDisplay.setFont(hexDisplayFont);
		hexDisplay.setForeground(hexDisplayColor);
		hexDisplay.setHorizontalAlignment(JTextField.RIGHT);
		hexDisplay.setToolTipText("" + 8 + "-digit hex code (base 16) display");
		hexDisplay.setEditable(true);
		hexDisplay.revalidate();
		hexDisplay.addKeyListener(new HexDisplayKeystrokeListener(8));

		JPanel hexPanel = new JPanel();
		hexPanel.add(hexDisplay);
		leftPanel.add(hexPanel);

		FlowLayout rightPanelLayout = new FlowLayout(FlowLayout.LEFT);
		JPanel place1 = new JPanel(rightPanelLayout);
		JPanel place2 = new JPanel(rightPanelLayout);

		JEditorPane hexExplain = new JEditorPane("text/html", expansionFontTag + "&lt;&nbsp;&nbsp;Hex code representation" + "</font>");
		hexExplain.setEditable(false);
		hexExplain.setFocusable(false);
		hexExplain.setForeground(Color.black);
		hexExplain.setBackground(place1.getBackground());
		place1.add(hexExplain);
		rightPanel.add(place1);


		// Editable display for code version of float value.
		codeDisplay = new JTextField(defaultCode, maxLengthcode + 1);
		codeDisplay.setFont(codeDisplayFont);
		codeDisplay.setForeground(codeDisplayColor);
		codeDisplay.setHorizontalAlignment(JTextField.RIGHT);
		codeDisplay.setToolTipText("MIPS Code");
		codeDisplay.setEditable(true);
		codeDisplay.revalidate();
		codeDisplay.addKeyListener(new CodeDisplayKeystrokeListener());


		JPanel codePanel = new JPanel();
		codePanel.add(codeDisplay);
		leftPanel.add(codePanel);


		JEditorPane codeExplain = new JEditorPane("text/html", expansionFontTag + "&lt;&nbsp;&nbsp;MIPS Code representation" + "</font>");
		codeExplain.setEditable(false);
		codeExplain.setFocusable(false);
		codeExplain.setForeground(Color.black);
		codeExplain.setBackground(place1.getBackground());
		place2.add(codeExplain);
		rightPanel.add(place2);

		JPanel instructionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		instructions = new InstructionsPane(instructionsPanel);
		instructionsPanel.add(instructions);
		instructionsPanel.setBorder(new TitledBorder("Instructions"));
		mainPanel.add(instructionsPanel);
		return mainPanel;

	}

	private void updateDisplays(AllChanges changes) {
		int hexIndex = (changes.hexString.charAt(0) == '0' && (changes.hexString.charAt(1) == 'x' || changes.hexString.charAt(1) == 'X')) ? 2 : 0;
		hexDisplay.setText(changes.hexString.substring(hexIndex).toUpperCase());
		codeDisplay.setText(changes.codeString);
	}
	private boolean errorcheck1(AllChanges changes) {
		if (changes.hexString == "INVALID CODE")
			return true;
		else
			return false;
	}
	private boolean errorcheck2(AllChanges changes) {
		if (changes.codeString == "INVALID HEX")
			return true;
		else
			return false;
	}
	private class AllChanges {
		String hexString;
		String codeString;
		String binaryString;
		String decimalString;
		String expansionString;
		int intValue;

		// Default object
		private AllChanges() {
			hexString = defaultHex;
			codeString = defaultCode;
		}

		//  Assign all fields given a string representing 32 bit hex value.
		public AllChanges buildCodeFromHexString(String hexString) {
			this.hexString = "0x" + hexString;
			this.codeString = buildCode(hexString);
			return this;
		}

		public AllChanges buildHexStringFromCode(String codeString) {
			this.hexString = buildHEX(codeString);
			this.codeString = codeString;
			return this;
		}

		private String buildHEX(String code) {
			String finalhex = "INVALID CODE";
			String y = code;

			String[] tokens = y.split(" ");

			String l = tokens[0];
			String type = "";
			String op = "";
			String funct = "";

			for (int i = 0; i < c.HEX_MIPS_TABLE.length; i++) {
				if (l.equals(c.HEX_MIPS_TABLE[i][0])) {
					type = c.HEX_MIPS_TABLE[i][1];
					op = c.HEX_MIPS_TABLE[i][2];
					funct = c.HEX_MIPS_TABLE[i][3];
				}
			}

			if (type == "" ) return finalhex;

			if (type.equalsIgnoreCase("R_TYPE")) {
				if (tokens.length > 4) {
					String a = hex2Bin(funct);
					String b = tokens[4];
					String d = tokens[2];
					String e = tokens[3];
					String f = tokens[1]; //balanc

					String z = "";
					String u = "";
					String w = "";
					String r = "";
					f = hex2Bin(f);

					StringBuilder sham = new StringBuilder();

					for (int i = f.length(); i < 5; i++) {
						sham.append("0");
					}

					String shamt = sham.toString() + f;
					shamt = bin2Hex(shamt);

					StringBuilder opc = new StringBuilder();

					for (int i = op.length(); i < 6; i++) {
						opc.append("0");
					}

					String special = opc.toString() + op;

					special = bin2Hex(special);

					StringBuilder builder = new StringBuilder();

					for (int i = a.length(); i < 6; i++) {
						builder.append("0");
					}

					String q = builder.toString() + a;

					if (!(b.startsWith("$")) || !(d.startsWith("$")) || !(e.startsWith("$"))) {
						d = "$" + d;
						b = "$" + b;
						e = "$" + e;
					}

					for (int i = 0; i < c.REGISTERS.length; i++) {
						if (d.equals(c.REGISTERS[i][0])) {
							u = c.REGISTERS[i][1];
						}

						if (b.equals(c.REGISTERS[i][0])) {
							z = c.REGISTERS[i][1];
						}

						if (e.equals(c.REGISTERS[i][0])) {
							w = c.REGISTERS[i][1];
						}
					}

					u = hex2Bin(u);
					z = hex2Bin(z);
					w = hex2Bin(w);

					StringBuilder builder2 = new StringBuilder();

					for (int i = u.length(); i < 5; i++) {
						builder2.append("0");
					}

					u = builder2.toString() + u;

					StringBuilder builder3 = new StringBuilder();

					for (int i = z.length(); i < 5; i++) {
						builder3.append("0");
					}

					z = builder3.toString() + z;

					StringBuilder builder4 = new StringBuilder();

					for (int i = w.length(); i < 5; i++) {
						builder4.append("0");
					}

					w = builder3.toString() + w;

					String bin = special + u + w + z + f + q;

					String hex = bin2Hex(bin);
					hex = "0x0" + hex;

					finalhex = hex;
					// instructions.setText(finalhex);
				}

				else {
					String a = hex2Bin(funct);
					String b = tokens[1];
					String d = tokens[2];
					String e = tokens[3];

					String z = "";
					String u = "";
					String w = "";
					String r = "";

					StringBuilder opc = new StringBuilder();

					for (int i = op.length(); i < 6; i++) {
						opc.append("0");
					}

					String special = opc.toString() + op;

					StringBuilder builder = new StringBuilder();

					for (int i = a.length(); i < 6; i++) {
						builder.append("0");
					}

					String q = builder.toString() + a;

					if (!(b.startsWith("$")) || !(d.startsWith("$")) || !(e.startsWith("$"))) {
						d = "$" + d;
						b = "$" + b;
						e = "$" + e;
					}
					boolean u1 = false, z1 = false, w1 = false;
					for (int i = 0; i < c.REGISTERS.length; i++) {
						if (d.equals(c.REGISTERS[i][0])) {
							u = c.REGISTERS[i][1];
							u1 = true;
						}

						if (b.equals(c.REGISTERS[i][0])) {
							z = c.REGISTERS[i][1];
							z1 = true;
						}

						if (e.equals(c.REGISTERS[i][0])) {
							w = c.REGISTERS[i][1];
							w1 = true;
						}
					}
					if (!u1 || !z1 || !w1) return finalhex;
					u = hex2Bin(u);
					z = hex2Bin(z);
					w = hex2Bin(w);

					StringBuilder builder2 = new StringBuilder();

					for (int i = u.length(); i < 5; i++) {
						builder2.append("0");
					}

					u = builder2.toString() + u;

					StringBuilder builder3 = new StringBuilder();

					for (int i = z.length(); i < 5; i++) {
						builder3.append("0");
					}

					z = builder3.toString() + z;

					StringBuilder builder4 = new StringBuilder();

					for (int i = w.length(); i < 5; i++) {
						builder4.append("0");
					}

					w = builder3.toString() + w;

					String bin = special + u + w + z + "00000" + q;

					String hex = bin2Hex(bin);
					hex = "0x0" + hex;

					finalhex = hex;
					// instructions.setText(finalhex);
				}
			}

			else if (type.equalsIgnoreCase("I_TYPE")) {
				String a = hex2Bin(op);//
				String b = tokens[1];
				String d = tokens[2];
				String e = tokens[3];

				String r = "";
				String z = "";
				String u = "";

				if (e.startsWith("0x")) {
					r = e.substring(2, e.length());
				}

				r = hex2Bin(r);

				StringBuilder builder = new StringBuilder();

				for (int i = a.length(); i < 6; i++) {
					builder.append("0");
				}

				if (!(d.startsWith("$")) || !(b.startsWith("$"))) {
					d = "$" + d;
					b = "$" + b;
				}
				boolean u1 = false, z1 = false;
				for (int i = 0; i < c.REGISTERS.length; i++) {
					if (d.equals(c.REGISTERS[i][0])) {
						u = c.REGISTERS[i][1]; u1 = true;
					}

					if (b.equals(c.REGISTERS[i][0])) {
						z = c.REGISTERS[i][1]; z1 = true;
					}
				}
				if (!u1 || !z1) return finalhex;
				u = hex2Bin(u);
				z = hex2Bin(z);

				StringBuilder builder1 = new StringBuilder();

				for (int i = r.length(); i < 16; i++) {
					builder1.append("0");
				}

				r = builder1.toString() + r;

				StringBuilder builder2 = new StringBuilder();

				for (int i = u.length(); i < 5; i++) {
					builder2.append("0");
				}

				u = builder2.toString() + u;

				StringBuilder builder3 = new StringBuilder();

				for (int i = z.length(); i < 5; i++) {
					builder3.append("0");
				}

				z = builder3.toString() + z;

				String bin = builder.toString() + a + u + z + r;
				String hex = bin2Hex(bin);
				hex = "0x" + hex;


				finalhex = hex;
				// instructions.setText(code);
			}

			else if (type.equalsIgnoreCase("J_TYPE")) {
				String a = hex2Bin(op);
				String b = tokens[1];
				String r = "";
				String z = "";

				if (b.startsWith("0x")) {
					r = b.substring(2, b.length());
				}

				r = hex2Bin(r);

				StringBuilder builder = new StringBuilder();

				for (int i = r.length(); i < 26; i++) {
					builder.append("0");
				}

				z = builder.toString() + r;

				StringBuilder builder2 = new StringBuilder();

				for (int i = a.length(); i < 6; i++) {
					builder2.append("0");
				}

				String bin = builder2.toString() + a + z;

				String hex = bin2Hex(bin);
				hex = "0x0" + hex;


				finalhex = hex;
			} else
				finalhex = "INVALID CODE";
			return finalhex;
		}

		private String buildCode(String hex) {
			boolean R = false;
			String y = hex;
			String z = "";
			String machinecode = "INVALID HEX";
			if (y.length() < 8) return machinecode;
			if (y.length() == 10) {
				y = y.substring(2, 10);
				z = hex2Bin(y);
			}

			else {
				z = hex2Bin(y);
			}

			String l = y.substring(6, 8);
			String rtype = "";

			for (int i = 0; i < c.HEX_MIPS_TABLE.length; i++) {
				if (l.equals(c.HEX_MIPS_TABLE[i][3])) {
					rtype = c.HEX_MIPS_TABLE[i][0];
					R = true;
				}
			}

			StringBuilder builder = new StringBuilder();

			for (int i = z.length(); i < 32; i++) {
				builder.append("0");
			}

			String r = builder.toString() + z;
			String n = "";
			String m = "";

			n = r.substring(0, 6);

			m = bin2Hex(n);

			String I = "";
			String J = "";
			String answer = "";

			for (int i = 0; i < c.HEX_MIPS_TABLE.length; i++) {
				if (m.equals(c.HEX_MIPS_TABLE[i][2])) {
					answer = c.HEX_MIPS_TABLE[i][0];
					I = c.HEX_MIPS_TABLE[i][1];
					J = c.HEX_MIPS_TABLE[i][1];
				}
			}

			if (R) {

				String special = r.substring(0, 6);
				String k = r.substring(6, 11);
				String t = r.substring(11, 16);
				String u = r.substring(16, 21);
				String shamt = r.substring(21, 26);

				special = bin2Hex(special);
				shamt = bin2Hex(shamt);
				k = bin2Hex(k);
				t = bin2Hex(t);
				u = bin2Hex(u);
				boolean u1 = false, t1 = false, k1 = false;
				for (int i = 0; i < c.REGISTERS.length; i++) {
					if (u.equals(c.REGISTERS[i][1])) {
						u = c.REGISTERS[i][0];
						u1 = true;
					}

					if (t.equals(c.REGISTERS[i][1])) {
						t = c.REGISTERS[i][0];
						t1 = true;
					}

					if (k.equals(c.REGISTERS[i][1])) {
						k = c.REGISTERS[i][0];
						k1 = true;
					}
				}
				if (!u1 || !k1 || !t1) return machinecode;

				if (!(special.equals("0")) || !(shamt.equals("0"))) {
					machinecode = rtype + " " + shamt + " " + u + " " + k + " " + t + " " + special;
				}

				else {
					machinecode = rtype + " " + u + " " + k + " " + t;
				}
			}

			else if (I.equals("I_TYPE")) {

				String k = r.substring(16, 32);
				String t = r.substring(6, 11);
				String u = r.substring(11, 16);

				k = bin2Hex(k);
				t = bin2Hex(t);
				u = bin2Hex(u);
				k = "0x" + k;
				boolean u1 = false, t1 = false;
				for (int i = 0; i < c.REGISTERS.length; i++) {
					if (u.equals(c.REGISTERS[i][1])) {
						u = c.REGISTERS[i][0];
						u1 = true;
					}

					if (t.equals(c.REGISTERS[i][1])) {
						t = c.REGISTERS[i][0];
						t1 = true;
					}
				}
				if (!u1 || !t1) return machinecode;
				machinecode = answer + " " + u + " " + t + " " + k;
			}

			else if (J.equals("J_TYPE")) {

				String k = r.substring(6, 32);
				k = bin2Hex(k);
				k = "0x0" + k;

				machinecode = answer + " " + k;
			}

			else {
				machinecode = "INVALID HEX";
			}
			return machinecode;
		}



	}


	private class CodeDisplayKeystrokeListener extends KeyAdapter {

		// Enter key is echoed on component after keyPressed but before keyTyped?
		// Consuming the VK_ENTER event in keyTyped does not suppress it but this will.
		public void keyPressed(KeyEvent e) {
			if (e.getKeyChar() == KeyEvent.VK_ENTER || e.getKeyChar() == KeyEvent.VK_TAB) {
				updateDisplays(new AllChanges().buildHexStringFromCode(((JTextField)e.getSource()).getText()));
				if (errorcheck1(new AllChanges().buildHexStringFromCode(((JTextField)e.getSource()).getText()))) {
					Toolkit.getDefaultToolkit().beep();
				}
				// instructions.setText(defaultInstructions);
				e.consume();
			}
		}
	}
	//  Class to handle input keystrokes for hexadecimal field

	private class HexDisplayKeystrokeListener extends KeyAdapter {

		private int digitLength; // maximum number of digits long

		public HexDisplayKeystrokeListener(int length) {
			digitLength = length;
		}


		// Process user keystroke.  If not valid for the context, this
		// will consume the stroke and beep.
		public void keyTyped(KeyEvent e) {
			JTextField source = (JTextField) e.getComponent();
			if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE || e.getKeyChar() == KeyEvent.VK_TAB  || e.getKeyChar() == KeyEvent.VK_CONTROL )
				return;
			if (!isHexDigit(e.getKeyChar()) ||
			        source.getText().length() == digitLength && source.getSelectedText() == null) {
				if (e.getKeyChar() != KeyEvent.VK_ENTER && e.getKeyChar() != KeyEvent.VK_TAB) {
					Toolkit.getDefaultToolkit().beep();
					if (source.getText().length() == digitLength && source.getSelectedText() == null) {
						instructions.setText("Maximum length of this field is " + digitLength + ".");
					} else {
						instructions.setText("Only digits and A-F (or a-f) are accepted in hex code field.");
					}
				}
				e.consume();
			}
		}

		// Enter key is echoed on component after keyPressed but before keyTyped?
		// Consuming the VK_ENTER event in keyTyped does not suppress it but this will.
		public void keyPressed(KeyEvent e) {
			if (e.getKeyChar() == KeyEvent.VK_ENTER || e.getKeyChar() == KeyEvent.VK_TAB) {
				updateDisplays(new AllChanges().buildCodeFromHexString(((JTextField)e.getSource()).getText()));
				if (errorcheck1(new AllChanges().buildHexStringFromCode(((JTextField)e.getSource()).getText()))) {
					Toolkit.getDefaultToolkit().beep();
				}
				// instructions.setText(defaultInstructions);
				e.consume();
			}
		}


		// handy utility.
		private boolean isHexDigit(char digit) {
			boolean result = false;
			switch (digit) {
			case '0': case '1': case '2': case '3': case '4':
			case '5': case '6': case '7': case '8': case '9':
			case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
			case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
				result = true;
			}
			return result;
		}
	}




	class InstructionsPane extends JLabel {

		InstructionsPane(Component parent) {
			super(defaultInstructions);
			this.setFont(instructionsFont);
			this.setBackground(parent.getBackground());
		}

		public void setText(String text) {
			super.setText(text);
		}
	}
	public static String hex2Bin(String hex) {
		long i = Long.parseLong(hex, 16);
		String bin = Long.toBinaryString(i);

		return bin;
	}

	public static String bin2Hex(String bin) {
		long decimal = Long.parseLong(bin, 2);
		String hex = Long.toString(decimal, 16);

		return hex;
	}

	public class Converter {
		public final String[][] HEX_MIPS_TABLE =

		{

			/*{name},  {format},   {opcode(hex)}, {funct(hex)}*/

			{"add",    "R_TYPE",      "0",           "20"},

			{"addi",   "I_TYPE",      "8",           null},

			{"addiu",  "I_TYPE",      "9",           null},

			{"addu",   "R_TYPE",      "0",           "21"},

			{"and",    "R_TYPE",      "0",           "24"},

			{"andi",   "I_TYPE",      "c",           null},

			{"beq",    "I_TYPE",      "4",           null},

			{"bne",    "I_TYPE",      "5",           null},

			{"j",      "J_TYPE",      "2",           null},

			{"jal",    "J_TYPE",      "3",           null},

			{"jr",     "R_TYPE",      "0",           "8"},

			{"lbu",    "I_TYPE",      "24",          null},

			{"lhu",    "I_TYPE",      "25",          null},

			{"ll",     "I_TYPE",      "30",          null},

			{"lui",    "I_TYPE",      "f",           null},

			{"lw",     "I_TYPE",      "23",          null},

			{"nor",    "R_TYPE",      "0",           "27"},

			{"or",     "R_TYPE",      "0",           "25"},

			{"ori",    "I_TYPE",      "d",           null},

			{"slt",    "R_TYPE",      "0",           "2a"},

			{"slti",   "I_TYPE",      "a",           null},

			{"sltiu",  "I_TYPE",      "b",           null},

			{"sltu",   "R_TYPE",      "0",           "2b"},

			{"sll",    "R_TYPE",      "0",           "0"},

			{"srl",    "R_TYPE",      "0",           "2"},

			{"sb",     "I_TYPE",      "28",          null},

			{"sc",     "I_TYPE",      "38",          null},

			{"sh",     "I_TYPE",      "29",          null},

			{"sw",     "I_TYPE",      "2b",          null},

			{"sub",    "R_TYPE",      "0",           "22"},

			{"subu",   "R_TYPE",      "0",           "23"}

		};




		public final String[][] REGISTERS =

		{

			/*name      number(hex)*/

			{"$zero",   "0"},

			{"$at",     "1"},

			{"$v0",     "2"},

			{"$v1",     "3"},

			{"$a0",     "4"},

			{"$a1",     "5"},

			{"$a2",     "6"},

			{"$a3",     "7"},

			{"$t0",     "8"},

			{"$t1",     "9"},

			{"$t2",     "a"},

			{"$t3",     "b"},

			{"$t4",     "c"},

			{"$t5",     "d"},

			{"$t6",     "e"},

			{"$t7",     "f"},

			{"$s0",     "10"},

			{"$s1",     "11"},

			{"$s2",     "12"},

			{"$s3",     "13"},

			{"$s4",     "14"},

			{"$s5",     "15"},

			{"$s6",     "16"},

			{"$s7",     "17"},

			{"$t8",     "18"},

			{"$t9",     "19"},

			{"$k0",     "1a"},

			{"$k1",     "1b"},

			{"$gp",     "1c"},

			{"$sp",     "1d"},

			{"$fp",     "1e"},

			{"$ra",     "1f"}

		};
	}


}

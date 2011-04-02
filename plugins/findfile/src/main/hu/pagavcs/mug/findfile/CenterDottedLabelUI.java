package hu.pagavcs.mug.findfile;

import java.awt.FontMetrics;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicLabelUI;

public class CenterDottedLabelUI extends BasicLabelUI {

	 protected String layoutCL(JLabel label, FontMetrics fontMetrics, String text, Icon icon, Rectangle viewR, Rectangle iconR, Rectangle textR) {

		String result = super.layoutCL(label, fontMetrics, text, icon, viewR, iconR, textR);

		if ((text == null) || text.equals("")) {
			return result;
		}

		if (!text.equals(result)) {
			int halfChunkSize = (result.length() - 3) / 2 - 1;

			result = text.substring(0, halfChunkSize) + "..." + text.substring(text.length() - halfChunkSize, text.length());
		}

		return result;
	}
}

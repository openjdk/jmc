package org.openjdk.jmc.ui.common.util;

import org.eclipse.jface.util.PropertyChangeEvent;

import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;

public class ThemeUtils {

	private static boolean isCurrentThemeDark;

	static {
		updateCurrentThemeDarkModeStatus(); // To Initialize the value
		IPropertyChangeListener propertyChangeListener = new IPropertyChangeListener() {

			public void propertyChange(PropertyChangeEvent event) {
				if (event.getProperty().equalsIgnoreCase(JFacePreferences.CONTENT_ASSIST_BACKGROUND_COLOR)) {
					updateCurrentThemeDarkModeStatus(); // To update the value whenever property changes
				}

			}

		};
		PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(propertyChangeListener);

	}

	public static boolean isDarkTheme() {
		return isCurrentThemeDark;
	}

	private static final double BRIGHTNESS_THRESHOLD = 0.5;

	private static void updateCurrentThemeDarkModeStatus() {
		ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
		RGB backgroundColor = colorRegistry.getRGB(JFacePreferences.CONTENT_ASSIST_BACKGROUND_COLOR);

		if (backgroundColor == null) {
			// Fallback to a default behavior if the color is not available
			isCurrentThemeDark = false;
		} else {
			isCurrentThemeDark = calculateBrightness(backgroundColor) < BRIGHTNESS_THRESHOLD;
		}

	}

	private static double calculateBrightness(RGB color) {
		// Using the HSP color model for perceived brightness
		// See: http://alienryderflex.com/hsp.html
		return Math.sqrt(
				0.299 * color.red * color.red + 0.587 * color.green * color.green + 0.114 * color.blue * color.blue)
				/ 255.0;
	}
}

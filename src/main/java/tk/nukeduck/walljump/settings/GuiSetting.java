package tk.nukeduck.walljump.settings;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.Property;

public class GuiSetting extends GuiButton {
	protected final Property property;

	public GuiSetting(int id, int x, int y, Property property) {
		this(id, x, y, 200, 20, property);
	}

	public GuiSetting(int buttonId, int x, int y, int widthIn, int heightIn, Property property) {
		super(buttonId, x, y, widthIn, heightIn, null);
		this.property = property;
		displayString = getText();
	}

	public String getName() {
		return property.getName();
	}

	protected String getDisplayValue() {
		return property.getString();
	}

	public void set(String value) {
		property.set(value);
		displayString = getText();
	}

	public void next() {
		String[] valid = property.getValidValues();
		int i = ArrayUtils.indexOf(valid, property.getString()) + 1;
		if(i == valid.length) i = 0;

		set(valid[i]);
	}

	private String getText() {
		return I18n.format("options.walljump." + property.getName()) + ": " + getDisplayValue();
	}
}

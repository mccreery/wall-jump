package tk.nukeduck.walljump.settings;

import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.Property;

public class GuiToggle extends GuiSetting {
	public GuiToggle(int id, int x, int y, Property property) {
		this(id, x, y, 200, 20, property);
	}

	public GuiToggle(int buttonId, int x, int y, int widthIn, int heightIn, Property property) {
		super(buttonId, x, y, widthIn, heightIn, property);
	}

	@Override
	public void next() {
		set(Boolean.toString(!property.getBoolean()));
	}

	@Override
	protected String getDisplayValue() {
		return I18n.format(property.getBoolean() ? "options.on" : "options.off");
	}
}

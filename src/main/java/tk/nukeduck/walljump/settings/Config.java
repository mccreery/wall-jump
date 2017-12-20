package tk.nukeduck.walljump.settings;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class Config extends Configuration {
	public Config(File file) {
		super(file);
	}

	private Property enabled, autoTurn, jumpKey, circleMode,
		barIcons, capacity, display;

	public Property exceptions;

	@Override
	public void load() {
		super.load();

		enabled = get(CATEGORY_GENERAL, "enabled", true);
		autoTurn = get(CATEGORY_GENERAL, "autoTurn", true);
		jumpKey = get(CATEGORY_GENERAL, "jumpKey", true);
		circleMode = get(CATEGORY_GENERAL, "circleMode", false);

		barIcons = get(CATEGORY_GENERAL, "barIcons", "boots", "[boots, feathers, players, smoke]", new String[] {"boots", "feathers", "players", "smoke"});
		capacity = get(CATEGORY_GENERAL, "capacity", "fixed", "[fixed, armor, unlimited]", new String[] {"fixed", "armor", "unlimited"});
		display = get(CATEGORY_GENERAL, "display", "icons", "[icons, text]", new String[] {"icons", "text"});
		exceptions = get(CATEGORY_GENERAL, "exceptions", "");

		if(hasChanged()) save();
	}

	public boolean enabled() {return enabled.getBoolean();}
	public boolean autoTurn() {return autoTurn.getBoolean();}
	public boolean jumpKey() {return jumpKey.getBoolean();}
	public boolean circleMode() {return circleMode.getBoolean();}
	public String barIcons() {return barIcons.getString();}
	public String capacity() {return capacity.getString();}
	public String display() {return display.getString();}

	public GuiSetting[] getButtons() {
		return new GuiSetting[] {
			new GuiToggle(0, 150, 20, enabled),
			new GuiToggle(0, 150, 20, autoTurn),
			new GuiToggle(0, 150, 20, jumpKey),
			new GuiToggle(0, 150, 20, circleMode),
			new GuiSetting(0, 150, 20, barIcons),
			new GuiSetting(0, 150, 20, capacity),
			new GuiSetting(0, 150, 20, display)
		};
	}
}

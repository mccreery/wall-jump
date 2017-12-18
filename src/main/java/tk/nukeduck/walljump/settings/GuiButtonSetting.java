package tk.nukeduck.walljump.settings;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.StatCollector;

public class GuiButtonSetting extends GuiButton {
	private WallJumpSettings<?> setting;
	
	public GuiButtonSetting(int buttonId, int x, int y, WallJumpSettings<?> setting) {
		super(buttonId, x, y, "");
		this.setting = setting;
	}
	
	public GuiButtonSetting(int buttonId, int x, int y, int widthIn, int heightIn, WallJumpSettings<?> setting) {
		super(buttonId, x, y, widthIn, heightIn, "");
		this.setting = setting;
	}
	
	public WallJumpSettings<?> getSetting() {
		return this.setting;
	}
	
	public String getLocalizedName() {
		return StatCollector.translateToLocal("menu.walljump.setting." + setting.getName());
	}
}

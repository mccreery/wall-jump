package tk.nukeduck.walljump.settings;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.lang3.math.NumberUtils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.command.NumberInvalidException;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class Config extends Configuration {
	public Config(File file) {
		super(file);
	}

	private static Property enabled, autoTurn, jumpKey, circleMode,
		barIcons, capacity, display, exceptionsP, position;

	@Override
	public void load() {
		super.load();

		enabled = get(CATEGORY_GENERAL, "enabled", true);
		autoTurn = get(CATEGORY_GENERAL, "autoTurn", true);
		jumpKey = get(CATEGORY_GENERAL, "jumpKey", true);
		circleMode = get(CATEGORY_GENERAL, "circleMode", false);

		barIcons = get(CATEGORY_GENERAL, "barIcons", "boots", "[boots, feathers, players, smoke]", icons);
		capacity = get(CATEGORY_GENERAL, "capacity", "fixed", "[fixed, armor, unlimited]", new String[] {"fixed", "armor", "unlimited"});
		display = get(CATEGORY_GENERAL, "display", "icons", "[icons, text]", new String[] {"icons", "text"});
		position = get(CATEGORY_GENERAL, "position", "bar", "[bar, ne, se, sw, nw]", new String[] {"bar, ne, se, sw, nw"});

		exceptionsP = get(CATEGORY_GENERAL, "exceptions", "");
		loadExceptions(exceptionsP.getString());

		if(hasChanged()) save();
	}

	public static boolean enabled() {return enabled.getBoolean();}
	public static boolean autoTurn() {return autoTurn.getBoolean();}
	public static boolean jumpKey() {return jumpKey.getBoolean();}
	public static boolean circleMode() {return circleMode.getBoolean();}

	private static final String[] icons = {"boots", "feathers", "players", "smoke"};

	public static int iconIndex() {
		String icon = barIcons.getString();

		for(int i = 0; i < icons.length; i++) {
			if(icons[i].equals(icon)) {
				return i;
			}
		}
		return 0;
	}

	public static String capacity() {return capacity.getString();}
	public static String display() {return display.getString();}
	public static String position() {return position.getString();}

	public static void updateExceptions(String exceptions) {
		Config.exceptionsP.set(exceptions);
		loadExceptions(exceptions);
	}

	/** An array of {@link IBlockState}s which are not jumpable */
	public static final ArrayList<IBlockState> exceptions = new ArrayList<IBlockState>();

	public static boolean canJumpFrom(IBlockState state) {
		return !exceptions.contains(state);
	}

	private static void loadExceptions(String exceptionsS) {
		String[] blocks = exceptionsS.split(",");
		exceptions.clear();
		exceptions.ensureCapacity(blocks.length);

		for(String block : blocks) {
			IBlockState state = parseState(block);

			if(state != null) {
				exceptions.add(state);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private static IBlockState parseState(String block) {
		int meta = 0;
		int colon = block.lastIndexOf(':');

		if(colon != -1) {
			String lastToken = block.substring(colon + 1);

			if(NumberUtils.isParsable(lastToken)) {
				meta = Integer.parseInt(lastToken);
				block = block.substring(0, colon);
			}
		}

		try {
			return CommandBase.getBlockByText(null, block).getStateFromMeta(meta);
		} catch(NumberInvalidException e) {
			System.err.println(I18n.format(e.getMessage()));
			return null;
		}
	}

	public static GuiSetting[] getButtons() {
		return new GuiSetting[] {
			new GuiToggle(0, 0, 0, 150, 20, enabled),
			new GuiToggle(0, 0, 0, 150, 20, autoTurn),
			new GuiToggle(0, 0, 0, 150, 20, jumpKey),
			new GuiToggle(0, 0, 0, 150, 20, circleMode),
			new GuiSetting(0, 0, 0, 150, 20, barIcons),
			new GuiSetting(0, 0, 0, 150, 20, capacity),
			new GuiSetting(0, 0, 0, 150, 20, display)
		};
	}
}

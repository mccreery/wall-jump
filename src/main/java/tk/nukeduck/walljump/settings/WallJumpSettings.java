package tk.nukeduck.walljump.settings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.util.StatCollector;
import net.minecraftforge.fml.common.Loader;

public class WallJumpSettings<T> {
	private static List<WallJumpSettings<?>> settings = new ArrayList<WallJumpSettings<?>>();
	
	public static WallJumpSettings<Boolean> flyingWallJump = new WallJumpSettings<Boolean>("flyingWallJump", false, Boolean.class),
		autoTurn = new WallJumpSettings<Boolean>("autoTurn", true, Boolean.class),
		wallJumping = new WallJumpSettings<Boolean>("wallJumping", true, Boolean.class),
		useSpace = new WallJumpSettings<Boolean>("useSpace", false, Boolean.class),
		circleMode = new WallJumpSettings<Boolean>("circleMode", false, Boolean.class);
	public static WallJumpSettings<String> barIcons = new WallJumpSettings<String>("barIcons", "boots", String.class).setPossibleValues(new String[] {"boots", "feathers", "players", "smoke"}),
		jumpType = new WallJumpSettings<String>("jumpType", "fixed", String.class).setPossibleValues(new String[] {"fixed", "limited", "unlimited"}),
		displayType = new WallJumpSettings<String>("displayType" , "barAboveFood", String.class).setPossibleValues(new String[] {"barAboveFood", "barAboveArmor", "barTopLeft",
			"barTopRight", "barBottomLeft", "barBottomRight", "textTopLeft", "textTopRight", "textBottomLeft", "textBottomRight"}),
		exceptions = new WallJumpSettings<String>("exceptions", "", String.class);
	
	private T[] possibleValues = null;
	private String name, description;
	public T value;
	public final Class<T> type;
	
	public WallJumpSettings(String name, String description, T defaultValue, Class<T> type) {
		this.name = name;
		this.description = description;
		this.value = defaultValue;
		this.type = type;
		this.register();
	}
	
	public WallJumpSettings(String name, T defaultValue, Class<T> type) {
		this(name, "", defaultValue, type);
	}
	
	public String getName() {
		return this.name;
	}
	
	public T getValue() {
		return this.value;
	}
	
	public String getLocalizedValue() {
		return StatCollector.translateToLocal("menu.walljump.setting." + this.getName() + "." + String.valueOf(this.value));
	}
	
	public void setValue(Object value) {
		if(value.getClass().equals(this.value.getClass())) {
			this.value = (T) value;
		} else if(value.getClass().equals(String.class)) {
			this.value = this.convertToObject((String) value);
		}
	}
	
	protected WallJumpSettings setPossibleValues(T[] values) {
		this.possibleValues = values;
		return this;
	}
	
	public T[] getPossibleValues() {
		return this.possibleValues;
	}
	
	public static void toggleValue(WallJumpSettings<Boolean> setting) {
		setting.setValue(!setting.getValue());
	}
	
	public static void incrementValue(WallJumpSettings<String> setting) {
		String[] possibles = setting.getPossibleValues();
		if(possibles != null) {
			int currentIndex = ArrayUtils.indexOf(possibles, setting.getValue());
			if(currentIndex < possibles.length - 1) {
				setting.setValue(possibles[currentIndex + 1]);
			} else {
				setting.setValue(possibles[0]);
			}
		}
	}
	
	public static void addToValue(WallJumpSettings<Integer> setting, int x) {
		setting.setValue(setting.getValue() + x);
	}
	
	/**
	 * Allows overriding certain values' format in the config file.
	 * @param value The value of the setting.
	 * @return A string representation of the given argument.
	 */
	private static String convertToString(WallJumpSettings setting) {
		Class type = setting.type;
		if(type.equals(String.class)) {
			return (String) setting.getValue();
		} else if(type.equals(boolean.class)) {
			return String.valueOf((Boolean) setting.getValue());
		} else if(type.equals(int.class)) {
			return String.valueOf((Integer) setting.getValue());
		}
		return String.valueOf(setting.getValue());
	}
	
	
	private T convertToObject(String value) {
		Class type = this.type;
		if(type.equals(String.class)) {
			return (T) value;
		} else if(type.equals(Boolean.class)) {
			return (T) Boolean.valueOf(value);
		} else if(type.equals(Integer.class)) {
			return (T) Integer.valueOf(value);
		}
		return null;
	}
	
	public void register() {register(this);}
	public static void register(WallJumpSettings setting) {settings.add(setting);}
	
	private static final String configPath = Loader.instance().getConfigDir() + "/walljump.txt";
	private static final String lineSeperator = System.getProperty("line.separator");
	
	/**
	 * Saves Wall Jump settings using a custom system I created because Forge
	 * configuration files are so limited and aren't really geared towards these
	 * kinds of settings.
	 */
	public static void saveSettings() {
		try {
			System.out.println("Saving WallJump settings...");
			FileWriter settingsWriter = new FileWriter(configPath);
			for(int i = 0; i < settings.size(); i++) {
				System.out.println("Writing " + settings.get(i).getName());
				settingsWriter.write(settings.get(i).getName() + ": " + convertToString(settings.get(i)));
				if(i < settings.size() - 1) settingsWriter.write(lineSeperator);
			}
			settingsWriter.close();
		} catch(IOException e) {
			System.out.println("Failed to save settings to "
				+ configPath + ". " + e.getMessage());
		}
	}
	
	/**
	 * Locates and loads the settings file for use in-game.
	 */
	public static void loadSettings() {
		try {
			String[] settings2 = readFileAsString(configPath).split(lineSeperator);
			for(String line : settings2) {
				String[] nameToValue = line.replace(": ", ":").split(":", 2);
				for(WallJumpSettings setting : settings) {
					if(setting.getName().equals(nameToValue[0])) {
						setting.setValue(nameToValue[1]);
					}
				}
			}
			for(int i = 0; i < settings.size(); i++) {
				if(settings2.length > i) settings.get(i).setValue(settings2[i].replace(": ", ":").split(":", 2)[1]);
			}
		} catch(Exception e) {
			System.out.println("Failed to load settings from "
				+ configPath + ". \n" + e.getMessage() + "\nThe default configuration was saved.");
			saveSettings();
		}
	}
	
	/**
	 * Reads a file from the input directory on the file system and returns it
	 * as its string contents.
	 * 
	 * @return String
	 * @throws java.io.IOException
	 */
	public static String readFileAsString(String filePath) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		char[] buf = new char[1024];
		
		int numRead = 0;
		while((numRead = reader.read(buf)) != -1) {
			String readData = String.valueOf(buf, 0, numRead);
			fileData.append(readData);
			buf = new char[1024];
		}
		reader.close();
		return fileData.toString();
	}
}
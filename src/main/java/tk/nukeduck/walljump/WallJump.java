package tk.nukeduck.walljump;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import tk.nukeduck.walljump.events.Events;
import tk.nukeduck.walljump.settings.Config;
import tk.nukeduck.walljump.settings.GuiWallJumpMenu;

@Mod(modid = "walljump", name = "Wall Jump", version = "1.2.4")
public class WallJump {
	public static Minecraft MINECRAFT = Minecraft.getMinecraft();
	public static Events    EVENTS    = new Events();

	public static Config config;

	public static final KeyBinding SETTINGS = new KeyBinding("wallJump.key.menu", Keyboard.KEY_J, "wallJump.category");
	public static final KeyBinding ACTION   = new KeyBinding("wallJump.key.jump", Keyboard.KEY_F, "wallJump.category");

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Config(event.getSuggestedConfigurationFile());
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		ClientRegistry.registerKeyBinding(SETTINGS);
		ClientRegistry.registerKeyBinding(ACTION);

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(EVENTS);

		EVENTS.reloadProhibitedBlocks();
	}

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		if(WallJump.MINECRAFT.currentScreen == null && Keyboard.getEventKeyState() && Keyboard.getEventKey() == SETTINGS.getKeyCode()) {
			WallJump.MINECRAFT.displayGuiScreen(new GuiWallJumpMenu());
		}
	}
}

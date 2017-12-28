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
	public static final KeyBinding SETTINGS = new KeyBinding("wallJump.key.menu", Keyboard.KEY_J, "wallJump.category");
	public static final KeyBinding ACTION   = new KeyBinding("wallJump.key.jump", Keyboard.KEY_F, "wallJump.category");

	public static Minecraft MC = Minecraft.getMinecraft();
	public static Config config;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		config = new Config(event.getSuggestedConfigurationFile());
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		ClientRegistry.registerKeyBinding(SETTINGS);
		ClientRegistry.registerKeyBinding(ACTION);

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(new Events());
	}

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		if(MC.currentScreen == null && Keyboard.getEventKeyState() && Keyboard.getEventKey() == SETTINGS.getKeyCode()) {
			MC.displayGuiScreen(new GuiWallJumpMenu());
		}
	}
}

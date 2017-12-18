package tk.nukeduck.walljump.events;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;
import tk.nukeduck.walljump.WallJump;
import tk.nukeduck.walljump.settings.GuiWallJumpMenu;

public class KeyEvents {
	public static KeyBinding settingsMenu = new KeyBinding("key.walljump_menu.desc", Keyboard.KEY_J, "key.walljump.category");
	public static KeyBinding wallJump = new KeyBinding("key.walljump.desc", Keyboard.KEY_F, "key.walljump.category");
	
	public KeyEvents() {
		ClientRegistry.registerKeyBinding(settingsMenu);
		ClientRegistry.registerKeyBinding(wallJump);
	}
	
	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		if(WallJump.mc.currentScreen == null && settingsMenu.isPressed()) {
			WallJump.mc.displayGuiScreen(new GuiWallJumpMenu());
		}
	}
}

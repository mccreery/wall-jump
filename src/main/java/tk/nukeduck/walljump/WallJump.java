package tk.nukeduck.walljump;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import tk.nukeduck.walljump.events.KeyEvents;
import tk.nukeduck.walljump.events.TickEvents;
import tk.nukeduck.walljump.settings.WallJumpSettings;

@Mod(modid = "walljump", name = "WallJump", version = "1.2.3")
public class WallJump {
	// TODO fix all this insane generics malarki

	public static Minecraft mc = Minecraft.getMinecraft();
	public static KeyEvents menuHandler;
	public static TickEvents tickHandler;
	
	@EventHandler
	public void load(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(menuHandler = new KeyEvents());
		MinecraftForge.EVENT_BUS.register(tickHandler = new TickEvents());
		
		WallJumpSettings.loadSettings();
		tickHandler.reloadProhibitedBlocks();
	}
}

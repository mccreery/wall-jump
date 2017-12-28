package tk.nukeduck.walljump.events;

import static tk.nukeduck.walljump.WallJump.MC;

import java.util.HashMap;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import tk.nukeduck.walljump.WallJump;
import tk.nukeduck.walljump.settings.Config;

public class Events {
	private static final ResourceLocation ICONS = new ResourceLocation("walljump", "textures/gui/wallJumpIcons.png");

	/** Indicates how many degrees are still to be turned by the player. */
	private static int rotationRemaining;
	/** Dictates whether the player should be turning to the left or the right. {@code true} means left, {@code false} means right. */
	private static boolean left;
	/** Keeps track of how many wall jumps the player has left before they run out of energy. */
	private static int jumpsLeft = 3;
	/** If this is {@code true} it means the player is in the air and has wall jumped since becoming air-bound. */
	public static boolean isWallJumping;

	/** What the player's Y motion should be set to as a bounce-back. */
	private static final double bounceBack = 0.6;

	private boolean shouldSprint;

	/** The maximum amount of jumps to be restored based on the boots being worn by the player. */
	private static final HashMap<ArmorMaterial, Integer> armorLimits = new HashMap<ArmorMaterial, Integer>();

	static {
		armorLimits.put(ArmorMaterial.LEATHER,  4);
		armorLimits.put(ArmorMaterial.CHAIN,   10);
		armorLimits.put(ArmorMaterial.IRON,     6);
		armorLimits.put(ArmorMaterial.GOLD,     8);
		armorLimits.put(ArmorMaterial.DIAMOND, 12);
	}

	public int getJumpCount(ItemStack boots) {
		if(boots == null || boots.getItem() == null || !(boots.getItem() instanceof ItemArmor)) return 3;
		ArmorMaterial material = ((ItemArmor) boots.getItem()).getArmorMaterial();
		return armorLimits.containsKey(material) ? armorLimits.get(material) : 3;
	}

	public static boolean isKeyDown() {
		return WallJump.ACTION.isKeyDown() || (Config.jumpKey() && MC.gameSettings.keyBindJump.isKeyDown());
	}

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent e) {
		if(e.phase == Phase.END && MC != null) {
			if(!MC.player.collidedHorizontally) {
				shouldSprint = MC.player.isSprinting();
			}
			IBlockState blockFacing = getWall(e.player.world, e.player);

			if(blockFacing != null && Config.canJumpFrom(blockFacing) && blockFacing.getMaterial().blocksMovement() && isKeyDown()) {
				if(rotationRemaining < 80 && MC.player.collidedHorizontally && MC.player.motionY < 0 && MC.player.fallDistance > 0 && jumpsLeft > 0 && !MC.player.isInWater() && Config.enabled()) {
					if(!MC.player.capabilities.isFlying) {
						playJumpSound(e.player.world, e.player);

						if(!MC.player.capabilities.isCreativeMode && !Config.capacity().equals("unlimited")) jumpsLeft -= 1;
						if(!Config.circleMode()) left = !left;
						MC.player.motionY = bounceBack;

						double a = Math.toRadians(MC.player.rotationYaw + 180);
						MC.player.motionX = -Math.sin(a) / 4;
						MC.player.motionZ = Math.cos(a) / 4;

						rotationRemaining = 180;
						//fallDistance = 0;
						isWallJumping = true;

						//WallJump.mc.player.fallDistance = 0; // Handled on server side, can't do anything about this.
					}
				}
			}

			if(!MC.isGamePaused() && rotationRemaining > 0) {
				if(Config.autoTurn()) {
					if(left) {
						if(rotationRemaining >= 25) {
							MC.player.rotationYaw -= 25;
							rotationRemaining -= 25;
						} else {
							MC.player.rotationYaw -= rotationRemaining;
							rotationRemaining = 0;
						}
					} else {
						if(rotationRemaining >= 25) {
							MC.player.rotationYaw += 25;
							rotationRemaining -= 25;
						} else {
							MC.player.rotationYaw += rotationRemaining;
							rotationRemaining = 0;
						}
					}
				} else {
					rotationRemaining = 0;
				}
			}
		}

		//Refill wall jump bar according to boots worn
		if(MC.world != null && MC.player.onGround) {
			String capacity = Config.capacity();
			if(capacity.equals("limited")) {
				if(MC.player.inventory.armorItemInSlot(0) != null) {
					jumpsLeft = getJumpCount(MC.player.inventory.armorItemInSlot(0));
				} else {
					jumpsLeft = 3; //Barefoot
				}
			} else if(capacity.equals("unlimited")) {
				jumpsLeft = Integer.MAX_VALUE;
			} else {
				jumpsLeft = 10;
			}
		}

		if(MC.player != null && MC.player.onGround) {
			isWallJumping = false;
		}

		if(shouldSprint && !MC.player.isSprinting()) {
			try {
				MC.player.setSprinting(true);
			} catch(Exception ex) {}
		}
	}

	@SubscribeEvent
	public void onRenderOverlayTick(RenderGameOverlayEvent.Post e) {
		if(e.getType() == ElementType.ALL && Minecraft.getMinecraft().playerController.shouldDrawHUD() && jumpsLeft > 0 && Config.enabled()) {
			MC.renderEngine.bindTexture(ICONS);

			ScaledResolution resolution = new ScaledResolution(MC);
			int width = resolution.getScaledWidth();
			int height = resolution.getScaledHeight();

			int left, top;
			String position = Config.position();
			boolean right = false;

			if("bar".equals(position)) {
				left = width / 2 - 90;
				top = height - GuiIngameForge.left_height;
				GuiIngameForge.left_height += 9;
			} else {
				top = position.charAt(0) == 's' ? height - 14 : 5;
				left = (right = position.charAt(1) == 'e') ? width - 5 : 5;
			}

			if("icons".equals(Config.display())) {
				drawBar(left, top, Config.iconIndex(), jumpsLeft, right);
			} else {
				drawText(left, top, right);
			}
		}
	}

	private static void drawText(int left, int top, boolean right) {
		String text = I18n.format("wallJump.remaining", Config.capacity().equals("unlimited") ? I18n.format("walljump.infinity") : jumpsLeft);

		if(right) {
			MC.ingameGUI.drawString(MC.fontRenderer, text, left - MC.fontRenderer.getStringWidth(text), top, 0xffffff);
		} else {
			MC.ingameGUI.drawString(MC.fontRenderer, text, left, top, 0xffffff);
		}
	}

	/** Draws {@code count} icons in a bar */
	private static void drawBar(int left, int top, int iconIndex, int count, boolean right) {
		iconIndex *= 9;

		if(count > 10) count = 10;
		if(right) left -= (count + 1) * 8;

		if(Config.enabled()) {
			for(int i = 0, x = left; i < count; i++, x += 8) {
				MC.ingameGUI.drawTexturedModalRect(x, top, iconIndex, 0, 9, 9);
			}
		}
	}

	/** Play a wall jump sound for the wall in front of the player */
	private static void playJumpSound(World world, EntityPlayer player) {
		BlockPos pos = getWallTarget(player);
		IBlockState state = world.getBlockState(pos);
		SoundType sound = state.getBlock().getSoundType(state, world, pos, player);

		player.playSound(sound.getStepSound(), sound.getVolume() * 0.5F, sound.getPitch() * 0.75F);
	}

	/** @return The wall in front of the player */
	private static IBlockState getWall(World world, EntityPlayer player) {
		return world.getBlockState(getWallTarget(player));
	}

	/** Gets the position of the potential wall in front of the player */
	private static BlockPos getWallTarget(EntityPlayer player) {
		return player.getPosition().offset(MC.getRenderViewEntity().getHorizontalFacing());
	}
}

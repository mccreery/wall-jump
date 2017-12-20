package tk.nukeduck.walljump.events;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.CommandBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import tk.nukeduck.walljump.WallJump;

public class Events {
	private static final ResourceLocation icons = new ResourceLocation("walljump", "textures/gui/wallJumpIcons.png");

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

	public Events() {}
	public IBlockState[] prohibitedBlocks = null;

	public boolean isProhibitedBlock(World world, EntityPlayer player) {
		return isProhibitedBlock(getWall(world, player));
	}

	public boolean isProhibitedBlock(IBlockState blockState) {
		for(IBlockState block : prohibitedBlocks) {
			if(blockState.equals(block))
				return true;
		}
		return false;
	}

	public IBlockState[] reloadProhibitedBlocks() {
		return prohibitedBlocks = this.getProhibitedBlocks();
	}

	public IBlockState[] getProhibitedBlocks() {
		String exceptions = WallJump.config.exceptions.getString();
		if(exceptions.length() == 0) return new IBlockState[0];

		ArrayList<IBlockState> stacks = new ArrayList<IBlockState>();
		String[] blocks = exceptions.split(",");

		for(String block : blocks) {
			String[] parts = block.split(":");

			if(parts.length == 3) parts = new String[] {parts[0] + ":" + parts[1], parts[2]};
			else if(parts.length == 2 && !isNumeric(parts[1])) parts = new String[] {parts[0] + ":" + parts[1]};

			System.out.println(parts[0] + (parts.length > 1 ? ", " + parts[1] : ""));

			try {
				stacks.add(CommandBase.getBlockByText(null, parts[0]).getStateFromMeta(parts.length > 1 ? Integer.parseInt(parts[1]) : 0));
			} catch(Exception e) {}
		}
		return stacks.toArray(new IBlockState[stacks.size()]);
	}

	public static boolean isNumeric(String s) {
		return s.matches("[0-9]+");
	}

	public static boolean isKeyDown() {
		return WallJump.ACTION.isKeyDown() || (WallJump.config.jumpKey() && WallJump.MINECRAFT.gameSettings.keyBindJump.isKeyDown());
	}

	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent e) {
		if(e.phase == Phase.END && WallJump.MINECRAFT != null) {
			if(!WallJump.MINECRAFT.player.collidedHorizontally) {
				shouldSprint = WallJump.MINECRAFT.player.isSprinting();
			}
			IBlockState blockFacing = getWall(e.player.world, e.player);

			if(blockFacing != null && !isProhibitedBlock(blockFacing) && blockFacing.getMaterial().blocksMovement() && isKeyDown()) {
				if(rotationRemaining < 80 && WallJump.MINECRAFT.player.collidedHorizontally && WallJump.MINECRAFT.player.motionY < 0 && WallJump.MINECRAFT.player.fallDistance > 0 && jumpsLeft > 0 && !WallJump.MINECRAFT.player.isInWater() && WallJump.config.enabled()) {
					if(!WallJump.MINECRAFT.player.capabilities.isFlying) {
						playJumpSound(e.player.world, e.player);

						if(!WallJump.MINECRAFT.player.capabilities.isCreativeMode && !WallJump.config.capacity().equals("unlimited")) jumpsLeft -= 1;
						if(!WallJump.config.circleMode()) left = !left;
						WallJump.MINECRAFT.player.motionY = bounceBack;

						double a = Math.toRadians(WallJump.MINECRAFT.player.rotationYaw + 180);
						WallJump.MINECRAFT.player.motionX = -Math.sin(a) / 4;
						WallJump.MINECRAFT.player.motionZ = Math.cos(a) / 4;

						rotationRemaining = 180;
						//fallDistance = 0;
						isWallJumping = true;

						//WallJump.mc.player.fallDistance = 0; // Handled on server side, can't do anything about this.
					}
				}
			}

			if(!WallJump.MINECRAFT.isGamePaused() && rotationRemaining > 0) {
				if(WallJump.config.autoTurn()) {
					if(left) {
						if(rotationRemaining >= 25) {
							WallJump.MINECRAFT.player.rotationYaw -= 25;
							rotationRemaining -= 25;
						} else {
							WallJump.MINECRAFT.player.rotationYaw -= rotationRemaining;
							rotationRemaining = 0;
						}
					} else {
						if(rotationRemaining >= 25) {
							WallJump.MINECRAFT.player.rotationYaw += 25;
							rotationRemaining -= 25;
						} else {
							WallJump.MINECRAFT.player.rotationYaw += rotationRemaining;
							rotationRemaining = 0;
						}
					}
				} else {
					rotationRemaining = 0;
				}
			}
		}

		//Refill wall jump bar according to boots worn
		if(WallJump.MINECRAFT.world != null && WallJump.MINECRAFT.player.onGround) {
			String capacity = WallJump.config.capacity();
			if(capacity.equals("limited")) {
				if(WallJump.MINECRAFT.player.inventory.armorItemInSlot(0) != null) {
					jumpsLeft = getJumpCount(WallJump.MINECRAFT.player.inventory.armorItemInSlot(0));
				} else {
					jumpsLeft = 3; //Barefoot
				}
			} else if(capacity.equals("unlimited")) {
				jumpsLeft = Integer.MAX_VALUE;
			} else {
				jumpsLeft = 10;
			}
		}

		if(WallJump.MINECRAFT.player != null && WallJump.MINECRAFT.player.onGround) {
			isWallJumping = false;
		}

		if(shouldSprint && !WallJump.MINECRAFT.player.isSprinting()) {
			try {
				WallJump.MINECRAFT.player.setSprinting(true);
			} catch(Exception ex) {}
		}
	}

	@SubscribeEvent
	public void onRenderOverlayTick(RenderGameOverlayEvent.Post e) {
		if(e.getType() == ElementType.ALL && Minecraft.getMinecraft().playerController.shouldDrawHUD()) {
			WallJump.MINECRAFT.renderEngine.bindTexture(icons);

			// Variables used to greatly shorten the following lines.
			ScaledResolution scaledResolution = new ScaledResolution(WallJump.MINECRAFT);

			int width = scaledResolution.getScaledWidth(), height = scaledResolution.getScaledHeight();
			int offsetX, offsetY;
			offsetX = offsetY = 5;

			String text = getText();

			switch(WallJump.config.display()) {
				case "icons":
					offsetX = width / 2 + 10;
					offsetY = WallJump.MINECRAFT.player.isInWater() ? height - 59 : height - 49; // TODO respond properly to bar height (GuiIngameForge)
					break;
				/*case 1: TODO reimplement moving the bar
					offsetX = width / 2 - 90;
					offsetY = (ForgeHooks.getTotalArmorValue(WallJump.MINECRAFT.player) > 0 ? height - 59 : height - 49);
					break;
				case 2:
					break; // Keep defaults
				case 3:
					offsetX = width - 85;
					// Keep default Y
					break;
				case 4:
					// Keep default X
					offsetY = height - 15;
					break;
				case 5:
					offsetX = width - 85;
					offsetY = height - 15;
					break;
				case 6:
					break; // Keep defaults
				case 7:
					offsetX = width - WallJump.MINECRAFT.fontRenderer.getStringWidth(text) - 5;
					// Keep default Y
					break;
				case 8:
					// Keep default X
					offsetY = height - 15;
					break;
				case 9:
					offsetX = width - WallJump.MINECRAFT.fontRenderer.getStringWidth(text) - 5;
					offsetY = height - 15;
					break;*/
			}

			if(jumpsLeft > 0 && WallJump.config.enabled()) {
				GuiIngame gui = WallJump.MINECRAFT.ingameGUI;

				if(WallJump.config.display().equals("icons")) {
					String barIcons = WallJump.config.barIcons();
					int u = barIcons.equals("feathers") ? 9 : barIcons.equals("boots") ? 0 : barIcons.equals("players") ? 18 : 27;
					for(int i = 0; i < jumpsLeft && i < 10; i++) {
						gui.drawTexturedModalRect(offsetX + 8 * i, offsetY, u, 0, 9, 9);
					}
				} else {
					gui.drawString(WallJump.MINECRAFT.fontRenderer, text, offsetX, offsetY, 0xffffff);
				}
			}
		}
	}

	public static String getText() {
		return I18n.format("wallJump.remaining", WallJump.config.capacity().equals("unlimited") ? I18n.format("walljump.infinity") : jumpsLeft);
	}

	/** Draws {@code count} icons in a bar */
	public static void drawIcons(int x, int y, int iconIndex, int count) {
		if(count > 0 && WallJump.config.enabled()) {
			// TODO move bar code
		}
	}

	/** Play a wall jump sound for the wall in front of the player */
	public static void playJumpSound(World world, EntityPlayer player) {
		BlockPos pos = getWallTarget(player);
		IBlockState state = world.getBlockState(pos);
		SoundType sound = state.getBlock().getSoundType(state, world, pos, player);

		player.playSound(sound.getStepSound(), sound.getVolume() * 0.5F, sound.getPitch() * 0.75F);
	}

	/** @return The wall in front of the player */
	public static IBlockState getWall(World world, EntityPlayer player) {
		return world.getBlockState(getWallTarget(player));
	}

	/** Gets the position of the potential wall in front of the player */
	public static BlockPos getWallTarget(EntityPlayer player) {
		return player.getPosition().offset(WallJump.MINECRAFT.getRenderViewEntity().getHorizontalFacing());
	}
}

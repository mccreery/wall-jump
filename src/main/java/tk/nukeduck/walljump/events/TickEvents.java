package tk.nukeduck.walljump.events;

import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glPopMatrix;
import static org.lwjgl.opengl.GL11.glPushMatrix;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang3.ArrayUtils;

import net.minecraft.block.Block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.command.CommandBase;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import tk.nukeduck.walljump.WallJump;
import tk.nukeduck.walljump.settings.WallJumpSettings;

public class TickEvents {
	private static final ResourceLocation icons = new ResourceLocation("walljump", "textures/gui/wallJumpIcons.png");
	
	/** Indicates how many degrees are still to be turned by the player. */
	private static int rotationRemaining;
	/** Dictates whether the player should be turning to the left or the right. {@code true} means left, {@code false} means right. */
	private static boolean left;
	/** Keeps track of how many wall jumps the player has left before they run out of energy. */
	private static int jumpsLeft = 3;
	/** If this is {@code true} it means the player is in the air and has wall jumped since becoming air-bound. */
	public static boolean isWallJumping;
	///**  Custom fall distance used in order to attempt fixing the fall damage bug. */
	//public static double fallDistance = 0;
	
	/** What the player's Y motion should be set to as a bounce-back. */
	private static final double bounceBack = 0.6;
	
	private boolean shouldSprint;
	
	/** The maximum amount of jumps to be restored based on the boots being worn by the player. */
	@SuppressWarnings("serial")
	private static HashMap<ArmorMaterial, Integer> armorLimits = new HashMap<ArmorMaterial, Integer>() {
		{
			put(ArmorMaterial.LEATHER, 4);
			put(ArmorMaterial.IRON, 6);
			put(ArmorMaterial.GOLD, 8);
			put(ArmorMaterial.DIAMOND, 12);
			put(ArmorMaterial.CHAIN, 10);
		}
	};
	
	public int getJumpCount(ItemStack boots) {
		if(boots == null || boots.getItem() == null || !(boots.getItem() instanceof ItemArmor)) return 3;
		ArmorMaterial material = ((ItemArmor) boots.getItem()).getArmorMaterial();
		return armorLimits.containsKey(material) ? armorLimits.get(material) : 3;
	}
	
	public TickEvents() {}
	public IBlockState[] prohibitedBlocks = null;
	
	public boolean isProhibitedBlock() {return isProhibitedBlock(getBlockFacing());}
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
		if(WallJumpSettings.exceptions.getValue().length() == 0) return new IBlockState[] {};
		
		ArrayList<IBlockState> stacks = new ArrayList<IBlockState>();
		String[] blocks = WallJumpSettings.exceptions.getValue().split(",");
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
		return KeyEvents.wallJump.isKeyDown() || (WallJumpSettings.useSpace.getValue() && WallJump.mc.gameSettings.keyBindJump.isKeyDown());
	}
	
	//public static IBlockState infront;
	
	@SubscribeEvent
	public void onPlayerTick(PlayerTickEvent e) {
		if(e.phase == Phase.END && WallJump.mc != null) {
			if(!WallJump.mc.thePlayer.isCollidedHorizontally) {
				shouldSprint = WallJump.mc.thePlayer.isSprinting();
			}
			IBlockState blockFacing = getBlockFacing();
			//infront = getBlockInFrontofPlayer();
			if(blockFacing != null && !isProhibitedBlock(blockFacing) && blockFacing.getBlock().getMaterial().blocksMovement() && isKeyDown()) {
				System.out.println("Jump!");
				if(rotationRemaining < 80 && WallJump.mc.thePlayer.isCollidedHorizontally && WallJump.mc.thePlayer.motionY < 0 && WallJump.mc.thePlayer.fallDistance > 0 && jumpsLeft > 0 && !WallJump.mc.thePlayer.isInWater() && WallJumpSettings.wallJumping.getValue()) {
					System.out.println("JUMP!");
					if(WallJumpSettings.flyingWallJump.getValue() || !WallJump.mc.thePlayer.capabilities.isFlying) {
						playStepSound(getBlockFacing());
						if(!WallJump.mc.thePlayer.capabilities.isCreativeMode && !WallJumpSettings.jumpType.getValue().equals("unlimited")) jumpsLeft -= 1;
						if(!WallJumpSettings.circleMode.getValue()) left = !left;
						WallJump.mc.thePlayer.motionY = bounceBack;
						
						double a = Math.toRadians(WallJump.mc.thePlayer.rotationYaw + 180);
						WallJump.mc.thePlayer.motionX = -Math.sin(a) / 4;
						WallJump.mc.thePlayer.motionZ = Math.cos(a) / 4;
						
						rotationRemaining = 180;
						//fallDistance = 0;
						isWallJumping = true;
						
						//WallJump.mc.thePlayer.fallDistance = 0; // Handled on server side, can't do anything about this.
					}
				}
			}
			
			if(!WallJump.mc.isGamePaused() && rotationRemaining > 0) {
				if(WallJumpSettings.autoTurn.getValue()) {
					if(left) {
						if(rotationRemaining >= 25) {
							WallJump.mc.thePlayer.rotationYaw -= 25;
							rotationRemaining -= 25;
						} else {
							WallJump.mc.thePlayer.rotationYaw -= rotationRemaining;
							rotationRemaining = 0;
						}
					} else {
						if(rotationRemaining >= 25) {
							WallJump.mc.thePlayer.rotationYaw += 25;
							rotationRemaining -= 25;
						} else {
							WallJump.mc.thePlayer.rotationYaw += rotationRemaining;
							rotationRemaining = 0;
						}
					}
				} else {
					rotationRemaining = 0;
				}
			}
		}
		
		String jumpType = WallJumpSettings.jumpType.getValue();
		// Break point between new and old code positioning
		
		/*if(WallJump.mc.currentScreen == null && WallJump.mc.thePlayer.motionY < 0 && WallJump.mc.theWorld != null) {
			fallDistance -= WallJump.mc.thePlayer.motionY;
		}*/
		
		//Refill wall jump bar according to boots worn
		if(WallJump.mc.theWorld != null && WallJump.mc.thePlayer.onGround)
		{
			if(jumpType.equals("limited")) {
				if(WallJump.mc.thePlayer.inventory.armorItemInSlot(0) != null) {
					jumpsLeft = getJumpCount(WallJump.mc.thePlayer.inventory.armorItemInSlot(0));
				} else {
					jumpsLeft = 3; //Barefoot
				}
			} else if(jumpType.equals("unlimited")) {
				jumpsLeft = Integer.MAX_VALUE;
			} else {
				jumpsLeft = 10;
			}
		}
		
		if(WallJump.mc.thePlayer != null && WallJump.mc.thePlayer.onGround) {
			isWallJumping = false;
		}
		
		if(shouldSprint && !WallJump.mc.thePlayer.isSprinting()) {
			try {
				WallJump.mc.thePlayer.setSprinting(true);
			} catch(Exception ex) {}
		}
	}
	
	/**
	 * Renders the Wall Jump bar (or text) into the player's in-game GUI, unless they are in Creative Mode, do not have any jumps left or have their GUI toggled off using F1.
	 */
	@SubscribeEvent
	public void onRenderOverlayTick(RenderGameOverlayEvent.Post e) {
		if(e.type != RenderGameOverlayEvent.ElementType.ALL) return;
		if(WallJump.mc != null && WallJump.mc.theWorld != null && !WallJump.mc.thePlayer.capabilities.isCreativeMode) {
			glPushMatrix(); {
				/*try {
					ItemStack i = new ItemStack(infront.getBlock(), infront.getBlock().getMetaFromState(infront));
					WallJump.mc.ingameGUI.drawString(WallJump.mc.fontRendererObj, i.getDisplayName(), 0, 0, 0xffffff);
				} catch(Exception m) {System.out.println("oh no");}*/
				
				WallJump.mc.renderEngine.bindTexture(icons);
				glColor3f(1.0F, 1.0F, 1.0F);
				
				// Variables used to greatly shorten the following lines.
				ScaledResolution scaledResolution = new ScaledResolution(WallJump.mc);
				
				int displayType = ArrayUtils.indexOf(WallJumpSettings.displayType.getPossibleValues(), WallJumpSettings.displayType.getValue());
				int width = scaledResolution.getScaledWidth(), height = scaledResolution.getScaledHeight();
				
				int offsetX, offsetY;
				offsetX = offsetY = 5;
				
				switch(displayType) {
					case 0:
						offsetX = width / 2 + 10;
						offsetY = WallJump.mc.thePlayer.isInWater() ? height - 59 : height - 49;
						break;
					case 1:
						offsetX = width / 2 - 90;
						offsetY = (ForgeHooks.getTotalArmorValue(WallJump.mc.thePlayer) > 0 ? height - 59 : height - 49);
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
						offsetX = width - WallJump.mc.fontRendererObj.getStringWidth("Wall Jumps Remaining: " + (WallJumpSettings.jumpType.getValue().equals("unlimited") ? "Infinity" : jumpsLeft)) - 5;
						// Keep default Y
						break;
					case 8:
						// Keep default X
						offsetY = height - 15;
						break;
					case 9:
						offsetX = width - WallJump.mc.fontRendererObj.getStringWidth("Wall Jumps Remaining: " + (WallJumpSettings.jumpType.getValue().equals("unlimited") ? "Infinity" : jumpsLeft)) - 5;
						offsetY = height - 15;
						break;
				}
				
				if(jumpsLeft > 0 && WallJumpSettings.wallJumping.getValue()) {
					GuiIngame gui = WallJump.mc.ingameGUI;
					
					if(displayType <= 5) {
						String barIcons = WallJumpSettings.barIcons.getValue();
						int u = barIcons.equals("feathers") ? 9 : barIcons.equals("boots") ? 0 : barIcons.equals("players") ? 18 : 27;
						for(int i = 0; i < jumpsLeft && i < 10; i++) {
							gui.drawTexturedModalRect(offsetX + 8 * i, offsetY, u, 0, 9, 9);
						}
					} else {
						gui.drawString(WallJump.mc.fontRendererObj, StatCollector.translateToLocal("walljump.remaining") + (WallJumpSettings.jumpType.getValue().equals("unlimited") ? StatCollector.translateToLocal("walljump.infinity") : jumpsLeft), offsetX, offsetY, 0xffffff);
					}
				}
			}
			glPopMatrix();
		}
	}
	
	/**
	 * Plays the sound of the block directly in front of the player, and one block down.
	 */
	public static void playStepSound(IBlockState blockState) {
		SoundType sound = blockState.getBlock().stepSound;
		WallJump.mc.thePlayer.playSound(sound.getStepSound(), sound.getVolume() * 0.5F, sound.getFrequency() * 0.75F);
	}
	
	/**
	 * Gets the block directly in front of the player.
	 */
	public static IBlockState getBlockFacing() {
        BlockPos pos = new BlockPos(WallJump.mc.thePlayer).offset(WallJump.mc.getRenderViewEntity().getHorizontalFacing());
		return WallJump.mc.theWorld.getBlockState(pos);
	}
}

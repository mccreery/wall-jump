package tk.nukeduck.walljump.settings;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import tk.nukeduck.walljump.WallJump;

@SideOnly(Side.CLIENT)
public class GuiWallJumpMenu extends GuiScreen {
	private GuiButtonSetting wallJumping, flyWallJump, barIcons, jumpType,
		autoTurn, displayType, shouldUseSpace, circleMode;
	
	/** A text box containing all block IDs which cannot be wall jumped from */
	private GuiTextField exceptions;
	/** Localized constants */
	String separator, on, off;
	
	/**
	 * Adds the buttons (and other controls) to the screen in question.
	 */
	public void initGui() {
		this.separator = StatCollector.translateToLocal("menu.walljump.separator");
		this.on = StatCollector.translateToLocal("options.on");
		this.off = StatCollector.translateToLocal("options.off");
		
		int yOffset = this.height / 4;
		int ySpacing = 24;
		
		Keyboard.enableRepeatEvents(true);
		this.buttonList.clear();
		
		this.buttonList.add(new GuiButton(0, this.width / 2 - 100, yOffset, StatCollector.translateToLocal("menu.returnToGame")));
		
		this.buttonList.add(wallJumping = new GuiButtonSetting(1, this.width / 2 - 100, yOffset + ySpacing, WallJumpSettings.wallJumping));
		this.buttonList.add(flyWallJump = new GuiButtonSetting(2, this.width / 2 - 152, yOffset + ySpacing * 2, 150, 20, WallJumpSettings.flyingWallJump));
		this.buttonList.add(barIcons = new GuiButtonSetting(3, this.width / 2 + 2, yOffset + ySpacing * 2, 150, 20, WallJumpSettings.barIcons));
		this.buttonList.add(jumpType = new GuiButtonSetting(4, this.width / 2 - 152, yOffset + ySpacing * 3, 150, 20, WallJumpSettings.jumpType));
		this.buttonList.add(autoTurn = new GuiButtonSetting(5, this.width / 2 + 2, yOffset + ySpacing * 3, 150, 20, WallJumpSettings.autoTurn));
		this.buttonList.add(displayType = new GuiButtonSetting(6, this.width / 2 - 152, yOffset + ySpacing * 4, 150, 20, WallJumpSettings.displayType));
		this.buttonList.add(shouldUseSpace = new GuiButtonSetting(7, this.width / 2 + 2, yOffset + ySpacing * 4, 150, 20, WallJumpSettings.useSpace));
		this.buttonList.add(circleMode = new GuiButtonSetting(8, this.width / 2 - 152, yOffset + ySpacing * 5, 150, 20, WallJumpSettings.circleMode));
		
		exceptions = new GuiTextField(9, mc.fontRendererObj, this.width / 2 - 125, yOffset + ySpacing * 5 + 54, 250, 20);
		exceptions.setMaxStringLength(200);
		exceptions.setEnabled(true);
		
		this.updateButtonText();
		exceptions.setText(WallJumpSettings.exceptions.getValue());
		flyWallJump.enabled = barIcons.enabled = jumpType.enabled = autoTurn.enabled = displayType.enabled = shouldUseSpace.enabled = WallJumpSettings.wallJumping.getValue();
	}
	
	private void updateButtonText() {
		for(Object button : this.buttonList) {
			updateButtonText(button);
		}
	}
	
	private void updateButtonText(Object button) {
		if(button instanceof GuiButtonSetting) {
			GuiButtonSetting setting = (GuiButtonSetting) button;
			if(setting.getSetting().type == Boolean.class) {
				setting.displayString = setting.getLocalizedName() + separator + ((Boolean) setting.getSetting().getValue() ? on : off);
			} else {
				setting.displayString = setting.getLocalizedName() + separator + setting.getSetting().getLocalizedValue();
			}
		}
	}

	/**
	 * Called when a control is clicked. This is the equivalent of ActionListener.actionPerformed(ActionEvent e).
	 */
	@SuppressWarnings("unchecked")
	protected void actionPerformed(GuiButton par1GuiButton) {
		switch(par1GuiButton.id) {
			case 0:
				this.mc.displayGuiScreen(null);
				this.mc.setIngameFocus();
				this.mc.getSoundHandler().resumeSounds();
				break;
			case 1:
				WallJumpSettings.toggleValue((WallJumpSettings<Boolean>)wallJumping.getSetting());
				flyWallJump.enabled = barIcons.enabled = jumpType.enabled = autoTurn.enabled = displayType.enabled = shouldUseSpace.enabled = WallJumpSettings.wallJumping.getValue();
				updateButtonText(wallJumping);
				break;
			default:
				GuiButtonSetting setting = (GuiButtonSetting) par1GuiButton;
				if(setting.getSetting().type == Boolean.class) {
					WallJumpSettings.toggleValue((WallJumpSettings<Boolean>)setting.getSetting());
				} else if(setting.getSetting().type == String.class) {
					WallJumpSettings.incrementValue((WallJumpSettings<String>)setting.getSetting());
				}
				updateButtonText(setting);
				break;
		}
	}
	
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
		WallJumpSettings.saveSettings();
		WallJump.tickHandler.reloadProhibitedBlocks();
	}
	
	/**
	 * Draws the screen and all the components in it.
	 */
	public void drawScreen(int par1, int par2, float par3)
	{
		this.drawDefaultBackground();
		this.drawCenteredString(mc.fontRendererObj, StatCollector.translateToLocal("menu.walljump.settings"), this.width / 2, 40, 16777215);
		super.drawScreen(par1, par2, par3);
		this.drawString(mc.fontRendererObj, StatCollector.translateToLocal("menu.walljump.moreStable"), this.width / 2 + 10, circleMode.yPosition + 6, 0xffffffff);
		this.drawCenteredString(mc.fontRendererObj, StatCollector.translateToLocal("menu.walljump.prohibited"), this.width / 2, exceptions.yPosition - 20, 16777215);
		
		exceptions.drawTextBox();
		
		RenderHelper.enableGUIStandardItemLighting();
		
		IBlockState[] prohibitedBlocks = WallJump.tickHandler.prohibitedBlocks;
		ItemStack[] stacks = new ItemStack[prohibitedBlocks.length];
		int toolTipIndex = -1;
		
		int yBase = this.exceptions.yPosition + this.exceptions.height + 5;
		int perLine = 12;
		
		for(int i = 0; i < prohibitedBlocks.length; i++) {
			IBlockState current = prohibitedBlocks[i];
			stacks[i] = new ItemStack(current.getBlock(), 1, current.getBlock().getMetaFromState(current));
			
			int x = this.width / 2 - Math.round(18 * perLine / 2F) + ((i % perLine) * 18);
			int y = yBase + ((i / perLine) * 18);
			
			mc.getRenderItem().renderItemAndEffectIntoGUI(stacks[i], x, y);
			if(par1 >= x && par1 <= x + 16 && par2 >= y && par2 <= y + 16) {
				toolTipIndex = i;
			}
		}
		
		RenderHelper.disableStandardItemLighting();
		
		if(toolTipIndex != -1) {
			this.renderToolTip(stacks[toolTipIndex], par1, par2);
		}
	}
	
	protected void keyTyped(char c, int i) throws IOException {
		super.keyTyped(c, i);
		exceptions.textboxKeyTyped(c, i);
		WallJumpSettings.exceptions.setValue(exceptions.getText());
		WallJump.tickHandler.reloadProhibitedBlocks();
	}
	
	protected void mouseClicked(int i, int j, int k) throws IOException {
		super.mouseClicked(i, j, k);
		exceptions.mouseClicked(i, j, k);
	}
}
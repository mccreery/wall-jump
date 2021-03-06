package tk.nukeduck.walljump.settings;

import static tk.nukeduck.walljump.WallJump.MC;

import java.io.IOException;

import org.lwjgl.input.Keyboard;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLockIconButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import tk.nukeduck.walljump.WallJump;

public class GuiWallJumpMenu extends GuiScreen {
	private GuiToggle enabled;
	private GuiLockIconButton lock;

	/** A text box containing all block IDs which cannot be wall jumped from */
	private GuiTextField exceptions;

	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		this.buttonList.clear();
		final int top = height / 6 - 12;

		buttonList.add(new GuiButton(0, this.width / 2 - 100, top, 180, 20, I18n.format("menu.returnToGame")));
		buttonList.add(lock = new GuiLockIconButton(1, this.width / 2 + 80, top));
		lock.setLocked(Config.locked());

		int idOffset = buttonList.size();
		GuiSetting[] buttons = Config.getButtons();

		for(int i = 0; i < buttons.length; i++) {
			GuiSetting button = buttons[i];
			button.x = width/2 - 152 + (i & 1) * 154;
			button.y = top + (i / 2 + 2) * 24;
			button.id = idOffset + i;

			if(button.getName().equals("enabled") && button instanceof GuiToggle) {
				enabled = (GuiToggle)button;
			}
			buttonList.add(button);
		}

		exceptions = new GuiTextField(9, mc.fontRenderer, this.width / 2 - 125, top + 174, 250, 20);
		exceptions.setMaxStringLength(200);
		exceptions.setEnabled(true);
	}

	protected void actionPerformed(GuiButton button) {
		if(button instanceof GuiSetting) {
			((GuiSetting)button).next();

			if(button == enabled) {
				for(int i = 0; i < buttonList.size(); i++) {
					GuiButton dependent = buttonList.get(i);

					if(dependent instanceof GuiSetting && dependent != button) {
						dependent.enabled = Config.enabled();
					}
				}
			}
		} else if(button == lock) {
			lock.setLocked(!lock.isLocked());
			Config.setLocked(lock.isLocked());
		} else { // Only button left is close
			this.mc.displayGuiScreen(null);
			this.mc.setIngameFocus();
			this.mc.getSoundHandler().resumeSounds();
		}
	}

	@Override
	public void onGuiClosed() {
		Keyboard.enableRepeatEvents(false);
		Config.updateExceptions(exceptions.getText());
		if(WallJump.config.hasChanged()) WallJump.config.save();
	}

	@Override
	public void drawScreen(int par1, int par2, float par3) {
		this.drawDefaultBackground();
		this.drawCenteredString(mc.fontRenderer, I18n.format("menu.walljump"), this.width / 2, 40, 0xffffff);
		super.drawScreen(par1, par2, par3);

		this.drawCenteredString(mc.fontRenderer, I18n.format("menu.walljump.prohibited"), this.width / 2, exceptions.y - 20, 16777215);
		exceptions.drawTextBox();

		RenderHelper.enableGUIStandardItemLighting();

		int yBase = exceptions.y + exceptions.height + 5;
		int perLine = 12;

		ItemStack tooltipStack = null;

		for(int i = 0; i < Config.exceptions.size(); i++) {
			IBlockState current = Config.exceptions.get(i);
			ItemStack stack = new ItemStack(current.getBlock(), 1, current.getBlock().getMetaFromState(current));

			int x = this.width / 2 - Math.round(18 * perLine / 2F) + ((i % perLine) * 18);
			int y = yBase + ((i / perLine) * 18);

			mc.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
			if(par1 >= x && par1 <= x + 16 && par2 >= y && par2 <= y + 16) {
				tooltipStack = stack;
			}
		}
		RenderHelper.disableStandardItemLighting();

		if(tooltipStack != null) {
			this.renderToolTip(tooltipStack, par1, par2);
		} else if(lock.mousePressed(MC, par1, par2)) {
			drawHoveringText(I18n.format("walljump.lock"), par1, par2);
		}
	}

	@Override
	protected void keyTyped(char c, int i) throws IOException {
		super.keyTyped(c, i);
		exceptions.textboxKeyTyped(c, i);
	}

	@Override
	protected void mouseClicked(int i, int j, int k) throws IOException {
		super.mouseClicked(i, j, k);
		exceptions.mouseClicked(i, j, k);
	}
}

package com.mob_check;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.inject.Inject;

import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class MobCheckOverlay extends Overlay
{
	private final MobCheckPlugin plugin;
	private final MobCheckConfig config;
	private final SpriteManager spriteManager;
	private final PanelComponent panelComponent = new PanelComponent();

	private static final Color INFOBOX_BG_COLOR = new Color(0, 0, 0, 160);
	private static final int MAX_INFO_BOXES = 4;

	private final InfoBoxComponent[] reusableInfoBoxes = new InfoBoxComponent[MAX_INFO_BOXES];
	{
		for (int i = 0; i < MAX_INFO_BOXES; i++)
		{
			reusableInfoBoxes[i] = new InfoBoxComponent();
		}
	}
	private final TitleComponent manticoreComboTitle = TitleComponent.builder()
		.text("Manticore Combo")
		.color(Color.YELLOW)
		.build();

	@Inject
	public MobCheckOverlay(MobCheckPlugin plugin, MobCheckConfig config, SpriteManager spriteManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showInfoBox())
		{
			return null;
		}

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		if (attacks.isEmpty())
		{
			return null;
		}

		panelComponent.getChildren().clear();

		boolean hasManticoreCombo = false;
		for (int i = 0; i < attacks.size(); i++)
		{
			if (attacks.get(i).isManticoreCombo)
			{
				hasManticoreCombo = true;
				break;
			}
		}

		if (hasManticoreCombo && config.showComboSequence())
		{
			panelComponent.getChildren().add(manticoreComboTitle);
		}

		int count = 0;
		for (MobCheckPlugin.AttackState attack : attacks)
		{
			if (count >= MAX_INFO_BOXES) // Show up to top N incoming attacks
			{
				break;
			}
			BufferedImage sprite = getPrayerSprite(attack.prayerStyle);
			if (sprite != null)
			{
				InfoBoxComponent infoBox = reusableInfoBoxes[count];
				infoBox.setImage(sprite);
				infoBox.setText(MobCheckPlugin.formatTicks(attack.ticks));
				infoBox.setTooltip(formatTooltip(attack));

				boolean itemProtected = plugin.isPrayerProtected(attack.prayerStyle);
				Color textColor;
				if (itemProtected)
				{
					textColor = Color.GREEN;
				}
				else if (attack.ticks <= config.warningThreshold())
				{
					textColor = Color.RED;
				}
				else
				{
					textColor = Color.WHITE;
				}

				infoBox.setColor(textColor);
				infoBox.setBackgroundColor(INFOBOX_BG_COLOR);
				panelComponent.getChildren().add(infoBox);
				count++;
			}
		}
		return panelComponent.render(graphics);
	}

	String getTooltipForIndex(int index)
	{
		return reusableInfoBoxes[index].getTooltip();
	}

	private static String formatTooltip(MobCheckPlugin.AttackState attack)
	{
		if (attack.npcName != null && !attack.npcName.isEmpty())
		{
			return attack.npcName + " - " + attack.getStyleDisplayName();
		}
		return attack.getStyleDisplayName();
	}

	private BufferedImage getPrayerSprite(MobCheckPlugin.PrayerStyle style)
	{
		if (style == null)
		{
			return null;
		}
		return spriteManager.getSprite(style.getSpriteId(), 0);
	}
}

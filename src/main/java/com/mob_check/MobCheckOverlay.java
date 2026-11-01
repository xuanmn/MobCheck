package com.mob_check;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

public class MobCheckOverlay extends Overlay
{
	private final Client client;
	private final MobCheckPlugin plugin;
	private final MobCheckConfig config;
	private final SpriteManager spriteManager;
	private final TooltipManager tooltipManager;
	private final PanelComponent panelComponent = new PanelComponent();

	private static final Color INFOBOX_BG_COLOR = new Color(0, 0, 0, 160);
	private static final int MAX_INFO_BOXES = 4;

	private final InfoBoxComponent[] reusableInfoBoxes = new InfoBoxComponent[MAX_INFO_BOXES];
	private final MobCheckPlugin.AttackState[] renderedAttacks = new MobCheckPlugin.AttackState[MAX_INFO_BOXES];
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
	public MobCheckOverlay(
		Client client,
		MobCheckPlugin plugin,
		MobCheckConfig config,
		SpriteManager spriteManager,
		TooltipManager tooltipManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		this.tooltipManager = tooltipManager;
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
				renderedAttacks[count] = attack;

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

		Dimension dimension = panelComponent.render(graphics);
		renderHoveredTooltip(count);
		Arrays.fill(renderedAttacks, 0, count, null);
		return dimension;
	}

	private void renderHoveredTooltip(int count)
	{
		if (client == null || tooltipManager == null || count <= 0)
		{
			return;
		}

		Point mouse = client.getMouseCanvasPosition();
		if (mouse == null)
		{
			return;
		}

		Rectangle overlayBounds = getBounds();
		int relX = mouse.getX() - overlayBounds.x;
		int relY = mouse.getY() - overlayBounds.y;

		for (int i = 0; i < count; i++)
		{
			Rectangle boxBounds = reusableInfoBoxes[i].getBounds();
			if (boxBounds != null && boxBounds.contains(relX, relY))
			{
				String tooltip = formatTooltip(renderedAttacks[i]);
				if (tooltip != null && !tooltip.isEmpty())
				{
					tooltipManager.add(new Tooltip(tooltip));
				}
				break;
			}
		}
	}

	private static String formatTooltip(MobCheckPlugin.AttackState attack)
	{
		if (attack == null)
		{
			return null;
		}
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

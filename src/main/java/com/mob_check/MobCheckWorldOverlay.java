package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MobCheckWorldOverlay extends Overlay
{
	private final Client client;
	private final MobCheckPlugin plugin;
	private final MobCheckConfig config;

	private static final Font NPC_FONT = new Font("Arial", Font.BOLD, 14);
	private static final Stroke NPC_STROKE = new BasicStroke(2f);

	@Inject
	public MobCheckWorldOverlay(Client client, MobCheckPlugin plugin, MobCheckConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(OverlayPriority.MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.highlightThreatNpc() && !config.highlightNpcTrueTile())
		{
			return null;
		}

		List<MobCheckPlugin.AttackState> attacks = plugin.getActiveAttacks();
		if (attacks.isEmpty())
		{
			return null;
		}

		Set<Integer> renderedNpcs = new HashSet<>();

		for (MobCheckPlugin.AttackState attack : attacks)
		{
			NPC npc = attack.sourceNpc;
			if (npc == null || renderedNpcs.contains(npc.getIndex()) || npc.isDead())
			{
				continue;
			}

			renderedNpcs.add(npc.getIndex());
			Color styleColor = attack.prayerStyle != null ? attack.prayerStyle.getColor() : Color.WHITE;

			// 1. Draw True Tile polygon on the ground
			if (config.highlightNpcTrueTile() && npc.getLocalLocation() != null)
			{
				Polygon tilePoly = Perspective.getCanvasTilePoly(client, npc.getLocalLocation());
				if (tilePoly != null)
				{
					graphics.setColor(styleColor);
					graphics.setStroke(NPC_STROKE);
					graphics.drawPolygon(tilePoly);

					Color fill = new Color(styleColor.getRed(), styleColor.getGreen(), styleColor.getBlue(), 35);
					graphics.setColor(fill);
					graphics.fillPolygon(tilePoly);
				}
			}

			// 2. Draw 3D Model Convex Hull outline
			if (config.highlightThreatNpc())
			{
				Shape hull = npc.getConvexHull();
				if (hull != null)
				{
					graphics.setColor(styleColor);
					graphics.setStroke(NPC_STROKE);
					graphics.draw(hull);

					Color fill = new Color(styleColor.getRed(), styleColor.getGreen(), styleColor.getBlue(), 25);
					graphics.setColor(fill);
					graphics.fill(hull);
				}
			}

			// 3. Render tick countdown above NPC head
			Point textLoc = npc.getCanvasTextLocation(graphics, attack.ticks + "t", npc.getLogicalHeight() + 30);
			if (textLoc != null)
			{
				graphics.setFont(NPC_FONT);
				graphics.setColor(Color.BLACK);
				graphics.drawString(attack.ticks + "t", textLoc.getX() + 1, textLoc.getY() + 1);

				graphics.setColor(attack.ticks <= config.warningThreshold() ? Color.RED : Color.WHITE);
				graphics.drawString(attack.ticks + "t", textLoc.getX(), textLoc.getY());
			}
		}

		return null;
	}
}

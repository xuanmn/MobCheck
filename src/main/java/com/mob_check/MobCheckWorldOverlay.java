package com.mob_check;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
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

@SuppressWarnings({"deprecation", "null"})
public class MobCheckWorldOverlay extends Overlay
{
	private final Client client;
	private final MobCheckPlugin plugin;
	private final MobCheckConfig config;

	private static final Font NPC_FONT = new Font("Arial", Font.BOLD, 14);
	private static final Stroke NPC_STROKE = new BasicStroke(2f);
	private static final Color DEFAULT_TILE_FILL = new Color(255, 255, 255, 35);
	private static final Color DEFAULT_HULL_FILL = new Color(255, 255, 255, 25);

	private final Set<Integer> renderedNpcs = new HashSet<>();

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

		renderedNpcs.clear();

		for (MobCheckPlugin.AttackState attack : attacks)
		{
			NPC npc = attack.sourceNpc;
			if (npc == null || npc.isDead() || !renderedNpcs.add(npc.getIndex()))
			{
				continue;
			}

			Color styleColor = attack.prayerStyle != null ? attack.prayerStyle.getColor() : Color.WHITE;
			Color tileFill = attack.prayerStyle != null ? attack.prayerStyle.getTileFillColor() : DEFAULT_TILE_FILL;
			Color hullFill = attack.prayerStyle != null ? attack.prayerStyle.getHullFillColor() : DEFAULT_HULL_FILL;

			// 1. Draw True Tile polygon on the ground
			// #11: Use getWorldLocation() for actual server-side true tile,
			// not getLocalLocation() which returns the interpolated visual position
			if (config.highlightNpcTrueTile())
			{
				WorldPoint wp = npc.getWorldLocation();
				if (wp != null)
				{
					LocalPoint lp = LocalPoint.fromWorld(client, wp);
					if (lp != null)
					{
						// #12: Use NPC size for multi-tile NPCs (e.g. 3x3 bosses)
						int npcSize = 1;
						NPCComposition composition = npc.getComposition();
						if (composition != null)
						{
							npcSize = composition.getSize();
						}

						Polygon tilePoly;
						if (npcSize > 1)
						{
							tilePoly = Perspective.getCanvasTileAreaPoly(client, lp, npcSize);
						}
						else
						{
							tilePoly = Perspective.getCanvasTilePoly(client, lp);
						}

						if (tilePoly != null)
						{
							graphics.setColor(styleColor);
							graphics.setStroke(NPC_STROKE);
							graphics.drawPolygon(tilePoly);

							graphics.setColor(tileFill);
							graphics.fillPolygon(tilePoly);
						}
					}
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

					graphics.setColor(hullFill);
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

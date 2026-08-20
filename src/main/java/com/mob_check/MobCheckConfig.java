package com.mob_check;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup("mobcheck")
public interface MobCheckConfig extends Config
{
	// ==========================================
	// SECTIONS
	// ==========================================

	@ConfigSection(
		name = "Overhead & HUD",
		description = "Settings for overhead icon, tick countdown, and visual progress bar",
		position = 1
	)
	String overheadSection = "overheadSection";

	@ConfigSection(
		name = "Prayer Book Highlights",
		description = "Settings for highlighting prayer buttons and remaining ticks in the prayer tab",
		position = 2
	)
	String prayerWidgetSection = "prayerWidgetSection";

	@ConfigSection(
		name = "Danger & Emergency Warnings",
		description = "Settings for screen flash and emergency indicators when prayer is wrong",
		position = 3
	)
	String dangerSection = "dangerSection";

	@ConfigSection(
		name = "Audio Alerts",
		description = "Sound effect settings per attack style and emergency warnings",
		position = 4
	)
	String audioSection = "audioSection";

	@ConfigSection(
		name = "NPC Threat & World Highlights",
		description = "Settings for highlighting attacking NPCs and true tiles",
		position = 5
	)
	String worldSection = "worldSection";

	@ConfigSection(
		name = "Info Box & Sequences",
		description = "Settings for the upcoming attack queue panel and combo visualizers",
		position = 6
	)
	String infoBoxSection = "infoBoxSection";

	// ==========================================
	// 1. OVERHEAD & HUD
	// ==========================================

	@ConfigItem(
		keyName = "showOverhead",
		name = "Show Overhead Icon",
		description = "Renders the required prayer icon and countdown ticks above your character",
		section = overheadSection,
		position = 1
	)
	default boolean showOverhead()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTickProgressRing",
		name = "Show Tick Progress Arc",
		description = "Draws a visual countdown progress ring around the overhead prayer icon",
		section = overheadSection,
		position = 2
	)
	default boolean showTickProgressRing()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 10
	)
	@ConfigItem(
		keyName = "warningThreshold",
		name = "Warning Threshold",
		description = "Number of remaining ticks before the overlay turns red to indicate immediate urgency",
		section = overheadSection,
		position = 3
	)
	default int warningThreshold()
	{
		return 1;
	}

	// ==========================================
	// 2. PRAYER BOOK HIGHLIGHTS
	// ==========================================

	@ConfigItem(
		keyName = "highlightPrayerWidget",
		name = "Highlight Prayer Button",
		description = "Highlights the required protection prayer directly in your Prayer Tab widget",
		section = prayerWidgetSection,
		position = 1
	)
	default boolean highlightPrayerWidget()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showWidgetTicks",
		name = "Show Ticks on Prayer Button",
		description = "Renders remaining ticks directly over the prayer button in your Prayer Tab",
		section = prayerWidgetSection,
		position = 2
	)
	default boolean showWidgetTicks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashWrongPrayerWidget",
		name = "Flash Wrong Prayer",
		description = "Pulses a bright red border around the required prayer when unprotected at 1 tick",
		section = prayerWidgetSection,
		position = 3
	)
	default boolean flashWrongPrayerWidget()
	{
		return true;
	}

	// ==========================================
	// 3. DANGER & EMERGENCY WARNINGS
	// ==========================================

	@ConfigItem(
		keyName = "flashScreenOnWrongPrayer",
		name = "Flash Screen on Wrong Prayer",
		description = "Flashes a red danger border around your screen if unprotected when an attack hits in 1 tick",
		section = dangerSection,
		position = 1
	)
	default boolean flashScreenOnWrongPrayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "dangerFlashColor",
		name = "Danger Flash Color",
		description = "Color of the emergency screen flash border",
		section = dangerSection,
		position = 2
	)
	default Color dangerFlashColor()
	{
		return new Color(255, 0, 0, 70);
	}

	// ==========================================
	// 4. AUDIO ALERTS
	// ==========================================

	@ConfigItem(
		keyName = "playSoundAlert",
		name = "Play Sound Alert",
		description = "Plays an audio cue when the priority prayer style changes",
		section = audioSection,
		position = 1
	)
	default boolean playSoundAlert()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 10000
	)
	@ConfigItem(
		keyName = "magicSoundId",
		name = "Magic Sound ID",
		description = "Sound effect ID to play when Magic protection is needed",
		section = audioSection,
		position = 2
	)
	default int magicSoundId()
	{
		return 2266;
	}

	@Range(
		min = 0,
		max = 10000
	)
	@ConfigItem(
		keyName = "rangeSoundId",
		name = "Range Sound ID",
		description = "Sound effect ID to play when Range protection is needed",
		section = audioSection,
		position = 3
	)
	default int rangeSoundId()
	{
		return 2266;
	}

	@Range(
		min = 0,
		max = 10000
	)
	@ConfigItem(
		keyName = "meleeSoundId",
		name = "Melee Sound ID",
		description = "Sound effect ID to play when Melee protection is needed",
		section = audioSection,
		position = 4
	)
	default int meleeSoundId()
	{
		return 2266;
	}

	@ConfigItem(
		keyName = "playWrongPrayerAlert",
		name = "Wrong Prayer Emergency Sound",
		description = "Plays an emergency sound cue if unprotected when an attack is 1 tick away",
		section = audioSection,
		position = 5
	)
	default boolean playWrongPrayerAlert()
	{
		return true;
	}

	@Range(
		min = 0,
		max = 10000
	)
	@ConfigItem(
		keyName = "wrongPrayerSoundId",
		name = "Wrong Prayer Sound ID",
		description = "Sound effect ID for wrong prayer emergency warning",
		section = audioSection,
		position = 6
	)
	default int wrongPrayerSoundId()
	{
		return 2277;
	}

	// ==========================================
	// 5. NPC THREAT & WORLD HIGHLIGHTS
	// ==========================================

	@ConfigItem(
		keyName = "highlightThreatNpc",
		name = "Highlight Attacking NPC",
		description = "Outlines the NPC model/hull currently attacking you with a style-coded color",
		section = worldSection,
		position = 1
	)
	default boolean highlightThreatNpc()
	{
		return true;
	}

	@ConfigItem(
		keyName = "highlightNpcTrueTile",
		name = "Highlight NPC True Tile",
		description = "Draws the ground true tile of the attacking NPC",
		section = worldSection,
		position = 2
	)
	default boolean highlightNpcTrueTile()
	{
		return true;
	}

	// ==========================================
	// 6. INFO BOX & SEQUENCES
	// ==========================================

	@ConfigItem(
		keyName = "showInfoBox",
		name = "Show Info Box",
		description = "Displays the upcoming threat queue in a side overlay panel",
		section = infoBoxSection,
		position = 1
	)
	default boolean showInfoBox()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showComboSequence",
		name = "Show Manticore/Combo Sequence",
		description = "Displays rapid attack sequences (like Colosseum Manticore 3-hit combos) in order",
		section = infoBoxSection,
		position = 2
	)
	default boolean showComboSequence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "trackUnknownProjectiles",
		name = "Track Unknown Projectiles",
		description = "Fallback to Pray Magic for projectiles targeting you that are not in the database",
		section = infoBoxSection,
		position = 3
	)
	default boolean trackUnknownProjectiles()
	{
		return false;
	}
}

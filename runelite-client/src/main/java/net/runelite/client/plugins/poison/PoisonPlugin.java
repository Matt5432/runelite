/*
 * Copyright (c) 2018 Hydrox6 <ikada@protonmail.ch>
 * Copyright (c) 2018, Lucas <https://github.com/Lucwousin>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.poison;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.SpriteID;
import net.runelite.api.VarPlayer;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ColorUtil;

@PluginDescriptor(
		name = "Poison",
		description = "Tracks current damage values for Poison and Venom",
		tags = {"combat", "poison", "venom", "anti"}
)
@Slf4j
public class PoisonPlugin extends Plugin
{
	private static final int GAME_TICK_MILLIS = 600;
	private static final int POISON_TICK_LENGTH = 30;
	private static final int POISON_TICK_DURATION = 18000;
	private static final int VENOM_THRESHOLD = 1000000;
	private static final int VENOM_MAXIMUM_DAMAGE = 20;

	@Inject
	private Client client;

	@Inject
	private PoisonOverlay poisonOverlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private PoisonConfig config;

	@Getter
	private int lastDamage;

	private boolean envenomed;
	private int nextPoisonTick;
	private int lastValue;
	private PoisonInfobox poisonBox; // For venom and poison
	private PoisonInfobox antiPBox; // Antipoison
	private PoisonInfobox antiVBox; // Antivenom

	Instant nextPoisonTickTime;
	Instant poisonDecayTime; // when poison/antipoison will have decayed

	@Provides
	PoisonConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PoisonConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(poisonOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(poisonOverlay);
		removeInfoBoxes();

		envenomed = false;
		nextPoisonTick = -1;
		nextPoisonTickTime = null;
		poisonDecayTime = null;
		lastDamage = 0;
		lastValue = 0;
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		final int poisonValue = client.getVar(VarPlayer.POISON);
		if (poisonValue == lastValue)
		{
			return;
		}

		// The poison tick only changes if the poison value was 0 before the change or a interface delayed it
		if (nextPoisonTick <= client.getTickCount() || lastValue == 0)
		{
			nextPoisonTick = client.getTickCount() + POISON_TICK_LENGTH;
			log.debug("Next poison tick expected at {}", nextPoisonTick);
		}

		nextPoisonTickTime = Instant.now().plus(Duration.of(
				GAME_TICK_MILLIS * (nextPoisonTick - client.getTickCount()), ChronoUnit.MILLIS));

		if (poisonValue < VENOM_THRESHOLD)
		{
			poisonDecayTime = nextPoisonTickTime.plus(Duration.of(
					POISON_TICK_DURATION * Math.abs(poisonValue), ChronoUnit.MILLIS));
			envenomed = false;
		}
		else
		{
			poisonDecayTime = null;
			envenomed = true;
		}

		final int damage = nextDamage(poisonValue, envenomed);
		this.lastDamage = damage;
		lastValue = poisonValue;

		addInfoBoxes(poisonValue, damage, envenomed);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals(PoisonConfig.GROUP)
				&& !config.showPoisonInfoboxes() && !config.showAntiInfoboxes())
		{
			removeInfoBoxes();
		}
	}

	private static int nextDamage(int poisonValue, boolean envenomed)
	{
		int damage;

		if (envenomed)
		{
			//Venom Damage starts at 6, and increments in twos;
			//The VarPlayer increments in values of 1, however.
			poisonValue -= VENOM_THRESHOLD - 3;
			damage = poisonValue * 2;
			//Venom Damage caps at 20, but the VarPlayer keeps increasing
			if (damage > VENOM_MAXIMUM_DAMAGE)
			{
				damage = VENOM_MAXIMUM_DAMAGE;
			}
		}
		else
		{
			damage = (int) Math.ceil(poisonValue / 5.0f);
		}

		return damage;
	}

	private BufferedImage getSplat(int id, int damage)
	{
		//Get a copy of the hitsplat to get a clean one each time
		final BufferedImage rawSplat = spriteManager.getSprite(id, 0);
		if (rawSplat == null)
		{
			return null;
		}

		final BufferedImage splat = new BufferedImage(
				rawSplat.getColorModel(),
				rawSplat.copyData(null),
				rawSplat.getColorModel().isAlphaPremultiplied(),
				null);

		final Graphics g = splat.getGraphics();
		g.setFont(FontManager.getRunescapeSmallFont());

		// Align the text in the centre of the hitsplat
		final FontMetrics metrics = g.getFontMetrics();
		final String text = String.valueOf(damage);
		final int x = (splat.getWidth() - metrics.stringWidth(text)) / 2;
		final int y = (splat.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();

		g.setColor(Color.BLACK);
		g.drawString(String.valueOf(damage), x + 1, y + 1);
		g.setColor(Color.WHITE);
		g.drawString(String.valueOf(damage), x, y);
		return splat;
	}

	private int antiPoisonType(int poisonValue)
	{
		if (poisonValue >= -5) // Antipoison
		{
			return ItemID.ANTIPOISON4;
		}
		else if (poisonValue >= -20) // Superantipoison or Sanfew serum
		{
			return ItemID.SUPERANTIPOISON4;
		}
		else if (poisonValue >= -30) // Antidote+
		{
			return ItemID.ANTIDOTE4;
		}
		else if (poisonValue >= -40) // Antidote++
		{
			return ItemID.ANTIDOTE4_5952;
		}
		else if (poisonValue >= -41) // Antivenom
		{
			return ItemID.ANTIVENOM4;
		}
		else if (poisonValue >= -50) // Antivenom+
		{
			return ItemID.ANTIVENOM4_12913;
		}
		else
		{
			return -1;
		}
	}

	private static String getFormattedTime(Instant endTime)
	{
		final Duration timeLeft = Duration.between(Instant.now(), endTime);
		int seconds = (int) (timeLeft.toMillis() / 1000L);
		int minutes = seconds / 60;
		int secs = seconds % 60;

		return String.format("%d:%02d", minutes, secs);
	}

	private void addInfoBoxes(int poisonValue, int damage, boolean envenomed)
	{
		if (config.showPoisonInfoboxes() && poisonValue > 1)
		{
			removeInfoBoxes();
			final BufferedImage image = getSplat(envenomed
					? SpriteID.HITSPLAT_DARK_GREEN_VENOM
					: SpriteID.HITSPLAT_GREEN_POISON, damage);

			if (image != null)
			{ // Display the time until next poison hit
				long period = Instant.now().until(nextPoisonTickTime, ChronoUnit.MILLIS);

				poisonBox = new PoisonInfobox(period, image, this);
				infoBoxManager.addInfoBox(poisonBox);
			}
		}
		if (config.showAntiInfoboxes() && poisonValue < -1)
		{
			removeInfoBoxes();
			final BufferedImage image = itemManager.getImage(antiPoisonType(poisonValue));

			if (image != null)
			{
				if (poisonValue <= -41)
				{ // If immune to venom
					long period = Instant.now()
							.minusMillis(-41 * POISON_TICK_DURATION)
							.until(poisonDecayTime, ChronoUnit.MILLIS);

					antiVBox = new PoisonInfobox(period, image, this);
					infoBoxManager.addInfoBox(antiVBox);
				}
				else
				{ // Display the time until antipoison runs out
					long period = Instant.now()
							.until(poisonDecayTime, ChronoUnit.MILLIS);

					antiPBox = new PoisonInfobox(period, image, this);
					infoBoxManager.addInfoBox(antiPBox);
				}
			}
		}
	}

	private void removeInfoBoxes()
	{
		if (poisonBox != null)
		{
			infoBoxManager.removeInfoBox(poisonBox);
			poisonBox = null;
		}
		if (antiVBox != null)
		{
			infoBoxManager.removeInfoBox(antiVBox);
			antiVBox = null;
		}
		if (antiPBox != null)
		{
			infoBoxManager.removeInfoBox(antiPBox);
			antiPBox = null;
		}
	}

	String createTooltip()
	{
		if (this.lastValue >= 0)
		{
			String line1 = MessageFormat.format("Next {0} damage: {1}</br>Time until damage: {2}",
					envenomed ? "venom" : "poison", ColorUtil.wrapWithColorTag(String.valueOf(lastDamage), Color.RED), getFormattedTime(nextPoisonTickTime));
			String line2 = envenomed ? "" : MessageFormat.format("</br>Time until cure: {0}", getFormattedTime(poisonDecayTime));

			return line1 + line2;
		}

		return "";
	}
}
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

import java.awt.Color;
import java.time.temporal.ChronoUnit;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.infobox.Timer;

class PoisonInfobox extends Timer
{
	private final PoisonPlugin plugin;

	PoisonInfobox(Long period, BufferedImage image, PoisonPlugin plugin)
	{
		super(period, ChronoUnit.MILLIS, image, plugin);
		this.plugin = plugin;
	}

	@Override
	public String getTooltip()
	{
		return plugin.createTooltip();
	}

	@Override
	public Color getTextColor()
	{
		// If the next poison tick is the end time (AKA when there's <18 sec left)
		if (getEndTime().equals(plugin.nextPoisonTickTime))
		{
			return Color.RED.brighter();
		}
		// Above should have caught all damage/venom hitting, so the only time the timer doesn't end
		// on poisonDecayTime is when it's the antivenom overlay.
		else if (!getEndTime().equals(plugin.poisonDecayTime))
		{
			return Color.GREEN;
		}
		else
		{
			return Color.WHITE;
		}
	}
}

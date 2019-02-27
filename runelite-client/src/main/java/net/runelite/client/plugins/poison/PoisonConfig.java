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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(PoisonConfig.GROUP)
public interface PoisonConfig extends Config
{
	String GROUP = "poison";

	@ConfigItem (
			keyName = "showPoisonInfoboxes",
			name = "Show Poison Infoboxes",
			description = "Configures whether to show the infoboxes for poison and venom<br>" +
					"Shows: Next hit time, next damage. On hover: Time to cure."
	)
	default boolean showPoisonInfoboxes()
	{
		return false;
	}

	@ConfigItem (
			keyName = "showAntiInfoboxes",
			name = "Show Antipoison Infoboxes",
			description = "Configures whether to show the infoboxes for antivenom and antipoisons.<br>" +
					"Shows: Antipoison and antivenom duration"
	)
	default boolean showAntiInfoboxes()
	{
		return true;
	}
}
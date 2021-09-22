# tts.*

## Overview

The tts plugin enables you to get devices tts status.


## Syntax

	local tts = require( "plugin.tts" )
	tts.check( function(event)
		print(event.name, event.freeSpace)
	end)


## Project Settings

To use this plugin, add an entry into the `plugins` table of `build.settings`. When added, the build server will integrate the plugin during the build phase.

``````lua
settings =
{
	plugins =
	{
		["plugin.tts"] =
		{
			publisherId = "com.labolado",
			suportedPlatform = {
				android = {url = ""},
			}
		},
	},
}
``````

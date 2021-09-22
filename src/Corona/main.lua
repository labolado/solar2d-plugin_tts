local tts = require "plugin.tts"

local rect = display.newRect(display.contentCenterX, display.contentCenterY, 100, 100)
rect:setFillColor(1, 0, 0, 1)
local count = 1
rect:addEventListener("touch", function(e)
	if e.phase == "began" then
		tts.init()
		tts.speak("你好，坦克" .. count)
		count = count + 1
	end
end)

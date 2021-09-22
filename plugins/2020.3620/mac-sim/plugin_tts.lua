local Library = require "CoronaLibrary"

-- Create library
local lib = Library:new{ name = 'plugin.tts', publisherId = 'com.labolado' }

local function showUnsupportedMessage()
	native.showAlert( 'Not Supported', 'The tts plugin is not supported on the simulator, please build for an iOS or Android device', { 'OK' } )
end

-- tts.setAllowedTypes
function lib.check(listener)
	showUnsupportedMessage()
end

-- Return an instance
return lib

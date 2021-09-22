//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.library"
package plugin.tts;

import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.speech.tts.TextToSpeech;

import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.NamedJavaFunction;

import java.io.File;


/**
 * Implements the Lua interface for a Corona plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
@SuppressWarnings("WeakerAccess")
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
	/** Lua registry ID to the Lua function to be called when the ad request finishes. */
	private int fListener;
	private TextToSpeech mTTS;
	private boolean mInitSuccess;

	/** This corresponds to the event name, e.g. [Lua] event.name */
	private static final String EVENT_NAME = "ttsStatus";
	private static final String LOG_TAG = "Corona.plugin.tts";


	/**
	 * Creates a new Lua interface to this plugin.
	 * <p>
	 * Note that a new LuaLoader instance will not be created for every CoronaActivity instance.
	 * That is, only one instance of this class will be created for the lifetime of the application process.
	 * This gives a plugin the option to do operations in the background while the CoronaActivity is destroyed.
	 */
	@SuppressWarnings("unused")
	public LuaLoader() {
		// Initialize member variables.
		fListener = CoronaLua.REFNIL;
		mInitSuccess = false;

		// Set up this plugin to listen for Corona runtime events to be received by methods
		// onLoaded(), onStarted(), onSuspended(), onResumed(), and onExiting().
		CoronaEnvironment.addRuntimeListener(this);

		localInit();
	}

	/**
	 * Called when this plugin is being loaded via the Lua require() function.
	 * <p>
	 * Note that this method will be called every time a new CoronaActivity has been launched.
	 * This means that you'll need to re-initialize this plugin here.
	 * <p>
	 * Warning! This method is not called on the main UI thread.
	 * @param L Reference to the Lua state that the require() function was called from.
	 * @return Returns the number of values that the require() function will return.
	 *         <p>
	 *         Expected to return 1, the library that the require() function is loading.
	 */
	@Override
	public int invoke(LuaState L) {
		// Register this plugin into Lua with the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
			new SpeekWrapper(),
			new IsSpeakingWrapper(),
			new StopWrapper(),
			new InitWrapper(),
		};
		String libName = L.toString( 1 );
		L.register(libName, luaFunctions);

		// Returning 1 indicates that the Lua require() function will return the above Lua library.
		return 1;
	}

	/**
	 * Called after the Corona runtime has been created and just before executing the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been loaded/initialized.
	 *                Provides a LuaState object that allows the application to extend the Lua API.
	 */
	@Override
	public void onLoaded(CoronaRuntime runtime) {
		// Note that this method will not be called the first time a Corona activity has been launched.
		// This is because this listener cannot be added to the CoronaEnvironment until after
		// this plugin has been required-in by Lua, which occurs after the onLoaded() event.
		// However, this method will be called when a 2nd Corona activity has been created.

	}

	/**
	 * Called just after the Corona runtime has executed the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been started.
	 */
	@Override
	public void onStarted(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been suspended which pauses all rendering, audio, timers,
	 * and other Corona related operations. This can happen when another Android activity (ie: window) has
	 * been displayed, when the screen has been powered off, or when the screen lock is shown.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been suspended.
	 */
	@Override
	public void onSuspended(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been resumed after a suspend.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been resumed.
	 */
	@Override
	public void onResumed(CoronaRuntime runtime) {
	}

	/**
	 * Called just before the Corona runtime terminates.
	 * <p>
	 * This happens when the Corona activity is being destroyed which happens when the user presses the Back button
	 * on the activity, when the native.requestExit() method is called in Lua, or when the activity's finish()
	 * method is called. This does not mean that the application is exiting.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that is being terminated.
	 */
	@Override
	public void onExiting(CoronaRuntime runtime) {
		mTTS.shutdown();
		// Remove the Lua listener reference.
		CoronaLua.deleteRef( runtime.getLuaState(), fListener );
		fListener = CoronaLua.REFNIL;
	}

	private void localInit() {
		if (!mInitSuccess) {
			android.content.Context ctx = CoronaEnvironment.getApplicationContext();
			if (ctx == null) {
			    Log.d(LOG_TAG, "CoronaEnvironment.getApplicationContext() is null!");
			} else {
				mTTS = new TextToSpeech(ctx, new TextToSpeech.OnInitListener() {
					@Override
					public void onInit(int status) {
						mInitSuccess = (status == TextToSpeech.SUCCESS);
					    Log.d(LOG_TAG, "init status = " + status);
					}
				});
			}
		}
	}

	@SuppressWarnings({"WeakerAccess", "SameReturnValue"})
	public int init(LuaState L) {
		localInit();
		return 0;
	}

	/**
	 * The following Lua function has been called:  library.check( listener )
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param L Reference to the Lua state that the Lua function was called from.
	 * @return Returns the number of values to be returned by the library.check() function.
	 */
	@SuppressWarnings({"WeakerAccess", "SameReturnValue"})
	public int speak(LuaState L) {
	    int textIndex = 1;
	    String text = L.toString(textIndex);
	    mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null);

		return 0;
	}

	@SuppressWarnings({"WeakerAccess", "SameReturnValue"})
	public int isSpeaking(LuaState L) {
		L.pushBoolean(mTTS.isSpeaking());

		return 1;
	}

	@SuppressWarnings({"WeakerAccess", "SameReturnValue"})
	public int stop(LuaState L) {
		mTTS.stop();

		return 0;
	}

	public static boolean externalMemoryAvailable() {
		return android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED);
	}

	public static long getAvailableInternalMemorySize() {
		File path = Environment.getDataDirectory();
//		File path = Environment.getDownloadCacheDirectory();
//		File path = Environment.getRootDirectory();
		StatFs stat = new StatFs(path.getAbsolutePath());
		long blockSize;
		long availableBlocks;
		if (Build.VERSION.SDK_INT < 18) {
			blockSize = stat.getBlockSize();
			availableBlocks = stat.getAvailableBlocks();
		} else {
			blockSize = stat.getBlockSizeLong();
			availableBlocks = stat.getAvailableBlocksLong();
		}
		return formatSize(availableBlocks * blockSize);
	}

	public static long getTotalInternalMemorySize() {
		File path = Environment.getDataDirectory();
//		File path = Environment.getDownloadCacheDirectory();
//		File path = Environment.getRootDirectory();
		StatFs stat = new StatFs(path.getAbsolutePath());
		long blockSize;
		long totalBlocks;
		if (Build.VERSION.SDK_INT < 18) {
			blockSize = stat.getBlockSize();
			totalBlocks = stat.getBlockCount();
		} else {
			blockSize = stat.getBlockSizeLong();
			totalBlocks = stat.getBlockCountLong();
		}
		return formatSize(totalBlocks * blockSize);
	}

	public static long getAvailableExternalMemorySize() {
		if (externalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize;
			long availableBlocks;
			if (Build.VERSION.SDK_INT < 18) {
				blockSize = stat.getBlockSize();
				availableBlocks = stat.getAvailableBlocks();
			} else {
				blockSize = stat.getBlockSizeLong();
				availableBlocks = stat.getAvailableBlocksLong();
			}
			return formatSize(availableBlocks * blockSize);
		} else {
			return 0;
		}
	}

	public static long getTotalExternalMemorySize() {
		if (externalMemoryAvailable()) {
			File path = Environment.getExternalStorageDirectory();
			StatFs stat = new StatFs(path.getPath());
			long blockSize;
			long totalBlocks;
			if (Build.VERSION.SDK_INT < 18) {
				blockSize = stat.getBlockSize();
				totalBlocks = stat.getBlockCount();
			} else {
				blockSize = stat.getBlockSizeLong();
				totalBlocks = stat.getBlockCountLong();
			}
			return formatSize(totalBlocks * blockSize);
		} else {
			return 0;
		}
	}

	public static long formatSize(long size) {
//		String suffix = null;
//
//		if (size >= 1024) {
//			suffix = "KB";
//			size /= 1024;
//			if (size >= 1024) {
//				suffix = "MB";
//				size /= 1024;
//			}
//		}
//
//		StringBuilder resultBuffer = new StringBuilder(Long.toString(size));
//
//		int commaOffset = resultBuffer.length() - 3;
//		while (commaOffset > 0) {
//			resultBuffer.insert(commaOffset, ',');
//			commaOffset -= 3;
//		}
//
//		if (suffix != null) resultBuffer.append(suffix);
//		return resultBuffer.toString();
        return  size / 1024 / 1024;
	}

	/** Implements the library.speak() Lua function. */
	@SuppressWarnings("unused")
	private class SpeekWrapper implements NamedJavaFunction {
		/**
		 * Gets the name of the Lua function as it would appear in the Lua script.
		 * @return Returns the name of the custom Lua function.
		 */
		@Override
		public String getName() {
			return "speak";
		}
		
		/**
		 * This method is called when the Lua function is called.
		 * <p>
		 * Warning! This method is not called on the main UI thread.
		 * @param L Reference to the Lua state.
		 *                 Needed to retrieve the Lua function's parameters and to return values back to Lua.
		 * @return Returns the number of values to be returned by the Lua function.
		 */
		@Override
		public int invoke(LuaState L) {
			return speak(L);
		}
	}

	/** Implements the library.isSpeaking() Lua function. */
	@SuppressWarnings("unused")
	private class IsSpeakingWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "isSpeaking";
		}

		@Override
		public int invoke(LuaState L) {
			return isSpeaking(L);
		}
	}

	/** Implements the library.stop() Lua function. */
	@SuppressWarnings("unused")
	private class StopWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "stop";
		}

		@Override
		public int invoke(LuaState L) {
			return stop(L);
		}
	}

	/** Implements the library.init() Lua function. */
	@SuppressWarnings("unused")
	private class InitWrapper implements NamedJavaFunction {
		@Override
		public String getName() {
			return "init";
		}

		@Override
		public int invoke(LuaState L) {
			return init(L);
		}
	}
}

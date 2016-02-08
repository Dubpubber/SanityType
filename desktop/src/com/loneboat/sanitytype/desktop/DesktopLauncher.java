package com.loneboat.sanitytype.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.loneboat.sanitytype.SanityType;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.width = 800;
		config.height = 600;
        config.backgroundFPS = 60;
		new LwjglApplication(new SanityType(), config);
	}
}

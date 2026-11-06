package com.nightbeam.reel;

import com.google.gson.JsonObject;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.resources.sounds.Sound;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Logs every sound the game starts during a capture, with the frame it starts on and where it plays, so the reel's
 * audio mix can place the game's own sounds exactly (the container has no audio device to record from).
 */
public final class ReelSounds {
	private static BufferedWriter out;
	/** Output-video time of the frame being produced. */
	static volatile double videoTime;
	static volatile boolean recording;

	private ReelSounds() {
	}

	static void open(Path file) throws IOException {
		close();
		out = Files.newBufferedWriter(file);
	}

	static void close() {
		if (out == null) return;
		try {
			out.close();
		} catch (IOException ignored) {
		}
		out = null;
	}

	public static void played(SoundInstance instance, SoundManager manager) {
		if (out == null || !recording) return;
		try {
			WeighedSoundEvents events = instance.resolve(manager);
			if (events == null) return;
			Sound sound = instance.getSound();
			if (sound == null || sound == SoundManager.EMPTY_SOUND || sound == SoundManager.INTENTIONALLY_EMPTY_SOUND) return;
			JsonObject row = new JsonObject();
			row.addProperty("t", videoTime);
			row.addProperty("event", instance.getLocation().toString());
			row.addProperty("file", sound.getPath().toString());
			row.addProperty("source", instance.getSource().getName());
			row.addProperty("volume", instance.getVolume());
			row.addProperty("pitch", instance.getPitch());
			row.addProperty("x", instance.getX());
			row.addProperty("y", instance.getY());
			row.addProperty("z", instance.getZ());
			row.addProperty("relative", instance.isRelative());
			row.addProperty("attenuation", instance.getAttenuation().name());
			row.addProperty("range", sound.getAttenuationDistance());
			row.addProperty("stream", sound.shouldStream());
			row.addProperty("looping", instance.isLooping());
			row.addProperty("camX", ReelCamera.x);
			row.addProperty("camY", ReelCamera.y);
			row.addProperty("camZ", ReelCamera.z);
			out.write(row.toString());
			out.newLine();
		} catch (RuntimeException | IOException ignored) {
		}
	}
}

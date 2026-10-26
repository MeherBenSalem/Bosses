package com.nightbeam.reel;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Streams the main framebuffer, frame by frame, into an ffmpeg encoder. */
final class ReelCapture implements AutoCloseable {
	private final Process ffmpeg;
	private final WritableByteChannel channel;
	private final int width, height;
	private final ByteBuffer buffer;
	private int frames;

	/** {@code still} writes a single PNG instead of an H.264 video. */
	ReelCapture(Path out, int width, int height, int fps, boolean still, Path log) throws IOException {
		this.width = width;
		this.height = height;
		List<String> cmd = new ArrayList<>(List.of("ffmpeg", "-hide_banner", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24",
				"-s", width + "x" + height, "-r", Integer.toString(fps), "-i", "-", "-vf", "vflip"));
		if (still) {
			if (!out.toString().contains("%")) cmd.addAll(List.of("-frames:v", "1"));
			cmd.add(out.toString());
		} else {
			cmd.addAll(List.of("-c:v", "libx264", "-preset", "medium", "-crf", "10", "-pix_fmt", "yuv444p", "-movflags", "+faststart",
					out.toString()));
		}
		ffmpeg = new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(log.toFile()).start();
		OutputStream stream = ffmpeg.getOutputStream();
		channel = Channels.newChannel(stream);
		buffer = MemoryUtil.memAlloc(width * height * 3);
	}

	int frames() {
		return frames;
	}

	/** Reads the bound main render target (called right after the frame is drawn, before it is presented). */
	void grab(RenderTarget target) throws IOException {
		if (target.width != width || target.height != height) {
			throw new IllegalStateException("framebuffer is " + target.width + "x" + target.height + ", expected " + width + "x" + height);
		}
		// Binds the target as both draw and read framebuffer; it is the bound target at this point anyway.
		target.bindWrite(false);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		buffer.clear();
		GL11.glReadPixels(0, 0, width, height, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buffer);
		buffer.rewind();
		while (buffer.hasRemaining()) channel.write(buffer);
		frames++;
	}

	@Override
	public void close() throws IOException {
		try {
			channel.close();
			if (!ffmpeg.waitFor(10, TimeUnit.MINUTES)) ffmpeg.destroyForcibly();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			MemoryUtil.memFree(buffer);
		}
	}
}

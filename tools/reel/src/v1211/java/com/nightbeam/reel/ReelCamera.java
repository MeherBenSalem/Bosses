package com.nightbeam.reel;

import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * The render camera the director imposes, plus keyframe interpolation. Positions and look targets use Hermite
 * splines with finite-difference tangents over the key times (Catmull-Rom on non-uniform knots), so the camera's
 * speed is continuous through every key.
 */
public final class ReelCamera {
	/** Render-thread state read by the mixins. */
	public static volatile boolean active;
	public static volatile boolean detached = true;
	public static double x, y, z;
	public static float yaw, pitch, roll;
	/** Field of view in degrees, or NaN to leave the game's own. */
	public static double fov = Double.NaN;
	public static volatile String hud = "full";
	public static volatile boolean hand = true;
	public static volatile boolean hidePlayer;
	public static volatile int hudOffset;
	/** Particles closer to the camera than this many blocks are not drawn (0 draws all). */
	public static volatile double particleClip;

	private ReelCamera() {
	}

	public static void off() {
		active = false;
		fov = Double.NaN;
		hud = "full";
		hand = true;
		hidePlayer = false;
		hudOffset = 0;
		particleClip = 0;
		roll = 0;
	}

	/** A resolved camera pose. */
	public record Pose(Vec3 pos, float yaw, float pitch, float roll, double fov) {
	}

	/** Look target of a key: an explicit point, an entity (resolved by the caller) or the key's own rotation. */
	static Vec3 lookPoint(Shot.Key key, Vec3 pos, Function<Shot.Key, Vec3> entityLook) {
		if (key.lookTag != null) {
			Vec3 at = entityLook.apply(key);
			if (at != null) return at;
		}
		if (key.look != null) return new Vec3(key.look[0], key.look[1], key.look[2]);
		double[] rot = key.rot != null ? key.rot : new double[]{0, 0};
		return pos.add(direction(rot[0], rot[1]).scale(16));
	}

	public static Vec3 direction(double yawDeg, double pitchDeg) {
		double yaw = Math.toRadians(yawDeg), pitch = Math.toRadians(pitchDeg);
		return new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
	}

	public static float yawOf(Vec3 d) {
		return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
	}

	public static float pitchOf(Vec3 d) {
		return (float) Math.toDegrees(Math.atan2(-d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
	}

	/** Remaps shot time for the whole path: "linear", "smooth" (ease in and out), "in" or "out". */
	static double timing(String mode, double t, double t0, double t1) {
		if (t1 <= t0) return t0;
		double u = Math.max(0, Math.min(1, (t - t0) / (t1 - t0)));
		u = switch (mode == null ? "linear" : mode) {
			case "smooth" -> u * u * (3 - 2 * u);
			case "smoother" -> u * u * u * (u * (u * 6 - 15) + 10);
			case "in" -> u * u;
			case "out" -> 1 - (1 - u) * (1 - u);
			case "expo" -> u < 0.5 ? Math.pow(2, 20 * u - 10) / 2 : (2 - Math.pow(2, -20 * u + 10)) / 2;
			default -> u;
		};
		return t0 + u * (t1 - t0);
	}

	/** Index of the segment holding {@code t}: keys[i].t <= t <= keys[i+1].t (clamped). */
	static int segment(List<Shot.Key> keys, double t) {
		int i = 0;
		while (i < keys.size() - 2 && t > keys.get(i + 1).t) i++;
		return i;
	}

	/** Hermite interpolation of four samples at four times, evaluated at t in [t1, t2]. */
	static double hermite(double p0, double p1, double p2, double p3, double t0, double t1, double t2, double t3, double t) {
		double h = t2 - t1;
		if (h <= 1e-9) return p2;
		double s = (t - t1) / h;
		double m1 = (p2 - p0) / Math.max(1e-9, t2 - t0) * h;
		double m2 = (p3 - p1) / Math.max(1e-9, t3 - t1) * h;
		double s2 = s * s, s3 = s2 * s;
		return (2 * s3 - 3 * s2 + 1) * p1 + (s3 - 2 * s2 + s) * m1 + (-2 * s3 + 3 * s2) * p2 + (s3 - s2) * m2;
	}

	/** Evaluates a keyframed pose at shot time {@code t}. */
	static Pose evaluate(Shot shot, List<Shot.Key> keys, double t, Function<Shot.Key, Vec3> entityLook) {
		if (keys.size() == 1) {
			Shot.Key k = keys.get(0);
			Vec3 pos = k.pos != null ? new Vec3(k.pos[0], k.pos[1], k.pos[2]) : Vec3.ZERO;
			Vec3 look = lookPoint(k, pos, entityLook);
			Vec3 d = look.subtract(pos);
			return new Pose(pos, yawOf(d), pitchOf(d), roll(k), k.fov != null ? k.fov : shot.fov);
		}
		double tt = timing(shot.timing, t, keys.get(0).t, keys.get(keys.size() - 1).t);
		int i = segment(keys, tt);
		Shot.Key k1 = keys.get(i), k2 = keys.get(i + 1);
		Shot.Key k0 = i > 0 ? keys.get(i - 1) : null, k3 = i + 2 < keys.size() ? keys.get(i + 2) : null;
		double t1 = k1.t, t2 = k2.t;
		double t0 = k0 != null ? k0.t : t1 - (t2 - t1), t3 = k3 != null ? k3.t : t2 + (t2 - t1);
		tt = Math.max(t1, Math.min(t2, tt));

		Vec3 p1 = pos(k1), p2 = pos(k2);
		Vec3 p0 = k0 != null ? pos(k0) : p1.subtract(p2.subtract(p1)), p3 = k3 != null ? pos(k3) : p2.add(p2.subtract(p1));
		Vec3 pos = spline(p0, p1, p2, p3, t0, t1, t2, t3, tt);

		Vec3 l1 = lookPoint(k1, p1, entityLook), l2 = lookPoint(k2, p2, entityLook);
		Vec3 l0 = k0 != null ? lookPoint(k0, p0, entityLook) : l1.subtract(l2.subtract(l1));
		Vec3 l3 = k3 != null ? lookPoint(k3, p3, entityLook) : l2.add(l2.subtract(l1));
		Vec3 look = spline(l0, l1, l2, l3, t0, t1, t2, t3, tt);
		Vec3 d = look.subtract(pos);

		float roll = (float) hermite(k0 != null ? roll(k0) : roll(k1), roll(k1), roll(k2), k3 != null ? roll(k3) : roll(k2), t0, t1, t2, t3, tt);
		double f1 = k1.fov != null ? k1.fov : shot.fov, f2 = k2.fov != null ? k2.fov : shot.fov;
		double f0 = k0 != null && k0.fov != null ? k0.fov : f1, f3 = k3 != null && k3.fov != null ? k3.fov : f2;
		double fov = hermite(f0, f1, f2, f3, t0, t1, t2, t3, tt);
		return new Pose(pos, yawOf(d), pitchOf(d), roll, fov);
	}

	private static float roll(Shot.Key k) {
		if (k.roll != null) return k.roll.floatValue();
		return k.rot != null && k.rot.length > 2 ? (float) k.rot[2] : 0;
	}

	private static Vec3 pos(Shot.Key k) {
		return new Vec3(k.pos[0], k.pos[1], k.pos[2]);
	}

	private static Vec3 spline(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t0, double t1, double t2, double t3, double t) {
		return new Vec3(hermite(p0.x, p1.x, p2.x, p3.x, t0, t1, t2, t3, t), hermite(p0.y, p1.y, p2.y, p3.y, t0, t1, t2, t3, t),
				hermite(p0.z, p1.z, p2.z, p3.z, t0, t1, t2, t3, t));
	}

	/** Piecewise-linear value curve (game speed); {@code fallback} when empty. */
	static double value(List<Shot.Value> values, double t, double fallback) {
		if (values == null || values.isEmpty()) return fallback;
		if (t <= values.get(0).t) return values.get(0).v;
		for (int i = 0; i < values.size() - 1; i++) {
			Shot.Value a = values.get(i), b = values.get(i + 1);
			if (t <= b.t) {
				double s = (t - a.t) / Math.max(1e-9, b.t - a.t);
				s = s * s * (3 - 2 * s);
				return a.v + (b.v - a.v) * s;
			}
		}
		return values.get(values.size() - 1).v;
	}

	/** Gentle, deterministic hand-held drift in degrees: yaw, pitch, roll. */
	static float[] handheld(double amplitude, double t) {
		if (amplitude <= 0) return new float[3];
		double a = amplitude;
		float yaw = (float) (a * (0.55 * Math.sin(t * 1.31 + 0.4) + 0.3 * Math.sin(t * 2.71 + 1.9) + 0.15 * Math.sin(t * 5.3 + 0.2)));
		float pitch = (float) (a * 0.7 * (0.6 * Math.sin(t * 1.07 + 2.2) + 0.3 * Math.sin(t * 3.11 + 0.7) + 0.1 * Math.sin(t * 6.1)));
		float roll = (float) (a * 0.35 * (0.7 * Math.sin(t * 0.83 + 1.1) + 0.3 * Math.sin(t * 2.3 + 2.9)));
		return new float[]{yaw, pitch, roll};
	}
}

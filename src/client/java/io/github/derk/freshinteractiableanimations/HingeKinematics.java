package io.github.derk.freshinteractiableanimations;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.DoorHinge;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public final class HingeKinematics {
    public static final float PANEL_THICKNESS = 0.1875f;
    private static final float FULL_SWING_DEGREES = 90.0f;

    private HingeKinematics() {
    }

    public static Offset computeHingeInset(Direction facing, DoorHinge hinge, float angleDegrees) {
        float normalizedSwing = (float) Math.sin(Math.toRadians(Math.abs(angleDegrees)));
        float amount = PANEL_THICKNESS * normalizedSwing;
        Anchor anchor = locateHingeAnchor(facing, hinge);

        return switch (facing) {
            case NORTH, SOUTH -> new Offset(anchor.x() < 0.5f ? amount : -amount, 0.0f);
            case EAST, WEST -> new Offset(0.0f, anchor.z() < 0.5f ? amount : -amount);
            default -> new Offset(0.0f, 0.0f);
        };
    }

    public static float calculateDesiredAngleDegrees(Direction facing, DoorHinge hinge, boolean opening) {
        if (!opening) {
            return 0.0f;
        }
        return FULL_SWING_DEGREES * computeRotationOrientation(facing, hinge);
    }

    public static int computeRotationOrientation(Direction facing, DoorHinge hinge) {
        return hinge == DoorHinge.LEFT ? 1 : -1;
    }

    public static Anchor locateHingeAnchor(Direction facing, DoorHinge hinge) {
        boolean leftSide = hinge == DoorHinge.LEFT;
        return switch (facing) {
            case NORTH -> new Anchor(leftSide ? 0.0f : 1.0f, 1.0f);
            case EAST -> new Anchor(0.0f, leftSide ? 0.0f : 1.0f);
            case SOUTH -> new Anchor(leftSide ? 1.0f : 0.0f, 0.0f);
            case WEST -> new Anchor(1.0f, leftSide ? 1.0f : 0.0f);
            default -> new Anchor(0.0f, 0.0f);
        };
    }

    public static float cubicOut(float rawProgress) {
        float clamped = Math.max(0.0f, Math.min(1.0f, rawProgress));
        return clamped * (3.0f + clamped * (clamped - 3.0f));
    }

    public static float sampleOpenProgress(boolean opening, long startedAtNanos, long sampledAtNanos, long totalDurationNanos) {
        float elapsedFraction = (float) (sampledAtNanos - startedAtNanos) / (float) totalDurationNanos;
        float easedFraction = cubicOut(elapsedFraction);
        return opening ? easedFraction : 1.0f - easedFraction;
    }

    public static float sampleDoorAngleDegrees(Direction facing, DoorHinge hinge, boolean opening, float progress) {
        return FULL_SWING_DEGREES * computeRotationOrientation(facing, hinge) * progress;
    }

    public static float sampleFenceGateAngleDegrees(Direction facing, boolean opening, float progress) {
        float direction = switch (facing) {
            case NORTH, EAST -> -1.0f;
            case SOUTH, WEST -> 1.0f;
            default -> 1.0f;
        };
        return FULL_SWING_DEGREES * direction * progress;
    }

    public static FenceGateLeafMotion describeFenceGateLeafMotion(Direction facing, boolean lowLeaf, float progress) {
        float lowLeafDirection = switch (facing) {
            case NORTH, EAST -> 1.0f;
            case SOUTH, WEST -> -1.0f;
            default -> 1.0f;
        };
        float degrees = FULL_SWING_DEGREES * progress * (lowLeaf ? lowLeafDirection : -lowLeafDirection);

        return switch (facing.getAxis()) {
            case Z -> new FenceGateLeafMotion(lowLeaf ? 0.125f : 0.875f, 0.5f, degrees);
            case X -> new FenceGateLeafMotion(0.5f, lowLeaf ? 0.125f : 0.875f, degrees);
            default -> new FenceGateLeafMotion(0.5f, 0.5f, 0.0f);
        };
    }

    public static TrapdoorPose describeTrapdoorPose(Direction facing, BlockHalf half, float progress) {
        float angleMagnitude = FULL_SWING_DEGREES * progress;
        float radians = (float) Math.toRadians(angleMagnitude);
        float sine = (float) Math.sin(radians);
        float cosine = (float) Math.cos(radians);
        float halfHeight = 0.5f * (sine + PANEL_THICKNESS * cosine);
        float sideInset = 0.5f * (cosine + PANEL_THICKNESS * sine);
        float centerY = half == BlockHalf.TOP ? 1.0f - halfHeight : halfHeight;
        float halfDirection = half == BlockHalf.TOP ? -1.0f : 1.0f;

        return switch (facing) {
            case NORTH -> new TrapdoorPose(Axis.X, angleMagnitude * halfDirection, 0.5f, centerY, 1.0f - sideInset);
            case SOUTH -> new TrapdoorPose(Axis.X, -angleMagnitude * halfDirection, 0.5f, centerY, sideInset);
            case EAST -> new TrapdoorPose(Axis.Z, angleMagnitude * halfDirection, sideInset, centerY, 0.5f);
            case WEST -> new TrapdoorPose(Axis.Z, -angleMagnitude * halfDirection, 1.0f - sideInset, centerY, 0.5f);
            default -> new TrapdoorPose(Axis.X, 0.0f, 0.5f, centerY, 0.5f);
        };
    }

    @Environment(EnvType.CLIENT)
    public record Anchor(float x, float z) {
    }

    @Environment(EnvType.CLIENT)
    public record Offset(float x, float z) {
    }

    @Environment(EnvType.CLIENT)
    public enum Axis {
        X,
        Z
    }

    @Environment(EnvType.CLIENT)
    public record FenceGateLeafMotion(float pivotX, float pivotZ, float degrees) {
    }

    @Environment(EnvType.CLIENT)
    public record TrapdoorPose(Axis axis, float degrees, float centerX, float centerY, float centerZ) {
    }
}

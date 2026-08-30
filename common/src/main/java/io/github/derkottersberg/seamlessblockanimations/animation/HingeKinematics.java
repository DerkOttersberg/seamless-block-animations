package io.github.derkottersberg.seamlessblockanimations.animation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;

public final class HingeKinematics {
    public static final float PANEL_THICKNESS = 0.1875f;
    private static final float FULL_SWING_DEGREES = 90.0f;

    private HingeKinematics() {
    }

    public static Offset computeHingeInset(Direction facing, DoorHingeSide hinge, float angleDegrees) {
        float normalizedSwing = (float) Math.sin(Math.toRadians(Math.abs(angleDegrees)));
        float amount = PANEL_THICKNESS * normalizedSwing;
        Anchor anchor = locateHingeAnchor(facing, hinge);

        return switch (facing) {
            case NORTH, SOUTH -> new Offset(anchor.x() < 0.5f ? amount : -amount, 0.0f);
            case EAST, WEST -> new Offset(0.0f, anchor.z() < 0.5f ? amount : -amount);
            default -> new Offset(0.0f, 0.0f);
        };
    }

    public static Anchor locateHingeAnchor(Direction facing, DoorHingeSide hinge) {
        boolean leftSide = hinge == DoorHingeSide.LEFT;
        return switch (facing) {
            case NORTH -> new Anchor(leftSide ? 0.0f : 1.0f, 1.0f);
            case EAST -> new Anchor(0.0f, leftSide ? 0.0f : 1.0f);
            case SOUTH -> new Anchor(leftSide ? 1.0f : 0.0f, 0.0f);
            case WEST -> new Anchor(1.0f, leftSide ? 1.0f : 0.0f);
            default -> new Anchor(0.0f, 0.0f);
        };
    }

    public static float sampleDoorAngleDegrees(Direction facing, DoorHingeSide hinge, float openProgress) {
        float orientation = hinge == DoorHingeSide.LEFT ? 1.0f : -1.0f;
        return FULL_SWING_DEGREES * orientation * openProgress;
    }

    public static FenceGateLeafMotion describeFenceGateLeafMotion(Direction facing, boolean lowLeaf, float progress) {
        float lowLeafDirection = switch (facing) {
            case NORTH, EAST -> 1.0f;
            case SOUTH, WEST -> -1.0f;
            default -> 1.0f;
        };
        float degrees = FULL_SWING_DEGREES * progress * (lowLeaf ? lowLeafDirection : -lowLeafDirection);
        float outerPostCenter = 1.0f / 16.0f;
        float oppositePostCenter = 15.0f / 16.0f;

        return switch (facing.getAxis()) {
            case Z -> new FenceGateLeafMotion(lowLeaf ? outerPostCenter : oppositePostCenter, 0.5f, degrees);
            case X -> new FenceGateLeafMotion(0.5f, lowLeaf ? outerPostCenter : oppositePostCenter, degrees);
            default -> new FenceGateLeafMotion(0.5f, 0.5f, 0.0f);
        };
    }

    public static TrapdoorPose describeTrapdoorPose(Direction facing, Half half, float progress) {
        float angleMagnitude = FULL_SWING_DEGREES * progress;
        float radians = (float) Math.toRadians(angleMagnitude);
        float sine = (float) Math.sin(radians);
        float cosine = (float) Math.cos(radians);
        float halfHeight = 0.5f * (sine + PANEL_THICKNESS * cosine);
        float sideInset = 0.5f * (cosine + PANEL_THICKNESS * sine);
        float centerY = half == Half.TOP ? 1.0f - halfHeight : halfHeight;
        float halfDirection = half == Half.TOP ? -1.0f : 1.0f;

        return switch (facing) {
            case NORTH -> new TrapdoorPose(Axis.X, angleMagnitude * halfDirection, 0.5f, centerY, 1.0f - sideInset);
            case SOUTH -> new TrapdoorPose(Axis.X, -angleMagnitude * halfDirection, 0.5f, centerY, sideInset);
            case EAST -> new TrapdoorPose(Axis.Z, angleMagnitude * halfDirection, sideInset, centerY, 0.5f);
            case WEST -> new TrapdoorPose(Axis.Z, -angleMagnitude * halfDirection, 1.0f - sideInset, centerY, 0.5f);
            default -> new TrapdoorPose(Axis.X, 0.0f, 0.5f, centerY, 0.5f);
        };
    }

    public record Anchor(float x, float z) {
    }

    public record Offset(float x, float z) {
    }

    public enum Axis {
        X,
        Z
    }

    public record FenceGateLeafMotion(float pivotX, float pivotZ, float degrees) {
    }

    public record TrapdoorPose(Axis axis, float degrees, float centerX, float centerY, float centerZ) {
    }
}

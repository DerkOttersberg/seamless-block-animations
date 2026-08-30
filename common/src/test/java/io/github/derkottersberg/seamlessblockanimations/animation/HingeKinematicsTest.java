package io.github.derkottersberg.seamlessblockanimations.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.Half;
import org.junit.jupiter.api.Test;

class HingeKinematicsTest {
    @Test
    void doorHingesStayOnTheirVanillaBlockEdges() {
        assertEquals(new HingeKinematics.Anchor(0.0f, 1.0f),
            HingeKinematics.locateHingeAnchor(Direction.NORTH, DoorHingeSide.LEFT));
        assertEquals(new HingeKinematics.Anchor(1.0f, 1.0f),
            HingeKinematics.locateHingeAnchor(Direction.NORTH, DoorHingeSide.RIGHT));
        assertEquals(new HingeKinematics.Anchor(0.0f, 0.0f),
            HingeKinematics.locateHingeAnchor(Direction.EAST, DoorHingeSide.LEFT));
    }

    @Test
    void trapdoorMovesFromHorizontalSlabToContainedVerticalPanel() {
        HingeKinematics.TrapdoorPose closed =
            HingeKinematics.describeTrapdoorPose(Direction.NORTH, Half.BOTTOM, 0.0f);
        HingeKinematics.TrapdoorPose open =
            HingeKinematics.describeTrapdoorPose(Direction.NORTH, Half.BOTTOM, 1.0f);

        assertEquals(HingeKinematics.PANEL_THICKNESS * 0.5f, closed.centerY(), 0.00001f);
        assertEquals(0.5f, closed.centerZ(), 0.00001f);
        assertEquals(0.5f, open.centerY(), 0.00001f);
        assertEquals(1.0f - HingeKinematics.PANEL_THICKNESS * 0.5f, open.centerZ(), 0.00001f);
        assertEquals(90.0f, open.degrees(), 0.00001f);
    }

    @Test
    void fenceGateLeavesUseOppositeOuterPostPivots() {
        HingeKinematics.FenceGateLeafMotion low =
            HingeKinematics.describeFenceGateLeafMotion(Direction.NORTH, true, 1.0f);
        HingeKinematics.FenceGateLeafMotion high =
            HingeKinematics.describeFenceGateLeafMotion(Direction.NORTH, false, 1.0f);

        assertEquals(1.0f / 16.0f, low.pivotX(), 0.00001f);
        assertEquals(15.0f / 16.0f, high.pivotX(), 0.00001f);
        assertEquals(-high.degrees(), low.degrees(), 0.00001f);
    }
}

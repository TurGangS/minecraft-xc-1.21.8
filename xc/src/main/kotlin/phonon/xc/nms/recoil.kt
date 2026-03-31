package phonon.xc.nms

import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.commands.arguments.EntityAnchorArgument
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

// 1.21.8 IMPORTS
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.entity.Relative
import net.minecraft.world.phys.Vec3
// Removed Vec2 import because we don't need it anymore!

private val RELATIVE_TELEPORT_FLAGS = setOf(
    Relative.X,
    Relative.Y,
    Relative.Z,
    Relative.Y_ROT,
    Relative.X_ROT,
)

public fun Player.sendRecoilPacketUsingRelativeTeleport(
    recoilHorizontal: Float,
    recoilVertical: Float,
) {
    // FIX: The constructor wants 4 arguments: (Position, Velocity, Yaw, Pitch)
    val change = PositionMoveRotation(
        Vec3(0.0, 0.0, 0.0),      // 1. Relative Position
        Vec3(0.0, 0.0, 0.0),      // 2. Relative Velocity (The missing argument!)
        recoilHorizontal,         // 3. Yaw (yRot)
        recoilVertical            // 4. Pitch (xRot)
    )

    val packet = ClientboundPlayerPositionPacket(
        0,      // Teleport ID
        change, // The movement/rotation object
        RELATIVE_TELEPORT_FLAGS
    )

    (this as CraftPlayer).handle.connection.send(packet)
}

public fun Player.sendRecoilPacketUsingLookAt(
    dirX: Double,
    dirY: Double,
    dirZ: Double,
) {
    val packet = ClientboundPlayerLookAtPacket(
        EntityAnchorArgument.Anchor.EYES,
        dirX,
        dirY,
        dirZ,
    )

    (this as CraftPlayer).handle.connection.send(packet)
}
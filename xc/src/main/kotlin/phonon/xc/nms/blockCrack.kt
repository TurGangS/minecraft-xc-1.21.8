package phonon.xc.nms

import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.World

/**
 * Broadcasts a block break animation (cracks) to nearby players.
 * 1.21.8 Update: Packet constructor remains the same (id, pos, stage).
 */
public fun World.broadcastBlockCrackAnimation(
    players: List<CraftPlayer>,
    entityId: Int,
    blx: Int,
    bly: Int,
    blz: Int,
    breakStage: Int,
) {
    // 1.21.8 Constructor: (int entityId, BlockPos pos, int destructionStage)
    val packet = ClientboundBlockDestructionPacket(
        entityId,
        BlockPos(blx, bly, blz),
        breakStage
    )

    // Uses the extension function from nms.kt
    players.broadcastPacketWithinDistance(
        packet,
        originX = blx.toDouble(),
        originY = bly.toDouble(),
        originZ = blz.toDouble(),
        maxDistance = 64.0,
    )
}
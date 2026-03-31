package phonon.xc.nms.gun

import kotlin.math.floor
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.monster.Shulker
import org.bukkit.Location
import org.bukkit.entity.Player

// 1.21 IMPORTS
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.entity.CraftPlayer
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.entity.Relative
import net.minecraft.world.phys.Vec3
import java.util.Collections

/**
 * Shulker entity is used to force player into crawling position.
 */
public class BoxEntity(
    location: Location,
) : Shulker(EntityType.SHULKER, (location.world as CraftWorld).handle) {

    init {
        this.persist = false
        this.isInvisible = true
        this.isNoGravity = true
        this.isInvulnerable = true
        this.isNoAi = true
        this.isSilent = true
        this.attachFace = Direction.UP
    }

    // REMOVED: Broken overrides (canChangeDimensions, isSensitiveToWater).
    // Defaults are fine for a fake entity.

    internal fun setPeekAmount(value: Double) {
        val peek = value.coerceIn(0.0, 1.0)
        super.setRawPeekAmount((peek * 100.0).toInt())
    }

    internal fun moveAboveLocation(loc: Location) {
        val heightInBlock = loc.y - floor(loc.y)
        val yHeightShulker: Double = if (heightInBlock >= 0.4) loc.y + 1.5 else loc.y + 0.5
        val yPeekAmount: Double = if (heightInBlock >= 0.4) 1.0 - heightInBlock else 0.0

        // FIX: "moveTo" is now "setPos" in 1.21 mappings
        this.setPos(loc.x, yHeightShulker, loc.z)
        this.setRot(0f, 0f)

        this.setPeekAmount(yPeekAmount)
    }

    internal fun sendCreatePacket(player: Player) {
        // FIX: Use 3-arg constructor: (Entity, data, BlockPos)
        val packetSpawn = ClientboundAddEntityPacket(this, 0, this.blockPosition())

        val connection = (player as CraftPlayer).handle.connection
        connection.send(packetSpawn)

        // Send Metadata
        val dataList = this.entityData.nonDefaultValues
        if (dataList != null) {
            val packetMetadata = ClientboundSetEntityDataPacket(this.id, dataList)
            connection.send(packetMetadata)
        }
    }

    internal fun sendDestroyPacket(player: Player) {
        val packet = ClientboundRemoveEntitiesPacket(this.id)
        (player as CraftPlayer).handle.connection.send(packet)
    }

    internal fun sendMovePacket(player: Player) {
        // FIX: Create PositionMoveRotation object (same logic as recoil.kt)
        val change = PositionMoveRotation(
            this.position(),   // Current Position (Vec3)
            Vec3.ZERO,         // Velocity (Vec3)
            this.yRot,         // Yaw
            this.xRot          // Pitch
        )

        // FIX: Constructor is (id, change, relatives, onGround)
        val packet = ClientboundTeleportEntityPacket(
            this.id,
            change,
            Collections.emptySet(), // No relative flags (Absolute teleport)
            this.onGround
        )

        (player as CraftPlayer).handle.connection.send(packet)
    }

    internal fun sendPeekMetadataPacket(player: Player) {
        val dataList = this.entityData.nonDefaultValues
        if (dataList != null) {
            val packet = ClientboundSetEntityDataPacket(this.id, dataList)
            (player as CraftPlayer).handle.connection.send(packet)
        }
    }
}
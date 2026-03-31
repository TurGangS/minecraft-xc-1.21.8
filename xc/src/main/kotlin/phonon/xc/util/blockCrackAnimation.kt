/**
 * Packet handler for block cracking.
 */

package phonon.xc.util.blockCrackAnimation

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Player
// FIX: Using the correct CraftPlayer import for modern Paper/Spigot
import org.bukkit.craftbukkit.entity.CraftPlayer
// FIX: Import our flattened NMS utility
import phonon.xc.nms.broadcastBlockCrackAnimation

/**
 * Block location to create block cracking animation.
 */
public data class BlockCrackAnimation(val world: World, val x: Int, val y: Int, val z: Int)

/**
 * Send packet for block breaking at location.
 */
public class TaskBroadcastBlockCrackAnimations(
    val animations: ArrayList<BlockCrackAnimation>,
): Runnable {
    // Cache of players in each world
    internal val worldPlayers: HashMap<UUID, List<CraftPlayer>> = HashMap()

    override fun run() {
        val random = ThreadLocalRandom.current()

        // Cache players as CraftPlayer objects
        if (animations.size > 0) {
            Bukkit.getWorlds().forEach { world ->
                // In modern Kotlin/Bukkit, we cast the handle's connection usually,
                // but for this utility, casting the player to CraftPlayer is correct.
                worldPlayers.put(world.uid, world.players.map { it as CraftPlayer })
            }
        }

        for (block in animations) {
            worldPlayers[block.world.uid]?.let { players ->
                val entityId = random.nextInt(Int.MAX_VALUE)
                val breakStage = random.nextInt(4) + 1
                // This calls the extension function we fixed in the NMS folder
                block.world.broadcastBlockCrackAnimation(players, entityId, block.x, block.y, block.z, breakStage)
            }
        }
    }
}
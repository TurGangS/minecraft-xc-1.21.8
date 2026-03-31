/**
 * Manage recoil packet wrapper and async task to send recoil
 * packets to clients.
 */

package phonon.xc.util.recoil

import java.util.concurrent.ThreadLocalRandom
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import phonon.xc.gun.Gun
import phonon.xc.util.math.directionFromYawPitch

// FIX: We moved these functions to the main 'phonon.xc.nms' package
import phonon.xc.nms.sendRecoilPacketUsingLookAt
import phonon.xc.nms.sendRecoilPacketUsingRelativeTeleport

/**
 * Wrapper for player gun recoil data needed to run asynchronously.
 */
public data class RecoilPacket(
    val player: Player,
    val isInVehicle: Boolean, // flag if player in vehicle, must use alternate recoil packet
    val mountOffsetY: Double, // mount offset y height, to adjust eye height for recoil calculation
    val recoilVertical: Double,
    val recoilHorizontal: Double,
    val multiplier: Double, // recoil multiplier
)

/**
 * Runnable task to send visual recoil packets to players.
 */
public class TaskRecoil(
    val packets: ArrayList<RecoilPacket>,
): Runnable {
    override fun run() {
        val random = ThreadLocalRandom.current()

        for ( p in packets ) {
            val (player, isInVehicle, mountOffsetY, recoilVertical, recoilHorizontal, multiplier) = p

            // skip if no recoil
            if ( recoilVertical == 0.0 && recoilHorizontal == 0.0 ) {
                continue
            }

            // calculate net recoil after multiplier
            val netRecoilVertical = recoilVertical * multiplier
            val netRecoilHorizontalRange = recoilHorizontal * multiplier
            val netRecoilHorizontal = random.nextDouble(-netRecoilHorizontalRange, netRecoilHorizontalRange)

            if ( isInVehicle ) {
                // calculate new look direction new yaw and pitch
                val playerEyeLocation = player.eyeLocation
                val newYaw = playerEyeLocation.yaw + netRecoilHorizontal.toFloat()
                val newPitch = playerEyeLocation.pitch - netRecoilVertical.toFloat()

                // new view direction for player after recoil
                val newViewDirection = directionFromYawPitch(newYaw, newPitch).multiply(50.0)

                player.sendRecoilPacketUsingLookAt(
                    playerEyeLocation.x + newViewDirection.x,
                    playerEyeLocation.y + mountOffsetY + newViewDirection.y,
                    playerEyeLocation.z + newViewDirection.z,
                )
            }
            else { // default recoil packet using relative teleport packet
                player.sendRecoilPacketUsingRelativeTeleport(
                    netRecoilHorizontal.toFloat(),
                    -netRecoilVertical.toFloat(),
                )
            }
        }
    }
}
/**
 * Vehicle utils
 */

package phonon.xc.util

import org.bukkit.entity.EntityType

/**
 * When players are in a vehicle, their eye height does not adjust properly.
 * Need to manually add vehicle mount eye height offset for proper
 * bullet shoot location, and recoil.
 *
 * Literally just guess and check these until they work...
 * Easier than trying to figure out how mineman black box works.
 */
public fun entityMountEyeHeightOffset(type: EntityType): Double {
    return when (type) {
        EntityType.HORSE -> 0.85

        // Hardcoded Boats
        EntityType.OAK_BOAT -> -0.45
        EntityType.SPRUCE_BOAT -> -0.45
        EntityType.BIRCH_BOAT -> -0.45
        EntityType.JUNGLE_BOAT -> -0.45
        EntityType.ACACIA_BOAT -> -0.45
        EntityType.DARK_OAK_BOAT -> -0.45
        EntityType.MANGROVE_BOAT -> -0.45
        EntityType.CHERRY_BOAT -> -0.45
        EntityType.PALE_OAK_BOAT -> -0.45
        EntityType.BAMBOO_RAFT -> -0.45

        // Hardcoded Chest Boats
        EntityType.OAK_CHEST_BOAT -> -0.45
        EntityType.SPRUCE_CHEST_BOAT -> -0.45
        EntityType.BIRCH_CHEST_BOAT -> -0.45
        EntityType.JUNGLE_CHEST_BOAT -> -0.45
        EntityType.ACACIA_CHEST_BOAT -> -0.45
        EntityType.DARK_OAK_CHEST_BOAT -> -0.45
        EntityType.MANGROVE_CHEST_BOAT -> -0.45
        EntityType.CHERRY_CHEST_BOAT -> -0.45
        EntityType.PALE_OAK_CHEST_BOAT -> -0.45
        EntityType.BAMBOO_CHEST_RAFT -> -0.45

        EntityType.ARMOR_STAND -> 0.0
        else -> 1.0
    }
}
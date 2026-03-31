/**
 * Plugin data storage and item stack management for custom item types
 * (guns, ammo, hats, etc.). Contains main storage container and
 * extension functions on XC to get custom items from the storage or
 * Bukkit item stacks.
 * * UPDATED FOR 1.21.8:
 * - Maintains NMS method signatures to prevent breaking other files.
 * - Internally uses Bukkit API (ItemMeta) because NBT tags are removed in 1.21.
 */
package phonon.xc.item

import java.util.EnumMap
import kotlin.math.min
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.persistence.PersistentDataContainer
// Keep imports to satisfy existing aliases, but we won't use the unsafe ones logic-wise
import phonon.xc.nms.NmsNBTTagCompound
import phonon.xc.nms.NmsNBTTagList
import phonon.xc.nms.NBTTagString
import phonon.xc.nms.NBTTagInt
import phonon.xc.nms.putTag
import phonon.xc.nms.containsKey
import phonon.xc.nms.containsKeyOfType
import phonon.xc.nms.NmsItemStack
import phonon.xc.nms.CraftItemStack
import phonon.xc.nms.CraftPlayer
import phonon.xc.nms.CraftMagicNumbers
import phonon.xc.nms.getMainHandNMSItem
import phonon.xc.XC
import phonon.xc.ammo.Ammo
import phonon.xc.armor.Hat
import phonon.xc.gun.Gun
import phonon.xc.landmine.Landmine
import phonon.xc.melee.MeleeWeapon
import phonon.xc.throwable.ThrowableItem

/**
 * Bukkit persistent data container (pdc) key.
 */
internal const val BUKKIT_STORAGE_TAG = "PublicBukkitValues"

/**
 * NBT tag type for integers.
 */
internal const val NBT_TAG_INT = 3

/**
 * Return custom item type player is holding in hand.
 */
public fun XC.getItemTypeInHand(player: Player): Int {
    // 1.21 Fix: Use Bukkit API directly (fast & safe)
    val item = player.inventory.itemInMainHand
    return this.config.materialToCustomItemType[item.type] ?: -1 // Handle potential null map return
}

/**
 * Return custom item id if item in hand matches the config material
 * for item type.
 */
public fun XC.getCustomItemIdInHand(player: Player, itemType: Int): Int {
    val item = player.inventory.itemInMainHand

    // 1. Check Type
    val itemTypeInHand = this.config.materialToCustomItemType[item.type]
    if ( itemTypeInHand != itemType ) {
        return -1
    }

    // 2. Check CustomModelData (Safe 1.21 replacement for NBT)
    if (item.hasItemMeta()) {
        val meta = item.itemMeta
        if (meta.hasCustomModelData()) {
            return meta.customModelData
        }
    }

    return -1
}

/**
 * Get custom item type from nms item stack using raw NBT tags.
 * * 1.21 ADAPTER: Takes NmsItemStack to satisfy your other code,
 * but converts to Bukkit Item internally to read data safely.
 */
public fun <T> getObjectFromNmsItemStack(
    nmsItem: NmsItemStack,
    materialType: Material,
    storage: Array<T>,
): T? {
    // Convert NMS -> Bukkit to safely read data (NBT tags are gone on NMS items)
    val item = CraftItemStack.asBukkitCopy(nmsItem)

    if ( item.type == materialType ) {
        if (item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta.hasCustomModelData()) {
                val modelId = meta.customModelData
                if ( modelId < storage.size ) {
                    return storage[modelId]
                }
            }
        }
    }

    return null
}

/**
 * Get a custom item from index in XC engine storage Array<T>
 * from nms item stack's custom model data as index.
 */
public fun <T> getCustomItemUnchecked(
    nmsItem: NmsItemStack,
    storage: Array<T>,
): T? {
    // Convert NMS -> Bukkit to safely read data
    val item = CraftItemStack.asBukkitCopy(nmsItem)

    if (item.hasItemMeta()) {
        val meta = item.itemMeta
        if (meta.hasCustomModelData()) {
            val modelId = meta.customModelData
            if ( modelId < storage.size ) {
                return storage[modelId]
            }
        }
    }

    return null
}

/**
 * Internal helper to get NMS item stack from a bukkit CraftItemStack.
 * Requires reflection to access private NMS item stack handle.
 * * KEPT AS IS: The handle field still exists in 1.21.
 */
internal object GetNmsItemStack {
    val privField = CraftItemStack::class.java.getDeclaredField("handle")

    init {
        privField.setAccessible(true)
    }

    public fun from(item: CraftItemStack): NmsItemStack {
        return privField.get(item) as NmsItemStack
    }
}

/**
 * For a bukkit ItemStack.
 * Uses PersistentDataContainer now (which matches PublicBukkitValues logic).
 */
public fun XC.getItemIntDataIfMaterialMatches(
    item: ItemStack,
    material: Material,
    key: String,
): Int {
    if ( item.type == material && item.hasItemMeta() ) {
        try {
            val meta = item.itemMeta
            // FIX: Check if string is already formatted as "namespace:key"
            val namespacedKey = NamespacedKey.fromString(key) ?: NamespacedKey(this.plugin, key)
            val container = meta.persistentDataContainer

            if ( container.has(namespacedKey, PersistentDataType.INTEGER) ) {
                return container.get(namespacedKey, PersistentDataType.INTEGER) ?: -1
            }
        } catch ( err: Exception ) {
            err.printStackTrace()
            this.logger.severe("Failed to get item PDC key: $err")
        }
    }

    return -1
}


/**
 * Internal helper to find player inventory slot for a custom item.
 */
internal fun XC.getInventorySlotForCustomItemWithNbtKey(
    player: Player,
    material: Material,
    nbtKey: String,
    value: Int,
): Int {
    val inventory = player.inventory
    val items = inventory.contents

    // FIX: Check if string is already formatted as "namespace:key"
    val namespacedKey = NamespacedKey.fromString(nbtKey) ?: NamespacedKey(this.plugin, nbtKey)

    for ( slot in items.indices ) {
        val item = items[slot]
        if ( item != null && item.type == material ) {
            if (item.hasItemMeta()) {
                val meta = item.itemMeta
                val container = meta.persistentDataContainer
                if (container.has(namespacedKey, PersistentDataType.INTEGER)) {
                    if (container.get(namespacedKey, PersistentDataType.INTEGER) == value) {
                        return slot
                    }
                }
            }
        }
    }

    return -1
}

/**
 * Set an item stack's armor attribute.
 * * 1.21 UPDATE: Manual NBT modification is impossible.
 * We must use the API. We ignore the UUID/String slot args and use 1.21 standards.
 */
internal fun XC.setItemArmorNMS(
    item: ItemStack,
    armor: Int,
    slot: String,
    uuidLeast: Int,
    uuidMost: Int,
): ItemStack {
    val meta = item.itemMeta ?: return item

    val slotGroup = when (slot.lowercase()) {
        "head" -> EquipmentSlotGroup.HEAD
        "chest" -> EquipmentSlotGroup.CHEST
        "legs" -> EquipmentSlotGroup.LEGS
        "feet" -> EquipmentSlotGroup.FEET
        "hand", "mainhand" -> EquipmentSlotGroup.MAINHAND
        "offhand" -> EquipmentSlotGroup.OFFHAND
        else -> EquipmentSlotGroup.ANY
    }

    meta.removeAttributeModifier(Attribute.ARMOR)

    // FIX: Use 'this.plugin' directly
    val key = NamespacedKey(this.plugin, "custom_armor_${slot}")

    val modifier = AttributeModifier(
        key,
        armor.toDouble(),
        AttributeModifier.Operation.ADD_NUMBER,
        slotGroup
    )

    meta.addAttributeModifier(Attribute.ARMOR, modifier)
    item.itemMeta = meta

    return item
}

/**
 * For item in main player hand.
 */
public fun XC.checkHandMaterialAndGetNbtIntKey(player: Player, material: Material, key: String): Int {
    val item = player.inventory.itemInMainHand

    if ( item.type == material && item.hasItemMeta() ) {
        // FIX: Check if string is already formatted as "namespace:key"
        val namespacedKey = NamespacedKey.fromString(key) ?: NamespacedKey(this.plugin, key)

        val container = item.itemMeta.persistentDataContainer

        if (container.has(namespacedKey, PersistentDataType.INTEGER)) {
            return container.get(namespacedKey, PersistentDataType.INTEGER) ?: -1
        }
    }

    return -1
}


// ============================================================================
// GUN ITEM GETTERS
// ============================================================================

public fun XC.getGunFromNmsItemStack(nmsItem: NmsItemStack): Gun? {
    return getObjectFromNmsItemStack(
        nmsItem,
        this.config.materialGun,
        this.storage.gun,
    )
}

public fun XC.getGunInHand(player: Player): Gun? {
    val item = player.inventory.itemInMainHand
    if (item.type != Material.AIR) {
        // We can pass the Bukkit item directly because getObjectFromNmsItemStack
        // will treat it as NmsItemStack (since they are aliased in 1.21)
        // OR we can convert it.
        // Given your alias: NmsItemStack = ItemStack, we can just pass it.
        // However, getObjectFromNmsItemStack calls asBukkitCopy.
        // Calling asBukkitCopy on a Bukkit item is safe.
        // To be safe regarding Types, we just pass the NMS handle if we can,
        // but here we have a Bukkit Player.

        // Simpler: Just re-use the logic locally to avoid NMS conversion overhead
        return getGunFromItemBukkit(item)
    }
    return null
}

public fun XC.getGunInHandUnchecked(player: Player): Gun? {
    val item = player.inventory.itemInMainHand
    if (item.type != Material.AIR) {
        if (item.hasItemMeta() && item.itemMeta.hasCustomModelData()) {
            val modelId = item.itemMeta.customModelData
            if (modelId < this.storage.gun.size) return this.storage.gun[modelId]
        }
    }
    return null
}

public fun XC.getGunInSlot(player: Player, slot: Int): Gun? {
    val item = player.inventory.getItem(slot)
    if ( item != null ) {
        return getGunFromItemBukkit(item)
    }
    return null
}

public fun XC.getGunFromItem(item: ItemStack): Gun? {
    // 1.21: The "Bukkit" method is now the only method.
    // The "NMS" method was just an NBT parser.
    return getGunFromItemBukkit(item)
}

internal fun XC.getGunFromItemNMS(item: ItemStack): Gun? {
    // Redirect to the safe Bukkit method
    return getGunFromItemBukkit(item)
}

internal fun XC.getGunFromItemBukkit(item: ItemStack): Gun? {
    if ( item.type == this.config.materialGun ) {
        if (item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta.hasCustomModelData()) {
                val modelId = meta.customModelData
                if ( modelId < this.config.maxGunTypes ) {
                    return this.storage.gun[modelId]
                }
            }
        }
    }
    return null
}


// ============================================================================
// THROWABLE ITEM GETTERS
// ============================================================================

public fun XC.getThrowableFromNmsItemStack(nmsItem: NmsItemStack): ThrowableItem? {
    return getObjectFromNmsItemStack(
        nmsItem,
        this.config.materialThrowable,
        this.storage.throwable,
    )
}

public fun XC.getThrowableInHand(player: Player): ThrowableItem? {
    val item = player.inventory.itemInMainHand
    if (item.type != Material.AIR) {
        return getThrowableFromItemBukkit(item)
    }
    return null
}

public fun XC.getThrowableInHandUnchecked(player: Player): ThrowableItem? {
    val item = player.inventory.itemInMainHand
    if (item.type != Material.AIR) {
        if (item.hasItemMeta() && item.itemMeta.hasCustomModelData()) {
            val modelId = item.itemMeta.customModelData
            if (modelId < this.storage.throwable.size) return this.storage.throwable[modelId]
        }
    }
    return null
}

public fun XC.getThrowableFromItem(item: ItemStack): ThrowableItem? {
    return getThrowableFromItemBukkit(item)
}

internal fun XC.getThrowableFromItemNMS(item: ItemStack): ThrowableItem? {
    return getThrowableFromItemBukkit(item)
}

internal fun XC.getThrowableFromItemBukkit(item: ItemStack): ThrowableItem? {
    if (item.type == this.config.materialThrowable) {
        if (item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta.hasCustomModelData()) {
                val modelId = meta.customModelData
                if (modelId < this.config.maxThrowableTypes) {
                    return this.storage.throwable[modelId]
                }
            }
        }
    }
    return null
}


// ============================================================================
// MELEE WEAPON ITEM GETTERS
// ============================================================================

public fun XC.getMeleeFromNmsItemStack(nmsItem: NmsItemStack): MeleeWeapon? {
    return getObjectFromNmsItemStack(
        nmsItem,
        this.config.materialMelee,
        this.storage.melee,
    )
}

public fun XC.getMeleeInHand(player: Player): MeleeWeapon? {
    val item = player.inventory.itemInMainHand
    if (item.type != Material.AIR) {
        return getMeleeFromItemStack(item)
    }
    return null
}

public fun XC.getMeleeInHandUnchecked(player: Player): MeleeWeapon? {
    val item = player.inventory.itemInMainHand
    if (item.type != Material.AIR) {
        if (item.hasItemMeta() && item.itemMeta.hasCustomModelData()) {
            val modelId = item.itemMeta.customModelData
            if (modelId < this.storage.melee.size) return this.storage.melee[modelId]
        }
    }
    return null
}

// Helper needed for the above
public fun XC.getMeleeFromItemStack(item: ItemStack): MeleeWeapon? {
    if (item.type == this.config.materialMelee) {
        if (item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta.hasCustomModelData()) {
                val modelId = meta.customModelData
                if (modelId < this.storage.melee.size) {
                    return this.storage.melee[modelId]
                }
            }
        }
    }
    return null
}


// ============================================================================
// ARMOR/HAT ITEM GETTERS
// ============================================================================

public fun XC.getHatFromNmsItemStack(nmsItem: NmsItemStack): Hat? {
    return getObjectFromNmsItemStack(
        nmsItem,
        this.config.materialArmor,
        this.storage.hat,
    )
}

public fun XC.getHatInHand(player: Player): Hat? {
    val item = player.inventory.itemInMainHand
    if (item.type != Material.AIR) {
        return getHatFromItemStack(item)
    }
    return null
}

public fun XC.getHatInHandUnchecked(player: Player): Hat? {
    val item = player.inventory.itemInMainHand
    if (item.type != Material.AIR) {
        if (item.hasItemMeta() && item.itemMeta.hasCustomModelData()) {
            val modelId = item.itemMeta.customModelData
            if (modelId < this.storage.hat.size) return this.storage.hat[modelId]
        }
    }
    return null
}

// Helper needed for the above
public fun XC.getHatFromItemStack(item: ItemStack): Hat? {
    if (item.type == this.config.materialArmor) {
        if (item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta.hasCustomModelData()) {
                val modelId = meta.customModelData
                if (modelId < this.storage.hat.size) {
                    return this.storage.hat[modelId]
                }
            }
        }
    }
    return null
}
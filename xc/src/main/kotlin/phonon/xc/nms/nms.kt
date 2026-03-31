package phonon.xc.nms

import net.minecraft.nbt.*
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

// 1.21 IMPORTS (No version numbers, as established)
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.util.CraftMagicNumbers

// TYPE ALIASES
internal typealias NmsItemStack = ItemStack
internal typealias NmsNBTTagCompound = CompoundTag
internal typealias NmsNBTTagList = ListTag
internal typealias NmsNBTTagString = StringTag
internal typealias NmsNBTTagInt = IntTag
internal typealias NmsPacketPlayOutSetSlot = ClientboundContainerSetSlotPacket
internal typealias CraftItemStack = CraftItemStack
internal typealias CraftPlayer = CraftPlayer
internal typealias CraftMagicNumbers = CraftMagicNumbers

// ============================================================================
// EXTENSION FUNCTIONS
// ============================================================================

internal fun CraftPlayer.getMainHandNMSItem(): NmsItemStack {
    return this.handle.mainHandItem
}

internal fun ServerPlayer.sendPacket(p: Packet<*>) {
    this.connection.send(p)
}

internal fun List<CraftPlayer>.broadcastPacketWithinDistance(
    packet: Packet<*>,
    originX: Double,
    originY: Double,
    originZ: Double,
    maxDistance: Double,
) {
    val maxDistanceSq = maxDistance * maxDistance
    for (player in this) {
        val loc = player.location
        val dx = loc.x - originX
        val dy = loc.y - originY
        val dz = loc.z - originZ
        val distanceSq = (dx * dx) + (dy * dy) + (dz * dz)

        if (distanceSq <= maxDistanceSq) {
            player.handle.connection.send(packet)
        }
    }
}

internal fun ServerPlayer.sendItemSlotChange(slot: Int, item: ItemStack) {
    val packet = ClientboundContainerSetSlotPacket(
        this.containerMenu.containerId,
        this.containerMenu.incrementStateId(),
        slot,
        item
    )
    this.connection.send(packet)
}

internal fun CompoundTag.putTag(key: String, tag: Tag) {
    this.put(key, tag)
}

internal fun CompoundTag.containsKey(key: String): Boolean {
    return this.contains(key)
}

internal fun CompoundTag.containsKeyOfType(key: String, ty: Int): Boolean {
    // FIX: Get the tag safely and check its ID directly
    val tag = this.get(key)
    return tag != null && tag.id == ty.toByte()
}

// ============================================================================
// NBT WRAPPERS
// ============================================================================

@JvmInline
internal value class NBTTagString(val tag: NmsNBTTagString) {
    constructor(s: String) : this(NmsNBTTagString.valueOf(s))
    fun toNms(): NmsNBTTagString = this.tag
}

@JvmInline
internal value class NBTTagInt(val tag: NmsNBTTagInt) {
    constructor(i: Int) : this(NmsNBTTagInt.valueOf(i))
    fun toNms(): NmsNBTTagInt = this.tag
}
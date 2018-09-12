package net.rocketgarden.minecraft.geocrafting

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.BookMeta

import java.util.*

/**
 * Metadata class describing geocaches
 */
class Geocache private constructor() {
    private var x: Double = 0.toDouble()
    private var y: Double = 0.toDouble()
    private var z: Double = 0.toDouble()
    private var world: String? = null
    @Transient
    private var location: Location? = null
    //awkward serializing hacks!

    var createdAt: Long = 0
        private set
    var owner: String
        private set
    var ownerUUID: UUID
        private set
    var finds: MutableList<String>
    var name: String
        private set
    var desc: String
        private set
    var hint: String?
        private set

    init {
        ownerUUID = UUID_EMPTY
        finds = ArrayList()
        owner = ""
        hint = ""
        desc = ""
        name = ""
    }

    fun getLocation(): Location {
        //todo what even is this??
        if (location == null) {
            val w = Bukkit.getWorld(world)
            if (w == null)
                Bukkit.getLogger().warning("Could not restore geocache location in world: $world")
            location = Location(w, x, y, z)
        }
        return location!!
    }

    /**
     * @return true if the player has not already found this cache
     */
    fun addFinder(player: Player): Boolean {
        val name = player.name
        return !finds.contains(name) && finds.add(name)
    }

    fun isOwnedBy(player: Player): Boolean {
        //        Bukkit.getLogger().info("UUID: " + ownerUUID + " - " + player.getUniqueId());
        return ownerUUID == player.uniqueId || owner == player.name
    }

    fun serialize(): String {
        val gson = Gson()
        return gson.toJson(this)
    }

    class Builder {
        @Transient
        private var g: Geocache = Geocache()

        fun build(): Geocache {
            return g
        }

        fun setLocation(loc: Location): Builder {
            g.location = loc
            g.x = loc.x
            g.y = loc.y
            g.z = loc.z
            g.world = loc.world.name
            return this
        }

        fun setCreationTime(createdAt: Long): Builder {
            g.createdAt = createdAt
            return this
        }

        fun setOwner(player: Player): Builder {
            g.owner = player.name
            g.ownerUUID = player.uniqueId
            return this
        }

        fun setOwner(playerName: String): Builder {
            g.owner = playerName
            g.ownerUUID = UUID_EMPTY
            return this
        }

        fun setFinds(finds: MutableList<String>): Builder {
            g.finds = finds
            return this
        }

        fun setTitle(title: String): Builder {
            g.name = title
            return this
        }

        fun setDescription(desc: String): Builder {
            g.desc = desc
            return this
        }

        fun setHint(hint: String): Builder {
            g.hint = hint
            return this
        }
    }

    companion object {

        val UUID_EMPTY = UUID(0L, 0L)

        fun deserialize(serialized: String): Geocache {
            val gson = Gson()
            return gson.fromJson(serialized, Geocache::class.java)
        }

        fun fromBlockPlayerBook(block: Block, player: Player, meta: BookMeta): Geocache {
            val b = Builder()
            b.setLocation(block.location)
                    .setCreationTime(System.currentTimeMillis())
                    .setOwner(player)
                    .setFinds(ArrayList())
                    .setDescription(meta.getPage(1))
                    .setTitle(meta.title)
            if (meta.pageCount > 1) {
                b.setHint(meta.getPage(2))
            }
            if ((player.hasPermission(Geocrafting.PERM_CHANGE_OWNER) || player.isOp) && meta.pageCount > 2) {
                b.setOwner(meta.getPage(3))
            }
            return b.build()
        }
    }
}
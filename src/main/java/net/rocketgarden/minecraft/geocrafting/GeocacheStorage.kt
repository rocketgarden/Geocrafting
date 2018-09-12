package net.rocketgarden.minecraft.geocrafting

import org.bukkit.Location
import org.bukkit.block.Block

/**
 * Interface defining classes that can save/load geocaches from various sources
 */
interface GeocacheStorage {

    val geocaches: List<Geocache>
    fun saveGeocache(geocache: Geocache)

    fun saveGeocaches(geocaches: List<Geocache>)

    fun getGeocache(location: Location): Geocache?

    fun removeGeocache(geocache: Geocache)
    fun removeGeocache(location: Location)

    fun isGeocache(location: Location): Boolean
    fun isGeocache(block: Block): Boolean

    fun onStop()
}

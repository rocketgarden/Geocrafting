package net.rocketgarden.minecraft.geocrafting

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration

import java.io.File
import java.io.IOException
import java.util.*
import java.util.logging.Level

/**
 * Class for saving lists of geocaches to disk
 * Relatively lightweight and naive alternative to SQL storage
 */
class GeocacheFileStorage(var plugin: Geocrafting) : GeocacheStorage {
    private var geocacheConfig: FileConfiguration
    private val geocacheConfigFile: File

    private var geocacheMap: MutableMap<Location, Geocache>

    override val geocaches: List<Geocache>
        get() = ArrayList(geocacheMap.values)

    init {
        geocacheConfigFile = File(plugin.dataFolder, GEOCACHE_DB_FILE) //todo get this file from constructor params?

        geocacheConfig = YamlConfiguration.loadConfiguration(geocacheConfigFile)

        val serializedCaches = geocacheConfig.getStringList(GEOCACHE_LIST_PATH)
        geocacheMap = HashMap() //todo switch to treemap with custom comparator
        for (s in serializedCaches) {
            val geocache = Geocache.deserialize(s)
            geocacheMap[geocache.getLocation()] = geocache
        }
    }


    override fun saveGeocache(geocache: Geocache) {
        geocacheMap[geocache.getLocation()] = geocache
        if (plugin.debug) {
            plugin.logger.info("Saved geocache: " + geocache.name)
        }
    }

    override fun saveGeocaches(geocaches: List<Geocache>) {
        for (g in geocaches) {
            geocacheMap[g.getLocation()] = g
        }
    }

    override fun getGeocache(location: Location): Geocache? {
        return geocacheMap[location]
    }

    override fun removeGeocache(geocache: Geocache) {
        val ret = geocacheMap.remove(geocache.getLocation())
        if (ret == null)
            plugin.logger.fine(
                    "Tried to remove a geocache from a location where there is none: ${geocache.getLocation()}")
    }

    override fun removeGeocache(location: Location) {
        val ret = geocacheMap.remove(location)
        if (ret == null)
            plugin.logger.fine(
                    "Tried to remove a geocache from a location where there is none: $location")
    }

    override fun isGeocache(location: Location): Boolean {
        return geocacheMap.containsKey(location)
    }

    override fun isGeocache(block: Block): Boolean {
        return (block.type == Material.CHEST && geocacheMap.containsKey(block.location))
    }

    override fun onStop() {
        saveCustomConfig()
    }

    private fun saveCustomConfig() {
        geocacheConfig.set(GEOCACHE_LIST_PATH, serializeCaches())
        try { //todo do this async
            geocacheConfig.save(geocacheConfigFile)
        } catch (ex: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save config to $geocacheConfigFile", ex)
        }

    }

    private fun serializeCaches(): List<String> {
        val serializedCaches = ArrayList<String>(geocacheMap.size)
        for (g in geocacheMap.values) {
            val s = g.serialize()
            serializedCaches.add(s)
        }
        return serializedCaches
    }

    companion object {
        const val GEOCACHE_DB_FILE = "geocaches.yml"
        const val GEOCACHE_LIST_PATH = "geocaches"
    }

}

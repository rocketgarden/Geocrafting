package net.rocketgarden.minecraft.geocrafting

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration

import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.logging.Level

/**
 * Class for saving lists of geocaches to disk
 *
 * Created by Vince on 4/12/2014.
 */
class GeocacheFileManager(internal var plugin: Geocrafting) : GeocacheManager {
    private var geocacheConfig: FileConfiguration? = null
    private var geocacheConfigFile: File? = null

    internal var geocacheMap: MutableMap<Location, Geocache>

    override val geocaches: List<Geocache>
        get() = ArrayList(geocacheMap.values)

    private val customConfig: FileConfiguration?
        get() {
            if (geocacheConfig == null) {
                reloadCustomConfig()
            }
            return geocacheConfig
        }

    init {

        reloadCustomConfig()

        val serializedCaches = geocacheConfig!!.getStringList(GEOCACHE_LIST_PATH)
        geocacheMap = HashMap()
        for (s in serializedCaches) {
            val geocache = Geocache.deserialize(s)
            geocacheMap[geocache.getLocation()] = geocache
        }
    }


    override fun saveGeocache(geocache: Geocache) {
        geocacheMap[geocache.getLocation()] = geocache
        if (plugin.debug) {
            plugin.logger.info("Saved geocache: " + geocache.name!!)
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
            plugin.logger.warning(
                    "Tried to remove a geocache from a location where there is none: " + geocache.getLocation())
    }

    override fun removeGeocache(location: Location) {
        val ret = geocacheMap.remove(location)
        if (ret == null)
            plugin.logger.warning(
                    "Tried to remove a geocache from a location where there is none: $location")
    }

    override fun isGeocache(location: Location): Boolean {
        return geocacheMap.containsKey(location)
    }

    override fun isGeocache(block: Block): Boolean {
        return (block.type == Material.CHEST && geocacheMap.containsKey(block.location))
        //seems redundant but first check should be O(1)
    }

    override fun onStop() {
        saveCustomConfig()
    }

    private fun reloadCustomConfig() {
        if (geocacheConfigFile == null) {
            geocacheConfigFile = File(plugin.dataFolder, GEOCACHE_CONFIG_FILE)
        }
        geocacheConfig = YamlConfiguration.loadConfiguration(geocacheConfigFile!!)

        // Look for defaults in the jar
        val defConfigStream = plugin.getResource(GEOCACHE_CONFIG_FILE)
        if (defConfigStream != null) {
            val defConfig = YamlConfiguration.loadConfiguration(InputStreamReader(defConfigStream))
            geocacheConfig!!.defaults = defConfig
        }
    }

    private fun saveCustomConfig() {
        if (geocacheConfig == null || geocacheConfigFile == null) {
            return
        }
        geocacheConfig!!.set(GEOCACHE_LIST_PATH, serializeCaches())
        try {
            customConfig!!.save(geocacheConfigFile!!)
        } catch (ex: IOException) {
            plugin.logger.log(Level.SEVERE, "Could not save config to " + geocacheConfigFile!!, ex)
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
        val GEOCACHE_CONFIG_FILE = "geocaches.yml"
        val GEOCACHE_LIST_PATH = "geocaches"
    }

}

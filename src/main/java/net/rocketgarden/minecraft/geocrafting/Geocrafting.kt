package net.rocketgarden.minecraft.geocrafting

import com.google.common.base.Strings
import org.bukkit.*
import org.bukkit.block.Chest
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BookMeta
import org.bukkit.plugin.java.JavaPlugin


class Geocrafting : JavaPlugin(), Listener {


    var debug: Boolean = false

    private lateinit var geocacheManager: GeocacheManager
    private lateinit var commandParser: CommandParser

    override fun onEnable() {
        server.pluginManager.registerEvents(this, this)

        geocacheManager = GeocacheFileManager(this)

                commandParser = CommandParser(this, geocacheManager)

        getCommand(CommandParser.CMD_LABEL_GEOCACHE).executor = commandParser
    }

    @EventHandler(ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK)
            return
        val item = event.item
        val block = event.clickedBlock ?: return
        if (block.type == Material.CHEST) {
            val chest = block.state as Chest
            val player = event.player

            //CREATE A GEOCACHE-----
            if (item != null && item.data.itemType == Material.WRITTEN_BOOK) {
                if (geocacheManager.isGeocache(block)) {
                    player.sendMessage(MESSAGE_ALREADY_CACHE)
                    return
                }
                //make a cache!
                event.isCancelled = true
                if (countItems(chest.inventory) > 0) {
                    player.sendMessage(MESSAGE_CHEST_NOT_EMPTY)
                    return
                }
                //player holding book
                val bookMeta = item.itemMeta as BookMeta
                logger.info("Contents: " + bookMeta.title + "\n" + bookMeta.getPage(1))

                val cache = Geocache.fromBlockPlayerBook(block, player, bookMeta)

                val vector = block.location.toVector()
                player.sendMessage(String.format(MESSAGE_CACHE_PLACED_SS, cache.name, vector.toString()))
                event.player.itemInHand = null
                geocacheManager.saveGeocache(cache)
                logger.info(player.name + " created cache \"" + cache.name + "\", at " + chest.getLocation())

                //FIND A GEOCACHE-----
            } else {
                geocacheManager.getGeocache(block.location)?.let { geocache ->
                    if (!geocache.isOwnedBy(player)) {
                        //not the owner, count as a find.
                        val ftf = geocache.finds.isEmpty()
                        if (geocache.addFinder(player)) {
                            logVerbose(player.name + " found a new cache, \"" + geocache.name + "\", at " + chest.getLocation())
                            player.sendMessage(String.format(MESSAGE_FIND_CONGRATS_SS, geocache.name, geocache.owner))
                            if (ftf)
                            //FTF!
                                player.sendMessage(MESSAGE_FTF_CONGRATS)
                        } else {
                            logVerbose(player.name + " opened cache \"" + geocache.name + "\" at " + chest.getLocation())
                            //already found
                            player.sendMessage(String.format(MESSAGE_ALREADY_LOGGED_S, geocache.name))
                        }
                    } else {
                        player.sendMessage(String.format(MESSAGE_YOU_OWN_CACHE_S, geocache?.name))
                    }
                }
            } //else no cache or book, do nothing
        }
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val block = event.block
        val player = event.player
        val override = player.isOp || player.hasPermission(PERM_REMOVE_CACHES)
        geocacheManager.getGeocache(block.location)?.let { geocache ->
            if (geocache.isOwnedBy(player) || override) {
                event.isCancelled = false
                //owner (or admin) broke it, let them
                val item = ItemStack(Material.WRITABLE_BOOK)
                val bookMeta = item.getItemMeta() as BookMeta
                val name = geocache.name
                val pages = ArrayList<String>(1)
                pages.add(geocache.desc)
                if (!Strings.isNullOrEmpty(geocache.hint)) {
                    geocache.hint?.let { pages.add(it) }
                }
                bookMeta.pages = pages
                item.itemMeta = bookMeta
                val location = event.block.location
                event.block.world.dropItem(location, item)

                logger.info(player.name + " broke cache " + name + " at " + location)
                geocacheManager.removeGeocache(location)
            } else {
                player.sendMessage(MESSAGE_NO_BREAKING)
                event.isCancelled = true
            }
        }
    }

    fun logVerbose(msg: String) {
        if (debug)
            logger.info(msg)
    }

    override fun onDisable() {
        super.onDisable()
        geocacheManager.onStop()
    }

    companion object {

        val MESSAGE_CHEST_NOT_EMPTY = ChatColor.YELLOW.toString() + "Only empty chests can be converted to geocaches"

        val MESSAGE_NO_BREAKING = ChatColor.YELLOW.toString() + "Only the owner can remove this geocache"

        val MESSAGE_ALREADY_CACHE = ChatColor.YELLOW.toString() + "This chest is already a geocache!"

        val MESSAGE_YOU_OWN_CACHE_S = ChatColor.GRAY.toString() + "You own %s"

        val MESSAGE_FTF_CONGRATS = ChatColor.DARK_AQUA.toString() + "Congrats on the FTF!"
        val MESSAGE_FIND_CONGRATS_SS = ChatColor.GREEN.toString() + "You found \"%1\$s\", by %2\$s."
        val MESSAGE_ALREADY_LOGGED_S = ChatColor.GOLD.toString() + "You have already logged \"%s\"!"

        val CONFIG_VERBOSE = "verbose"

        val PERM_REMOVE_CACHES = "geocrafting.caches.remove"
        val PERM_PLACE_CACHES = "geocrafting.caches.place"
        val PERM_FIND_CACHES = "geocrafting.caches.find"
        val PERM_LIST_CACHES = "geocrafting.caches.list"
        val PERM_CHANGE_OWNER = "geocrafting.caches.change_owner"
        val MESSAGE_CACHE_PLACED_SS = "\"%s\" placed at %s"

        fun countItems(inv: Inventory): Int {
            var i = 0
            for (item in inv) {
                if (item != null) {
                    i++
                }
            }
            return i
        }
    }
}
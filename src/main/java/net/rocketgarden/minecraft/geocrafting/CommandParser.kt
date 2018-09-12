package net.rocketgarden.minecraft.geocrafting

import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Parses commands
 */
class CommandParser(private val plugin: Geocrafting, private val geocacheStorage: GeocacheStorage) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (args.isEmpty())
            return false
        val subCommand = args[0]
        when (subCommand) {
            CMD_LIST -> return listCommand(sender, subCommand, args)
            CMD_NEARBY -> return nearbyCommand(sender, subCommand, args)
            CMD_HELP -> {
                sender.sendMessage(MESSAGE_HELP_PLACEMENT)
                return true
            }
        }
        return false
    }

    private fun listCommand(sender: CommandSender, subCommand: String, args: Array<String>): Boolean {
        if (!sender.hasPermission(Geocrafting.PERM_FIND_CACHES)) {
            sender.sendMessage(MESSAGE_NEED_PERMISSION)
            return true
        }

        val stringBuilder = StringBuilder("Caches:\n")
        val geocaches = geocacheStorage.geocaches

        for (cache in geocaches) {
            if (sender !is Player || sender.isOp()) {
                stringBuilder.append(cache.name)
                        .append(": ")
                        .append(cache.getLocation().toVector())
                        .append("\n")
            } else {
                val location = sender.location
                val distSq = location.distanceSquared(cache.getLocation())
                stringBuilder.append(cache.name).append(": ")
                stringBuilder.append(getApproximateDirections(location, cache, distSq))
                stringBuilder.append("\n")
            }
        }

        sender.sendMessage(stringBuilder.toString())
        return true
    }

    private fun nearbyCommand(sender: CommandSender, subCommand: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(MESSAGE_ONLY_PLAYERS)
            return true
        } else if (!sender.hasPermission(Geocrafting.PERM_FIND_CACHES)) {
            sender.sendMessage(MESSAGE_NEED_PERMISSION)
            return true
        }

        val stringBuilder = StringBuilder(MESSAGE_NEARBY_CACHES_PREFIX)
        val geocaches = geocacheStorage.geocaches
        val location = sender.location

        for (cache in geocaches) { //TODO replace this with a K-D tree
            val distSq = location.distanceSquared(cache.getLocation())
            if (distSq < NEARBY_THRESHOLD_SQ) {
                stringBuilder.append(cache.name).append(": ")
                stringBuilder.append(getApproximateDirections(location, cache, distSq))
                stringBuilder.append("\n")
            }
        }

        val directionMessage = stringBuilder.toString()
        sender.sendMessage(directionMessage)
        return true
    }

    private fun getApproximateDirections(location: Location, cache: Geocache, distSq: Double): String {
        //todo make this better; round coords instead of distance
        if (distSq < CLOSE_THRESHOLD_SQ) {
            return MESSAGE_CACHE_CLOSE
        } else {
            val stringBuilder = StringBuilder()
            val v = cache.getLocation().toVector()
            val res = v.subtract(location.toVector())
            val xDist = roundNearestTen(res.x)
            val zDist = roundNearestTen(res.z)

            stringBuilder.append("About ")
                    .append(Math.abs(xDist))
                    .append(if (xDist > 0) " East, " else " West, ")
                    .append(Math.abs(zDist))
                    .append(if (zDist > 0) " South" else " North")
            return stringBuilder.toString()
        }
    }

    private fun roundNearestTen(d: Double): Int {
        return Math.round(d / 10).toInt() * 10
    }

    companion object {

        const val CMD_LABEL_GEOCACHE = "geocache"
        const val CMD_LIST = "list"
        const val CMD_NEARBY = "nearby"
        const val CMD_HELP = "help"
        const val CMD_INFO = "info"
        const val CMD_FIND = "find"
        const val CMD_HINT = "hint"

        const val MESSAGE_NEARBY_CACHES_PREFIX = "Nearby caches:\n"

        const val NEARBY_THRESHOLD_SQ = 65536.0 //256 blocks
        const val CLOSE_THRESHOLD_SQ = 256.0 //16 blocks (~1 chunk)

        const val MESSAGE_NEED_PERMISSION = "You do not have permission for that command"
        const val MESSAGE_ONLY_PLAYERS = "Only players can use this command!"
        const val MESSAGE_CACHE_CLOSE = "Very close, start looking!"

        //coords positive in S & E direction

        val MESSAGE_HELP_PLACEMENT = ChatColor.YELLOW.toString() + "How to make a geocache:\n" +
                "You need: A Book & Quill, a Chest, and a cool spot to place a cache. " +
                "Write your cache description on the first page of the book, and an optional hint on the second. " +
                "Sign the book; the title will be the name of the geocache. " +
                "Place the chest where you want your cache, and right click with the book. " +
                "Don't forget to add some FTF prizes!"
    }
}

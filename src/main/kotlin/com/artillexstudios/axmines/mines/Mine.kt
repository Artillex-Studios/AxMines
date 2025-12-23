package com.artillexstudios.axmines.mines

import com.artillexstudios.axapi.scheduler.Scheduler
import com.artillexstudios.axapi.selection.Cuboid
import com.artillexstudios.axapi.serializers.Serializers
import com.artillexstudios.axapi.utils.ActionBar
import com.artillexstudios.axapi.utils.ItemBuilder
import com.artillexstudios.axapi.utils.StringUtils
import com.artillexstudios.axmines.config.impl.Config
import com.artillexstudios.axmines.config.impl.MineConfig
import com.artillexstudios.axmines.mines.setter.BlockSetter
import com.artillexstudios.axmines.mines.setter.BukkitBlockSetter
import com.artillexstudios.axmines.mines.setter.FastBlockSetter
import com.artillexstudios.axmines.mines.setter.ItemsAdderBukkitBlockSetter
import com.artillexstudios.axmines.mines.setter.ItemsAdderFastBlockSetter
import com.artillexstudios.axmines.mines.setter.OraxenBukkitBlockSetter
import com.artillexstudios.axmines.mines.setter.OraxenFastBlockSetter
import com.artillexstudios.axmines.utils.TimeUtils
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.apache.commons.math3.random.RandomDataGenerator
import org.apache.commons.math3.util.Pair
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Mine(val file: File, reset: Boolean = true) {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(Mine::class.java)
    }

    val name: String = file.nameWithoutExtension
    val config: MineConfig = MineConfig("mines/${file.name}")
    lateinit var cuboid: Cuboid
    private lateinit var placer: BlockSetter
    var volume = 0.0
        private set
    var blocks = 0.0
        private set
    private var actionBarTick = 0L
    var tick = 0L
        private set
    private val rewards = arrayListOf<Reward>()
    private val random = RandomDataGenerator()
    private val running = AtomicBoolean(false)

    init {
        Mines.register(this)
        reload(reset)
    }

    fun tick() {
        tick++
        actionBarTick++

        if (tick >= config.RESET_TICKS) {
            reset()
            tick = 0
        }

        if (config.ACTION_BAR_ENABLED && actionBarTick >= 10) {
            actionBarTick = 0
            val formatted = StringUtils.format(
                config.ACTION_BAR,
                Placeholder.parsed("notbroken", blocks.toString()),
                Placeholder.parsed("total", volume.toString()),
                Placeholder.parsed("percent", String.format("%.2f", (blocks / volume * 100.0))),
                Placeholder.parsed("blocksbroken", String.format("%.2f", (volume - blocks))),
                Placeholder.parsed("time", TimeUtils.format((config.RESET_TICKS - tick) / 20 * 1000, this))
            )
            val bar = ActionBar(formatted)

            val x1 = cuboid.maxX.toDouble()
            val y1 = cuboid.maxY.toDouble()
            val z1 = cuboid.maxZ.toDouble()

            val x2 = cuboid.minX.toDouble()
            val y2 = cuboid.minY.toDouble()
            val z2 = cuboid.minZ.toDouble()

            cuboid.world.players.forEach {
                val location = it.location
                val x = location.x
                val y = location.y
                val z = location.z

                val x0 = min(x1, max(x, x2))
                val y0 = min(y1, max(y, y2))
                val z0 = min(z1, max(z, z2))

                // Get the distance to the closest point on the cuboid
                val distance = (x - x0).pow(2.0) + (y - y0).pow(2.0) + (z - z0).pow(2.0)

                if (Config.DEBUG) {
                    LOGGER.info("Distance to $name is $distance!")
                }

                if (distance <= config.ACTION_BAR_RANGE * config.ACTION_BAR_RANGE) {
                    bar.send(it)
                }
            }
        }
    }

    fun onBlockBreak(player: Player, block: Block, event: BlockBreakEvent) {
        blocks--

        val rewards = randomRewards(block)
        if (rewards.isNotEmpty()) {
            rewards.forEach {
                if (it.preventDrops) {
                    event.isDropItems = false
                }

                it.execute(player)
            }
        }

        val percent = blocks / volume * 100.0
        if (Config.DEBUG) {
            LOGGER.info("A block was broken in mine $name! Current percentage of not broken blocks: $percent blocks: $blocks, volume: $volume")
        }

        if (percent < config.RESET_PERCENT) {
            reset()
        }
    }


    fun reset(silent: Boolean = false) {
        tick = 0
        if (running.get()) return
        running.set(true)

        var placed: Int
        val start = System.currentTimeMillis()
        placer.fill(cuboid) {
            placed = it
            val took = System.currentTimeMillis() - start

            this.blocks = this.volume

            if (Config.DEBUG) {
                LOGGER.info("Reset mine $name and placed $placed blocks in $took milliseconds!")
            }

            Scheduler.get().run { _ ->
                config.RESET_COMMANDS.forEach {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it)
                }

                var broadcast = config.BROADCAST_RESET
                if (silent) {
                    broadcast = -2
                }

                when (broadcast) {
                    0 -> {
                        val formatted = StringUtils.formatToString(
                            config.PREFIX + config.RESET,
                            Placeholder.parsed("mine", config.DISPLAY_NAME)
                        )
                        cuboid.world.players.forEach {
                            it.sendMessage(formatted)
                        }
                    }

                    -1 -> {
                        val formatted = StringUtils.formatToString(
                            config.PREFIX + config.RESET,
                            Placeholder.parsed("mine", config.DISPLAY_NAME)
                        )
                        Bukkit.getOnlinePlayers().forEach {
                            it.sendMessage(formatted)
                        }
                    }

                    -2 -> {}
                    else -> {
                        val formatted = StringUtils.formatToString(
                            config.PREFIX + config.RESET,
                            Placeholder.parsed("mine", config.DISPLAY_NAME)
                        )
                        val x1 = cuboid.maxX.toDouble()
                        val y1 = cuboid.maxY.toDouble()
                        val z1 = cuboid.maxZ.toDouble()

                        val x2 = cuboid.minX.toDouble()
                        val y2 = cuboid.minY.toDouble()
                        val z2 = cuboid.minZ.toDouble()

                        cuboid.world.players.forEach {
                            val location = it.location
                            val x = location.x
                            val y = location.y
                            val z = location.z

                            val x0 = min(x1, max(x, x2))
                            val y0 = min(y1, max(y, y2))
                            val z0 = min(z1, max(z, z2))

                            // Get the distance to the closest point on the cuboid
                            val distance = (x - x0).pow(2.0) + (y - y0).pow(2.0) + (z - z0).pow(2.0)

                            if (Config.DEBUG) {
                                LOGGER.info("Distance to $name is $distance!")
                            }

                            if (distance <= config.BROADCAST_RESET * config.BROADCAST_RESET) {
                                it.sendMessage(formatted)
                            }
                        }
                    }
                }

                when (config.TELEPORT_ON_RESET) {
                    0 -> {
                        cuboid.world.players.forEach {
                            if (cuboid.contains(it.location)) {
                                it.teleport(
                                    cuboid.world.getHighestBlockAt(it.location).location.add(0.0, 1.0, 0.0)
                                )
                            }
                        }
                    }

                    1 -> {
                        val tpLocation = Serializers.LOCATION.deserialize(config.TELEPORT_LOCATION)
                        cuboid.world.players.forEach {
                            if (cuboid.contains(it.location)) {
                                it.teleport(tpLocation)
                            }
                        }
                    }

                    else -> {}
                }
            }

            running.set(false)
        }
    }

    fun randomRewards(block: Block): List<Reward> {
        val list = mutableListOf<Reward>()

        rewards.forEach {
            if (!it.test(block)) return@forEach

            val chance = it.chance / 100

            val random = random.nextUniform(0.0, 1.0)

            if (random <= chance) {
                list.add(it)
            }
        }

        return list
    }

    fun reload(reset: Boolean = true) {
        config.reload()
        val corner1 = Serializers.LOCATION.deserialize(config.SELECTION_CORNER_1)
        val corner2 = Serializers.LOCATION.deserialize(config.SELECTION_CORNER_2)
        if (corner1.world == null) {
            LOGGER.error(
                "The world provided is null! Location: {}. Worlds: {}",
                config.SELECTION_CORNER_1,
                Bukkit.getWorlds().joinToString(", ")
            )
            return
        }

        cuboid = Cuboid(
            corner1.world,
            corner1.blockX,
            corner2.blockX,
            corner1.blockZ,
            corner2.blockZ,
            corner1.blockY,
            corner2.blockY
        )

        if (Config.DEBUG) {
            LOGGER.info("Reloaded config! $corner1 $corner2")
        }

        var oraxen = false
        var itemsAdder = false
        run breaking@{
            config.CONTENTS.forEach { (k, _) ->
                if (k.toString().contains("oraxen", true)) {
                    oraxen = true
                    return@breaking
                } else if (k.toString().contains("itemsadder", true)) {
                    itemsAdder = true
                    return@breaking
                }
            }
        }

        rewards.clear()
        config.RANDOM_REWARDS.forEach {
            val chance = it["chance"]
            if (chance == null || chance !is Number) {
                LOGGER.error("An error occurred while loading reward! No chance set!")
                return@forEach
            }

            val blockTypes: List<String> = (it["blocks"] ?: listOf<String>()) as? List<String> ?: return@forEach
            val blocks = mutableListOf<Material>()
            blockTypes.forEach blocks@{ type ->
                val material = Material.matchMaterial(type.uppercase(Locale.ENGLISH)) ?: return@blocks
                blocks.add(material)
            }

            val items: List<HashMap<Any, Any>> =
                (it["items"] ?: listOf<HashMap<Any, Any>>()) as? List<HashMap<Any, Any>> ?: return@forEach
            val itemStacks = mutableListOf<ItemStack>()
            items.forEach { map ->
                itemStacks.add(ItemBuilder.create(map).get())
            }

            val commands: List<String> = (it["commands"] ?: listOf<String>()) as? List<String> ?: return@forEach
            if (Config.DEBUG) {
                LOGGER.info("Added new reward!")
            }

            val preventDrops = it["prevent-drops"] as? Boolean ?: false
            rewards.add(Reward(chance.toDouble(), commands, itemStacks, blocks, preventDrops))
        }

        if (oraxen) {
            val list = ArrayList<Pair<String, Double>>(config.CONTENTS.size)

            config.CONTENTS.forEach { (k, v) ->
                list.add(
                    Pair.create(
                        k.toString(),
                        (v as? Number)?.toDouble() ?: return@forEach
                    )
                )
            }

            if (list.isEmpty()) {
                LOGGER.error("No blocks set up!")
                return
            }

            val distribution = EnumeratedDistribution(list)

            placer = when (config.SETTER.lowercase(Locale.ENGLISH)) {
                "parallel" -> OraxenFastBlockSetter(cuboid.world, distribution)
                "fast" -> OraxenFastBlockSetter(cuboid.world, distribution)
                else -> OraxenBukkitBlockSetter(cuboid.world, distribution)
            }
        } else if (itemsAdder) {
            val list = ArrayList<Pair<String, Double>>(config.CONTENTS.size)

            config.CONTENTS.forEach { (k, v) ->
                list.add(
                    Pair.create(
                        k.toString(),
                        (v as? Number)?.toDouble() ?: return@forEach
                    )
                )
            }

            if (list.isEmpty()) {
                LOGGER.error("No blocks set up!")
                return
            }

            val distribution = EnumeratedDistribution(list)

            placer = when (config.SETTER.lowercase(Locale.ENGLISH)) {
                "parallel", "fast" -> ItemsAdderFastBlockSetter(cuboid.world, distribution)
                else -> ItemsAdderBukkitBlockSetter(cuboid.world, distribution)
            }
        } else {
            val list = ArrayList<Pair<BlockData, Double>>(config.CONTENTS.size)

            config.CONTENTS.forEach { (k, v) ->
                list.add(
                    Pair.create(
                        Material.matchMaterial(k.toString().uppercase(Locale.ENGLISH))?.createBlockData()
                            ?: return@forEach,
                        (v as? Number)?.toDouble() ?: return@forEach
                    )
                )
            }

            if (list.isEmpty()) {
                LOGGER.error("No blocks set up!")
                return
            }

            val distribution = EnumeratedDistribution(list)

            placer = when (config.SETTER.lowercase(Locale.ENGLISH)) {
                "parallel", "fast" -> FastBlockSetter(cuboid.world, distribution)
                else -> BukkitBlockSetter(cuboid.world, distribution)
            }
        }


        // Calculate total block amount in the area
        val width = abs(cuboid.maxX - cuboid.minX) + 1
        val length = abs(cuboid.maxZ - cuboid.minZ) + 1
        val height = abs(cuboid.maxY - cuboid.minY) + 1

        this.volume = (width * length * height).toDouble()
        if (Config.DEBUG) {
            LOGGER.info("Mine volume: $volume. Width: $width, length: $length, height: $height")
        }
        this.blocks = this.volume

        if (reset) {
            reset()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Mine) return false

        if (file != other.file) return false

        return true
    }

    override fun hashCode(): Int {
        return file.hashCode()
    }

    @JvmRecord
    data class Reward(
        val chance: Double,
        val commands: List<String>,
        val items: List<ItemStack>,
        val blocks: List<Material>,
        val preventDrops: Boolean
    ) {

        fun execute(player: Player) {
            commands.forEach {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), it.replace("<player>", player.name))
            }

            items.forEach {
                player.inventory.addItem(it)
            }
        }

        fun test(block: Block): Boolean {
            return blocks.isEmpty() || block.type in blocks
        }
    }
}

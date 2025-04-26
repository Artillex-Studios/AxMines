package com.artillexstudios.axmines.listener

import com.artillexstudios.axapi.serializers.Serializers
import com.artillexstudios.axmines.mines.Mines
import com.artillexstudios.axmines.selection.SelectionWand
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent

class BlockListener : Listener {

    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        Mines.getTypes().forEach { (_, mine) ->
            if (mine.cuboid.contains(event.player.location)) {
                when (mine.config.TELEPORT_ON_RESET) {
                    0 -> {
                        event.player.teleport(
                            mine.cuboid.world.getHighestBlockAt(event.player.location).location.add(0.0, 1.0, 0.0)
                        )
                    }

                    1 -> {
                        event.player.teleport(Serializers.LOCATION.deserialize(mine.config.TELEPORT_LOCATION))
                    }

                    else -> {}
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onBlockBreakEvent(event: BlockBreakEvent) {
        Mines.getTypes().forEach { (_, mine) ->
            if (mine.cuboid.contains(event.block.location)) {
                mine.onBlockBreak(event.player, event.block, event)
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onPlayerInteractEvent(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.LEFT_CLICK_BLOCK) return
        if (event.clickedBlock == null) return
        if (event.item == null || event.item?.type?.isAir ?: return) return
        if (!SelectionWand.isWand(event.item ?: return)) return

        when (event.action) {
            Action.LEFT_CLICK_BLOCK -> {
                SelectionWand.select(event.player, event.clickedBlock?.location ?: return, true)
                event.isCancelled = true
            }

            Action.RIGHT_CLICK_BLOCK -> {
                SelectionWand.select(event.player, event.clickedBlock?.location ?: return, false)
                event.isCancelled = true
            }

            else -> {}
        }
    }
}
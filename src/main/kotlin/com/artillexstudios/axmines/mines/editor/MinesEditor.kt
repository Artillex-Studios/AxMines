package com.artillexstudios.axmines.mines.editor

import com.artillexstudios.axapi.utils.ItemBuilder
import com.artillexstudios.axmines.mines.Mines
import dev.triumphteam.gui.components.util.GuiFiller
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class MinesEditor(private val player: Player) {

    fun open() {
        val gui = Gui.paginated()
            .disableAllInteractions()
            .title(MiniMessage.miniMessage().deserialize("<color:#00AAFF>Mines editor"))
            .pageSize(21)
            .rows(5)
            .create()

        GuiFiller(gui).fillBorder(GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)))

        gui.setItem(38, GuiItem(ItemBuilder.create(Material.ARROW).setName("<color:#00AAFF>Previous page").get()) {
            gui.previous()
        })

        gui.setItem(42, GuiItem(ItemBuilder.create(Material.ARROW).setName("<color:#00AAFF>Next page").get()) {
            gui.next()
        })

        Mines.getTypes().forEach { (_, mine) ->
            val item = ItemBuilder.create(
                mapOf(
                    Pair("material", "golden_pickaxe"),
                    Pair("name", mine.config.DISPLAY_NAME),
                    Pair("lore", listOf("", "<color:#00AAFF>Click here to edit this mine!"))
                )).get()

            gui.addItem(GuiItem(item) {
                val editor = MineEditor(mine, player)
                editor.open()
            })
        }

        gui.open(player)
    }
}
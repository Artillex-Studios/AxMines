package com.artillexstudios.axmines.mines.editor

import com.artillexstudios.axapi.utils.ItemBuilder
import com.artillexstudios.axmines.mines.Mine
import dev.triumphteam.gui.components.util.GuiFiller
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import java.util.Locale
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class MineContentsEditor(val mine: Mine, val player: Player) {

    fun open() {
        val gui = Gui.paginated()
            .disableAllInteractions()
            .title(MiniMessage.miniMessage().deserialize("<color:#00AAFF>Content editor"))
            .pageSize(21)
            .rows(5)
            .create()

        GuiFiller(gui).fillBorder(GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)))

        gui.setItem(36, GuiItem(ItemBuilder.create(Material.TIPPED_ARROW).setName("<color:#00AAFF>Go back").get()) {
            MineEditor(mine, player).open()
        })

        gui.setItem(38, GuiItem(ItemBuilder.create(Material.ARROW).setName("<color:#00AAFF>Previous page").get()) {
            gui.previous()
        })

        gui.setItem(42, GuiItem(ItemBuilder.create(Material.ARROW).setName("<color:#00AAFF>Next page").get()) {
            gui.next()
        })

        gui.setPlayerInventoryAction { event ->
            mine.config.CONTENTS[event.currentItem?.type?.name?.lowercase(Locale.ENGLISH)] = 10
            mine.config.config.set("contents", mine.config.CONTENTS)
            open()
        }

        mine.config.CONTENTS.forEach { (type, chance) ->
            val item = ItemBuilder.create(
                mapOf(
                    Pair("material", type), Pair(
                        "lore", listOf(
                            "", "<color:#DDDDDD>> <color:#00AAFF>Chance: <white><chance>",
                            "",
                            "<gray>> <color:#00AAFF>Chances don't have to add up to 100%!",
                            "<gray>> <color:#00AAFF>Chances are relative to eachother, so you don't have",
                            "  <color:#00AAFF>to worry about chances being too high or too low!",
                            "",
                            "<color:#00FF00>Left click to increase! (Shift for +10)",
                            "<color:#FF0000>Right click to decrease! (Shift for -10)",
                            "<#DD0000>Drop to remove!"
                        )
                    )
                ), Placeholder.unparsed("chance", chance.toString())
            ).get()

            gui.addItem(GuiItem(item) { event ->
                if (event.click == ClickType.DROP) {
                    mine.config.CONTENTS.remove(type)
                } else if (event.isLeftClick) {
                    if (event.isShiftClick) {
                        mine.config.CONTENTS[type] = (chance as Number).toDouble() + 10.0
                    } else {
                        mine.config.CONTENTS[type] = (chance as Number).toDouble() + 1.0
                    }
                } else if (event.isRightClick) {
                    if (event.isShiftClick) {
                        mine.config.CONTENTS[type] = (chance as Number).toDouble() - 10.0
                    } else {
                        mine.config.CONTENTS[type] = (chance as Number).toDouble() - 1.0
                    }
                }

                mine.config.config.set("contents", mine.config.CONTENTS)
                open()
            })
        }

        gui.setCloseGuiAction {
            mine.config.config.save()
            mine.reload(false)
        }

        gui.open(player)
    }
}
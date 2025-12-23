package com.artillexstudios.axmines.mines.editor

import com.artillexstudios.axapi.utils.ItemBuilder
import com.artillexstudios.axapi.utils.StringUtils
import com.artillexstudios.axmines.AxMinesPlugin
import com.artillexstudios.axmines.mines.Mine
import com.artillexstudios.axmines.utils.TimeUtils
import dev.triumphteam.gui.components.util.GuiFiller
import dev.triumphteam.gui.guis.Gui
import dev.triumphteam.gui.guis.GuiItem
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.conversations.ConversationContext
import org.bukkit.conversations.ConversationFactory
import org.bukkit.conversations.Prompt
import org.bukkit.conversations.StringPrompt
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class MineEditor(val mine: Mine, val player: Player) {

    fun open() {
        val gui = Gui.gui()
            .disableAllInteractions()
            .title(MiniMessage.miniMessage().deserialize("<color:#00AAFF>Mines editor"))
            .rows(5)
            .create()

        GuiFiller(gui).fillBorder(GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)))

        gui.setItem(36, GuiItem(ItemBuilder.create(Material.TIPPED_ARROW).setName("<color:#00AAFF>Go back").get()) {
            MinesEditor(player).open()
        })

        val displayName = ItemBuilder.create(ItemStack(Material.NAME_TAG))
            .setName("<color:#00AAFF><bold>Display name")
            .setLore(
                listOf("", "<gray>> <color:#00AAFF>Current name: <white><name>", "", "<color:#00AAFF>Click to edit!"),
                Placeholder.parsed("name", mine.config.DISPLAY_NAME)
            )
            .get()

        val contents = ItemBuilder.create(ItemStack(Material.EMERALD_BLOCK))
            .setName("<color:#00AAFF><bold>Contents")
            .setLore(listOf("", "<color:#00AAFF>Click to edit!"))
            .get()

        val teleportOnReset = ItemBuilder.create(ItemStack(Material.ENDER_PEARL))
            .setName("<color:#00AAFF><bold>Teleport on reset")
            .setLore(
                listOf(
                    "",
                    "<gray>> <color:#00AAFF>Current value: <white><value>",
                    "",
                    " <gray>- <white>0: <color:#00AAFF>Top block at player's location",
                    " <gray>- <white>1: <color:#00AAFF>Teleport location",
                    " <gray>- <white>Other: <color:#00AAFF>No teleport",
                    "",
                    "<color:#00FF00>Left click to increase!",
                    "<color:#FF0000>Right click to decrease!"
                ), Placeholder.unparsed("value", mine.config.TELEPORT_ON_RESET.toString())
            )
            .get()

        val actionBarEnabled = ItemBuilder.create(ItemStack(Material.STONE_BUTTON))
            .setName("<color:#00AAFF><bold>Action bar enabled")
            .setLore(
                listOf(
                    "",
                    "<gray>> <color:#00AAFF>Current value: <white><value>",
                    "",
                    "<color:#00AAFF>Click to toggle!"
                ), Placeholder.unparsed("value", mine.config.ACTION_BAR_ENABLED.toString())
            )
            .get()

        val actionBarRange = ItemBuilder.create(ItemStack(Material.LEAD))
            .setName("<color:#00AAFF><bold>Action bar range")
            .setLore(
                listOf(
                    "",
                    "<gray>> <color:#00AAFF>Current value: <white><value>",
                    "",
                    "<color:#00FF00>Left click to increase! (Shift for +10)",
                    "<color:#FF0000>Right click to decrease! (Shift for -10)"
                ), Placeholder.unparsed("value", mine.config.ACTION_BAR_RANGE.toString())
            )
            .get()

        val broadcastReset = ItemBuilder.create(ItemStack(Material.OAK_BUTTON))
            .setName("<color:#00AAFF><bold>Broadcast reset")
            .setLore(
                listOf(
                    "",
                    "<gray>> <color:#00AAFF>Current value: <white><value>",
                    "",
                    " <gray>- <white>0: <color:#00AAFF>Whole world",
                    " <gray>- <white>-1: <color:#00AAFF>All worlds",
                    " <gray>- <white>-2: <color:#00AAFF>Silent",
                    " <gray>- <white>< -2: <color:#00AAFF>Same as not negative",
                    " <gray>- <white>>= 1: <color:#00AAFF>broadcast in range",
                    "",
                    "<color:#00FF00>Left click to increase! (Shift for +10)",
                    "<color:#FF0000>Right click to decrease! (Shift for -10)"
                ), Placeholder.unparsed("value", mine.config.BROADCAST_RESET.toString())
            )
            .get()

        val resetTicks = ItemBuilder.create(ItemStack(Material.CHEST))
            .setName("<color:#00AAFF><bold>Reset ticks")
            .setLore(
                listOf(
                    "",
                    "<gray>> <color:#00AAFF>Current value: <white><value> ticks <gray>(<time> formatted)",
                    "",
                    "<gray>20 ticks = 1 second",
                    "",
                    "<color:#00AAFF>Click to edit"
                ), Placeholder.unparsed("value", mine.config.RESET_TICKS.toString()),
                Placeholder.unparsed("time", TimeUtils.format(mine.config.RESET_TICKS / 20 * 1000, mine))
            )
            .get()

        val resetPercent = ItemBuilder.create(ItemStack(Material.ANVIL))
            .setName("<color:#00AAFF><bold>Reset percent")
            .setLore(
                listOf(
                    "",
                    "<gray>> <color:#00AAFF>Current value: <white><value>%",
                    "",
                    "<color:#00FF00>Left click to increase! (Shift for +10)",
                    "<color:#FF0000>Right click to decrease! (Shift for -10)"
                ), Placeholder.unparsed("value", mine.config.RESET_PERCENT.toString())
            )
            .get()

        val rewards = ItemBuilder.create(ItemStack(Material.CHEST))
            .setName("<color:#00AAFF><bold>Rewards")
            .setLore(
                listOf(
                    "",
                    "<color:#00AAFF>Click to edit!",
                )
            )
            .get()

        gui.addItem(GuiItem(displayName) { event ->
            event.whoClicked.closeInventory()

            val factory = ConversationFactory(AxMinesPlugin.INSTANCE)
            val prompt = object : StringPrompt() {
                override fun getPromptText(context: ConversationContext): String {
                    return StringUtils.formatToString("<color:#00FF00>Please type the new displayname of this mine!")
                }

                override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
                    if (input == null) return END_OF_CONVERSATION

                    mine.config.DISPLAY_NAME = input
                    mine.config.config.set("display-name", mine.config.DISPLAY_NAME)
                    open()
                    return END_OF_CONVERSATION
                }
            }

            factory.withFirstPrompt(prompt)
            factory.buildConversation(event.whoClicked as Player).begin()
        })

        gui.addItem(GuiItem(contents) { _ ->
            MineContentsEditor(mine, player).open()
        })

        gui.addItem(GuiItem(teleportOnReset) { event ->
            if (event.isLeftClick) {
                mine.config.TELEPORT_ON_RESET++
            } else {
                mine.config.TELEPORT_ON_RESET--
            }

            mine.config.config.set("teleport-on-reset", mine.config.TELEPORT_ON_RESET)
            open()
        })

        gui.addItem(GuiItem(actionBarEnabled) { _ ->
            mine.config.ACTION_BAR_ENABLED = !mine.config.ACTION_BAR_ENABLED
            mine.config.config.set("actionbar.enabled", mine.config.ACTION_BAR_ENABLED)

            open()
        })

        gui.addItem(GuiItem(actionBarRange) { event ->
            if (event.isLeftClick) {
                if (event.isShiftClick) {
                    mine.config.ACTION_BAR_RANGE += 10
                } else {
                    mine.config.ACTION_BAR_RANGE++
                }
            } else {
                if (event.isShiftClick) {
                    mine.config.ACTION_BAR_RANGE -= 10
                } else {
                    mine.config.ACTION_BAR_RANGE--
                }
            }

            mine.config.config.set("actionbar.range", mine.config.ACTION_BAR_RANGE)

            open()
        })

        gui.addItem(GuiItem(broadcastReset) { event ->
            if (event.isLeftClick) {
                if (event.isShiftClick) {
                    mine.config.BROADCAST_RESET += 10
                } else {
                    mine.config.BROADCAST_RESET++
                }
            } else {
                if (event.isShiftClick) {
                    mine.config.BROADCAST_RESET -= 10
                } else {
                    mine.config.BROADCAST_RESET--
                }
            }

            mine.config.config.set("broadcast-reset", mine.config.BROADCAST_RESET)

            open()
        })

        gui.addItem(GuiItem(resetTicks) { event ->
            event.whoClicked.closeInventory()

            val factory = ConversationFactory(AxMinesPlugin.INSTANCE)
            val prompt = object : StringPrompt() {
                override fun getPromptText(context: ConversationContext): String {
                    return StringUtils.formatToString("<color:#00FF00>Please type how often the mine should reset in ticks! (1 second = 20 ticks)")
                }

                override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
                    if (input == null) return END_OF_CONVERSATION

                    mine.config.RESET_TICKS = input.toLong()
                    mine.config.config.set("reset.ticks", mine.config.RESET_TICKS)
                    open()
                    return END_OF_CONVERSATION
                }
            }

            factory.withFirstPrompt(prompt)
            factory.buildConversation(event.whoClicked as Player).begin()
        })

        gui.addItem(GuiItem(resetPercent) { event ->
            if (event.isLeftClick) {
                if (event.isShiftClick) {
                    mine.config.RESET_PERCENT += 10
                } else {
                    mine.config.RESET_PERCENT++
                }
            } else {
                if (event.isShiftClick) {
                    mine.config.RESET_PERCENT -= 10
                } else {
                    mine.config.RESET_PERCENT--
                }
            }

            mine.config.config.set("reset.percent", mine.config.RESET_PERCENT)

            open()
        })

        gui.addItem(GuiItem(rewards) {
            MineRewardsEditor(mine, player).open()
        })

        gui.setCloseGuiAction {
            mine.config.config.save()
            mine.reload(false)
        }

        gui.open(player)
    }
}
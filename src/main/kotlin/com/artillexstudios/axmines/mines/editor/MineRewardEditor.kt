package com.artillexstudios.axmines.mines.editor

import com.artillexstudios.axapi.utils.ItemBuilder
import com.artillexstudios.axapi.utils.StringUtils
import com.artillexstudios.axmines.AxMinesPlugin
import com.artillexstudios.axmines.mines.Mine
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
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class MineRewardEditor(val mine: Mine, val player: Player, val map: MutableMap<String, Any>) {

    fun open() {
        val gui = Gui.gui()
            .disableAllInteractions()
            .title(MiniMessage.miniMessage().deserialize("<color:#00AAFF>Reward editor"))
            .rows(5)
            .create()

        GuiFiller(gui).fillBorder(GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)))

        gui.setItem(36, GuiItem(ItemBuilder.create(Material.TIPPED_ARROW).setName("<gray>Go back").get()) {
            MineRewardsEditor(mine, player).open()
        })

        gui.addItem(GuiItem(
            ItemBuilder.create(Material.GOLD_BLOCK).setName("<color:#00AAFF>Chance").setLore(
                listOf(
                    "",
                    "<gray>> <color:#00AAFF>Current chance: <white><chance>",
                    "",
                    "<color:#00FF00>Left click to increase! (Shift for +10)",
                    "<color:#FF0000>Right click to decrease! (Shift for -10)"
                ), Placeholder.unparsed("chance", map.getOrDefault("chance", 10.0).toString())
            ).get()
        ) { event ->
            mine.config.RANDOM_REWARDS.remove(map)
            if (event.isLeftClick) {
                if (event.isShiftClick) {
                    map["chance"] = (map.getOrDefault("chance", 10.0) as Number).toDouble() + 10.0
                } else {
                    map["chance"] = (map.getOrDefault("chance", 10.0) as Number).toDouble() + 1.0
                }
            } else if (event.isRightClick) {
                if (event.isShiftClick) {
                    map["chance"] = (map.getOrDefault("chance", 10.0) as Number).toDouble() - 10.0
                } else {
                    map["chance"] = (map.getOrDefault("chance", 10.0) as Number).toDouble() - 1.0
                }
            }


            mine.config.RANDOM_REWARDS.add(map)
            mine.config.config.set("random-rewards", mine.config.RANDOM_REWARDS)
            mine.config.config.save()
            mine.reload(false)

            open()
        })

        gui.addItem(GuiItem(
            ItemBuilder.create(Material.ANVIL).setName("<color:#00AAFF>Commands").setLore(
                listOf(
                    "",
                    "<gray>> The commands that are ran when this reward is given.",
                    "",
                    "<color:#00AAFF>Click to edit!",
                )
            ).get()
        ) {
            Commands(mine, player, map).open()
        })

        gui.addItem(GuiItem(
            ItemBuilder.create(Material.IRON_HOE).setName("<color:#00AAFF>Items").setLore(
                listOf(
                    "",
                    "<gray>> The items that are given when this reward is given.",
                    "",
                    "<color:#00AAFF>Click to edit!",
                )
            ).get()
        ) {
            Items(mine, player, map).open()
        })

        gui.addItem(GuiItem(
            ItemBuilder.create(Material.STONE).setName("<color:#00AAFF>Blocks").setLore(
                listOf(
                    "",
                    "<gray>> The blocks that this reward triggers on.",
                    "<color:#FF0000>No block means it triggers on any blocks!",
                    "",
                    "<color:#00AAFF>Click to edit!",
                )
            ).get()
        ) {
            Blocks(mine, player, map).open()
        })

        gui.open(player)
    }

    class Commands(val mine: Mine, val player: Player, val map: MutableMap<String, Any>) {

        fun open() {
            val gui = Gui.paginated()
                .disableAllInteractions()
                .title(MiniMessage.miniMessage().deserialize("<color:#00AAFF>Commands editor"))
                .pageSize(21)
                .rows(5)
                .create()

            GuiFiller(gui).fillBorder(GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)))

            gui.setItem(36, GuiItem(ItemBuilder.create(Material.TIPPED_ARROW).setName("<color:#00AAFF>Go back").get()) {
                MineRewardEditor(mine, player, map).open()
            })

            gui.setItem(38, GuiItem(ItemBuilder.create(Material.ARROW).setName("<color:#00AAFF>Previous page").get()) {
                gui.previous()
            })

            gui.setItem(42, GuiItem(ItemBuilder.create(Material.ARROW).setName("<color:#00AAFF>Next page").get()) {
                gui.next()
            })


            (map["commands"] as? MutableList<String>)?.forEach {
                gui.addItem(
                    GuiItem(
                        ItemBuilder.create(Material.PAPER).setName(it).setLore(listOf("", "<color:#FF0000>Drop to remove!")).get()
                    ) { event ->
                        if (event.click == ClickType.DROP) {
                            mine.config.RANDOM_REWARDS.remove(map)
                            (map["commands"] as MutableList<String>).remove(it)
                            if ((map["commands"] as MutableList<String>).isEmpty()) {
                                map.remove("commands")
                            }

                            mine.config.RANDOM_REWARDS.add(map)
                            mine.config.config.set("random-rewards", mine.config.RANDOM_REWARDS)
                            mine.config.config.save()
                            mine.reload(false)

                            open()
                        }
                    })
            }

            gui.setItem(40, GuiItem(ItemBuilder.create(Material.PAPER).setName("<gray>Add new command").get()) { event ->
                val commands = map.getOrDefault("commands", ArrayList<String>()) as MutableList<String>

                event.whoClicked.closeInventory()

                val factory = ConversationFactory(AxMinesPlugin.INSTANCE)
                val prompt = object : StringPrompt() {

                    override fun getPromptText(context: ConversationContext): String {
                        return StringUtils.formatToString("<color:#00FF00>Please type the command you'd like to add! You can use the <white><player></white> placeholder here!")
                    }

                    override fun acceptInput(context: ConversationContext, input: String?): Prompt? {
                        if (input == null) return END_OF_CONVERSATION
                        commands.add(input)

                        mine.config.RANDOM_REWARDS.remove(map)
                        map["commands"] = commands
                        mine.config.RANDOM_REWARDS.add(map)
                        mine.config.config.set("random-rewards", mine.config.RANDOM_REWARDS)
                        mine.config.config.save()
                        mine.reload(false)
                        open()
                        return END_OF_CONVERSATION
                    }
                }

                factory.withFirstPrompt(prompt)
                factory.buildConversation(event.whoClicked as Player).begin()
            })

            gui.open(player)
        }
    }

    class Items(val mine: Mine, val player: Player, val map: MutableMap<String, Any>) {

        fun open() {
            val gui = Gui.paginated()
                .disableAllInteractions()
                .title(MiniMessage.miniMessage().deserialize("<color:#FF0000>Items editor"))
                .pageSize(21)
                .rows(5)
                .create()

            GuiFiller(gui).fillBorder(GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)))

            gui.setItem(36, GuiItem(ItemBuilder.create(Material.TIPPED_ARROW).setName("<color:#00AAFF>Go back").get()) {
                MineRewardEditor(mine, player, map).open()
            })

            gui.setItem(38, GuiItem(ItemBuilder.create(Material.ARROW).setName("<color:#00AAFF>Previous page").get()) {
                gui.previous()
            })

            gui.setItem(42, GuiItem(ItemBuilder.create(Material.ARROW).setName("<color:#00AAFF>Next page").get()) {
                gui.next()
            })

            (map["items"] as? MutableList<MutableMap<Any, Any>>)?.forEach {
                gui.addItem(GuiItem(ItemBuilder.create(it).get()) { event ->
                    if (event.click == ClickType.DROP) {
                        mine.config.RANDOM_REWARDS.remove(map)
                        (map["items"] as MutableList<MutableMap<Any, Any>>).remove(it)
                        if ((map["items"] as MutableList<MutableMap<Any, Any>>).isEmpty()) {
                            map.remove("items")
                        }

                        mine.config.RANDOM_REWARDS.add(map)
                        mine.config.config.set("random-rewards", mine.config.RANDOM_REWARDS)
                        mine.config.config.save()
                        mine.reload(false)

                        open()
                    }
                })
            }

            gui.setPlayerInventoryAction { event ->
                mine.config.RANDOM_REWARDS.remove(map)
                val items =
                    map.getOrDefault("items", ArrayList<MutableMap<Any, Any>>()) as ArrayList<MutableMap<Any, Any>>
                items.add(ItemBuilder.create(event.currentItem ?: return@setPlayerInventoryAction).serialize(true))
                
                map["items"] = items
                mine.config.RANDOM_REWARDS.add(map)
                mine.config.config.set("random-rewards", mine.config.RANDOM_REWARDS)
                mine.config.config.save()
                mine.reload(false)
                open()
            }

            gui.open(player)
        }
    }

    class Blocks(val mine: Mine, val player: Player, val map: MutableMap<String, Any>) {

        fun open() {
            val gui = Gui.paginated()
                .disableAllInteractions()
                .title(MiniMessage.miniMessage().deserialize("<color:#FF0000>Blocks editor"))
                .pageSize(21)
                .rows(5)
                .create()

            GuiFiller(gui).fillBorder(GuiItem(ItemStack(Material.GRAY_STAINED_GLASS_PANE)))

            gui.setItem(36, GuiItem(ItemBuilder.create(Material.TIPPED_ARROW).setName("<gray>Go back").get()) {
                MineRewardEditor(mine, player, map).open()
            })

            gui.setItem(38, GuiItem(ItemBuilder.create(Material.ARROW).setName("<gray>Previous page").get()) {
                gui.previous()
            })

            gui.setItem(42, GuiItem(ItemBuilder.create(Material.ARROW).setName("<gray>Next page").get()) {
                gui.next()
            })

            (map["blocks"] as? MutableList<String>)?.forEach {
                gui.addItem(
                    GuiItem(
                        ItemBuilder.create(
                            mapOf(
                                Pair("material", it),
                                Pair("lore", listOf("", "<color:#FF0000>Drop to remove!"))
                            )
                        ).get()
                    ) { event ->
                        if (event.click == ClickType.DROP) {
                            mine.config.RANDOM_REWARDS.remove(map)
                            (map["blocks"] as MutableList<String>).remove(it)
                            if ((map["blocks"] as MutableList<String>).isEmpty()) {
                                map.remove("blocks")
                            }

                            mine.config.RANDOM_REWARDS.add(map)
                            mine.config.config.set("random-rewards", mine.config.RANDOM_REWARDS)
                            mine.config.config.save()
                            mine.reload(false)

                            open()
                        }
                    })
            }

            gui.setPlayerInventoryAction { event ->
                mine.config.RANDOM_REWARDS.remove(map)
                val items = map.getOrDefault("blocks", ArrayList<MutableMap<Any, Any>>()) as ArrayList<String>
                items.add(event.currentItem?.type?.name ?: return@setPlayerInventoryAction)
                
                map["blocks"] = items
                mine.config.RANDOM_REWARDS.add(map)
                mine.config.config.set("random-rewards", mine.config.RANDOM_REWARDS)
                mine.config.config.save()
                mine.reload(false)
                open()
            }

            gui.open(player)
        }
    }
}
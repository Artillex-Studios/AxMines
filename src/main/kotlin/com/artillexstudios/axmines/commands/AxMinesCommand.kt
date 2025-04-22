package com.artillexstudios.axmines.commands

import com.artillexstudios.axapi.libs.boostedyaml.YamlDocument
import com.artillexstudios.axapi.serializers.Serializers
import com.artillexstudios.axapi.utils.PaperUtils
import com.artillexstudios.axapi.utils.StringUtils
import com.artillexstudios.axmines.AxMinesPlugin
import com.artillexstudios.axmines.converters.CataMinesConverter
import com.artillexstudios.axmines.mines.Mine
import com.artillexstudios.axmines.mines.Mines
import com.artillexstudios.axmines.mines.editor.MinesEditor
import com.artillexstudios.axmines.selection.SelectionWand
import com.artillexstudios.axmines.utils.FileUtils
import java.util.Locale
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.DefaultFor
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission

@Command("mines", "mine", "axmines")
class AxMinesCommand {

    @Subcommand("reload")
    @CommandPermission("axmines.command.reload")
    fun reload(sender: CommandSender) {
        sender.sendMessage(
            StringUtils.formatToString(
                AxMinesPlugin.MESSAGES.PREFIX + AxMinesPlugin.MESSAGES.RELOAD,
                Placeholder.unparsed("time", AxMinesPlugin.INSTANCE.reloadWithTime().toString())
            )
        )
    }

    @Subcommand("wand")
    @CommandPermission("axmines.command.wand")
    fun wand(sender: Player) {
        sender.inventory.addItem(SelectionWand.getWand())
    }

    @Subcommand("delete")
    @CommandPermission("axmines.command.delete")
    fun delete(sender: CommandSender, mine: Mine) {
        Mines.unregister(mine)
    }

    @Subcommand("list")
    @CommandPermission("axmines.command.list")
    fun list(sender: CommandSender) {
        val list = mutableListOf<String>()
        Mines.getTypes().forEach { (_, v) ->
            list.add(v.name + " (${v.config.DISPLAY_NAME}<reset>)")
        }

        val mines = list.joinToString()
        AxMinesPlugin.MESSAGES.LIST.forEach { line ->
            sender.sendMessage(StringUtils.formatToString(line, Placeholder.parsed("mines", mines)))
        }
    }

    @Subcommand("redefine")
    @CommandPermission("axmines.command.redefine")
    fun redefine(sender: Player, mine: Mine) {
        val selection = SelectionWand.getSelection(sender)
        if (selection?.position1 == null || selection.position2 == null) {
            sender.sendMessage(StringUtils.formatToString(AxMinesPlugin.MESSAGES.PREFIX + AxMinesPlugin.MESSAGES.NO_SELECTION))
            return
        }

        mine.config.config.set("selection.1", Serializers.LOCATION.serialize(selection.position1))
        mine.config.config.set("selection.2", Serializers.LOCATION.serialize(selection.position2))
        mine.config.config.save()
        val maxY = mine.cuboid.maxY
        mine.reload(false)
        val topLoc = if (selection.position1?.blockY == maxY) selection.position1 else selection.position2
        mine.config.config.set("teleport-location", Serializers.LOCATION.serialize(topLoc))
        mine.config.config.save()
        sender.sendMessage(StringUtils.formatToString(AxMinesPlugin.MESSAGES.PREFIX + AxMinesPlugin.MESSAGES.REDEFINE))
    }

    @Subcommand("teleport")
    @CommandPermission("axmines.command.teleport")
    fun teleport(player: Player, mine: Mine) {
        val tpLoc = Serializers.LOCATION.deserialize(mine.config.TELEPORT_LOCATION) ?: return

        PaperUtils.teleportAsync(player, tpLoc)
        player.sendMessage(StringUtils.formatToString(AxMinesPlugin.MESSAGES.PREFIX + AxMinesPlugin.MESSAGES.TELEPORT))
    }

    @Subcommand("editor")
    @CommandPermission("axmines.command.editor")
    fun editor(player: Player) {
        MinesEditor(player).open()
    }

    @Subcommand("setteleport")
    @CommandPermission("axmines.command.setteleport")
    fun teleportSet(player: Player, mine: Mine) {
        val tpLoc = Serializers.LOCATION.serialize(player.location) ?: return
        mine.config.TELEPORT_LOCATION = tpLoc
        mine.config.config.set("teleport-location", tpLoc)
        mine.config.config.save()
        player.sendMessage(StringUtils.formatToString(AxMinesPlugin.MESSAGES.PREFIX + AxMinesPlugin.MESSAGES.SET_TELEPORT))
    }

    @Subcommand("create")
    @CommandPermission("axmines.command.create")
    fun create(player: Player, name: String) {
        val mine = Mines.getTypes()[name.lowercase(Locale.ENGLISH)]
        if (mine != null) {
            player.sendMessage(StringUtils.formatToString(AxMinesPlugin.MESSAGES.PREFIX + AxMinesPlugin.MESSAGES.ALREADY_EXISTS))
            return
        }

        val selection = SelectionWand.getSelection(player)
        if (selection?.position1 == null || selection.position2 == null) {
            player.sendMessage(StringUtils.formatToString(AxMinesPlugin.MESSAGES.PREFIX + AxMinesPlugin.MESSAGES.NO_SELECTION))
            return
        }

        val file = FileUtils.extractFile(
            AxMinesPlugin::class.java,
            "mines/_example.yml",
            "$name.yml",
            FileUtils.PLUGIN_DIRECTORY.resolve("mines"),
            false
        )
        val config = YamlDocument.create(file)
        config.set("selection.1", Serializers.LOCATION.serialize(selection.position1))
        config.set("selection.2", Serializers.LOCATION.serialize(selection.position2))
        config.set("display-name", "<#FF0000>$name</#FF0000>")
        config.save()
        val createdMine = Mine(file, false)
        val maxY = createdMine.cuboid.maxY
        createdMine.reload(false)
        val topLoc = if (selection.position1?.blockY == maxY) selection.position1 else selection.position2
        createdMine.config.config.set("teleport-location", Serializers.LOCATION.serialize(topLoc))
        createdMine.config.config.save()
        createdMine.reload()
    }

    @Subcommand("reset")
    @CommandPermission("axmines.command.reset")
    fun reset(sender: CommandSender, mine: Mine) {
        mine.reset()
    }

    @Subcommand("convert")
    @CommandPermission("axmines.command.convert")
    fun convert(sender: CommandSender) {
        CataMinesConverter.convertAll()
    }

    @DefaultFor("~", "~ help")
    @CommandPermission("axmines.command.help")
    fun help(sender: CommandSender) {
        AxMinesPlugin.MESSAGES.HELP.forEach {
            sender.sendMessage(StringUtils.formatToString(it))
        }
    }
}
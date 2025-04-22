package com.artillexstudios.axmines.converters

import com.artillexstudios.axapi.libs.boostedyaml.YamlDocument
import com.artillexstudios.axapi.serializers.Serializers
import com.artillexstudios.axmines.AxMinesPlugin
import com.artillexstudios.axmines.mines.Mine
import com.artillexstudios.axmines.utils.FileUtils
import java.io.File
import java.util.Locale
import org.bukkit.Bukkit
import org.bukkit.Location

object CataMinesConverter {

    fun convertAll() {
        val minesDir = FileUtils.PLUGIN_DIRECTORY.resolve("../CataMines/mines/").toFile()
        minesDir.listFiles()?.forEach { file ->
            convert(file)
        }
    }

    private fun convert(file: File) {
        val document = YamlDocument.create(file)
        val name = document.getString("Mine.name")
        val world = document.getString("Mine.region.world")
        val p1x = document.getDouble("Mine.region.p1.x")
        val p1y = document.getDouble("Mine.region.p1.y")
        val p1z = document.getDouble("Mine.region.p1.z")
        val p2x = document.getDouble("Mine.region.p2.x")
        val p2y = document.getDouble("Mine.region.p2.y")
        val p2z = document.getDouble("Mine.region.p2.z")

        val composition = document.getMapList("Mine.composition")
        val resetDelay = document.getInt("Mine.resetDelay")
        val resetPercent = document.getDouble("Mine.resetPercentage")

        val teleportLocX = document.getDouble("Mine.teleportLocation.x")
        val teleportLocY = document.getDouble("Mine.teleportLocation.y")
        val teleportLocZ = document.getDouble("Mine.teleportLocation.z")
        val resetMode = document.getString("Mine.resetMode")

        val loc1 = Location(Bukkit.getWorld(world), p1x, p1y, p1z)
        val loc2 = Location(Bukkit.getWorld(world), p2x, p2y, p2z)

        val file2 = FileUtils.extractFile(AxMinesPlugin::class.java, "mines/_example.yml", "$name.yml", FileUtils.PLUGIN_DIRECTORY.resolve("mines"), false)
        val createdMine = Mine(file2, false)
        createdMine.config.config.set("selection.1", Serializers.LOCATION.serialize(loc1))
        createdMine.config.config.set("selection.2", Serializers.LOCATION.serialize(loc2))
        createdMine.config.config.set("display-name", name)
        createdMine.config.config.set("reset.ticks", if (resetMode.equals("TIME", true) || resetMode.equals("TIME_PERCENTAGE", true)) resetDelay * 20 else Int.MAX_VALUE)
        createdMine.config.config.set("reset.percent", resetPercent)

        val map = HashMap<Any, Any>()
        for (mutableMap in composition) {
            val block = mutableMap["block"].toString().replace("minecraft:", "")
            val chance = mutableMap["chance"]

            map[block.lowercase(Locale.ENGLISH)] = chance.toString().toDouble()
        }
        createdMine.config.config.set("contents", map)
        createdMine.config.config.set("reset-commands", listOf<Any>())
        createdMine.config.config.set("random-rewards", listOf<Any>())

        createdMine.config.config.save()
        val maxY = createdMine.cuboid.maxY
        createdMine.reload(false)

        val topLoc: Location
        if (teleportLocX == null) {
            topLoc = if (loc1.blockY == maxY) loc1 else loc2
        } else {
            topLoc = Location(Bukkit.getWorld(world), teleportLocX, teleportLocY, teleportLocZ)
        }

        createdMine.config.config.set("teleport-location", Serializers.LOCATION.serialize(topLoc))
        createdMine.config.config.save()
        createdMine.reload()
    }
}
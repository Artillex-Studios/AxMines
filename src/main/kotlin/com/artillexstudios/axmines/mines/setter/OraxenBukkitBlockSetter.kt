package com.artillexstudios.axmines.mines.setter

import com.artillexstudios.axapi.selection.Cuboid
import io.th0rgal.oraxen.api.OraxenBlocks
import java.util.Locale
import java.util.function.IntConsumer
import kotlin.math.max
import kotlin.math.min
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World

class OraxenBukkitBlockSetter(world: World, distribution: EnumeratedDistribution<String>) :
    BlockSetter(world, distribution) {

    override fun fill(cuboid: Cuboid, consumer: IntConsumer) {
        var blockCount = 0
        val chunkMinX: Int = cuboid.minX shr 4
        val chunkMaxX: Int = cuboid.maxX shr 4
        val chunkMinZ: Int = cuboid.minZ shr 4
        val chunkMaxZ: Int = cuboid.maxZ shr 4

        for (chunkX in chunkMinX..chunkMaxX) {
            val minX =
                max(cuboid.minX.toDouble(), (chunkX shl 4).toDouble()).toInt()
            val maxX = min(cuboid.maxX.toDouble(), ((chunkX shl 4) + 15).toDouble())
                .toInt()

            for (chunkZ in chunkMinZ..chunkMaxZ) {
                val minZ =
                    max(cuboid.minZ.toDouble(), (chunkZ shl 4).toDouble()).toInt()
                val maxZ = min(cuboid.maxZ.toDouble(), ((chunkZ shl 4) + 15).toDouble())
                    .toInt()

                for (y in cuboid.minY..cuboid.getMaxY()) {
                    for (x in minX..maxX) {
                        for (z in minZ..maxZ) {
                            ++blockCount
                            val sample = distribution.sample() as String
                            if (sample.contains("oraxen:")) {
                                OraxenBlocks.place(
                                    sample.substring("oraxen:".length),
                                    Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                                )
                            } else {
                                OraxenBlocks.remove(Location(world, x.toDouble(), y.toDouble(), z.toDouble()), null)
                                world.setBlockData(
                                    x,
                                    y,
                                    z,
                                    Material.matchMaterial(sample.uppercase(Locale.ENGLISH))?.createBlockData()
                                        ?: continue
                                )
                            }
                        }
                    }
                }
            }
        }

        consumer.accept(blockCount)
    }
}
package com.artillexstudios.axmines.mines.setter

import com.artillexstudios.axapi.nms.wrapper.WrapperRegistry
import com.artillexstudios.axapi.selection.Cuboid
import com.artillexstudios.axapi.selection.ParallelBlockSetter
import java.lang.invoke.MethodHandles
import java.util.function.IntConsumer
import org.apache.commons.math3.distribution.EnumeratedDistribution
import org.bukkit.World
import org.bukkit.block.data.BlockData

class ParallelBlockSetter(world: World, distribution: EnumeratedDistribution<BlockData>) :
    BlockSetter(world, distribution) {
    private val setter = WrapperRegistry.WORLD.map(world).parallelSetter()
    private val invoker = MethodHandles.publicLookup().unreflect(
        ParallelBlockSetter::class.java.getMethod(
            "fill",
            Cuboid::class.java,
            EnumeratedDistribution::class.java,
            IntConsumer::class.java
        )
    ).bindTo(setter)

    override fun fill(cuboid: Cuboid, consumer: IntConsumer) {
        invoker.invoke(cuboid, distribution, consumer)
    }
}
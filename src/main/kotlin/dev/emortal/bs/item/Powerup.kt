package dev.emortal.bs.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.ItemMetaBuilder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.tag.Tag
import world.cepi.kstom.item.item
import java.util.concurrent.ThreadLocalRandom

sealed class Powerup(
    val name: Component,
    id: String,
    material: Material,
    rarity: Rarity,
    val interactType: PowerupInteractType,
    val spawnType: SpawnType,
    itemCreate: (ItemMetaBuilder) -> Unit = { }
) : Item(id, material, rarity, itemCreate) {

    companion object {
        val taskIDTag = Tag.Integer("taskID")

        var Player.heldPowerup: Powerup?
            get() = itemInMainHand.getPowerup
            set(value) {
                if (value == null) {
                    itemInMainHand = ItemStack.AIR
                    return
                }
                itemInMainHand = value.createItemStack()
            }
        val ItemStack.getPowerup: Powerup?
            get() = registeredMap[getTag(itemIdTag)]

        val registeredMap: Map<String, Powerup>
            get() = Powerup::class.sealedSubclasses.mapNotNull { it.objectInstance }.associateBy { it.id }

        fun randomWithRarity(spawnType: SpawnType): Powerup {
            val possiblePowerups =
                registeredMap.values.filter { it.rarity != Rarity.IMPOSSIBLE && it.spawnType == spawnType }
            var totalWeight = 0
            for (powerup in possiblePowerups) {
                totalWeight += powerup.rarity.weight
            }

            var idx = 0

            var r = ThreadLocalRandom.current().nextInt(totalWeight)
            while (idx < possiblePowerups.size - 1) {
                r -= possiblePowerups[idx].rarity.weight
                if (r <= 0.0) break
                ++idx
            }

            return possiblePowerups[idx]
        }
    }

    abstract fun use(player: Player, pos: Pos?, entity: Entity? = null)
    open fun collide(entity: Entity) {

    }

    override fun createItemStack(): ItemStack {
        return item(material) {
            displayName(name.decoration(TextDecoration.ITALIC, false))
            setTag(itemIdTag, id)
            lore(rarity.component.decoration(TextDecoration.ITALIC, false))
            itemCreate.invoke(this)
        }
    }

    fun removeOne(player: Player) {
        val itemInHand = player.inventory.itemInMainHand
        player.inventory.itemInMainHand = itemInHand.withAmount(itemInHand.amount - 1)
    }

}
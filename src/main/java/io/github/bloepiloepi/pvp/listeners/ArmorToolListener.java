package io.github.bloepiloepi.pvp.listeners;

import io.github.bloepiloepi.pvp.enums.ArmorMaterial;
import io.github.bloepiloepi.pvp.enums.Tool;
import net.minestom.server.attribute.Attribute;
import net.minestom.server.attribute.AttributeInstance;
import net.minestom.server.attribute.AttributeModifier;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.item.EntityEquipEvent;
import net.minestom.server.event.player.PlayerChangeHeldSlotEvent;
import net.minestom.server.event.trait.EntityEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.packet.server.play.EntityPropertiesPacket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ArmorToolListener {
	
	public static EventNode<EntityEvent> events() {
		EventNode<EntityEvent> node = EventNode.type("armor-tool-events", EventFilter.ENTITY);
		
		node.addListener(EntityEquipEvent.class, event -> {
			if (!(event.getEntity() instanceof LivingEntity)) return;
			LivingEntity livingEntity = (LivingEntity) event.getEntity();
			
			if (event.getSlot().isArmor()) {
				changeArmorModifiers(livingEntity, event.getSlot(), event.getEquippedItem());
			} else if (event.getSlot().isHand()) {
				changeHandModifiers(livingEntity, event.getSlot(), event.getEquippedItem());
			}
		});
		
		node.addListener(EventListener.builder(PlayerChangeHeldSlotEvent.class).handler(event ->
				changeHandModifiers(event.getPlayer(), EquipmentSlot.MAIN_HAND,
						event.getPlayer().getInventory().getItemStack(event.getSlot())))
				.ignoreCancelled(false).build());
		
		return node;
	}
	
	private static void changeArmorModifiers(LivingEntity entity, EquipmentSlot slot, ItemStack newItem) {
		//Remove previous armor
		ItemStack previousStack = entity.getEquipment(slot);
		ArmorMaterial material = ArmorMaterial.fromMaterial(previousStack.getMaterial());
		if (material != null) {
			removeAttributeModifiers(entity, material.getAttributes(slot, previousStack));
		}
		
		//Add new armor
		material = ArmorMaterial.fromMaterial(newItem.getMaterial());
		if (material != null) {
			addAttributeModifiers(entity, material.getAttributes(slot, newItem));
		}
	}
	
	private static void changeHandModifiers(LivingEntity entity, EquipmentSlot slot, ItemStack newItem) {
		//Remove previous attribute modifiers
		ItemStack previousStack = entity.getEquipment(slot);
		Tool tool = Tool.fromMaterial(previousStack.getMaterial());
		if (tool != null) {
			removeAttributeModifiers(entity, tool.getAttributes(slot, previousStack));
		}
		
		//Add new attribute modifiers
		tool = Tool.fromMaterial(newItem.getMaterial());
		if (tool != null) {
			addAttributeModifiers(entity, tool.getAttributes(slot, newItem));
		}
	}
	
	private static void removeAttributeModifiers(LivingEntity entity, Map<Attribute, AttributeModifier> modifiers) {
		for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entrySet()) {
			AttributeInstance attribute = entity.getAttribute(entry.getKey());
			
			List<AttributeModifier> toRemove = new ArrayList<>();
			attribute.getModifiers().forEach((modifier) -> {
				if (modifier.getId().equals(entry.getValue().getId())) {
					toRemove.add(modifier);
				}
			});
			
			toRemove.forEach(attribute::removeModifier);
		}
	}
	
	private static void addAttributeModifiers(LivingEntity entity, Map<Attribute, AttributeModifier> modifiers) {
		for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entrySet()) {
			AttributeInstance attribute = entity.getAttribute(entry.getKey());
			attribute.addModifier(entry.getValue());
		}
	}
	
	// Exact code from minestom but for a specific set of attributes
	public static @NotNull EntityPropertiesPacket getPropertiesPacket(@NotNull LivingEntity entity,
	                                                                  @NotNull Collection<AttributeInstance> update) {
		// Get all the attributes which should be sent to the client
		final AttributeInstance[] instances = update.stream()
				.filter(i -> i.getAttribute().isShared())
				.toArray(AttributeInstance[]::new);
		
		
		EntityPropertiesPacket propertiesPacket = new EntityPropertiesPacket();
		propertiesPacket.entityId = entity.getEntityId();
		
		EntityPropertiesPacket.Property[] properties = new EntityPropertiesPacket.Property[instances.length];
		for (int i = 0; i < properties.length; ++i) {
			EntityPropertiesPacket.Property property = new EntityPropertiesPacket.Property();
			
			final float value = instances[i].getBaseValue();
			
			property.instance = instances[i];
			property.attribute = instances[i].getAttribute();
			property.value = value;
			
			properties[i] = property;
		}
		
		propertiesPacket.properties = properties;
		return propertiesPacket;
	}
}

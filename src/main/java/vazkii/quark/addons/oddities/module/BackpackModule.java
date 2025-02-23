package vazkii.quark.addons.oddities.module;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.addons.oddities.client.screen.BackpackInventoryScreen;
import vazkii.quark.addons.oddities.inventory.BackpackMenu;
import vazkii.quark.addons.oddities.item.BackpackItem;
import vazkii.quark.base.block.QuarkBlock;
import vazkii.quark.base.item.QuarkItem;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.base.network.QuarkNetwork;
import vazkii.quark.base.network.message.oddities.HandleBackpackMessage;

@LoadModule(category = ModuleCategory.ODDITIES, hasSubscriptions = true)
public class BackpackModule extends QuarkModule {

	@Config(description = "Set this to true to allow the backpacks to be unequipped even with items in them")
	public static boolean superOpMode = false;

	@Config(flag = "ravager_hide")
	public static boolean enableRavagerHide = true;

	@Config
	public static boolean itemsInBackpackTick = true;

	@Config public static int baseRavagerHideDrop = 1;
	@Config public static double extraChancePerLooting = 0.5;

	public static Item backpack;
	public static Item ravager_hide;

	public static Block bonded_ravager_hide;

	public static MenuType<BackpackMenu> menyType;
	private static ItemStack heldStack = null;

	@OnlyIn(Dist.CLIENT)
	private static boolean backpackRequested;

	@Override
	public void register() {
		backpack = new BackpackItem(this);
		ravager_hide = new QuarkItem("ravager_hide", this, new Item.Properties().rarity(Rarity.RARE).tab(CreativeModeTab.TAB_MATERIALS)).setCondition(() -> enableRavagerHide);

		menyType = IForgeMenuType.create(BackpackMenu::fromNetwork);
		RegistryHelper.register(menyType, "backpack");

		bonded_ravager_hide = new QuarkBlock("bonded_ravager_hide", this, CreativeModeTab.TAB_BUILDING_BLOCKS, Block.Properties.of(Material.WOOL, DyeColor.BLACK)
				.strength(1F)
				.sound(SoundType.WOOL))
		.setCondition(() -> enableRavagerHide);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void clientSetup() {
		MenuScreens.register(menyType, BackpackInventoryScreen::new);

		enqueue(() -> ItemProperties.register(backpack, new ResourceLocation("has_items"),
				(stack, world, entity, i) -> (!BackpackModule.superOpMode && BackpackItem.doesBackpackHaveItems(stack)) ? 1 : 0));
	}

	@SubscribeEvent
	public void onDrops(LivingDropsEvent event) {
		LivingEntity entity = event.getEntityLiving();
		if(enableRavagerHide && entity.getType() == EntityType.RAVAGER) {
			int amount = baseRavagerHideDrop;
			double chance = (double) event.getLootingLevel() * extraChancePerLooting;
			while(chance > baseRavagerHideDrop) {
				chance--;
				amount++;
			}
			if(chance > 0 && entity.level.random.nextDouble() < chance)
				amount++;

			event.getDrops().add(new ItemEntity(entity.level, entity.getX(), entity.getY(), entity.getZ(), new ItemStack(ravager_hide, amount)));
		}
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void onOpenGUI(ScreenOpenEvent event) {
		Player player = Minecraft.getInstance().player;
		if(player != null && isInventoryGUI(event.getScreen()) && !player.getAbilities().instabuild && isEntityWearingBackpack(player)) {
			requestBackpack();
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void clientTick(ClientTickEvent event) {
		Minecraft mc = Minecraft.getInstance();
		if(isInventoryGUI(mc.screen) && !backpackRequested && isEntityWearingBackpack(mc.player)) {
			requestBackpack();
			mc.player.inventoryMenu.setCarried(mc.player.getItemBySlot(EquipmentSlot.CHEST));
			backpackRequested = true;
		} else if(mc.screen instanceof BackpackInventoryScreen) {
			if(heldStack != null) {
				mc.player.inventoryMenu.setCarried(heldStack);
				heldStack = null;
			}

			backpackRequested = false;
		}
	}

	private void requestBackpack() {
		heldStack = Minecraft.getInstance().player.inventoryMenu.getCarried();
		QuarkNetwork.sendToServer(new HandleBackpackMessage(true));
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void removeCurseTooltip(ItemTooltipEvent event) {
		if(!superOpMode && event.getItemStack().getItem() instanceof BackpackItem)
			for(Component s : event.getToolTip())
				if(s.getString().equals(Enchantments.BINDING_CURSE.getFullname(1).getString())) {
					event.getToolTip().remove(s);
					return;
				}
	}

	@OnlyIn(Dist.CLIENT)
	private static boolean isInventoryGUI(Screen gui) {
		return gui != null && gui.getClass() == InventoryScreen.class;
	}

	public static boolean isEntityWearingBackpack(Entity e) {
		if(e instanceof LivingEntity living) {
			ItemStack chestArmor = living.getItemBySlot(EquipmentSlot.CHEST);
			return chestArmor.getItem() instanceof BackpackItem;
		}

		return false;
	}

	public static boolean isEntityWearingBackpack(Entity e, ItemStack stack) {
		if(e instanceof LivingEntity living) {
			ItemStack chestArmor = living.getItemBySlot(EquipmentSlot.CHEST);
			return chestArmor == stack;
		}

		return false;
	}

}

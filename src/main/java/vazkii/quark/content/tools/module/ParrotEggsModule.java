package vazkii.quark.content.tools.module;

import net.minecraft.Util;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import vazkii.arl.util.RegistryHelper;
import vazkii.quark.base.Quark;
import vazkii.quark.base.handler.QuarkSounds;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;
import vazkii.quark.content.tools.entity.ParrotEgg;
import vazkii.quark.content.tools.item.ParrotEggItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@LoadModule(category = ModuleCategory.TOOLS, hasSubscriptions = true)
public class ParrotEggsModule extends QuarkModule {

	private static final ResourceLocation KOTO = new ResourceLocation("quark", "textures/model/entity/kotobirb.png");
	private static final String EGG_TIMER = "quark:parrot_egg_timer";

	private static final List<String> NAMES = List.of("red_blue", "blue", "green", "yellow_blue", "grey");

	public static EntityType<ParrotEgg> parrotEggType;

	public static TagKey<Item> feedTag;

	public static List<Item> parrotEggs;

	@Config(description = "The chance feeding a parrot will produce an egg")
	public static double chance = 0.05;
	@Config(description = "How long it takes to create an egg")
	public static int eggTime = 12000;
	@Config(name = "Enable Special Awesome Parrot")
	public static boolean enableKotobirb = true;

	private static boolean isEnabled;

	@Override
	public void register() {
		parrotEggType = EntityType.Builder.<ParrotEgg>of(ParrotEgg::new, MobCategory.MISC)
				.sized(0.4F, 0.4F)
				.clientTrackingRange(64)
				.updateInterval(10) // update interval
				.setCustomClientFactory((spawnEntity, world) -> new ParrotEgg(parrotEggType, world))
				.build("parrot_egg");
		RegistryHelper.register(parrotEggType, "parrot_egg");

		parrotEggs = new ArrayList<>();
		for (int i = 0; i < ParrotEgg.VARIANTS; i++) {
			int variant = i;

			Item parrotEgg = new ParrotEggItem(NAMES.get(variant), variant, this);
			parrotEggs.add(parrotEgg);

			DispenserBlock.registerBehavior(parrotEgg, new AbstractProjectileDispenseBehavior() {
				@Nonnull
				@Override
				protected Projectile getProjectile(@Nonnull Level world, @Nonnull Position pos, @Nonnull ItemStack stack) {
					return Util.make(new ParrotEgg(world, pos.x(), pos.y(), pos.z()), (parrotEgg) -> {
						parrotEgg.setItem(stack);
						parrotEgg.setVariant(variant);
					});
				}
			});

		}
	}

	@Override
	public void setup() {
		feedTag = ItemTags.create(new ResourceLocation(Quark.MOD_ID, "parrot_feed"));
	}

	@Override
	public void configChanged() {
		// Pass over to a static reference for easier computing the coremod hook
		isEnabled = this.enabled;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void clientSetup() {
		EntityRenderers.register(parrotEggType, ThrownItemRenderer::new);
	}

	@Nullable
	@OnlyIn(Dist.CLIENT)
	public static ResourceLocation getTextureForParrot(Parrot parrot) {
		if (!isEnabled || !enableKotobirb)
			return null;

		UUID uuid = parrot.getUUID();
		if (parrot.getVariant() == 4 && uuid.getLeastSignificantBits() % 20 == 0)
			return KOTO;

		return null;
	}

	@SubscribeEvent
	public void entityInteract(PlayerInteractEvent.EntityInteract event) {
		Entity e = event.getTarget();
		if (e instanceof Parrot parrot && e.getPersistentData().getInt(EGG_TIMER) <= 0) {
			if (!parrot.isTame())
				return;

			Player player = event.getPlayer();
			ItemStack stack = player.getMainHandItem();
			if (stack.isEmpty() || !stack.is(feedTag)) {
				stack = player.getOffhandItem();
			}

			if (!stack.isEmpty() && stack.is(feedTag)) {
				event.setCanceled(true);
				if (parrot.level.isClientSide || event.getHand() == InteractionHand.OFF_HAND)
					return;

				if (!player.getAbilities().instabuild)
					stack.shrink(1);

				if (parrot.level instanceof ServerLevel ws) {
					ws.playSound(null, parrot.getX(), parrot.getY(), parrot.getZ(), SoundEvents.PARROT_EAT, SoundSource.NEUTRAL, 1.0F, 1.0F + (ws.random.nextFloat() - ws.random.nextFloat()) * 0.2F);

					if (ws.random.nextDouble() < chance) {
						parrot.getPersistentData().putInt(EGG_TIMER, eggTime);
						ws.sendParticles(ParticleTypes.HAPPY_VILLAGER, parrot.getX(), parrot.getY(), parrot.getZ(), 10, parrot.getBbWidth(), parrot.getBbHeight(), parrot.getBbWidth(), 0);
					} else
						ws.sendParticles(ParticleTypes.SMOKE, parrot.getX(), parrot.getY(), parrot.getZ(), 10, parrot.getBbWidth(), parrot.getBbHeight(), parrot.getBbWidth(), 0);
				}
			}
		}
	}

	@SubscribeEvent
	public void entityUpdate(LivingUpdateEvent event) {
		Entity e = event.getEntity();
		if(e instanceof Parrot parrot) {
			int time = parrot.getPersistentData().getInt(EGG_TIMER);
			if(time > 0) {
				if(time == 1) {
					e.playSound(QuarkSounds.ENTITY_PARROT_EGG, 1.0F, (parrot.level.random.nextFloat() - parrot.level.random.nextFloat()) * 0.2F + 1.0F);
					e.spawnAtLocation(new ItemStack(parrotEggs.get(getResultingEggColor(parrot))), 0);
				}
				e.getPersistentData().putInt(EGG_TIMER, time - 1);
			}
		}
	}

	private int getResultingEggColor(Parrot parrot) {
		int color = parrot.getVariant();
		Random rand = parrot.level.random;
		if(rand.nextBoolean())
			return color;
		return rand.nextInt(ParrotEgg.VARIANTS);
	}
}

package vazkii.quark.content.experimental.module;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import vazkii.quark.base.module.LoadModule;
import vazkii.quark.base.module.ModuleCategory;
import vazkii.quark.base.module.QuarkModule;
import vazkii.quark.base.module.config.Config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@LoadModule(category = ModuleCategory.EXPERIMENTAL, enabledByDefault = false)
public class SpawnerReplacerModule extends QuarkModule {

	@Config(description = "Mobs to be replaced with other mobs.\n" +
			"Format is: \"mob1,mob2\", i.e. \"minecraft:spider,minecraft:skeleton\"")
	public static List<String> replaceMobs = Lists.newArrayList();

	private static boolean staticEnabled;

	private static final Map<EntityType<?>, EntityType<?>> spawnerReplacements = Maps.newHashMap();

	@Override
	public void configChanged() {
		staticEnabled = enabled;

		spawnerReplacements.clear();

		for (String replaceKey : replaceMobs) {
			String[] split = replaceKey.split(",");
			if (split.length == 2) {
				Optional<EntityType<?>> before = EntityType.byString(split[0]);
				Optional<EntityType<?>> after = EntityType.byString(split[1]);
				if (before.isPresent() && after.isPresent()) {
					spawnerReplacements.put(before.get(), after.get());
				}
			}
		}
	}

	public static void spawnerUpdate(Level level, BlockPos pos, BlockState state, SpawnerBlockEntity be) {
		if (!staticEnabled)
			return;

		BaseSpawner spawner = be.getSpawner();
		Entity example = spawner.getOrCreateDisplayEntity(level);
		if (example != null) {
			EntityType<?> present = example.getType();
			if (spawnerReplacements.containsKey(present)) {
				spawner.setEntityId(spawnerReplacements.get(present));

				be.setChanged();
				level.sendBlockUpdated(pos, state, state, 3);
			}
		}
	}

}

package einstein.jmc.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import einstein.jmc.blocks.BaseCakeBlock;
import einstein.jmc.data.CakeEffects;
import einstein.jmc.init.ModItems;
import einstein.jmc.init.ModPotions;
import einstein.jmc.mixin.RecipeManagerAccessor;
import einstein.jmc.mixin.StructureTemplatePoolAccessor;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static einstein.jmc.JustMoreCakes.*;

public class Util {

    public static final Gson GSON = new GsonBuilder().create();
    public static final Random RANDOM = new Random();
    public static final LootItemCondition.Builder HAS_CAKE_SPATULA = MatchTool.toolMatches(ItemPredicate.Builder.item().of(ModItems.CAKE_SPATULA.get()));

    public static <T extends Item> ResourceLocation getItemId(T item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    public static <T extends Block> ResourceLocation getBlockId(T block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    @Nullable
    public static <T extends MobEffect> ResourceLocation getMobEffectId(T effect) {
        return BuiltInRegistries.MOB_EFFECT.getKey(effect);
    }

    public static void createExplosion(final Level level, final BlockPos pos, final float size) {
        if (level.isClientSide) {
            return;
        }
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, size, Level.ExplosionInteraction.TNT);
    }

    public static boolean teleportRandomly(final LivingEntity entity, final double radius) {
        int attempts;
        int tries;
        boolean teleported;
        double x;
        double y;
        double z;
        for (attempts = 20, tries = 0, teleported = false; !teleported && tries++ <= attempts; teleported = entity.randomTeleport(x, y, z, true)) {
            x = entity.xo + (RANDOM.nextDouble() - RANDOM.nextDouble()) * radius;
            y = entity.yo + (RANDOM.nextDouble() - RANDOM.nextDouble()) * radius;
            z = entity.zo + (RANDOM.nextDouble() - RANDOM.nextDouble()) * radius;
        }
        return teleported;
    }

    public static Block getBlock(ResourceLocation location) {
        Block block = BuiltInRegistries.BLOCK.get(location);
        if (block != Blocks.AIR) {
            return block;
        }
        else {
            throw new NullPointerException("Could not find block: " + location);
        }
    }

    public static boolean timeGoneBy(Level level, int ticks) {
        if (ticks == 0) {
            return true;
        }
        return level.getGameTime() % (ticks) == 0;
    }

    /**
     * Copied from com.illusivesoulworks.cakechomps.CakeChompsMod
     * and slightly changed to work with JustMoreCakes
     */

    public static void registerVillageBuilding(MinecraftServer server, String biome, String name, int weight) {
        String path = MOD_ID + ":village/" + biome + "/houses/" + biome + "_" + name;

        if (weight < 1) {
            return;
        }

        if (weight > 150) {
            LOGGER.error("Failed to register village building: " + path + " - weight must be less than 150");
            return;
        }

        RegistryAccess.Frozen access = server.registryAccess();
        Registry<StructureTemplatePool> templatePoolRegistry = access.registry(Registries.TEMPLATE_POOL).orElseThrow();
        Registry<StructureProcessorList> processorRegistry = access.registry(Registries.PROCESSOR_LIST).orElseThrow();

        StructureTemplatePool templatePool = templatePoolRegistry.get(mcLoc("village/" + biome + "/houses"));

        if (templatePool == null) {
            LOGGER.warn("Failed to register village building: " + path + "  - template pool is null");
            return;
        }

        StructurePoolElement structure = StructurePoolElement.legacy(path, processorRegistry.getHolderOrThrow(ProcessorLists.MOSSIFY_10_PERCENT)).apply(StructureTemplatePool.Projection.RIGID);
        StructureTemplatePoolAccessor templateAccessor = ((StructureTemplatePoolAccessor) templatePool);

        for (int i = 0; i < weight; i++) {
            templateAccessor.getTemplates().add(structure);
        }

        List<Pair<StructurePoolElement, Integer>> rawTemplates = new ArrayList<>(templateAccessor.getRawTemplates());
        rawTemplates.add(Pair.of(structure, weight));
        templateAccessor.setRawTemplates(rawTemplates);
    }

    public static Map<ResourceLocation, CakeEffects> deserializeCakeEffects(ResourceManager manager) {
        ImmutableMap.Builder<ResourceLocation, CakeEffects> builder = ImmutableMap.builder();
        var locations = manager.listResources("cake_effects", location -> location.getPath().endsWith(".json"));

        locations.forEach((location, resource) -> {
            try (InputStream stream = resource.open();
                 Reader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonObject object = GsonHelper.fromJson(GSON, reader, JsonObject.class);

                CakeEffects.CODEC.parse(JsonOps.INSTANCE, object)
                        .resultOrPartial(error -> LOGGER.error("Failed to decode cake effect with json id {} - Error: {}", location, error))
                        .ifPresent(entry -> builder.put(location, entry));
            }
            catch (Exception exception) {
                LOGGER.error("Error occurred while loading resource json " + location.toString(), exception);
            }
        });

        Map<ResourceLocation, CakeEffects> map = builder.build();
        LOGGER.info("Loaded {} cake effects", map.size());
        return map;
    }

    public static void livingEntityJump(Entity entity) {
        if (entity instanceof Player player) {
            if (player.hasEffect(ModPotions.BOUNCING_EFFECT.get())) {
                player.push(0, 0.15F, 0);
            }
        }
    }

    public static void livingEntityTick(Level level, LivingEntity entity) {
        RandomSource random = entity.getRandom();
        if (entity.hasEffect(ModPotions.BOUNCING_EFFECT.get())) {
            if (entity.verticalCollision && entity.onGround() && !entity.isSleeping()) {
                float f = 0.65F;

                if (entity.hasEffect(MobEffects.JUMP)) {
                    f += 0.1F * (entity.getEffect(MobEffects.JUMP).getAmplifier() + 1);
                }

                entity.push(0, f, 0);
                entity.playSound(SoundEvents.SLIME_SQUISH, 1, 1);

                if (level.isClientSide) {
                    for (int i = 0; i < 8; ++i) {
                        float f1 = random.nextFloat() * ((float) Math.PI * 2F);
                        float f2 = random.nextFloat() * 0.5F + 0.5F;
                        float f3 = Mth.sin(f1) * 1 * 0.5F * f2;
                        float f4 = Mth.cos(f1) * 1 * 0.5F * f2;
                        level.addParticle(ParticleTypes.ITEM_SLIME, entity.getX() + f3, entity.getY(), entity.getZ() + f4, 0, 0, 0);
                    }
                }
            }
        }
    }

    public static Predicate<ItemEntity> pandaItems() {
        return itemEntity -> {
            ItemStack stack = itemEntity.getItem();
            return (stack.is(Blocks.BAMBOO.asItem()) || stack.is(Blocks.CAKE.asItem()) || isCake().test(stack)) && itemEntity.isAlive() && !itemEntity.hasPickUpDelay();
        };
    }

    public static Predicate<ItemStack> isCake() {
        return stack -> {
            for (Supplier<BaseCakeBlock> cake : CakeBuilder.BUILDER_BY_CAKE.keySet()) {
                if (stack.is(cake.get().asItem())) {
                    return true;
                }
            }
            return false;
        };
    }

    public static <K, V> Map<K, V> createKeySortedMap(Map<K, V> map, Comparator<K> comparator) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByKey(comparator))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static <K, V> Map<K, V> createValueSortedMap(Map<K, V> map, Comparator<V> comparator) {
        return map.entrySet().stream().sorted(Map.Entry.comparingByValue(comparator))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    public static LootTable.Builder addDropWhenCakeSpatulaPool(LootTable.Builder builder, Block block) {
        return builder.withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(block).when(HAS_CAKE_SPATULA)));
    }

    public static ImmutableList<Block> getVanillaCandleCakes() {
        return ImmutableList.of(Blocks.CANDLE_CAKE, Blocks.WHITE_CANDLE_CAKE,
                Blocks.ORANGE_CANDLE_CAKE, Blocks.MAGENTA_CANDLE_CAKE, Blocks.LIGHT_BLUE_CANDLE_CAKE,
                Blocks.YELLOW_CANDLE_CAKE, Blocks.LIME_CANDLE_CAKE, Blocks.PINK_CANDLE_CAKE, Blocks.GRAY_CANDLE_CAKE,
                Blocks.LIGHT_GRAY_CANDLE_CAKE, Blocks.CYAN_CANDLE_CAKE, Blocks.PURPLE_CANDLE_CAKE, Blocks.BLUE_CANDLE_CAKE,
                Blocks.BROWN_CANDLE_CAKE, Blocks.GREEN_CANDLE_CAKE, Blocks.RED_CANDLE_CAKE, Blocks.BLACK_CANDLE_CAKE);
    }

    public static void removeRecipe(RecipeManager recipeManager, RecipeType<?> type, ResourceLocation id) {
        RecipeManagerAccessor manager = (RecipeManagerAccessor) recipeManager;
        Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> recipesByType = new HashMap<>(manager.getRecipes());
        Map<ResourceLocation, RecipeHolder<?>> recipes = new HashMap<>(recipesByType.get(type));

        for (ResourceLocation recipeId : recipes.keySet()) {
            if (recipeId.equals(id)) {
                recipes.remove(recipeId);
                break;
            }
        }

        recipesByType.replace(type, recipes);
        manager.setRecipes(recipesByType);
    }

    public static void applyEffectFromHolder(CakeEffects.MobEffectHolder holder, LivingEntity entity) {
        MobEffectInstance instance = new MobEffectInstance(holder.effect(), holder.duration().orElse(0), holder.amplifier().orElse(0));
        if (holder.effect().isInstantenous()) {
            instance.getEffect().applyInstantenousEffect(entity, entity, entity, instance.getAmplifier(), 1);
        }
        else {
            entity.addEffect(instance);
        }
    }
}

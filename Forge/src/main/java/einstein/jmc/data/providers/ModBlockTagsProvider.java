package einstein.jmc.data.providers;

import einstein.jmc.JustMoreCakes;
import einstein.jmc.init.ModBlocks;
import einstein.jmc.util.CakeBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {

    public static final TagKey<Block> F_CAKES = BlockTags.create(new ResourceLocation("forge", "cakes"));
    public static final TagKey<Block> F_CANDLE_CAKES = BlockTags.create(new ResourceLocation("forge", "candle_cakes"));

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, JustMoreCakes.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ModBlocks.CAKE_OVEN.get(), ModBlocks.OBSIDIAN_CAKE.get());
        tag(ModBlockTags.CAKE_SPATULA_USABLE).add(ModBlocks.CHOCOLATE_CAKE.get());
        CakeBuilder.BUILDER_BY_CAKE.forEach((cake, cakeBuilder) -> {
            tag(ModBlockTags.CAKES).add(cake.get());
            cakeBuilder.getCandleCakeByCandle().forEach((candle, candleCake) -> {
                tag(ModBlockTags.CANDLE_CAKES).add(candleCake.get());
            });
        });

        tag(ModBlockTags.C_CAKES).addTag(ModBlockTags.CAKES);
        tag(ModBlockTags.C_CANDLE_CAKES).addTag(ModBlockTags.CANDLE_CAKES);
        tag(F_CAKES).addTag(ModBlockTags.CAKES);
        tag(F_CANDLE_CAKES).addTag(ModBlockTags.CANDLE_CAKES);
    }
}

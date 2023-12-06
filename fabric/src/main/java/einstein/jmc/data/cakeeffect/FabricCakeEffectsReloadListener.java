package einstein.jmc.data.cakeeffect;

import einstein.jmc.JustMoreCakes;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

public class FabricCakeEffectsReloadListener implements SimpleSynchronousResourceReloadListener {

    @Override
    public ResourceLocation getFabricId() {
        return JustMoreCakes.loc("cake_effects");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        CakeEffectsManager.deserializeCakeEffects(manager);
    }
}
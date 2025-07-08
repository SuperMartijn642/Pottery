package com.supermartijn642.pottery.content;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Supplier;

/**
 * Created 27/12/2024 by SuperMartijn642
 */
public class PotItemModel implements ItemModel.Unbaked {

    public static final MapCodec<PotItemModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("model").forGetter(model -> model.model)
    ).apply(instance, PotItemModel::new));

    private final ResourceLocation model;

    public PotItemModel(ResourceLocation model){
        this.model = model;
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context){
        ModelBaker modelBaker = context.blockModelBaker();
        ResolvedModel model = modelBaker.getModel(this.model);
        TextureSlots textures = model.getTopTextureSlots();
        List<BakedQuad> quads = model.bakeTopGeometry(textures, modelBaker, BlockModelRotation.X0_Y0).getAll();
        ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(modelBaker, model, textures);
        Supplier<Vector3f[]> extents = Suppliers.memoize(() -> BlockModelWrapper.computeExtents(quads));
        return (state, stack, modelResolver, displayContext, level, entity, someRandomId) -> {
            ItemStackRenderState.LayerRenderState layer = state.newLayer();
            if(stack.hasFoil())
                layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
            layer.setExtents(extents);
            layer.setRenderType(ItemBlockRenderTypes.getRenderType(stack));
            properties.applyToLayer(layer, displayContext);
            layer.prepareQuadList().addAll(PotBakedModel.getItemQuads(stack, quads));
        };
    }

    @Override
    public void resolveDependencies(Resolver resolver){
        resolver.markDependency(this.model);
    }

    @Override
    public MapCodec<? extends ItemModel.Unbaked> type(){
        return CODEC;
    }
}

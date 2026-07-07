package com.supermartijn642.pottery.content;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.PotDecorations;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

import java.util.List;
import java.util.function.Supplier;

/**
 * Created 27/12/2024 by SuperMartijn642
 */
public class PotItemModel implements ItemModel.Unbaked {

    public static final MapCodec<PotItemModel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Identifier.CODEC.fieldOf("model").forGetter(model -> model.model)
    ).apply(instance, PotItemModel::new));

    private final Identifier model;

    public PotItemModel(Identifier model){
        this.model = model;
    }

    @Override
    public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation){
        ModelBaker modelBaker = context.blockModelBaker();
        ResolvedModel model = modelBaker.getModel(this.model);
        TextureSlots textures = model.getTopTextureSlots();
        List<BakedQuad> quads = model.bakeTopGeometry(textures, modelBaker, BlockModelRotation.IDENTITY).getAll();
        boolean animated = quads.stream().anyMatch(quad -> quad.materialInfo().sprite().contents().isAnimated());
        ModelRenderProperties properties = ModelRenderProperties.fromResolvedModel(modelBaker, model, textures);
        Supplier<Vector3fc[]> extents = Suppliers.memoize(() -> CuboidItemModelWrapper.computeExtents(quads));
        return (state, stack, modelResolver, displayContext, level, entity, someRandomId) -> {
            state.appendModelIdentityElement(this);
            state.appendModelIdentityElement(this.model);
            ItemStackRenderState.LayerRenderState layer = state.newLayer();
            if(stack.hasFoil()){
                layer.setFoilType(ItemStackRenderState.FoilType.STANDARD);
                state.appendModelIdentityElement(ItemStackRenderState.FoilType.STANDARD);
                state.setAnimated();
            }
            layer.setExtents(extents);
            layer.setLocalTransform(transformation);
            properties.applyToLayer(layer, displayContext);
            state.appendModelIdentityElement(stack.getOrDefault(DataComponents.POT_DECORATIONS, PotDecorations.EMPTY));
            layer.prepareQuadList().addAll(PotBakedModel.getItemQuads(stack, quads));
            if(animated)
                state.setAnimated();
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

package net.minecraft.client.renderer.model;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class MultipartBakedModel implements IBakedModel
{
    private final List<Pair<Predicate<BlockState>, IBakedModel>> selectors;
    protected final boolean ambientOcclusion;
    protected final boolean gui3D;
    protected final boolean isSideLit;
    protected final TextureAtlasSprite particleTexture;
    protected final ItemCameraTransforms cameraTransforms;
    protected final ItemOverrideList overrides;
    private final Map<BlockState, BitSet> bitSetCache = new Object2ObjectOpenCustomHashMap<>(Util.identityHashStrategy());

    public MultipartBakedModel(List<Pair<Predicate<BlockState>, IBakedModel>> selectors)
    {
        this.selectors = selectors;
        IBakedModel ibakedmodel = selectors.iterator().next().getRight();
        this.ambientOcclusion = ibakedmodel.isAmbientOcclusion();
        this.gui3D = ibakedmodel.isGui3d();
        this.isSideLit = ibakedmodel.isSideLit();
        this.particleTexture = ibakedmodel.getParticleTexture();
        this.cameraTransforms = ibakedmodel.getItemCameraTransforms();
        this.overrides = ibakedmodel.getOverrides();
    }

    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand)
    {
        if (state == null)
        {
            return Collections.emptyList();
        }
        else
        {
            BitSet bitset = this.bitSetCache.get(state);

            if (bitset == null)
            {
                bitset = new BitSet();

                for (int i = 0; i < this.selectors.size(); ++i)
                {
                    Pair<Predicate<BlockState>, IBakedModel> pair = this.selectors.get(i);

                    if (pair.getLeft().test(state))
                    {
                        bitset.set(i);
                    }
                }

                this.bitSetCache.put(state, bitset);
            }

            List<BakedQuad> list = Lists.newArrayList();
            long k = rand.nextLong();

            for (int j = 0; j < bitset.length(); ++j)
            {
                if (bitset.get(j))
                {
                    list.addAll(this.selectors.get(j).getRight().getQuads(state, side, new Random(k)));
                }
            }

            return list;
        }
    }

    public boolean isAmbientOcclusion()
    {
        return this.ambientOcclusion;
    }

    public boolean isGui3d()
    {
        return this.gui3D;
    }

    public boolean isSideLit()
    {
        return this.isSideLit;
    }

    public boolean isBuiltInRenderer()
    {
        return false;
    }

    public TextureAtlasSprite getParticleTexture()
    {
        return this.particleTexture;
    }

    public ItemCameraTransforms getItemCameraTransforms()
    {
        return this.cameraTransforms;
    }

    public ItemOverrideList getOverrides()
    {
        return this.overrides;
    }

    public static class Builder
    {
        private final List<Pair<Predicate<BlockState>, IBakedModel>> selectors = Lists.newArrayList();

        public void putModel(Predicate<BlockState> predicate, IBakedModel model)
        {
            this.selectors.add(Pair.of(predicate, model));
        }

        public IBakedModel build()
        {
            return new MultipartBakedModel(this.selectors);
        }
    }
}

package dev.matrixman.cobblemonutils.mixin;

import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;


@Mixin(PokemonRenderer.class)
public abstract class PokemonNameMixin<T extends Entity> {
    @ModifyArg(method = "renderNameTag",
               at = @At(value ="INVOKE",
                        target="Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
                        ordinal = 0),
               index = 0
    )
    private Text changePokemonName$ThroughBlocks (Text t) {
        return changePokemonName(t);
    }

    @ModifyArg(method = "renderNameTag",
               at = @At(value ="INVOKE",
                        target="Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
                        ordinal = 1),
               index = 0
    )
    private Text changePokemonName$Normal (Text t) {
        return changePokemonName(t);
    }


    @Unique
    private Text changePokemonName (Text t) {
        return Text.literal(t.getString() + " hit");
    }
}


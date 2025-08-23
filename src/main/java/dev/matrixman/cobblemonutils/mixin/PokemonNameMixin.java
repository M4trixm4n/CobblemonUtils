package dev.matrixman.cobblemonutils.mixin;

import com.cobblemon.mod.common.api.pokedex.AbstractPokedexManager;
import com.cobblemon.mod.common.api.pokedex.FormDexRecord;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.SpeciesDexRecord;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.render.pokemon.PokemonRenderer;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;


@Mixin(PokemonRenderer.class)
public abstract class PokemonNameMixin {
    @Unique
    private static final float ICON_W = 10f, ICON_H = 10f;

    /**
     * Inject after Cobblemon has finished assembling and drawing the label text.
     * We draw a tiny textured quad (the icon) to the right of the text.
     */
    @Inject(method = "renderNameTag",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/font/TextRenderer;draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I",
                     shift = At.Shift.AFTER,
                     ordinal = 1) // after the second (NORMAL) text draw
            )
    private void cobblemonutils$drawIconAfterLabel (PokemonEntity entity,
                                                    Text text,
                                                    MatrixStack matrices,
                                                    VertexConsumerProvider vertexConsumers,
                                                    int light,
                                                    float tickDelta,
                                                    CallbackInfo ci,
                                                    @Local(name = "label") MutableText label) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int stringWidth = tr.getWidth(label);
        float vanillaX = stringWidth / 2f;
        float centerY = 4f;
        float centerX = vanillaX + 7f;

        VertexConsumer vc = vertexConsumers.getBuffer(getRenderLayer(entity));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();

        // Half-width and height
        float hw = ICON_W / 2f;
        float hh = ICON_H / 2f;
        float z = 0f; // No z adjustment
        int[] tint = chooseTintColor(entity);
        int r = tint[0], g = tint[1], b = tint[2], a = 255; // full colors
        int fullbright = LightmapTextureManager.MAX_LIGHT_COORDINATE; // full brightness (not working ?)

        vc.vertex(posMat, centerX - hw, centerY + hh, z)
          .color(r, g, b, a)
          .texture(0f, 1f)
          .overlay(OverlayTexture.DEFAULT_UV)
          .light(fullbright)
          .normal(entry, 0f, 1f, 0f); // bottom left
        vc.vertex(posMat, centerX - hw, centerY - hh, z)
          .color(r, g, b, a)
          .texture(0f, 0f)
          .overlay(OverlayTexture.DEFAULT_UV)
          .light(fullbright)
          .normal(entry, 0f, 1f, 0f); // top left
        vc.vertex(posMat, centerX + hw, centerY - hh, z)
          .color(r, g, b, a)
          .texture(1f, 0f)
          .overlay(OverlayTexture.DEFAULT_UV)
          .light(fullbright)
          .normal(entry, 0f, 1f, 0f); // top right
        vc.vertex(posMat, centerX + hw, centerY + hh, z)
          .color(r, g, b, a)
          .texture(1f, 1f)
          .overlay(OverlayTexture.DEFAULT_UV)
          .light(fullbright)
          .normal(entry, 0f, 1f, 0f); // bottom right
    }

    @Unique
    public RenderLayer getRenderLayer (PokemonEntity entity) {
        Identifier iconTexture = chooseTexture(entity);
        return RenderLayer.of("icon", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, // Every necessary call after .builder()
                              VertexFormat.DrawMode.QUADS, 2048, // Even if it's too small, it will auto-resize, and it will be logged if it happens
                              false,  // hasCrumbling, useless to have here
                              false, // not translucent, don't care
                              RenderLayer.MultiPhaseParameters
                                      .builder()
                                      .program(RenderPhase.TRANSPARENT_TEXT_PROGRAM)
                                      .texture(new RenderPhase.Texture(iconTexture, false, false))
                                      .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                                      .cull(RenderPhase.DISABLE_CULLING) // Cull enabled = backside isn't rendered, doesn't matter for 2D stuff
                                      .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                                      .build(true));
    }

    /**
     * Choose the texture to show depending on the capture status of this pokemon and its other forms
     * @param entity The pokemon we're looking at, whose name tag is used
     * @return A texture to the right icon
     */
    @Unique
    public Identifier chooseTexture (PokemonEntity entity) {
        Identifier noneCaptured = Identifier.of("cobblemonutils", "textures/captured/captured_no.png");
        Identifier thisCaptured = Identifier.of("cobblemonutils", "textures/captured/captured_this.png");
        Identifier otherCaptured = Identifier.of("cobblemonutils", "textures/captured/captured_other.png");
        Identifier allCaptured = Identifier.of("cobblemonutils", "textures/captured/captured_all.png");

        AbstractPokedexManager pm = CobblemonClient.INSTANCE.getClientPokedexData();
        Pokemon pokemon = entity.getPokemon();
        Identifier speciesId = pokemon.getSpecies().getResourceIdentifier();

        // If not caught at all, none
        PokedexEntryProgress speciesProgress = pm.getHighestKnowledgeForSpecies(speciesId);
        boolean caughtAnyForm = (speciesProgress == PokedexEntryProgress.CAUGHT);
        if (!caughtAnyForm) return noneCaptured;

        Species sp = PokemonSpecies.INSTANCE.getByIdentifier(speciesId);
        if (sp == null) return noneCaptured;
        // If caught but only 1 form, all
        List<FormData> forms = sp.getForms();
        if (forms.size() <= 1) return allCaptured;

        SpeciesDexRecord sr = pm.getSpeciesRecord(speciesId);
        if (sr == null) return noneCaptured;
        boolean all = true;
        boolean thisOne = false;
        for (FormData f: forms) {
            FormDexRecord fr = sr.getFormRecord(f.getName());
            if (fr == null || fr.getKnowledge() != PokedexEntryProgress.CAUGHT) {
                all = false;
                continue;
            }
            if (f == pokemon.getForm()) thisOne = true;
        }
        if (all) return allCaptured;
        if (thisOne) return thisCaptured;
        return otherCaptured;
    }

    /**
     * (UNIMPLEMENTED) Choose the right color depending on the capture status of this pokemon's shiny variant and its other forms' status
     * (CURRENT) Default to silver color
     * @param entity The pokemon we're looking at, whose name tag is used
     * @return The right color for the situation
     */
    @Unique
    public int[] chooseTintColor (PokemonEntity entity) {
        int[] gold      = {255, 215, 0};    // All shiny forms captured
        int[] silver    = {192, 192, 192};  // No shiny forms captured
        int[] copper    = {184, 115, 51};   // Some shiny forms captured, but not this one
        int[] softRed   = {255, 107, 107};  // This shiny form captured (not all)

        return silver;
    }
}

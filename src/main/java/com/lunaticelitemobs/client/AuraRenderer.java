package com.lunaticelitemobs.client;

import com.lunaticelitemobs.LunaticEliteMobs;
import com.lunaticelitemobs.config.EliteClientConfig;
import com.lunaticelitemobs.config.EliteClientConfig.Quality;
import com.lunaticelitemobs.config.EliteClientConfig.Style;
import com.lunaticelitemobs.elite.EliteManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws elite auras. Everything except the "particles" style is drawn directly as glowing geometry
 * during the mob's render pass (no entities, no particles), which keeps the cost very low.
 * All sizes follow the mob's hitbox so modded mobs of any size look right.
 */
@EventBusSubscriber(modid = LunaticEliteMobs.MODID, value = Dist.CLIENT)
public final class AuraRenderer {
    /** Wing outline (canvas units, x outward, y up), traced from the preview. */
    private static final float[][] WING = {
            {0f, 0f}, {1.0f, 0.545f}, {0.873f, 0.145f}, {1.055f, -0.073f},
            {0.727f, -0.182f}, {0.836f, -0.473f}, {0.364f, -0.327f}
    };

    private AuraRenderer() {}

    private static boolean shouldShow(LivingEntity e) {
        if (!EliteClientConfig.ENABLED.get()) return false;
        if (EliteClientConfig.HIDE_WHEN_INVISIBLE.get() && e.isInvisible()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        double d = EliteClientConfig.RENDER_DISTANCE.get();
        return e.distanceToSqr(mc.player) <= d * d;
    }

    private static int segments() {
        Quality q = EliteClientConfig.QUALITY.get();
        return q == Quality.LOW ? 20 : q == Quality.HIGH ? 56 : 36;
    }

    /** Orbit / sigil radius that follows the hitbox width. */
    private static float baseRadius(LivingEntity e) {
        return e.getBbWidth() * 0.5f + (float) (EliteClientConfig.WIDTH_PADDING.get() * EliteClientConfig.SIZE_SCALE.get());
    }

    // ================================================================== name tag

    /** The elite name is only changed, not floated above the head (unless the tier forces it visible). */
    @SubscribeEvent
    public static void onNameTag(RenderNameTagEvent event) {
        if (!EliteClientConfig.HIDE_NAMETAG.get()) return;
        Entity e = event.getEntity();
        if (EliteManager.getTier(e) > 0 && !e.isCustomNameVisible()) {
            event.setCanRender(TriState.FALSE);
        }
    }

    // ================================================================== particles (tick based)

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity ent = event.getEntity();
        if (!ent.level().isClientSide() || !(ent instanceof LivingEntity e)) return;
        int tier = EliteManager.getTier(e);
        if (tier <= 0) return;
        EliteClientConfig.Parsed p = EliteClientConfig.parsed(tier);
        if (!p.styles.contains(Style.PARTICLES) || !shouldShow(e)) return;
        EliteClientConfig.TierVisual v = EliteClientConfig.tier(tier);

        int interval = v.particleIntervalTicks.get();
        if (EliteClientConfig.QUALITY.get() == Quality.LOW) interval *= 2;
        if ((e.tickCount + e.getId()) % interval != 0) return;

        RandomSource r = e.getRandom();
        float w = e.getBbWidth(), h = e.getBbHeight();
        float radius = w * 0.5f + 0.15f;
        float size = (float) Mth.clamp(v.particleSize.get() * (0.7 + w * 0.35) * EliteClientConfig.SIZE_SCALE.get(), 0.1, 4.0);
        int count = v.particleCount.get();
        if (EliteClientConfig.QUALITY.get() == Quality.HIGH) count += 1;
        for (int i = 0; i < count; i++) {
            float[] c = r.nextBoolean() ? p.primary : p.secondary;
            float ang = r.nextFloat() * Mth.TWO_PI;
            float rad = radius * (0.4f + r.nextFloat() * 0.6f);
            double x = e.getX() + Mth.cos(ang) * rad;
            double y = e.getY() + r.nextFloat() * h;
            double z = e.getZ() + Mth.sin(ang) * rad;
            e.level().addParticle(new DustParticleOptions(new Vector3f(c[0], c[1], c[2]), size), x, y, z, 0.0, 0.04, 0.0);
        }
    }

    // ================================================================== geometry (render based)

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity e = event.getEntity();
        int tier = EliteManager.getTier(e);
        if (tier <= 0 || !shouldShow(e)) return;
        EliteClientConfig.Parsed p = EliteClientConfig.parsed(tier);
        if (p.styles.isEmpty()) return;
        EliteClientConfig.TierVisual v = EliteClientConfig.tier(tier);

        PoseStack ps = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        float pt = event.getPartialTick();
        float time = (e.tickCount + pt) * (float) EliteClientConfig.ANIMATION_SPEED.get().doubleValue();
        float spin = time * (float) v.rotationSpeed.get().doubleValue();
        float alpha = (float) v.alpha.get().doubleValue();

        if (p.styles.contains(Style.SIGIL)) drawSigil(e, ps, buf, p, spin, time, alpha);
        if (p.styles.contains(Style.ORBS)) drawOrbs(e, ps, buf, p, v.orbCount.get(), spin, time, alpha);
        if (p.styles.contains(Style.BLADES)) drawBlades(e, ps, buf, p, v.bladeCount.get(), spin, time, alpha);
        if (p.styles.contains(Style.WINGS)) drawWings(e, ps, buf, p, pt, time, alpha);
    }

    // ---------------------------------------------------------------- A: orbiting orbs
    private static void drawOrbs(LivingEntity e, PoseStack ps, MultiBufferSource buf, EliteClientConfig.Parsed p,
                                 int count, float spin, float time, float alpha) {
        float w = e.getBbWidth(), h = e.getBbHeight();
        float scale = (float) EliteClientConfig.SIZE_SCALE.get().doubleValue();
        float radius = baseRadius(e) + 0.25f * scale;
        float s = Mth.clamp(0.06f + 0.04f * w, 0.06f, 0.4f) * scale;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        for (int i = 0; i < count; i++) {
            float a = spin * 0.05f + i * Mth.TWO_PI / count;
            float x = Mth.cos(a) * radius;
            float z = Mth.sin(a) * radius;
            float y = h * 0.55f + Mth.sin(time * 0.08f + i * 1.7f) * h * 0.08f;
            float pulse = 0.75f + 0.25f * Mth.sin(time * 0.15f + i);
            AuraDraw.cube(vc, m, x, y, z, s * 2.2f, p.primary[0], p.primary[1], p.primary[2], 0.18f * alpha * pulse);
            AuraDraw.cube(vc, m, x, y, z, s * 1.4f, p.secondary[0], p.secondary[1], p.secondary[2], 0.30f * alpha);
            AuraDraw.cube(vc, m, x, y, z, s, p.primary[0], p.primary[1], p.primary[2], 0.9f * alpha);
        }
    }

    // ---------------------------------------------------------------- G: orbiting shadow blades
    private static void drawBlades(LivingEntity e, PoseStack ps, MultiBufferSource buf, EliteClientConfig.Parsed p,
                                   int count, float spin, float time, float alpha) {
        float h = e.getBbHeight();
        float scale = (float) EliteClientConfig.SIZE_SCALE.get().doubleValue();
        float radius = baseRadius(e) + 0.4f * scale;
        float len = Mth.clamp(0.25f * h + 0.25f, 0.4f, 3.5f) * scale;
        float hw = len * 0.075f;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        for (int i = 0; i < count; i++) {
            float a = spin * 0.035f + i * Mth.TWO_PI / count;
            float cr = Mth.cos(a), sr = Mth.sin(a);      // radial
            float ct = -sr, st = cr;                     // tangent
            float px = cr * radius, pz = sr * radius;
            float y0 = h * 0.3f + Mth.sin(time * 0.1f + i * 1.3f) * h * 0.05f; // guard height
            float y1 = y0 + len;                                                // tip

            // blade, two crossed planes (tangent plane + radial plane), tapered tip
            float r = p.primary[0], g = p.primary[1], b = p.primary[2], A = 0.85f * alpha;
            float yt = y1 - len * 0.18f;
            AuraDraw.quad(vc, m, px - ct * hw, y0, pz - st * hw, px + ct * hw, y0, pz + st * hw,
                    px + ct * hw, yt, pz + st * hw, px - ct * hw, yt, pz - st * hw, r, g, b, A);
            AuraDraw.quad(vc, m, px - ct * hw, yt, pz - st * hw, px + ct * hw, yt, pz + st * hw,
                    px, y1, pz, px, y1, pz, r, g, b, A);
            AuraDraw.quad(vc, m, px - cr * hw, y0, pz - sr * hw, px + cr * hw, y0, pz + sr * hw,
                    px + cr * hw, yt, pz + sr * hw, px - cr * hw, yt, pz - sr * hw, r, g, b, A * 0.6f);
            // bright core edge
            float hc = hw * 0.3f;
            AuraDraw.quad(vc, m, px - ct * hc, y0, pz - st * hc, px + ct * hc, y0, pz + st * hc,
                    px + ct * hc, yt, pz + st * hc, px - ct * hc, yt, pz - st * hc,
                    p.secondary[0], p.secondary[1], p.secondary[2], 0.55f * alpha);
            // guard
            float gw = hw * 3.2f, gh = len * 0.05f;
            AuraDraw.quad(vc, m, px - ct * gw, y0 - gh, pz - st * gw, px + ct * gw, y0 - gh, pz + st * gw,
                    px + ct * gw, y0, pz + st * gw, px - ct * gw, y0, pz - st * gw, r * 0.6f, g * 0.6f, b * 0.6f, A);
            // handle
            float hh = hw * 0.6f, hl = len * 0.18f;
            AuraDraw.quad(vc, m, px - ct * hh, y0 - gh - hl, pz - st * hh, px + ct * hh, y0 - gh - hl, pz + st * hh,
                    px + ct * hh, y0 - gh, pz + st * hh, px - ct * hh, y0 - gh, pz - st * hh, r * 0.45f, g * 0.45f, b * 0.45f, A);
        }
    }

    // ---------------------------------------------------------------- B: rotating sigil on the ground
    private static void drawSigil(LivingEntity e, PoseStack ps, MultiBufferSource buf, EliteClientConfig.Parsed p,
                                  float spin, float time, float alpha) {
        float scale = (float) EliteClientConfig.SIZE_SCALE.get().doubleValue();
        float R = baseRadius(e) + 0.55f * scale;
        float th = Mth.clamp(0.04f + 0.012f * e.getBbWidth(), 0.04f, 0.25f) * scale;
        float pulse = 0.55f + 0.3f * Mth.sin(time * 0.12f);
        float r = p.primary[0], g = p.primary[1], b = p.primary[2], A = pulse * alpha;
        int seg = segments();

        ps.pushPose();
        ps.mulPose(Axis.YP.rotation(spin * 0.02f));
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        float y = 0.03f;
        AuraDraw.ring(vc, m, y, R, th, seg, r, g, b, A);
        AuraDraw.ring(vc, m, y, R * 0.72f, th * 0.8f, seg, r, g, b, A * 0.9f);
        // outer ring pulses outward
        float wave = (time * 0.02f) % 1.0f;
        AuraDraw.ring(vc, m, y, R * (1.0f + wave * 0.35f), th * 0.6f, seg, r, g, b, A * (1.0f - wave) * 0.6f);
        // pentagram
        float sr = R * 0.72f;
        for (int k = 0; k < 5; k++) {
            float a0 = -Mth.HALF_PI + k * Mth.TWO_PI / 5f;
            float a1 = -Mth.HALF_PI + ((k + 2) % 5) * Mth.TWO_PI / 5f;
            AuraDraw.groundLine(vc, m, y, Mth.cos(a0) * sr, Mth.sin(a0) * sr, Mth.cos(a1) * sr, Mth.sin(a1) * sr, th * 0.8f, r, g, b, A);
        }
        // rune dots
        float dr = R * 0.86f, ds = th * 1.4f;
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8f;
            float cx = Mth.cos(a) * dr, cz = Mth.sin(a) * dr;
            AuraDraw.quad(vc, m, cx - ds, y, cz - ds, cx + ds, y, cz - ds, cx + ds, y, cz + ds, cx - ds, y, cz + ds, r, g, b, A);
        }
        ps.popPose();
    }

    // ---------------------------------------------------------------- I: shadow wings
    private static void drawWings(LivingEntity e, PoseStack ps, MultiBufferSource buf, EliteClientConfig.Parsed p,
                                  float pt, float time, float alpha) {
        float w = e.getBbWidth(), h = e.getBbHeight();
        float scale = (float) EliteClientConfig.SIZE_SCALE.get().doubleValue();
        float S = Mth.clamp(0.55f * h, 0.5f, 6.0f) * scale;
        float bodyYaw = Mth.rotLerp(pt, e.yBodyRotO, e.yBodyRot);
        float flap = Mth.sin(time * 0.12f) * 15f;

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw)); // +Z = facing, -Z = back
        ps.translate(0, h * 0.72f, -(w * 0.5f + 0.06f));

        // dark membrane (translucent), both wings first to keep batches together
        VertexConsumer fill = buf.getBuffer(RenderType.debugQuads());
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            wingTransform(ps, side, w, flap, S);
            Matrix4f m = ps.last().pose();
            for (int i = 1; i < WING.length - 1; i++) {
                AuraDraw.quad(fill, m, 0, 0, 0, WING[i][0], WING[i][1], 0, WING[i + 1][0], WING[i + 1][1], 0,
                        WING[i + 1][0], WING[i + 1][1], 0,
                        p.secondary[0], p.secondary[1], p.secondary[2], 0.82f * alpha);
            }
            ps.popPose();
        }

        // glowing red edge. Drawn slightly in front of AND behind the membrane (never in the same plane),
        // otherwise the edge and the dark fill fight for the same pixels and flicker.
        VertexConsumer glow = buf.getBuffer(RenderType.lightning());
        float pulse = 0.72f + 0.08f * Mth.sin(time * 0.06f); // gentle breathing, no blinking
        float off = 0.025f / S;                               // ~2.5 cm in world units
        for (int side = -1; side <= 1; side += 2) {
            ps.pushPose();
            wingTransform(ps, side, w, flap, S);
            Matrix4f m = ps.last().pose();
            float t = 0.045f;
            for (float z : new float[]{off, -off}) {
                for (int i = 0; i < WING.length; i++) {
                    float[] a = WING[i], b = WING[(i + 1) % WING.length];
                    AuraDraw.planeLine(glow, m, a[0], a[1], b[0], b[1], z, t, p.primary[0], p.primary[1], p.primary[2], pulse * alpha);
                }
                // bones
                AuraDraw.planeLine(glow, m, 0, 0, WING[3][0], WING[3][1], z, t * 0.7f, p.primary[0], p.primary[1], p.primary[2], pulse * 0.55f * alpha);
                AuraDraw.planeLine(glow, m, 0, 0, WING[5][0], WING[5][1], z, t * 0.7f, p.primary[0], p.primary[1], p.primary[2], pulse * 0.55f * alpha);
            }
            ps.popPose();
        }
        ps.popPose();
    }

    private static void wingTransform(PoseStack ps, int side, float w, float flap, float S) {
        ps.translate(side * w * 0.12f, 0, 0);
        ps.scale(side, 1, 1);                                   // mirror for the left wing
        ps.mulPose(Axis.YP.rotationDegrees(25f + flap));        // sweep back + flap
        ps.mulPose(Axis.ZP.rotationDegrees(17f));               // tilt upward
        ps.scale(S, S, S);
    }
}

package com.lunaticelitemobs.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Tiny immediate-style shape helpers. Every quad is emitted with both windings so culling never hides it. */
final class AuraDraw {
    private AuraDraw() {}

    static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.addVertex(m, x, y, z).setColor(r, g, b, a);
    }

    static void quad(VertexConsumer vc, Matrix4f m,
                     float x1, float y1, float z1, float x2, float y2, float z2,
                     float x3, float y3, float z3, float x4, float y4, float z4,
                     float r, float g, float b, float a) {
        v(vc, m, x1, y1, z1, r, g, b, a);
        v(vc, m, x2, y2, z2, r, g, b, a);
        v(vc, m, x3, y3, z3, r, g, b, a);
        v(vc, m, x4, y4, z4, r, g, b, a);
        v(vc, m, x4, y4, z4, r, g, b, a);
        v(vc, m, x3, y3, z3, r, g, b, a);
        v(vc, m, x2, y2, z2, r, g, b, a);
        v(vc, m, x1, y1, z1, r, g, b, a);
    }

    /** Axis-aligned cube centred on (cx,cy,cz) with half-size h. */
    static void cube(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float h, float r, float g, float b, float a) {
        float x0 = cx - h, x1 = cx + h, y0 = cy - h, y1 = cy + h, z0 = cz - h, z1 = cz + h;
        quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, r, g, b, a);
        quad(vc, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(vc, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
        quad(vc, m, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
        quad(vc, m, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, a);
        quad(vc, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
    }

    /** Flat ring on the XZ plane at height y. */
    static void ring(VertexConsumer vc, Matrix4f m, float y, float radius, float thickness, int segments,
                     float r, float g, float b, float a) {
        float in = radius - thickness * 0.5f, out = radius + thickness * 0.5f;
        float step = Mth.TWO_PI / segments;
        for (int i = 0; i < segments; i++) {
            float a0 = i * step, a1 = (i + 1) * step;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(vc, m, c0 * in, y, s0 * in, c0 * out, y, s0 * out, c1 * out, y, s1 * out, c1 * in, y, s1 * in, r, g, b, a);
        }
    }

    /** Flat line on the XZ plane. */
    static void groundLine(VertexConsumer vc, Matrix4f m, float y, float x1, float z1, float x2, float z2, float thickness,
                           float r, float g, float b, float a) {
        float dx = x2 - x1, dz = z2 - z1;
        float len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4f) return;
        float nx = -dz / len * thickness * 0.5f, nz = dx / len * thickness * 0.5f;
        quad(vc, m, x1 + nx, y, z1 + nz, x2 + nx, y, z2 + nz, x2 - nx, y, z2 - nz, x1 - nx, y, z1 - nz, r, g, b, a);
    }

    /** Line on a local XY plane at depth z. */
    static void planeLine(VertexConsumer vc, Matrix4f m, float x1, float y1, float x2, float y2, float z, float thickness,
                          float r, float g, float b, float a) {
        float dx = x2 - x1, dy = y2 - y1;
        float len = Mth.sqrt(dx * dx + dy * dy);
        if (len < 1.0E-4f) return;
        float nx = -dy / len * thickness * 0.5f, ny = dx / len * thickness * 0.5f;
        quad(vc, m, x1 + nx, y1 + ny, z, x2 + nx, y2 + ny, z, x2 - nx, y2 - ny, z, x1 - nx, y1 - ny, z, r, g, b, a);
    }
}

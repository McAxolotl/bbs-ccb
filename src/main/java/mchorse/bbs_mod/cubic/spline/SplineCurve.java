package mchorse.bbs_mod.cubic.spline;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Shared interpolating curve for the solver and its editor overlay. Coordinates are blocks. */
public final class SplineCurve
{
    private SplineCurve()
    {}

    /** Uniform Catmull-Rom with linear endpoint tangents. Two controls describe an exact line. */
    public static Vector3f evaluate(List<Vector3f> points, float t)
    {
        int count = points.size();

        if (count == 0) return new Vector3f();
        if (count == 1) return new Vector3f(points.get(0));

        float scaled = Math.max(0F, Math.min(1F, t)) * (count - 1);
        int i = Math.min(count - 2, (int) scaled);
        float u = scaled - i;
        Vector3f b = points.get(i);
        Vector3f c = points.get(i + 1);
        Vector3f a = i == 0 ? new Vector3f(b).mul(2F).sub(c) : points.get(i - 1);
        Vector3f d = i + 2 == count ? new Vector3f(c).mul(2F).sub(b) : points.get(i + 2);
        float u2 = u * u;
        float u3 = u2 * u;

        return new Vector3f(b).mul(2F)
            .fma(u, new Vector3f(c).sub(a))
            .fma(u2, new Vector3f(a).mul(2F).fma(-5F, b).fma(4F, c).sub(d))
            .fma(u3, new Vector3f(a).negate().fma(3F, b).fma(-3F, c).add(d))
            .mul(0.5F);
    }

    public static List<Vector3f> sample(List<Vector3f> points, int subdivisionsPerSegment)
    {
        if (points.isEmpty()) return List.of();

        int steps = Math.max(1, subdivisionsPerSegment) * Math.max(1, points.size() - 1);
        List<Vector3f> result = new ArrayList<>(steps + 1);

        for (int i = 0; i <= steps; i++) result.add(evaluate(points, i / (float) steps));

        return result;
    }
}

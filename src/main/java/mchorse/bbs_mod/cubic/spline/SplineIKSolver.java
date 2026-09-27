package mchorse.bbs_mod.cubic.spline;

import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/** Stateless spline sampling and rotation-minimizing frames; no renderer or game state. */
public final class SplineIKSolver
{
    private static final float EPS = 1E-6F;

    private SplineIKSolver()
    {}

    public record Result(Vector3f[] joints, Vector3f[] tangents)
    {}

    /**
     * There is one length between adjacent joints. Preserve mode intersects the sampled curve
     * with a sphere around each joint: arc-length stepping would silently shorten bent limbs.
     * Past the end, the last tangent continues the curve. Fit distributes joints by arc length;
     * it changes their separation, and does not promise to scale the model's geometry.
     */
    public static Result solve(List<Vector3f> controls, float[] lengths, boolean fit)
    {
        return solve(controls, lengths, fit, 0F);
    }

    /**
     * Progress moves the whole chain along the curve, measured in multiples of its arc length.
     * Zero starts at the first control, one at the last; values outside that range continue
     * along the endpoint tangents. Fit shifts its entire interval without shortening it.
     */
    public static Result solve(List<Vector3f> controls, float[] lengths, boolean fit, float progress)
    {
        if (controls.size() < 2 || lengths.length == 0 || !Float.isFinite(progress)) return null;

        for (Vector3f point : controls) if (point == null || !point.isFinite()) return null;
        for (float length : lengths) if (!Float.isFinite(length) || length <= EPS) return null;

        List<Vector3f> samples = SplineCurve.sample(controls, 48);
        float[] distances = new float[samples.size()];

        for (int i = 1; i < samples.size(); i++) distances[i] = distances[i - 1] + samples.get(i).distance(samples.get(i - 1));

        float total = distances[distances.length - 1];
        if (!Float.isFinite(total) || total <= EPS) return null;

        Vector3f startTangent = new Vector3f();
        for (int i = 1; i < samples.size(); i++)
        {
            startTangent.set(samples.get(i)).sub(samples.get(0));
            if (startTangent.lengthSquared() > EPS * EPS) break;
        }
        startTangent.normalize();

        Vector3f endTangent = new Vector3f();
        for (int i = samples.size() - 2; i >= 0; i--)
        {
            endTangent.set(samples.get(samples.size() - 1)).sub(samples.get(i));
            if (endTangent.lengthSquared() > EPS * EPS) break;
        }
        endTangent.normalize();

        Vector3f[] joints = new Vector3f[lengths.length + 1];
        Vector3f[] tangents = new Vector3f[joints.length];
        float startDistance = progress * total;
        if (!Float.isFinite(startDistance)) return null;
        ArcSample first = sampleDistance(samples, distances, startTangent, endTangent, startDistance, 1);
        joints[0] = first.point();
        int cursor = first.cursor();
        if (!joints[0].isFinite()) return null;
        float sum = 0F;
        for (float length : lengths) sum += length;
        float travelled = 0F;

        for (int i = 1; i < joints.length; i++)
        {
            float length = lengths[i - 1];

            if (fit)
            {
                travelled += length;
                float distance = startDistance + total * travelled / sum;
                if (!Float.isFinite(distance)) return null;
                ArcSample sample = sampleDistance(samples, distances, startTangent, endTangent, distance, cursor);
                joints[i] = sample.point();
                cursor = sample.cursor();
            }
            else
            {
                Vector3f center = joints[i - 1];
                Vector3f start = new Vector3f(center);
                Vector3f result = null;

                while (cursor < samples.size())
                {
                    Vector3f end = samples.get(cursor);
                    result = sphereExit(center, length, start, end);
                    if (result != null) break;
                    start.set(end);
                    cursor++;
                }

                if (result == null)
                {
                    /* The curve can curl entirely inside the sphere. Its extension still has an
                     * exact positive exit; this also handles a chain longer than a straight curve. */
                    Vector3f delta = new Vector3f(start).sub(center);
                    float b = delta.dot(endTangent);
                    float d = Math.max(0F, b * b - delta.lengthSquared() + length * length);
                    result = new Vector3f(start).fma(Math.max(0F, -b + (float) Math.sqrt(d)), endTangent);
                }

                joints[i] = result;
            }

            Vector3f tangent = new Vector3f(joints[i]).sub(joints[i - 1]);
            if (!joints[i].isFinite() || !tangent.isFinite() || tangent.lengthSquared() <= EPS * EPS) return null;
            tangents[i - 1] = tangent.normalize();
        }

        tangents[tangents.length - 1] = cursor == 0 ? startTangent : cursor >= samples.size() - 1
            ? endTangent : new Vector3f(samples.get(cursor)).sub(samples.get(cursor - 1)).normalize();
        if (!tangents[tangents.length - 1].isFinite()) tangents[tangents.length - 1] = new Vector3f(tangents[tangents.length - 2]);

        return new Result(joints, tangents);
    }

    private record ArcSample(Vector3f point, int cursor)
    {}

    /** Cursor names the next polyline vertex, including the initial/final tangent extensions. */
    private static ArcSample sampleDistance(List<Vector3f> samples, float[] distances,
        Vector3f startTangent, Vector3f endTangent, float distance, int cursor)
    {
        int last = samples.size() - 1;
        if (distance < 0F) return new ArcSample(new Vector3f(samples.get(0)).fma(distance, startTangent), 0);
        if (distance > distances[last]) return new ArcSample(new Vector3f(samples.get(last)).fma(distance - distances[last], endTangent), samples.size());
        cursor = Math.max(1, Math.min(last, cursor));
        while (cursor < last && distances[cursor] < distance) cursor++;
        float span = distances[cursor] - distances[cursor - 1];
        float alpha = span <= EPS ? 0F : (distance - distances[cursor - 1]) / span;
        return new ArcSample(new Vector3f(samples.get(cursor - 1)).lerp(samples.get(cursor), Math.max(0F, Math.min(1F, alpha))), cursor);
    }

    private static Vector3f sphereExit(Vector3f center, float radius, Vector3f start, Vector3f end)
    {
        Vector3f direction = new Vector3f(end).sub(start);
        Vector3f offset = new Vector3f(start).sub(center);
        float a = direction.lengthSquared();
        if (a <= EPS * EPS) return null;
        float b = offset.dot(direction);
        float discriminant = b * b - a * (offset.lengthSquared() - radius * radius);
        if (discriminant < 0F) return null;
        float t = (-b + (float) Math.sqrt(discriminant)) / a;

        return t >= 0F && t <= 1F ? new Vector3f(start).fma(t, direction) : null;
    }

    /** Deterministic normal, independent of the previous frame/tick or another actor. */
    public static Vector3f perpendicular(Vector3f tangent)
    {
        Vector3f axis = Math.abs(tangent.y) < 0.9F ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        return axis.fma(-axis.dot(tangent), tangent).normalize();
    }

    /** Parallel transport; at an exact reversal the previous normal supplies the half-turn axis. */
    public static Vector3f transport(Vector3f normal, Vector3f from, Vector3f to)
    {
        Vector3f result = new Vector3f(normal);

        if (from.dot(to) < -0.9999F) new Quaternionf().rotationAxis((float) Math.PI, normal).transform(result);
        else new Quaternionf().rotationTo(from, to).transform(result);

        result.fma(-result.dot(to), to);
        return result.lengthSquared() <= EPS * EPS ? perpendicular(to) : result.normalize();
    }

    /** A proper basis with the tangent on Z and the transported normal on Y. */
    public static Quaternionf frame(Vector3f tangent, Vector3f normal)
    {
        Vector3f x = new Vector3f(normal).cross(tangent).normalize();
        Vector3f y = new Vector3f(tangent).cross(x).normalize();
        return new Quaternionf().setFromNormalized(new Matrix3f().setColumn(0, x).setColumn(1, y).setColumn(2, tangent));
    }
}

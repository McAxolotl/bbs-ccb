package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.spline.SplineCurve;
import mchorse.bbs_mod.cubic.spline.SplineIK;
import mchorse.bbs_mod.cubic.spline.SplinePoint;
import mchorse.bbs_mod.ui.framework.UIContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/** Screen-space spline handles shared by the form, state and film viewports. */
public class SplineOverlay
{
    public record Hit(SplineIK chain, SplinePoint point, float x, float y, float depth) {}

    private final List<Hit> handles = new ArrayList<>();

    /** Shares the IK debug switch, including the dashboard's combined IK/physics shortcut. */
    public boolean begin()
    {
        this.handles.clear();
        return BBSSettings.ikDebug.enabled.get();
    }

    public Hit pick(int x, int y)
    {
        /* The shortcut can hide the overlay between its last draw and the next click. */
        if (!BBSSettings.ikDebug.enabled.get()) return null;
        Hit best = null;
        float distance = 100;
        for (Hit hit : this.handles)
        {
            float d = (hit.x - x) * (hit.x - x) + (hit.y - y) * (hit.y - y);
            if (d < distance || (d == distance && best != null && hit.depth < best.depth))
            {
                best = hit;
                distance = d;
            }
        }
        return best;
    }

    /** Called in the ordinary GUI pass, after the viewport restores its projection. */
    public void draw(UIContext context, Matrix4f parentModelView, Matrix4f projection, Area viewport, SplineIK chain, String selectedPointId)
    {
        Matrix4f matrix = new Matrix4f(projection).mul(parentModelView);
        List<Vector3f> positions = new ArrayList<>();
        for (SplinePoint point : chain.points.getAllTyped()) positions.add(new Vector3f(point.position.get().translate));
        if (positions.isEmpty()) return;
        int color = chain.enabled.get() ? 0xFF63D9E8 : 0xFF929AA6;
        context.batcher.clip(viewport, context);
        Vector3f previous = null;
        for (Vector3f point : SplineCurve.sample(positions, 20))
        {
            Vector3f current = project(matrix, viewport, point);
            if (previous != null && current != null) line(context, previous, current, color);
            previous = current;
        }
        for (SplinePoint point : chain.points.getAllTyped())
        {
            Vector3f p = project(matrix, viewport, point.position.get().translate);
            if (p == null || !viewport.isInside((int) p.x, (int) p.y)) continue;
            boolean selected = point.getId().equals(selectedPointId);
            float radius = selected ? 5 : 4;
            context.batcher.box(p.x - radius - 1, p.y - radius - 1, p.x + radius + 1, p.y + radius + 1, 0xFF15212B);
            context.batcher.box(p.x - radius, p.y - radius, p.x + radius, p.y + radius, selected ? 0xFFFFDA65 : color);
            this.handles.add(new Hit(chain, point, p.x, p.y, p.z));
        }
        context.batcher.unclip(context);
    }

    private static Vector3f project(Matrix4f matrix, Area viewport, Vector3f point)
    {
        Vector4f clip = matrix.transform(new Vector4f(point, 1));
        if (!Float.isFinite(clip.w) || clip.w <= 0.00001F) return null;
        clip.div(clip.w);
        if (!Float.isFinite(clip.x) || !Float.isFinite(clip.y) || clip.z < -1 || clip.z > 1) return null;
        return new Vector3f(viewport.x + (clip.x + 1) * viewport.w * 0.5F, viewport.y + (1 - clip.y) * viewport.h * 0.5F, clip.z);
    }

    private static void line(UIContext context, Vector3f from, Vector3f to, int color)
    {
        float dx = to.x - from.x;
        float dy = to.y - from.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (!Float.isFinite(length) || length < 0.01F) return;
        var matrices = context.batcher.getContext().getMatrices();
        matrices.push();
        matrices.translate(from.x, from.y, 0);
        matrices.multiply(new Quaternionf().rotationZ((float) Math.atan2(dy, dx)));
        context.batcher.box(0, -1, length, 1, color);
        matrices.pop();
    }
}

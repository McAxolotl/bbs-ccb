package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.cubic.chains.ChainControl;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.AutoBezier;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Animatable state only. Chain topology and the ordered point identities live in the rig. */
public class SplineControl extends ChainControl<SplineControl>
{
    public float influence = 1F;
    public float progress;
    public float twist;
    public final Map<String, Transform> points = new HashMap<>();

    public Transform point(String id) { return this.points.computeIfAbsent(id, key -> new Transform()); }

    @Override
    public SplineControl withDefaults(SplineControl defaults)
    {
        SplineControl result = this.copy();
        defaults.points.forEach((id, point) -> result.points.putIfAbsent(id, KeyframeFactories.TRANSFORM.copy(point)));
        return result;
    }

    @Override
    public void identity()
    {
        this.bindings.clear();
        this.influence = 1F;
        this.progress = this.twist = 0F;
        this.points.clear();
    }

    @Override
    public SplineControl copy()
    {
        SplineControl copy = new SplineControl();
        copy.copy(this);
        return copy;
    }

    @Override
    public void copy(SplineControl other)
    {
        if (other == this) return;
        this.copyBindings(other);
        this.influence = other.influence;
        this.progress = other.progress;
        this.twist = other.twist;
        this.points.clear();
        other.points.forEach((id, point) -> this.points.put(id, KeyframeFactories.TRANSFORM.copy(point)));
    }

    @Override
    public boolean isDefault()
    {
        return !this.hasBindingsOrMetadata() && this.influence == 1F && this.progress == 0F && this.twist == 0F && this.points.isEmpty();
    }

    @Override
    public boolean equals(Object object)
    {
        return object instanceof SplineControl other && this.sameBindings(other) && this.influence == other.influence
            && this.progress == other.progress && this.twist == other.twist && this.points.equals(other.points);
    }

    @Override
    public void lerp(SplineControl p, SplineControl a, SplineControl b, SplineControl q, IInterp interp, float x)
    {
        this.copyBindings(a);
        this.influence = (float) interp.interpolate(IInterp.context.set(p.influence, a.influence, b.influence, q.influence, x));
        this.progress = (float) interp.interpolate(IInterp.context.set(p.progress, a.progress, b.progress, q.progress, x));
        this.twist = (float) interp.interpolate(IInterp.context.set(p.twist, a.twist, b.twist, q.twist, x));
        for (String id : pointIds(p, a, b, q))
            this.point(id).copy(KeyframeFactories.TRANSFORM.interpolate(read(p, id), read(a, id), read(b, id), read(q, id), interp, x));
    }

    @Override
    public void autoLerp(SplineControl p, SplineControl a, SplineControl b, SplineControl q, float pt, float at, float bt, float qt, boolean clamped, float x)
    {
        this.copyBindings(a);
        this.influence = (float) AutoBezier.get(p.influence, a.influence, b.influence, q.influence, pt, at, bt, qt, clamped, x);
        this.progress = (float) AutoBezier.get(p.progress, a.progress, b.progress, q.progress, pt, at, bt, qt, clamped, x);
        this.twist = (float) AutoBezier.get(p.twist, a.twist, b.twist, q.twist, pt, at, bt, qt, clamped, x);
        for (String id : pointIds(p, a, b, q))
            this.point(id).autoLerp(read(p, id), read(a, id), read(b, id), read(q, id), pt, at, bt, qt, clamped, x);
    }

    private static Transform read(SplineControl value, String id)
    {
        return value.points.getOrDefault(id, new Transform());
    }

    private static Set<String> pointIds(SplineControl... values)
    {
        Set<String> ids = new HashSet<>();
        for (SplineControl value : values) ids.addAll(value.points.keySet());
        return ids;
    }

    @Override
    public void toData(MapType data)
    {
        this.writeBindings(data);
        data.putFloat("influence", this.influence);
        data.putFloat("progress", this.progress);
        data.putFloat("twist", this.twist);
        MapType points = new MapType();
        this.points.forEach((id, point) -> points.put(id, point.toData()));
        data.put("points", points);
    }

    @Override
    public void fromData(MapType data)
    {
        this.readBindings(data);
        this.influence = data.getFloat("influence", 1F);
        this.progress = data.getFloat("progress", 0F);
        this.twist = data.getFloat("twist", 0F);
        this.points.clear();
        MapType points = data.getMap("points");
        for (String id : points.keys()) this.point(id).fromData(points.getMap(id));
    }
}

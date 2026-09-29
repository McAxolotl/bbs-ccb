package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** Animation addresses are point IDs, never indices in the curve's topology. */
public class SplinePositions extends HashMap<String, Transform> implements IMapSerializable
{
    public Transform point(String id) { return this.computeIfAbsent(id, key -> new Transform()); }

    public SplinePositions copy()
    {
        SplinePositions result = new SplinePositions();
        this.forEach((id, value) -> result.put(id, KeyframeFactories.TRANSFORM.copy(value)));
        return result;
    }

    public SplinePositions withDefaults(SplinePositions defaults)
    {
        SplinePositions result = this.copy();
        defaults.forEach((id, value) -> result.putIfAbsent(id, KeyframeFactories.TRANSFORM.copy(value)));
        return result;
    }

    public void lerp(SplinePositions p, SplinePositions a, SplinePositions b, SplinePositions q, IInterp interp, float x)
    {
        this.clear();
        for (String id : ids(p, a, b, q))
            this.point(id).lerp(read(p, id), read(a, id), read(b, id), read(q, id), interp, x);
    }

    public void autoLerp(SplinePositions p, SplinePositions a, SplinePositions b, SplinePositions q,
        float pt, float at, float bt, float qt, boolean clamped, float x)
    {
        this.clear();
        for (String id : ids(p, a, b, q))
            this.point(id).autoLerp(read(p, id), read(a, id), read(b, id), read(q, id), pt, at, bt, qt, clamped, x);
    }

    private static Transform read(SplinePositions value, String id) { return value.getOrDefault(id, new Transform()); }

    private static Set<String> ids(SplinePositions... values)
    {
        Set<String> ids = new HashSet<>();
        for (SplinePositions value : values) ids.addAll(value.keySet());
        return ids;
    }

    @Override public void toData(MapType data) { this.forEach((id, value) -> data.put(id, value.toData())); }
    @Override public void fromData(MapType data)
    {
        this.clear();
        for (String id : data.keys()) this.point(id).fromData(data.getMap(id));
    }
}

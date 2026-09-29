package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.cubic.spline.SplinePositions;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class SplinePointsKeyframeFactory implements IKeyframeFactory<SplinePositions>
{
    @Override public SplinePositions withDefaults(SplinePositions value, SplinePositions defaults) { return value.withDefaults(defaults); }
    @Override public SplinePositions createEmpty() { return new SplinePositions(); }
    @Override public SplinePositions copy(SplinePositions value) { return value.copy(); }
    @Override public BaseType toData(SplinePositions value) { return value.toData(); }
    @Override public SplinePositions fromData(BaseType data)
    {
        SplinePositions value = this.createEmpty();
        if (data.isMap()) value.fromData(data.asMap());
        return value;
    }
    @Override public SplinePositions interpolate(SplinePositions p, SplinePositions a, SplinePositions b, SplinePositions q, IInterp interp, float x)
    {
        SplinePositions result = new SplinePositions();
        result.lerp(p, a, b, q, interp, x);
        return result;
    }
    @Override public SplinePositions interpolate(Keyframe<SplinePositions> p, Keyframe<SplinePositions> a,
        Keyframe<SplinePositions> b, Keyframe<SplinePositions> q, IInterp interp, float x)
    {
        if (!interp.has(Interpolations.AUTO) && !interp.has(Interpolations.AUTO_CLAMPED))
            return IKeyframeFactory.super.interpolate(p, a, b, q, interp, x);
        SplinePositions result = new SplinePositions();
        result.autoLerp(p.getValue(), a.getValue(), b.getValue(), q.getValue(), p.getTick(), a.getTick(),
            b.getTick(), q.getTick(), interp.has(Interpolations.AUTO_CLAMPED), x);
        return result;
    }
}

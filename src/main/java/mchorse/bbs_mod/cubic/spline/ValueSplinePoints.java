package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueStableList;

/** Point identity is independent of its position along the curve. */
public class ValueSplinePoints extends ValueStableList<SplinePoint>
{
    public ValueSplinePoints(String id)
    {
        super(id);
    }

    @Override
    public SplinePoint get(String id)
    {
        return (SplinePoint) super.get(id);
    }

    @Override
    protected SplinePoint create(String id)
    {
        return new SplinePoint(id);
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        this.fromData(group.toData());
    }
}

package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueStableList;

/** Point identity is independent of its position along the curve. */
public class ValueSplinePoints extends ValueStableList<SplinePoint>
{
    @Override
    public void add(SplinePoint point)
    {
        super.add(point);
        if (this.getParent() instanceof SplineSource spline) spline.bindPoint(point);
    }

    @Override
    public void add(int index, SplinePoint point)
    {
        super.add(index, point);
        if (this.getParent() instanceof SplineSource spline) spline.bindPoint(point);
    }
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

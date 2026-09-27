package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueStableList;

/** Stable chain ids keep animation addresses intact across list edits and form copies. */
public class ValueSplineIKs extends ValueStableList<SplineIK>
{
    public ValueSplineIKs(String id)
    {
        super(id);
    }

    @Override
    public SplineIK get(String id)
    {
        return (SplineIK) super.get(id);
    }

    @Override
    protected SplineIK create(String id)
    {
        return new SplineIK(id);
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        this.fromData(group.toData());
    }
}

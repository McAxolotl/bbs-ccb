package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueStableList;
import mchorse.bbs_mod.forms.forms.ModelForm;

/** Stable chain ids keep animation addresses intact across list edits and form copies. */
public class ValueSplineIKs extends ValueStableList<SplineIK>
{
    @Override
    public void add(SplineIK spline)
    {
        super.add(spline);
        this.bind(spline);
    }

    @Override
    public void add(int index, SplineIK spline)
    {
        super.add(index, spline);
        this.bind(spline);
    }

    private void bind(SplineIK spline)
    {
        if (this.getParent() instanceof ModelForm form) spline.bind(form);
    }
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

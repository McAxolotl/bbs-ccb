package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.forms.forms.ModelForm;

/** One form's curve and chain settings. The shared model asset never owns these values. */
public class SplineIK extends ValueGroup
{
    private ModelForm owner;

    public void bind(ModelForm owner)
    {
        if (this.owner != null) return;
        SplineControl initial = new SplineControl();
        initial.influence = this.influence.getOriginalValue();
        initial.progress = this.progress.getOriginalValue();
        initial.twist = this.twist.getOriginalValue();
        this.owner = owner;
        owner.splineIK.getOriginalValue().controls.put(this.getId(), initial);
        this.influence.bind(owner.splineIK, () -> this.original().influence, () -> this.state().influence, value -> this.original().influence = value);
        this.progress.bind(owner.splineIK, () -> this.original().progress, () -> this.state().progress, value -> this.original().progress = value);
        this.twist.bind(owner.splineIK, () -> this.original().twist, () -> this.state().twist, value -> this.original().twist = value);
        for (SplinePoint point : this.points.getAllTyped()) this.bindPoint(point);
    }

    public SplineControl original() { return this.owner.splineIK.getOriginalValue().get(this.getId()); }
    public SplineControl state() { return this.owner.splineIK.get().get(this.getId()); }

    public void bindPoint(SplinePoint point)
    {
        if (this.owner == null || point.position.isBound()) return;
        this.original().points.put(point.getId(), point.position.getOriginalValue());
        point.position.bind(this.owner.splineIK, () -> this.original().point(point.getId()),
            () -> this.state().point(point.getId()), value -> this.original().points.put(point.getId(), value));
    }

    public final ValueString name = new ValueString("name", "Spline IK");
    /** Number of bones including the tip; zero walks all the way to the root. */
    public final ValueInt chainLength = new ValueInt("chainLength", 0, 0, Integer.MAX_VALUE);
    public final ValueString tip = new ValueString("tip", "");
    public final ValueFloat influence = new ValueFloat("influence", 1F, 0F, 1F);
    /** Roll along the chain, in degrees. */
    public final ValueFloat twist = new ValueFloat("twist", 0F);
    /** Travel along the curve, in percent; negative values and travel beyond 100 are supported. */
    public final ValueFloat progress = new ValueFloat("progress", 0F);
    public final ValueBoolean moveModel = new ValueBoolean("moveModel", false);
    public final ValueBoolean fit = new ValueBoolean("fit", false);
    public final ValueSplinePoints points = new ValueSplinePoints("points");

    public SplineIK(String id)
    {
        super(id);
        this.add(this.name.animatable(false).invisible());
        this.add(this.chainLength.animatable(false).invisible());
        this.add(this.tip.animatable(false).invisible());
        this.add(this.influence);
        this.add(this.twist);
        this.add(this.progress);
        this.add(this.moveModel.animatable(false).invisible());
        this.add(this.fit.animatable(false).invisible());
        this.add(this.points);
    }
}

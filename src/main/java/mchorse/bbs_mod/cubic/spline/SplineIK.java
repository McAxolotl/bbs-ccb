package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;

/** One form's curve and chain settings. The shared model asset never owns these values. */
public class SplineIK extends ValueGroup
{
    public final ValueString name = new ValueString("name", "Spline IK");
    public final ValueString root = new ValueString("root", "");
    public final ValueString tip = new ValueString("tip", "");
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
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
        this.add(this.name.invisible());
        this.add(this.root.invisible());
        this.add(this.tip.invisible());
        this.add(this.enabled.invisible());
        this.add(this.influence);
        this.add(this.twist);
        this.add(this.progress);
        this.add(this.moveModel.invisible());
        this.add(this.fit.invisible());
        this.add(this.points);
    }
}

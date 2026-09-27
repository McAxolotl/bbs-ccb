package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.utils.pose.Transform;

/** A curve control point in blocks, relative to the chain root's parent frame. */
public class SplinePoint extends ValueGroup
{
    /** Only translation is used by Spline IK; the ordinary transform factory supplies keyframes and gizmos. */
    public final ValueTransform position = new ValueTransform("position", new Transform());

    public SplinePoint(String id)
    {
        super(id);
        this.add(this.position);
    }
}

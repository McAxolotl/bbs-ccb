package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.utils.pose.Transform;

/** A curve control point in blocks, local to its SplineSource. */
public class SplinePoint extends ValueGroup
{
    /** The same language-independent label in the form panel and animation tracks. */
    public static String displayName(int number)
    {
        return "point_" + number;
    }

    /** Only translation shapes the curve; the ordinary transform factory supplies keyframes and gizmos. */
    public final ValueTransform position = new ValueTransform("position", new Transform());

    public SplinePoint(String id)
    {
        super(id);
        this.add(this.position);
    }
}

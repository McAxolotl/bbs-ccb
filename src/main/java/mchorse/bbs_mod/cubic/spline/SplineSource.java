package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.utils.pose.Transform;

/** The small geometry contract shared by an IK chain and an independent path. */
public interface SplineSource
{
    ValueSplinePoints points();
    Transform position(String id);
    void bindPoint(SplinePoint point);
    default boolean closed() { return false; }
}

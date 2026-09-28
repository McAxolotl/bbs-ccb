package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.cubic.spline.SplineControl;
import mchorse.bbs_mod.cubic.spline.SplineControls;

public class SplineKeyframeFactory extends ChainKeyframeFactory<SplineControl, SplineControls>
{
    @Override public SplineControls createEmpty() { return new SplineControls(); }
}

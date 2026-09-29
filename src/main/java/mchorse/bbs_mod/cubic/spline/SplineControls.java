package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.cubic.chains.ChainControls;

public class SplineControls extends ChainControls<SplineControl, SplineControls>
{
    @Override protected SplineControls createControls() { return new SplineControls(); }
    @Override protected SplineControl createControl() { return new SplineControl(); }
    @Override protected String getDataKey() { return "spline_ik"; }
}

package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;

public class UIFloatKeyframeFactory extends UINumericKeyframeFactory<Float>
{
    public UIFloatKeyframeFactory(UITrackValue<Float> track, UIKeyframes editor)
    {
        super(track, editor);
    }

    @Override
    protected double getNumericValue(Float value)
    {
        return value;
    }

    @Override
    protected Float convertValue(double value)
    {
        return (float) value;
    }
}

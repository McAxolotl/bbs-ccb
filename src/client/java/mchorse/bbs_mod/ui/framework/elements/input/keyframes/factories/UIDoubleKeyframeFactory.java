package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;

public class UIDoubleKeyframeFactory extends UINumericKeyframeFactory<Double>
{
    public UIDoubleKeyframeFactory(UITrackValue<Double> track, UIKeyframes editor)
    {
        super(track, editor);
    }

    @Override
    protected double getNumericValue(Double value)
    {
        return value;
    }

    @Override
    protected Double convertValue(double value)
    {
        return value;
    }
}

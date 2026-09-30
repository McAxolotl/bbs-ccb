package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;

public class UIIntegerKeyframeFactory extends UINumericKeyframeFactory<Integer>
{
    public UIIntegerKeyframeFactory(UITrackValue<Integer> track, UIKeyframes editor)
    {
        super(track, editor);
        this.value.integer();
    }

    @Override
    protected double getNumericValue(Integer value)
    {
        return value;
    }

    @Override
    protected Integer convertValue(double value)
    {
        return (int) value;
    }
}

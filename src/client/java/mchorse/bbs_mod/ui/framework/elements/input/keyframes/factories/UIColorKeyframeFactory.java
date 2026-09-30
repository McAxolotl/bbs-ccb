package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;

public class UIColorKeyframeFactory extends UIKeyframeFactory<Color>
{
    private UIColorPicker color;

    public UIColorKeyframeFactory(UITrackValue<Color> track, UIKeyframes editor)
    {
        super(track, editor);

        this.color = new UIColorPicker((c) -> this.setValue(Color.rgba(c))).embedded().editAlpha();
        this.color.setColor(track.getValue().getARGBColor());

        this.scroll.add(this.color);
    }
    @Override
    public void update()
    {
        this.color.setColor(this.getDisplayValue().getARGBColor());
    }

}

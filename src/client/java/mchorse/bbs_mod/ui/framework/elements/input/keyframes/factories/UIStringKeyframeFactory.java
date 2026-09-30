package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;

public class UIStringKeyframeFactory extends UIKeyframeFactory<String>
{
    private UITextbox string;

    public UIStringKeyframeFactory(UITrackValue<String> track, UIKeyframes editor)
    {
        super(track, editor);

        this.string = new UITextbox(1000, this::setValue);
        this.string.setText(track.getValue());

        this.scroll.add(this.string);
    }
    @Override
    public void update()
    {
        if (!this.string.isFocused()) this.string.setText(this.getDisplayValue());
    }

}
package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;

public class UIBooleanKeyframeFactory extends UIKeyframeFactory<Boolean>
{
    private UIToggle toggle;

    public UIBooleanKeyframeFactory(UITrackValue<Boolean> track, UIKeyframes editor)
    {
        super(track, editor);

        this.toggle = new UIToggle(UIKeys.GENERIC_KEYFRAMES_BOOLEAN_TRUE, (b) -> this.setValue(b.getValue()));
        this.toggle.setValue(track.getValue());

        this.keys().register(Keys.TRANSFORMATIONS_TRANSLATE, () ->
        {
            this.setValue(!this.getDisplayValue());
            this.update();
        }).category(UIKeys.TRANSFORMS_KEYS_CATEGORY);

        this.scroll.add(this.toggle);
    }
    @Override
    public void update()
    {
        this.toggle.setValue(this.getDisplayValue());
    }

}

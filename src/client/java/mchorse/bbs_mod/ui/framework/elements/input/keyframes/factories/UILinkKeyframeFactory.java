package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;

public class UILinkKeyframeFactory extends UIKeyframeFactory<Link>
{
    public UILinkKeyframeFactory(UITrackValue<Link> track, UIKeyframes editor)
    {
        super(track, editor);

        this.scroll.add(new UIButton(UIKeys.GENERIC_KEYFRAMES_LINK_PICK_TEXTURE, (b) ->
        {
            UITexturePicker.open(this.getContext(), this.track.getValue(), (l) ->
            {
                this.setValue(l);
            });
        }));
    }
}
package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import org.joml.Vector4f;

public class UIVector4fKeyframeFactory extends UIKeyframeFactory<Vector4f>
{
    private UITrackpad x;
    private UITrackpad y;
    private UITrackpad z;
    private UITrackpad w;

    public UIVector4fKeyframeFactory(UITrackValue<Vector4f> track, UIKeyframes editor)
    {
        super(track, editor);

        Vector4f value = track.getValue();

        this.x = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.x.setValue(value.x);
        this.y = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.y.setValue(value.y);
        this.z = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.z.setValue(value.z);
        this.w = new UITrackpad((v) -> this.setValue(this.getValue()));
        this.w.setValue(value.w);

        this.scroll.add(UI.row(this.x, this.y), UI.row(this.z, this.w));
    }

    private Vector4f getValue()
    {
        return new Vector4f(
            (float) this.x.getValue(), (float) this.y.getValue(),
            (float) this.z.getValue(), (float) this.w.getValue()
        );
    }
    @Override
    public void update()
    {
        var v = this.getDisplayValue();
        if (!this.x.isUserEditing()) this.x.setValue(v.x);
        if (!this.y.isUserEditing()) this.y.setValue(v.y);
        if (!this.z.isUserEditing()) this.z.setValue(v.z);
        if (!this.w.isUserEditing()) this.w.setValue(v.w);
    }

}
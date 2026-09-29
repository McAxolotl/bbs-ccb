package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.cubic.ik.IKControl;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import java.util.function.Consumer;

/** Shared value editor fields; the caller owns selection, storage and notifications. */
public class UIIKControlFields
{
    public final UISliderTrackpad weight;
    public final UISliderTrackpad softness;
    public final UISliderTrackpad poleAngle;

    public UIIKControlFields(Consumer<Consumer<IKControl>> edit)
    {
        this.weight = new UISliderTrackpad(v -> edit.accept(c -> c.weight = v.floatValue()));
        this.weight.normalized().tooltip(UIKeys.FORMS_EDITORS_MODEL_IK_WEIGHT);
        this.softness = new UISliderTrackpad(v -> edit.accept(c -> c.softness = v.floatValue()));
        this.softness.normalized().tooltip(UIKeys.FORMS_EDITORS_MODEL_IK_SOFTNESS);
        this.poleAngle = new UISliderTrackpad(v -> edit.accept(c -> c.poleAngle = v.floatValue()));
        this.poleAngle.angle180().tooltip(UIKeys.FORMS_EDITORS_MODEL_IK_POLE_ANGLE);
    }
}

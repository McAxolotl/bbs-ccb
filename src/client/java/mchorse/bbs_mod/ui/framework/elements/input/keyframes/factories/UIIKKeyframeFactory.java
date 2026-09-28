package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.ik.IKControl;
import mchorse.bbs_mod.cubic.ik.IKControls;
import mchorse.bbs_mod.forms.forms.utils.FormBone;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.ui.utils.UIIKControlFields;

/**
 * Editor for the {@code ik} keyframe track: chains are keyed by their tip bone.
 */
public class UIIKKeyframeFactory extends UIChainKeyframeFactory<IKControl, IKControls>
{
    public UISliderTrackpad weight;
    public UISliderTrackpad softness;
    public UISliderTrackpad poleAngle;
    public UIToggle enabled;
    public UIToggle pole;

    public UIIKKeyframeFactory(Keyframe<IKControls> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        var fields = new UIIKControlFields(this::edit);
        this.weight = this.input(fields.weight);
        this.softness = this.input(fields.softness);
        this.poleAngle = this.input(fields.poleAngle);

        this.enabled = this.input(new UIToggle(UIKeys.FORMS_EDITORS_MODEL_IK_ENABLED, (b) -> this.edit((control) -> control.enabled = b.getValue())));
        this.pole = this.input(new UIToggle(UIKeys.FORMS_EDITORS_MODEL_IK_POLE, (b) -> this.edit((control) -> control.pole = b.getValue())));

        this.setup(
            UI.labelRow(UIKeys.FORMS_EDITORS_MODEL_IK_WEIGHT, this.weight),
            UI.labelRow(UIKeys.FORMS_EDITORS_MODEL_IK_SOFTNESS, this.softness),
            UI.labelRow(UIKeys.FORMS_EDITORS_MODEL_IK_POLE_ANGLE, this.poleAngle),
            this.enabled,
            this.pole,
            this.binding("target", UIKeys.FORMS_EDITORS_MODEL_IK_TARGET_LABEL, UIKeys.FORMS_EDITORS_MODEL_IK_TARGET),
            this.binding("pole", UIKeys.FORMS_EDITORS_MODEL_IK_POLE_TARGET_LABEL, UIKeys.FORMS_EDITORS_MODEL_IK_POLE_TARGET)
        );
    }

    @Override
    protected boolean hasChain(FormBone bone)
    {
        return bone.hasChain();
    }

    @Override
    protected void sync(IKControl control)
    {
        this.weight.setValue(control.weight);
        this.softness.setValue(control.softness);
        this.poleAngle.setValue(control.poleAngle);
        this.enabled.setValue(control.enabled);
        this.pole.setValue(control.pole);
    }

    @Override
    protected IKControl configControl(String bone)
    {
        FormBone formBone = this.form == null ? null : this.form.bones.getBone(bone);

        return formBone == null ? new IKControl() : formBone.ik.getOriginalValue().copy();
    }
}

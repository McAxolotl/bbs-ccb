package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.physics.PhysicsControl;
import mchorse.bbs_mod.cubic.physics.PhysicsControls;
import mchorse.bbs_mod.forms.forms.utils.FormBone;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.ui.utils.UIPhysicsControlFields;

/**
 * Editor for the {@code physics} keyframe track: chains are keyed by their root bone.
 */
public class UIPhysicsKeyframeFactory extends UIChainKeyframeFactory<PhysicsControl, PhysicsControls>
{
    public UISliderTrackpad weight;
    public UISliderTrackpad gravity;
    public UISliderTrackpad damping;
    public UISliderTrackpad stiffness;

    public UIPhysicsKeyframeFactory(Keyframe<PhysicsControls> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        var fields = new UIPhysicsControlFields(this::edit);
        this.weight = this.input(fields.weight);
        this.gravity = this.input(fields.gravity);
        this.damping = this.input(fields.damping);
        this.stiffness = this.input(fields.stiffness);


        this.setup(
            UI.labelRow(UIKeys.FORMS_EDITORS_MODEL_IK_WEIGHT, this.weight),
            UI.labelRow(UIKeys.FORMS_EDITORS_MODEL_PHYSICS_GRAVITY, this.gravity),
            UI.labelRow(UIKeys.FORMS_EDITORS_MODEL_PHYSICS_DAMPING, this.damping),
            UI.labelRow(UIKeys.FORMS_EDITORS_MODEL_PHYSICS_STIFFNESS, this.stiffness),
            this.binding("target", UIKeys.FORMS_EDITORS_MODEL_IK_TARGET_LABEL, UIKeys.FORMS_EDITORS_MODEL_IK_TARGET)
        );
    }

    @Override
    protected boolean hasChain(FormBone bone)
    {
        return bone.hasPhysicsChain();
    }

    @Override
    protected void sync(PhysicsControl control)
    {
        this.weight.setValue(control.weight);
        this.gravity.setValue(control.gravity);
        this.damping.setValue(control.damping);
        this.stiffness.setValue(control.stiffness);
    }

    @Override
    protected PhysicsControl configControl(String bone)
    {
        FormBone formBone = this.form == null ? null : this.form.bones.getBone(bone);

        return formBone == null ? new PhysicsControl() : formBone.physics.getOriginalValue().copy();
    }
}

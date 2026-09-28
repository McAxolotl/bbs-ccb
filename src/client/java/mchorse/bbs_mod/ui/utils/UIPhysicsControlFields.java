package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.cubic.physics.PhysicsControl;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import java.util.function.Consumer;

public class UIPhysicsControlFields
{
    public final UISliderTrackpad weight;
    public final UISliderTrackpad gravity;
    public final UISliderTrackpad damping;
    public final UISliderTrackpad stiffness;

    public UIPhysicsControlFields(Consumer<Consumer<PhysicsControl>> edit)
    {
        this.weight = new UISliderTrackpad(v -> edit.accept(c -> c.weight = v.floatValue()));
        this.weight.normalized().tooltip(UIKeys.FORMS_EDITORS_MODEL_IK_WEIGHT);
        this.gravity = new UISliderTrackpad(v -> edit.accept(c -> c.gravity = v.floatValue()));
        this.gravity.onlyNumbers().values(0.1D, 0.01D, 0.5D).increment(0.25D).limit(0D, 10D);
        this.gravity.tooltip(UIKeys.FORMS_EDITORS_MODEL_PHYSICS_GRAVITY);
        this.damping = new UISliderTrackpad(v -> edit.accept(c -> c.damping = v.floatValue()));
        this.damping.normalized().tooltip(UIKeys.FORMS_EDITORS_MODEL_PHYSICS_DAMPING);
        this.stiffness = new UISliderTrackpad(v -> edit.accept(c -> c.stiffness = v.floatValue()));
        this.stiffness.normalized().tooltip(UIKeys.FORMS_EDITORS_MODEL_PHYSICS_STIFFNESS);
    }
}

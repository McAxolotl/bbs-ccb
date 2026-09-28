package mchorse.bbs_mod.film.replays.tracks;

import mchorse.bbs_mod.film.replays.tracks.behaviours.TargetTrack;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.utils.FormBone;
import mchorse.bbs_mod.settings.values.base.BaseValue;

/** Resolve evaluated bindings only when the caller owns world-target lifetime. */
public final class SolverBindings
{
    private SolverBindings() {}

    public static void apply(TrackContext context)
    {
        if (context.solvers() && context.anchors() != null) apply(context.root(), context);
    }

    private static void apply(Form form, TrackContext context)
    {
        if (form == null) return;
        if (form instanceof ModelForm model)
        {
            for (BaseValue value : model.bones.getAll())
            {
                if (!(value instanceof FormBone bone)) continue;
                if (bone.hasChain())
                {
                    var control = model.ik.get().get(bone.getId());
                    String target = bone.ikTarget.get();
                    String pole = bone.ikPoleTarget.get();
                    /* A saved independent target curve keeps its precedence. */
                    if (!model.ikTargetOverrides.containsKey(target))
                        TargetTrack.applyAnchor(context, control.binding("target"), target, model.ikTargetOverrides, model.ikTargetWeights);
                    if (!pole.isEmpty() && !model.poleTargetOverrides.containsKey(pole))
                        TargetTrack.applyAnchor(context, control.binding("pole"), pole, model.poleTargetOverrides, model.poleTargetWeights);
                }
                if (bone.hasPhysicsChain() && !model.physicsTargetOverrides.containsKey(bone.getId()))
                    TargetTrack.applyAnchor(context, model.physics.get().get(bone.getId()).binding("target"), bone.getId(), model.physicsTargetOverrides, model.physicsTargetWeights);
            }
        }
        for (var part : form.parts.getAllTyped()) apply(part.getForm(), context);
    }
}

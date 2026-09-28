package mchorse.bbs_mod.film.replays.tracks.compatibility;

import mchorse.bbs_mod.film.replays.tracks.*;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.cubic.ik.IKControl;
import mchorse.bbs_mod.cubic.ik.IKControls;
import mchorse.bbs_mod.cubic.physics.PhysicsControl;
import mchorse.bbs_mod.cubic.physics.PhysicsControls;

/** Old solver curves keep their interpolation and film-only applicability. */
public class LegacyControlsTrack implements TrackBehaviour
{
    private final TrackKind kind;

    public LegacyControlsTrack(TrackKind kind) { this.kind = kind; }

    @Override
    public IKeyframeFactory factory(TrackId track)
    {
        return switch (this.kind)
        {
            case IK_CONTROLS -> KeyframeFactories.IK;
            case PHYSICS_CONTROLS -> KeyframeFactories.PHYSICS;
            default -> KeyframeFactories.WIND;
        };
    }

    private BaseValueBasic property(ModelForm form)
    {
        return switch (this.kind)
        {
            case IK_CONTROLS -> form.ik;
            case PHYSICS_CONTROLS -> form.physics;
            default -> form.wind;
        };
    }

    @Override
    public void apply(TrackContext context, TrackId track, KeyframeChannel channel, float tick, float blend)
    {
        if (!context.solvers() || !(FormUtils.getForm(context.root(), track.formPath()) instanceof ModelForm form)) return;
        var segment = channel.find(tick);
        Object value = segment == null ? null : segment.createInterpolated();
        /* Old factories omitted default entries from snapshots, including interpolated results. */
        if (value instanceof IKControls controls)
            controls.controls.values().removeIf(IKControl::isDefault);
        if (value instanceof PhysicsControls controls)
            controls.controls.values().removeIf(PhysicsControl::isDefault);
        this.property(form).setRuntimeValue(value);
    }

    @Override
    public void reset(Form root, TrackId track)
    {
        if (FormUtils.getForm(root, track.formPath()) instanceof ModelForm form) this.property(form).setRuntimeValue(null);
    }
}

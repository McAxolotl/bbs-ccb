package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.cubic.chains.ChainControl;
import mchorse.bbs_mod.cubic.chains.ChainControls;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.HashSet;
import java.util.Set;

/**
 * Keyframe factory for a solver track's per-chain scalars. Interpolation runs over the union of
 * the chains the surrounding keyframes mention, the way the pose track interpolates over bones,
 * so a chain that appears on only one side still eases in from its defaults.
 */
public abstract class ChainKeyframeFactory <C extends ChainControl<C>, S extends ChainControls<C, S>> implements IKeyframeFactory<S>
{
    private final Set<String> keys = new HashSet<>();

    /** Reused across frames: interpolation happens every frame and its result is read at once. */
    private final S interpolated = this.createEmpty();

    @Override
    public S fromData(BaseType data)
    {
        S controls = this.createEmpty();

        if (data.isMap())
        {
            controls.fromData(data.asMap());
        }

        return controls;
    }

    @Override
    public BaseType toData(S value)
    {
        return value.toData();
    }

    @Override
    public S copy(S value)
    {
        return value.copy();
    }

    @Override
    public S withDefaults(S value, S defaults)
    {
        S result = defaults.copy();
        result.overlayMetadata(value);
        value.controls.forEach((id, control) -> result.controls.put(id,
            defaults.controls.containsKey(id) ? control.withDefaults(defaults.controls.get(id)) : control.copy()));
        return result;
    }

    @Override
    public S blend(S current, S sampled, float amount)
    {
        S result = this.copy(this.interpolate(current, current, sampled, sampled, Interpolations.LINEAR, amount));
        for (String key : result.controls.keySet())
        {
            C from = this.read(current, key);
            C to = this.read(sampled, key);
            Set<String> bindings = new HashSet<>(from.bindings.keySet());
            bindings.addAll(to.bindings.keySet());
            for (String id : bindings)
            {
                var anchor = KeyframeFactories.ANCHOR.copy(to.binding(id));
                anchor.blendSource = KeyframeFactories.ANCHOR.copy(from.binding(id));
                anchor.blendWeight = amount;
                result.get(key).bindings.put(id, anchor);
            }
        }
        return result;
    }

    @Override
    public S interpolate(Keyframe<S> preA, Keyframe<S> a, Keyframe<S> b, Keyframe<S> postB, IInterp interpolation, float x)
    {
        if (interpolation.has(Interpolations.AUTO) || interpolation.has(Interpolations.AUTO_CLAMPED))
        {
            S preAp = preA.getValue();
            S ap = a.getValue();
            S bp = b.getValue();
            S postBp = postB.getValue();

            this.collect(preAp, ap, bp, postBp);

            boolean clamped = interpolation.has(Interpolations.AUTO_CLAMPED);
            float pt = preA.getTick();
            float at = a.getTick();
            float bt = b.getTick();
            float qt = postB.getTick();

            for (String key : this.keys)
            {
                this.interpolated.get(key).autoLerp(this.read(preAp, key), this.read(ap, key), this.read(bp, key), this.read(postBp, key), pt, at, bt, qt, clamped, x);
            }

            return this.interpolated;
        }

        return IKeyframeFactory.super.interpolate(preA, a, b, postB, interpolation, x);
    }

    @Override
    public S interpolate(S preA, S a, S b, S postB, IInterp interpolation, float x)
    {
        this.collect(preA, a, b, postB);

        for (String key : this.keys)
        {
            this.interpolated.get(key).lerp(this.read(preA, key), this.read(a, key), this.read(b, key), this.read(postB, key), interpolation, x);
        }

        return this.interpolated;
    }

    private void collect(S preA, S a, S b, S postB)
    {
        this.keys.clear();

        if (preA != a && preA != null) this.keys.addAll(preA.controls.keySet());
        if (a != null) this.keys.addAll(a.controls.keySet());
        if (b != null) this.keys.addAll(b.controls.keySet());
        if (postB != b && postB != null) this.keys.addAll(postB.controls.keySet());

        this.interpolated.controls.clear();
        if (a != null) this.interpolated.copyMetadata(a);
    }

    private C read(S value, String key)
    {
        C control = value == null ? null : value.controls.get(key);
        return control == null ? this.createEmpty().get(key) : control;
    }
}

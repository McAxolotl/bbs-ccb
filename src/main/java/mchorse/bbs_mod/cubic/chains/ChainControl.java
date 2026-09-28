package mchorse.bbs_mod.cubic.chains;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.cubic.ik.IKControl;
import mchorse.bbs_mod.cubic.physics.PhysicsControl;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

/**
 * The animatable scalars of a single solver chain, layered over the form's own config at
 * playback (the chain structure stays on the config). The element of a {@link ChainControls}
 * keyframe value — see {@link IKControl} and
 * {@link PhysicsControl}.
 */
public abstract class ChainControl <C extends ChainControl<C>> implements IMapSerializable
{
    public final java.util.Map<String, Anchor> bindings = new java.util.HashMap<>();
    private MapType extra = new MapType();

    public Anchor binding(String id)
    {
        return this.bindings.getOrDefault(id, new Anchor());
    }

    protected void copyBindings(ChainControl<?> other)
    {
        if (other == this) return;
        this.extra = (MapType) other.extra.copy();
        this.bindings.clear();
        other.bindings.forEach((id, anchor) -> this.bindings.put(id, KeyframeFactories.ANCHOR.copy(anchor)));
    }

    protected boolean sameBindings(ChainControl<?> other)
    {
        return this.bindings.equals(other.bindings) && this.extra.equals(other.extra);
    }

    protected boolean hasBindingsOrMetadata()
    {
        return !this.bindings.isEmpty() || !this.extra.isEmpty();
    }

    protected void readBindings(MapType data)
    {
        this.extra = new MapType();
        for (String key : data.keys())
            if (key.contains(":")) this.extra.put(key, data.get(key).copy());
        this.bindings.clear();
        var map = data.getMap("bindings");
        for (String id : map.keys()) this.bindings.put(id, KeyframeFactories.ANCHOR.fromData(map.get(id)));
    }

    protected void writeBindings(MapType data)
    {
        data.combine(this.extra);
        var map = new MapType();
        this.bindings.forEach((id, anchor) -> map.put(id, anchor.toData()));
        if (!map.isEmpty()) data.put("bindings", map);
        else data.remove("bindings");
    }

    protected void lerpBindings(ChainControl<?> p, ChainControl<?> a, ChainControl<?> b, ChainControl<?> q,
        IInterp interpolation, float x, float pt, float at, float bt, float qt, boolean timed)
    {
        this.copyBindings(a);
        java.util.Set<String> ids = new java.util.HashSet<>(a.bindings.keySet());
        ids.addAll(b.bindings.keySet());
        var factory = KeyframeFactories.ANCHOR;
        for (String id : ids)
        {
            var value = timed ? factory.interpolate(
                new Keyframe<>("p", factory, pt, p.binding(id)),
                new Keyframe<>("a", factory, at, a.binding(id)),
                new Keyframe<>("b", factory, bt, b.binding(id)),
                new Keyframe<>("q", factory, qt, q.binding(id)), interpolation, x)
                : factory.interpolate(p.binding(id), a.binding(id), b.binding(id), q.binding(id), interpolation, x);
            this.bindings.put(id, factory.copy(value));
        }
    }
    /** Reset to the defaults — what a chain the keyframe doesn't mention behaves like. */
    public abstract void identity();

    public abstract C copy();

    /** Complete a sparse element against the authored state without modifying either input. */
    public C withDefaults(C defaults) { return this.copy(); }

    public abstract void copy(C other);

    public abstract boolean isDefault();

    /** Ease this control from {@code a} to {@code b}; the flags step, so they take {@code a}'s. */
    public abstract void lerp(C preA, C a, C b, C postB, IInterp interp, float x);

    public abstract void autoLerp(C preA, C a, C b, C postB, float pt, float at, float bt, float qt, boolean clamped, float x);
}

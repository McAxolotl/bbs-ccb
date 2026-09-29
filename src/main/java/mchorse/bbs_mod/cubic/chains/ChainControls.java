package mchorse.bbs_mod.cubic.chains;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.HashMap;
import java.util.Map;
import mchorse.bbs_mod.utils.pose.Pose;

/**
 * Keyframe value holding the per-chain {@link ChainControl} scalars, keyed by the bone that
 * names the chain. Mirrors {@link Pose} (its
 * {@code Map<bone, PoseTransform>}), so the ordinary keyframe-track path handles a solver
 * track with the same union-of-keys interpolation as the pose track.
 */
public abstract class ChainControls <C extends ChainControl<C>, S extends ChainControls<C, S>> implements IMapSerializable
{
    public final Map<String, C> controls = new HashMap<>();
    private MapType extra = new MapType();

    public void copyMetadata(ChainControls<?, ?> other)
    {
        this.extra = (MapType) other.extra.copy();
    }

    public void overlayMetadata(ChainControls<?, ?> other)
    {
        this.extra.combine((MapType) other.extra.copy());
    }

    /** An empty container of this kind. */
    protected abstract S createControls();

    protected abstract C createControl();

    /** The key this container's map is stored under, and the name of the track it drives. */
    protected abstract String getDataKey();

    public C get(String bone)
    {
        C control = this.controls.get(bone);

        if (control == null)
        {
            control = this.createControl();

            this.controls.put(bone, control);
        }

        return control;
    }

    public S copy()
    {
        S controls = this.createControls();

        controls.copy(this);

        return controls;
    }

    public void copy(ChainControls<C, S> other)
    {
        if (other == this) return;
        this.copyMetadata(other);
        this.controls.clear();

        for (Map.Entry<String, C> entry : other.controls.entrySet())
        {
            this.controls.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public boolean isEmpty()
    {
        return this.controls.isEmpty();
    }

    @Override
    public void toData(MapType data)
    {
        data.combine(this.extra);
        MapType map = new MapType();

        for (Map.Entry<String, C> entry : this.controls.entrySet())
        {
            map.put(entry.getKey(), entry.getValue().toData());
        }

        data.put(this.getDataKey(), map);
    }

    @Override
    public void fromData(MapType data)
    {
        this.controls.clear();
        this.extra = new MapType();
        for (String key : data.keys())
            if (key.contains(":")) this.extra.put(key, data.get(key).copy());

        MapType map = data.getMap(this.getDataKey());

        for (String key : map.keys())
        {
            C control = this.createControl();

            control.fromData(map.getMap(key));

            this.controls.put(key, control);
        }
    }

    /** Presence matters: an omitted element uses the form's source, an explicit default overrides it. */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null || obj.getClass() != this.getClass())
        {
            return false;
        }

        ChainControls<C, S> other = (ChainControls<C, S>) obj;

        return this.controls.equals(other.controls) && this.extra.equals(other.extra);
    }
}

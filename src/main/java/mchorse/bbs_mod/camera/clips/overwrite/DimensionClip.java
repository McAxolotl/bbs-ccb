package mchorse.bbs_mod.camera.clips.overwrite;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValuePosition;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dimension fixture / clip.
 *
 * <p>Specifies the target Minecraft dimension for the duration of the clip, while providing
 * a baseline camera position for flight mode editing and layering.</p>
 */
public class DimensionClip extends CameraClip
{
    public static final String CLIP_DATA_KEY = "bbs:dimension";

    public final ValueLink dimension = new ValueLink("dimension", Link.create("minecraft:overworld"));
    public final ValuePosition position = new ValuePosition("position");

    public DimensionClip()
    {
        super();

        this.envelope.enabled.set(false);
        this.add(this.dimension);
        this.add(this.position);
    }

    @Override
    public void fromCamera(Camera camera)
    {
        this.position.get().set(camera);
    }

    public static Map<String, Link> getDimensionData(ClipContext context)
    {
        return context.clipData.get(CLIP_DATA_KEY, ConcurrentHashMap::new);
    }

    public static Link getActiveDimension(ClipContext context)
    {
        Map<String, Link> map = getDimensionData(context);

        return map.get("current");
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        getDimensionData(context).put("current", this.dimension.get());
        position.copy(this.position.get());
    }

    @Override
    protected Clip create()
    {
        return new DimensionClip();
    }
}

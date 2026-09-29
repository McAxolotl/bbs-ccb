package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.clips.overwrite.SplineClip;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import org.joml.Vector3d;

public class SplineClientClip extends SplineClip
{
    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        if (!(context instanceof CameraClipContext camera)) return;
        TrackerFrame frame = TrackerFrame.resolvePath(camera.entities, this,
            context.relativeTick + context.transition, position.point.x, position.point.y, position.point.z, context.transition);
        if (frame == null) return;

        Vector3d point = frame.position(this.offset.get());
        Angle angle = frame.angles(this.angle.get());
        position.point.x = this.isActive(0) ? point.x : position.point.x;
        position.point.y = this.isActive(1) ? point.y : position.point.y;
        position.point.z = this.isActive(2) ? point.z : position.point.z;
        position.angle.yaw = this.isActive(3) ? angle.yaw : position.angle.yaw;
        position.angle.pitch = this.isActive(4) ? angle.pitch : position.angle.pitch;
        position.angle.roll = this.isActive(5) ? angle.roll : position.angle.roll;
        position.angle.fov = this.isActive(6) ? this.fov.get() : position.angle.fov;
    }

    @Override
    protected Clip create()
    {
        return new SplineClientClip();
    }
}

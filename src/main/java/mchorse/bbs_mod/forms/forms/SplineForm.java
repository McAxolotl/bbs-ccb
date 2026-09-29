package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import mchorse.bbs_mod.cubic.spline.*;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;

/** Editor-only path geometry. Rendering it never produces scene geometry. */
public class SplineForm extends Form implements SplineSource
{
    public final BaseKeyframeFactoryValue<SplinePositions> curve = new BaseKeyframeFactoryValue<>("curve", KeyframeFactories.SPLINE_POINTS, new SplinePositions());
    public final ValueSplinePoints points = new ValueSplinePoints("points");
    public final ValueBoolean closed = new ValueBoolean("closed", false);

    public SplineForm()
    {
        this.add(this.curve);
        this.add(this.points);
        this.add(this.closed.animatable(false));
        for (int i = 0; i < 2; i++)
        {
            SplinePoint point = new SplinePoint("");
            point.position.getOriginalValue().translate.set(0, 0, i * 2);
            this.points.add(point);
        }
    }

    @Override public Icon getIcon() { return Icons.GRAPH; }

    @Override public ValueSplinePoints points() { return this.points; }
    @Override public boolean closed() { return this.closed.get(); }
    @Override public Transform position(String id)
    {
        return this.curve.get().getOrDefault(id, this.curve.getOriginalValue().point(id));
    }
    @Override public void bindPoint(SplinePoint point)
    {
        if (point.position.isBound()) return;
        this.curve.getOriginalValue().put(point.getId(), point.position.getOriginalValue());
        point.position.bind(this.curve, () -> this.curve.getOriginalValue().point(point.getId()),
            () -> this.position(point.getId()), value -> this.curve.getOriginalValue().put(point.getId(), value));
    }
    @Override public void fromData(BaseType data)
    {
        super.fromData(data);
        if (data.isMap() && data.asMap().has("curve")) this.curve.fromData(data.asMap().getMap("curve"));
    }
    @Override protected BaseType serializeChild(BaseValue value)
    {
        return value == this.points ? this.points.toData(point -> new MapType()) : super.serializeChild(value);
    }
}

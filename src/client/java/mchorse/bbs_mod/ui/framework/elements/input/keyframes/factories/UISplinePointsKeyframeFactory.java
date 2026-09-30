package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.cubic.spline.*;
import mchorse.bbs_mod.forms.forms.SplineForm;
import mchorse.bbs_mod.film.replays.tracks.TrackId;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.*;
import mchorse.bbs_mod.ui.utils.*;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3f;
import java.util.function.Consumer;

public class UISplinePointsKeyframeFactory extends UIKeyframeFactory<SplinePositions> implements SplineKeyframeEditor
{
    @Override
    public Transform getGizmoTransform(SplinePositions value)
    {
        if (this.points.point() == null) return null;
        value.putAll(this.resolved(value));
        return value.point(this.points.pointId());
    }

    private final SplineForm form;
    private final UISplinePointsEditor points;

    public UISplinePointsKeyframeFactory(UITrackValue<SplinePositions> track, UIKeyframes editor)
    {
        super(track, editor);
        UIKeyframeSheet sheet = track.sheet;
        this.form = sheet != null && sheet.form instanceof SplineForm spline ? spline : null;
        UIKeyframePropTransform transform = new UIKeyframePropTransform()
        {
            @Override protected UIKeyframes getKeyframes() { return editor; }
            @Override protected void applyToSelection(Consumer<Transform> edit)
            {
                track.edit(selected ->
                {
                    SplinePositions value = UISplinePointsKeyframeFactory.this.resolved((SplinePositions) selected);
                    for (String id : UISplinePointsKeyframeFactory.this.points.selected()) edit.accept(value.point(id));
                    ((SplinePositions) selected).putAll(value);
                });
            }

        };
        this.points = new UISplinePointsEditor(() -> this.form, id -> this.resolved(this.getDisplayValue()).point(id), transform, Vector3f::new)
        {
            @Override protected void editStructure(Consumer<ValueSplinePoints> edit)
            {
                editor.editForm(UISplinePointsKeyframeFactory.this.form, () -> edit.accept(UISplinePointsKeyframeFactory.this.form.points));
            }
            @Override protected void editPositions(Consumer<SplinePositions> edit)
            {
                track.edit(selected ->
                {
                    SplinePositions value = UISplinePointsKeyframeFactory.this.resolved((SplinePositions) selected);
                    edit.accept(value);
                    ((SplinePositions) selected).putAll(value);
                });
            }
        };
        transform.enableHotkeys(() -> this.points.point() != null, op -> op == TransformOp.TRANSLATE);
        this.scroll.add(this.points);
        if (this.form != null)
        {
            var closed = new UIToggle(
                L10n.lang("bbs.ui.spline.closed"),
                button -> editor.editForm(this.form, () -> this.form.closed.set(button.getValue())));
            closed.setValue(this.form.closed.get());
            this.scroll.add(closed);
        }
        this.points.refresh();
        if (this.form != null && !this.form.points.getAllTyped().isEmpty()) this.points.select(this.form.points.getAllTyped().get(0).getId());
    }
    private SplinePositions resolved(SplinePositions value)
    { return this.form == null ? value.copy() : value.withDefaults(this.form.curve.getOriginalValue()); }
    @Override public UISplinePointsEditor pointEditor() { return this.points; }
    @Override public SplineSource source() { return this.form; }
    @Override public void selectPoint(String path)
    {
        TrackId id = TrackId.parse(path);
        if (id != null) this.points.selectInViewport(id.subject().split("/")[1],
            mchorse.bbs_mod.graphics.window.Window.isCtrlPressed() || mchorse.bbs_mod.graphics.window.Window.isShiftPressed());
    }
}

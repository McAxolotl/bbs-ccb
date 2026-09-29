package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.spline.*;
import mchorse.bbs_mod.forms.forms.SplineForm;
import mchorse.bbs_mod.film.replays.tracks.TrackId;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.*;
import mchorse.bbs_mod.ui.utils.*;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3f;
import java.util.function.Consumer;

public class UISplinePointsKeyframeFactory extends UIKeyframeFactory<SplinePositions> implements SplineKeyframeEditor
{
    private final SplineForm form;
    private final UISplinePointsEditor points;

    public UISplinePointsKeyframeFactory(Keyframe<SplinePositions> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);
        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        this.form = sheet != null && sheet.form instanceof SplineForm spline ? spline : null;
        UIKeyframePropTransform transform = new UIKeyframePropTransform()
        {
            @Override protected UIKeyframes getKeyframes() { return editor; }
            @Override protected Transform getTargetTransform()
            {
                Float tick = this.getKeyframes().getAutoKeyframeTick();
                if (tick != null) return this.getAutoKeyTransform(tick);
                return UISplinePointsKeyframeFactory.this.points.point() == null ? null
                    : UISplinePointsKeyframeFactory.this.resolved(UISplinePointsKeyframeFactory.this.getDisplayValue()).point(UISplinePointsKeyframeFactory.this.points.pointId());
            }
            @Override protected void applyToSelection(Consumer<Transform> edit)
            {
                UIReplaysEditorUtils.forEachSelectedKeyframe(editor, keyframe, selected ->
                {
                    SplinePositions value = UISplinePointsKeyframeFactory.this.resolved((SplinePositions) selected.getValue());
                    selected.preNotify();
                    for (String id : UISplinePointsKeyframeFactory.this.points.selected()) edit.accept(value.point(id));
                    ((SplinePositions) selected.getValue()).putAll(value);
                    selected.postNotify();
                });
            }
            @Override protected Transform getAutoKeyTransform(float tick)
            {
                if (sheet == null || UISplinePointsKeyframeFactory.this.points.point() == null) return null;
                Keyframe<SplinePositions> target = sheet.ensureKeyframe(tick);
                if (target == null) return null;
                SplinePositions value = target.getValue();
                value.putAll(UISplinePointsKeyframeFactory.this.resolved(value));
                return value.point(UISplinePointsKeyframeFactory.this.points.pointId());
            }
        };
        this.points = new UISplinePointsEditor(() -> this.form, id -> this.resolved(this.getDisplayValue()).point(id), transform, Vector3f::new, false);
        transform.enableHotkeys(() -> this.points.point() != null, op -> op == TransformOp.TRANSLATE);
        this.scroll.add(this.points);
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

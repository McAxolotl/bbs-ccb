package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.cubic.spline.SplineSource;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.SplineForm;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.*;
import mchorse.bbs_mod.ui.framework.elements.input.drag.*;
import mchorse.bbs_mod.ui.utils.*;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.*;
import java.util.List;
import java.util.function.Consumer;

public class UISplineFormPanel extends UIFormPanel<SplineForm> implements SplineFormTool
{
    public final UISplinePointsEditor points;
    private final UIToggle closed;

    public UISplineFormPanel(UIForm editor)
    {
        super(editor);
        UIPropTransform transform = new UIDeltaPropTransform()
        {
            @Override protected void applyToSelection(Consumer<Transform> edit)
            {
                for (String id : UISplineFormPanel.this.points.selected()) edit.accept(UISplineFormPanel.this.form.curve.getOriginalValue().point(id));
            }
        };
        transform.callbacks(() -> this.form.curve.preNotify(), () -> this.form.curve.postNotify(),
            () -> this.form.curve.preNotify(IValueListener.FLAG_UNMERGEABLE));
        transform.hotkeyDrag(() -> this.editor.editor == null ? null : this.editor.editor.buildHotkeyDrag(transform));
        this.points = new UISplinePointsEditor(() -> this.form, id -> this.form.curve.getOriginalValue().point(id), transform, Vector3f::new);
        transform.enableHotkeys(() -> this.editor.view == this && this.points.point() != null, op -> op == TransformOp.TRANSLATE);
        this.closed = new UIToggle(L10n.lang("bbs.ui.spline.closed"), b -> this.form.closed.set(b.getValue()));
        this.options.add(this.points, this.closed);
    }
    @Override public void startEdit(SplineForm form)
    {
        super.startEdit(form);
        this.points.refresh();
        this.closed.setValue(form.closed.get());
    }
    @Override public void finishEdit() { this.points.endEdit(); super.finishEdit(); }
    @Override public List<SplineSource> splineSources() { return this.form == null ? List.of() : List.of(this.form); }
    @Override public SplineSource activeSpline() { return this.form; }
    @Override public UISplinePointsEditor pointEditor() { return this.points; }
    @Override public void selectSpline(SplineSource source) {}
    @Override public UIPropTransform getGizmoTransform() { return this.points.point() == null ? null : this.points.position; }
    @Override public Matrix4f getGizmoOrigin(float transition, TransformSpace space)
    {
        if (this.getGizmoTransform() == null || this.editor.editor == null) return null;
        Matrix4f matrix = SplineEditorUtils.parentMatrix(FormUtils.getRoot(this.form), this.editor.editor.renderer.getTargetEntity(), transition, this.form, this.form);
        return matrix == null ? null : matrix.translate(this.points.point().position.getOriginalValue().translate);
    }
}

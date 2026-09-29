package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.forms.forms.SplineForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.SplineOverlay;
import org.joml.Matrix4f;

/** Only a palette thumbnail; editor overlays are drawn by the viewport, never in a final frame. */
public class SplineFormRenderer extends FormRenderer<SplineForm>
{
    public SplineFormRenderer(SplineForm form) { super(form); }
    @Override protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        Matrix4f matrix = new Matrix4f(ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2));
        this.applyTransforms(matrix, context.getTransition());
        matrix.scale(this.form.uiScale.get());
        SplineOverlay.drawPreview(context, this.form, matrix);
    }
}

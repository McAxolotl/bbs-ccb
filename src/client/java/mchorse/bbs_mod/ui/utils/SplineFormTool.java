package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.api.client.editor.FormEditorTool;
import mchorse.bbs_mod.cubic.spline.SplineSource;
import java.util.List;

/** Only the data source changes between path and IK panels; viewport input stays shared. */
public interface SplineFormTool extends FormEditorTool
{
    List<SplineSource> splineSources();
    SplineSource activeSpline();
    UISplinePointsEditor pointEditor();
    void selectSpline(SplineSource source);
}

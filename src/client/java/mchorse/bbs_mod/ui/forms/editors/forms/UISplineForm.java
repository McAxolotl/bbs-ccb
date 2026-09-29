package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.forms.SplineForm;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.ui.forms.editors.panels.UISplineFormPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UISplineForm extends UIForm<SplineForm>
{
    public UISplineForm()
    {
        this.defaultPanel = new UISplineFormPanel(this);
        this.registerPanel(this.defaultPanel, L10n.lang("bbs.ui.spline.title"), Icons.GRAPH);
        this.registerDefaultPanels();
    }
}

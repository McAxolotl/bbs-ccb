package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIActionsConfigEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;

public class UIActionsConfigKeyframeFactory extends UIKeyframeFactory<ActionsConfig>
{
    public UIActionsConfigEditor actionsEditor;
    private ActionsConfig displayed;

    /* Which arrangement the fields are in (null until the first layout), so a resize
     * that stays on the same side of the threshold doesn't rebuild the subtree */
    private Boolean wide;

    public UIActionsConfigKeyframeFactory(UITrackValue<ActionsConfig> track, UIKeyframes editor)
    {
        super(track, editor);

        ModelForm form = (ModelForm) FormUtils.getForm(track.sheet.property);
        ModelFormRenderer renderer = (ModelFormRenderer) FormUtilsClient.getRenderer(form);

        this.actionsEditor = new UIActionsConfigEditor(() ->
        {

        }, () ->
        {
            renderer.resetAnimator();
            this.track.setValue(this.displayed);
        });
        this.displayed = track.getValue();
        this.actionsEditor.setConfigs(this.displayed, form);

        this.scroll.add(this.actionsEditor);
    }

    @Override
    public void update()
    {
        if (this.actionsEditor.speed.isUserEditing() || this.actionsEditor.fade.isUserEditing() || this.actionsEditor.tick.isUserEditing()) return;
        this.displayed = this.getDisplayValue();
        this.actionsEditor.refreshConfigs(this.displayed);
    }

    @Override
    public void resize()
    {
        boolean wide = this.getFlex().getW() > 240;

        if (this.wide == null || this.wide != wide)
        {
            this.wide = wide;
            this.rebuild(wide);
        }

        super.resize();
    }

    private void rebuild(boolean wide)
    {
        this.actionsEditor.removeAll();

        if (wide)
        {
            this.actionsEditor.add(UI.row(
                UI.column(
                    UI.label(UIKeys.FORMS_EDITORS_MODEL_ACTIONS), this.actionsEditor.actions,
                    UI.labelRow(UIKeys.FORMS_EDITORS_ACTIONS_SPEED, this.actionsEditor.speed).marginTop(UIConstants.SECTION_GAP),
                    this.actionsEditor.loop.marginTop(UIConstants.SECTION_GAP)
                ),
                UI.column(
                    UI.label(UIKeys.FORMS_EDITORS_ACTIONS_ANIMATIONS), this.actionsEditor.animations,
                    UI.labelRow(UIKeys.FORMS_EDITORS_ACTIONS_FADE, this.actionsEditor.fade),
                    UI.labelRow(UIKeys.FORMS_EDITORS_ACTIONS_TICK, this.actionsEditor.tick)
                )
            ));
        }
        else
        {
            this.actionsEditor.add(UI.label(UIKeys.FORMS_EDITORS_MODEL_ACTIONS), this.actionsEditor.actions);
            this.actionsEditor.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_ANIMATIONS).marginTop(UIConstants.SECTION_GAP), this.actionsEditor.animations, this.actionsEditor.loop.marginTop(UIConstants.SECTION_GAP));
            this.actionsEditor.add(UI.labelRow(UIKeys.FORMS_EDITORS_ACTIONS_SPEED, this.actionsEditor.speed).marginTop(UIConstants.SECTION_GAP));
            this.actionsEditor.add(UI.labelRow(UIKeys.FORMS_EDITORS_ACTIONS_FADE, this.actionsEditor.fade));
            this.actionsEditor.add(UI.labelRow(UIKeys.FORMS_EDITORS_ACTIONS_TICK, this.actionsEditor.tick));
        }
    }
}
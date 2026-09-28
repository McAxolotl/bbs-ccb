package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.cubic.spline.*;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIModelSplineFormPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.*;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import mchorse.bbs_mod.ui.utils.UISplineControlFields;
import mchorse.bbs_mod.ui.utils.UISplinePointList;

/** One shared key time; chain/point selection edits elements of the compound snapshot. */
public class UISplineKeyframeFactory extends UIKeyframeFactory<SplineControls>
{
    private final ModelForm form;
    public final UIStringList chains;
    public final UISplinePointList points;
    public final UIPropTransform transform;
    private final UISliderTrackpad influence;
    private final UITrackpad progress;
    private final UITrackpad twist;
    private String chainId = "";
    private String pointId = "";
    private final Set<String> selectedPoints = new LinkedHashSet<>();

    public UISplineKeyframeFactory(Keyframe<SplineControls> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);
        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        this.form = sheet != null && sheet.form instanceof ModelForm model ? model : null;
        this.chains = new UIStringList(values -> this.select(values.isEmpty() ? "" : values.get(0), ""))
        {
            @Override protected String elementToString(UIContext context, int index, String id)
            {
                SplineIK chain = UISplineKeyframeFactory.this.form == null ? null : UISplineKeyframeFactory.this.form.splines.get(id);
                return chain == null ? id : chain.tip.get();
            }
        };
        this.chains.background().h(UIConstants.LIST_ITEM_HEIGHT * 6).expand();
        this.points = new UISplinePointList(this::selectPoints);
        var fields = new UISplineControlFields(this::edit);
        this.influence = fields.influence;
        this.progress = fields.progress;
        this.twist = fields.twist;
        this.transform = new UIKeyframePropTransform()
        {
            @Override protected UIKeyframes getKeyframes() { return editor; }
            @Override protected Transform getTargetTransform()
            {
                return UISplineKeyframeFactory.this.pointId.isEmpty() ? null
                    : UISplineKeyframeFactory.this.control(UISplineKeyframeFactory.this.getDisplayValue()).point(UISplineKeyframeFactory.this.pointId);
            }
            @Override protected void applyToSelection(Consumer<Transform> consumer)
            {
                if (!UISplineKeyframeFactory.this.pointId.isEmpty())
                    UISplineKeyframeFactory.this.edit(control ->
                    {
                        for (String id : UISplineKeyframeFactory.this.selectedPoints) consumer.accept(control.point(id));
                    });
            }
            @Override protected Transform getAutoKeyTransform(float tick)
            {
                if (sheet == null || UISplineKeyframeFactory.this.pointId.isEmpty()) return null;
                Keyframe<SplineControls> target = sheet.ensureKeyframe(tick);
                return target == null ? null : UISplineKeyframeFactory.this.control(target.getValue()).point(UISplineKeyframeFactory.this.pointId);
            }
        };
        this.transform.noScale().setRotationVisible(false);
        this.transform.enableHotkeys(() -> !this.pointId.isEmpty(), op -> op == TransformOp.TRANSLATE);
        this.scroll.add(UI.column(this.chains,
            UI.labelRow(UIModelSplineFormPanel.key("influence"), this.influence),
            UI.labelRow(UIModelSplineFormPanel.key("progress_short"), this.progress),
            UI.labelRow(UIModelSplineFormPanel.key("twist"), this.twist), this.points, this.transform).expand());
        List<String> ids = new ArrayList<>();
        if (this.form != null) for (SplineIK chain : this.form.splines.getAllTyped()) ids.add(chain.getId());
        this.chains.setList(ids);
        this.select(ids.isEmpty() ? "" : ids.get(0), "");
    }

    private SplineControl control(SplineControls value)
    {
        SplineControl stored = value.controls.get(this.chainId);
        SplineControl defaults = this.form == null ? new SplineControl() : this.form.splineIK.getOriginalValue().get(this.chainId);
        return stored == null ? defaults.copy() : stored.withDefaults(defaults);
    }

    private void edit(Consumer<SplineControl> edit)
    {
        if (this.chainId.isEmpty()) return;
        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, selected ->
        {
            SplineControls value = (SplineControls) selected.getValue();
            SplineControl control = this.control(value);
            selected.preNotify();
            edit.accept(control);
            value.controls.put(this.chainId, control);
            selected.postNotify();
        });
    }

    public void select(String chainId, String pointId)
    {
        if (this.transform.isEditing()) this.transform.endGesture();
        this.chainId = chainId;
        SplineIK chain = this.form == null ? null : this.form.splines.get(chainId);
        List<String> ids = new ArrayList<>();
        if (chain != null) for (SplinePoint point : chain.points.getAllTyped()) ids.add(point.getId());
        this.pointId = ids.contains(pointId) ? pointId : ids.isEmpty() ? "" : ids.get(0);
        this.selectedPoints.clear();
        if (!this.pointId.isEmpty()) this.selectedPoints.add(this.pointId);
        this.chains.setCurrent(chainId);
        this.points.setList(ids);
        this.points.setCurrent(this.pointId);
        this.points.h(UIConstants.LIST_ITEM_HEIGHT * Math.max(1, Math.min(6, ids.size())));
        SplineControl control = this.control(this.getDisplayValue());
        this.influence.setValue(control.influence);
        this.progress.setValue(control.progress);
        this.twist.setValue(control.twist);
        this.influence.setEnabled(chain != null);
        this.progress.setEnabled(chain != null);
        this.twist.setEnabled(chain != null);
        this.transform.setVisible(!this.pointId.isEmpty());
        this.transform.setTransform(this.pointId.isEmpty() ? null : control.point(this.pointId));
        this.scroll.resize();
    }

    private void selectPoints(List<String> ids)
    {
        if (this.transform.isEditing()) this.transform.endGesture();
        this.selectedPoints.clear();
        this.selectedPoints.addAll(ids);
        String anchor = this.points.selection.getAnchor();
        this.pointId = ids.contains(anchor) ? anchor : ids.isEmpty() ? "" : ids.get(ids.size() - 1);
        this.transform.setVisible(!this.pointId.isEmpty());
        this.transform.setTransform(this.pointId.isEmpty() ? null : this.control(this.getDisplayValue()).point(this.pointId));
        this.scroll.resize();
    }

    public Set<String> selectedPoints(SplineIK chain)
    {
        return this.form != null && this.form.splines.get(this.chainId) == chain ? Set.copyOf(this.selectedPoints) : Set.of();
    }

    public String hoveredPoint(UIContext context, SplineIK chain)
    {
        if (this.form == null || this.form.splines.get(this.chainId) != chain || !this.points.area.isInside(context)) return "";
        return this.points.hoveredPoint(context);
    }

    public void viewportHover(SplineIK chain, String point)
    {
        this.points.viewportHover = this.form != null && chain != null && this.form.splines.get(this.chainId) == chain ? point : "";
    }

    public String pointPath()
    {
        SplineIK chain = this.form == null ? null : this.form.splines.get(this.chainId);
        SplinePoint point = chain == null ? null : chain.points.get(this.pointId);
        return point == null ? null : FormUtils.getPropertyPath(point.position);
    }

    @Override
    public void render(UIContext context)
    {
        SplineControl control = this.control(this.getDisplayValue());
        if (!this.influence.isUserEditing()) this.influence.setValue(control.influence);
        if (!this.progress.isUserEditing()) this.progress.setValue(control.progress);
        if (!this.twist.isUserEditing()) this.twist.setValue(control.twist);
        if (!this.transform.isUserEditing() && !this.pointId.isEmpty()) this.transform.setTransform(control.point(this.pointId));
        super.render(context);
    }
}

package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.api.client.editor.FormEditorTool;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.spline.ModelSplineRuntime;
import mchorse.bbs_mod.cubic.spline.SplineIK;
import mchorse.bbs_mod.cubic.spline.SplineCurve;
import mchorse.bbs_mod.cubic.spline.SplinePoint;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.SplineEditorUtils;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.bones.UIBonePicker;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.ModelSplineManager;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Spline controls are form values: fields, gestures, undo and animation share their identity. */
public class UIModelSplineFormPanel extends UIBoneListFormPanel implements FormEditorTool
{
    public final UIToggle debug;
    public final UIStringList points;
    public final UIPropTransform position;
    private final UIIcon removePoint;
    private final UISection advanced;
    private final UIBonePicker tip;
    private final UIToggle enabled;
    private final UIToggle fit;
    private final UIToggle moveModel;
    private final UISliderTrackpad influence;
    private final UITrackpad progress;
    private final UITrackpad twist;
    private final UILabel status;
    private final UIElement fields;
    private String chainId = "";
    private String pointId = "";

    public static IKey key(String name)
    {
        return L10n.lang("bbs.ui.forms.editors.model.spline." + name);
    }

    public UIModelSplineFormPanel(UIForm editor)
    {
        super(editor);
        this.bones.tooltip(key("root"));
        this.bonePresets(ModelSplineManager.INSTANCE, "_CopyModelSpline",
            key("context.copy"), key("context.paste"), key("context.reset"), key("context.save"), key("context.name"),
            this::toPresetData, this::applyPresetData);
        this.debug = new UIToggle(UIKeys.FORMS_EDITORS_MODEL_IK_DEBUG, b -> BBSSettings.ikDebug.enabled.set(b.getValue()));
        this.debug.setValue(BBSSettings.ikDebug.enabled.get());
        this.points = new UIStringList(list -> this.select(this.chainId, list.isEmpty() ? "" : list.get(0)))
        {
            @Override
            public UIContextMenu createContextMenu(UIContext context)
            {
                int index = this.getIndexAtCursor(context);
                if (index >= 0) UIModelSplineFormPanel.this.select(UIModelSplineFormPanel.this.chainId, this.getList().get(index));
                return super.createContextMenu(context);
            }

            @Override
            protected String elementToString(UIContext context, int i, String id)
            {
                return key("point").format(i + 1).get();
            }
        };
        this.points.background();
        this.points.context(this::pointMenu);
        this.points.tooltip(key("points_tooltip"));
        this.removePoint = new UIIcon(Icons.REMOVE, b -> this.removePoint());
        this.removePoint.tooltip(UIKeys.GENERAL_REMOVE);
        this.tip = this.bonePicker();
        this.enabled = new UIToggle(key("enabled"), b -> this.setChainEnabled(b.getValue()));
        this.fit = new UIToggle(key("fit"), b -> { if (this.chain() != null) this.chain().fit.set(b.getValue()); });
        this.fit.tooltip(key("fit_tooltip"));
        this.influence = new UISliderTrackpad(v -> { if (this.chain() != null) this.chain().influence.set(v.floatValue()); });
        this.influence.normalized();
        this.progress = new UITrackpad(v -> { if (this.chain() != null) this.chain().progress.set(v.floatValue()); });
        this.progress.tooltip(key("progress_tooltip"));
        this.moveModel = new UIToggle(key("move_model"), b -> { if (this.chain() != null) this.chain().moveModel.set(b.getValue()); });
        this.moveModel.tooltip(key("move_model_tooltip"));
        this.twist = new UITrackpad(v -> { if (this.chain() != null) this.chain().twist.set(v.floatValue()); });
        this.position = new UIPropTransform();
        this.position.noScale().setRotationVisible(false);
        this.position.callbacks(
            () -> { if (this.point() != null) this.point().position.preNotify(); },
            () -> { if (this.point() != null) this.point().position.postNotify(); },
            () -> { if (this.point() != null) this.point().position.preNotify(IValueListener.FLAG_UNMERGEABLE); }
        );
        this.position.enableHotkeys(() -> this.editor.view == this && this.getGizmoTransform() != null, op -> op == TransformOp.TRANSLATE);
        this.position.hotkeyDrag(() -> this.editor.editor == null ? null : this.editor.editor.buildHotkeyDrag(this.position));
        this.status = UI.label(IKey.EMPTY, UIConstants.CONTROL_HEIGHT, Colors.ORANGE);
        this.status.labelAnchor(0, 0.5F);
        UISection properties = this.section(key("properties"), "spline.properties", true);
        this.advanced = this.section(key("advanced"), "spline.advanced", false);
        this.advanced.fields.add(this.moveModel, UI.labelRow(key("twist"), this.twist), this.fit);
        UIElement pointHeader = UI.row(UIConstants.MARGIN, 0, UIConstants.CONTROL_HEIGHT,
            UI.label(key("points"), UIConstants.CONTROL_HEIGHT).labelAnchor(0, 0.5F),
            new UIIcon(Icons.ADD, b -> this.addPoint()).wh(UIConstants.CONTROL_HEIGHT, UIConstants.CONTROL_HEIGHT).tooltip(UIKeys.GENERAL_ADD),
            this.removePoint.wh(UIConstants.CONTROL_HEIGHT, UIConstants.CONTROL_HEIGHT));
        pointHeader.context(this::pointMenu);
        this.fields = UI.column(UIConstants.MARGIN,
            UI.labelRow(key("tip"), this.tip), this.status,
            UI.labelRow(key("influence"), this.influence), UI.labelRow(key("progress_short"), this.progress),
            pointHeader, this.points, this.position);
        properties.fields.add(this.enabled, this.fields);
        this.options.add(this.debug, this.bonesSearch, properties, this.advanced);
    }

    private MapType toPresetData()
    {
        MapType data = new MapType();

        if (this.form != null && !this.form.splines.getAllTyped().isEmpty())
            data.put("splines", this.form.splines.toData());

        return data;
    }

    private void applyPresetData(MapType data)
    {
        if (this.form == null) return;
        this.endPointEdit();
        /* A preset is the whole model's spline setup, including stable animation addresses. */
        BaseValue.edit(this.form.splines, IValueListener.FLAG_UNMERGEABLE, value -> value.fromData(data.getList("splines")));
        this.updateFields();
    }

    private void pointMenu(ContextMenuManager menu)
    {
        SplineIK chain = this.chain();
        if (chain == null) return;
        if (ModelSplineRuntime.getChain(this.form, chain.root.get(), chain.tip.get()).size() >= 2)
            menu.action(Icons.REFRESH, UIKeys.GENERAL_RESET, this::resetCurve);
        menu.action(Icons.ADD, UIKeys.GENERAL_ADD, this::addPoint);
        int index = this.pointIndex();
        if (index < 0) return;
        if (index > 0) menu.action(Icons.MOVE_UP, key("up"), () -> this.movePoint(-1));
        if (index + 1 < chain.points.getAllTyped().size()) menu.action(Icons.MOVE_DOWN, key("down"), () -> this.movePoint(1));
        menu.action(Icons.REMOVE, UIKeys.GENERAL_REMOVE, this::removePoint);
    }

    private UIBonePicker bonePicker()
    {
        UIBonePicker picker = new UIBonePicker(bone ->
        {
            SplineIK chain = this.chain();
            if (chain == null || ModelSplineRuntime.getChain(this.form, chain.root.get(), bone).size() < 2) return;
            this.endPointEdit();
            /* Choose the end and seed a new curve as one edit. Existing controls stay intact. */
            SplineIK copy = new SplineIK(chain.getId());
            copy.fromData(chain.toData());
            copy.tip.set(bone);
            if (copy.points.getAllTyped().isEmpty())
                this.setCurvePoints(copy, ModelSplineRuntime.createPoints(this.form, copy.root.get(), bone));
            chain.copy(copy);
            this.updateFields();
        });
        picker.menu(menu ->
        {
            ModelInstance model = this.form == null ? null : ModelFormRenderer.getModel(this.form);
            SplineIK chain = this.chain();
            if (model == null || model.model == null || chain == null) return;
            menu.bones(model.model, model.getDisabledBones()).set(chain.tip.get());
            menu.disabled(bone -> ModelSplineRuntime.getChain(this.form, chain.root.get(), bone).size() < 2);
        });
        picker.viewport(this.viewportBonePicking());
        return picker;
    }

    @Override
    protected float getDefaultOptionsWidth() { return 0.3F; }

    @Override
    public void startEdit(ModelForm form)
    {
        this.endPointEdit();
        this.debug.setValue(BBSSettings.ikDebug.enabled.get());
        super.startEdit(form);
    }

    @Override
    public void finishEdit()
    {
        this.endPointEdit();
        super.finishEdit();
    }

    public SplineIK chain() { return this.form == null ? null : this.form.splines.get(this.chainId); }
    public SplinePoint point() { SplineIK chain = this.chain(); return chain == null ? null : chain.points.get(this.pointId); }
    public String getChainId() { return this.chainId; }
    public String getPointId() { return this.pointId; }
    public ModelForm getModelForm() { return this.form; }

    public void select(String chain, String point)
    {
        this.endPointEdit();
        this.chainId = chain;
        this.pointId = point;
        SplineIK selected = this.chain();
        if (selected != null)
        {
            this.selectedBone = selected.root.get();
            this.boneSelection().set(this.selectedBone);
            this.bones.setCurrentScroll(this.selectedBone);
        }
        this.updateFields();
    }

    private void endPointEdit()
    {
        if (this.position.isEditing()) this.position.endGesture();
        this.position.setTransform(null);
    }

    @Override
    protected void setElementsEnabled(boolean enabled)
    {
        this.bonesSearch.setEnabled(enabled);
        this.bones.setEnabled(enabled);
        this.enabled.setEnabled(enabled && !this.selectedBone.isEmpty());
    }

    @Override
    protected void updateFields()
    {
        SplineIK chain = this.chain();
        if (chain == null || !chain.root.get().equals(this.selectedBone))
        {
            this.endPointEdit();
            chain = null;
            if (this.form != null)
            {
                for (SplineIK candidate : this.form.splines.getAllTyped())
                {
                    if (!candidate.root.get().equals(this.selectedBone)) continue;
                    if (chain == null) chain = candidate;
                    if (candidate.enabled.get()) { chain = candidate; break; }
                }
            }
            this.chainId = chain == null ? "" : chain.getId();
            this.pointId = "";
        }
        boolean on = chain != null && chain.enabled.get();
        this.enabled.setEnabled(chain != null || this.availableBones.contains(this.selectedBone));
        this.enabled.setValue(on);
        this.fields.setVisible(on);
        this.advanced.setVisible(on);
        if (chain != null)
        {
            this.tip.setLabel(IKey.constant(chain.tip.get().isEmpty() ? "—" : chain.tip.get()));
            this.fit.setValue(chain.fit.get());
            this.influence.setValue(chain.influence.get());
            this.progress.setValue(chain.progress.get());
            this.moveModel.setValue(chain.moveModel.get());
            this.twist.setValue(chain.twist.get());
            String reason = ModelSplineRuntime.validate(this.form, chain);
            boolean showStatus = reason != null && !reason.equals("invalid_chain");
            this.status.label = showStatus ? key("error." + reason) : IKey.EMPTY;
            this.status.tooltip(this.status.label);
            this.status.setVisible(showStatus);
            List<String> pointIds = new ArrayList<>();
            for (SplinePoint point : chain.points.getAllTyped()) pointIds.add(point.getId());
            if (!pointIds.contains(this.pointId)) this.pointId = pointIds.isEmpty() ? "" : pointIds.get(0);
            this.points.setList(pointIds);
            this.points.setCurrent(this.pointId);
        }
        else
        {
            this.pointId = "";
            this.points.setList(List.of());
        }
        this.points.h(this.pointListHeight());
        this.removePoint.setEnabled(this.point() != null);
        this.position.setVisible(on && this.point() != null);
        this.position.setTransform(!on || this.point() == null ? null : this.point().position.get());
        this.options.resize();
    }

    private void setChainEnabled(boolean enabled)
    {
        this.endPointEdit();
        SplineIK chain = this.chain();
        if (chain != null) chain.enabled.set(enabled);
        else if (enabled) this.addChain();
        this.updateFields();
    }

    private void addChain()
    {
        if (this.form == null || !this.availableBones.contains(this.selectedBone)) return;
        this.endPointEdit();
        SplineIK chain = new SplineIK("");
        chain.name.set(key("title").get() + " " + (this.form.splines.getAllTyped().size() + 1));
        chain.root.set(this.selectedBone);
        BaseValue.edit(this.form.splines, list -> list.add(chain));
        this.select(chain.getId(), "");
    }

    private void resetCurve()
    {
        SplineIK chain = this.chain();
        if (chain == null) return;
        List<Vector3f> reference = ModelSplineRuntime.createPoints(this.form, chain.root.get(), chain.tip.get());
        if (reference.size() < 2) return;
        this.endPointEdit();
        /* A reset preserves the animator's control count and IDs, including keyed points. */
        int count = chain.points.getAllTyped().size();
        List<Vector3f> points = new ArrayList<>();
        if (count == 0) points.addAll(reference);
        else for (int i = 0; i < count; i++) points.add(SplineCurve.evaluate(reference, i / (float) Math.max(1, count - 1)));
        BaseValue.edit(chain.points, list -> this.setCurvePoints(chain, points));
        this.updateFields();
    }

    private void setCurvePoints(SplineIK chain, List<Vector3f> positions)
    {
        /* Keep existing IDs: resetting a curve must not orphan its animation tracks. */
        List<SplinePoint> points = chain.points.getAllTyped();
        while (points.size() < positions.size()) chain.points.add(new SplinePoint(""));
        while (points.size() > positions.size()) points.remove(points.size() - 1);
        for (int i = 0; i < positions.size(); i++) points.get(i).position.get().translate.set(positions.get(i));
    }

    private void addPoint()
    {
        SplineIK chain = this.chain();
        if (chain == null) return;
        this.endPointEdit();
        List<SplinePoint> points = chain.points.getAllTyped();
        int index = Math.max(0, this.pointIndex() + 1);
        SplinePoint point = new SplinePoint("");
        if (!points.isEmpty())
        {
            point.position.get().translate.set(points.get(Math.max(0, index - 1)).position.get().translate);
            if (index < points.size()) point.position.get().translate.lerp(points.get(index).position.get().translate, 0.5F);
            else if (points.size() > 1) point.position.get().translate.mul(2).sub(points.get(points.size() - 2).position.get().translate);
        }
        BaseValue.edit(chain.points, list -> list.add(index, point));
        this.select(this.chainId, point.getId());
    }

    private void removePoint()
    {
        SplineIK chain = this.chain();
        SplinePoint point = this.point();
        if (chain == null || point == null) return;
        this.endPointEdit();
        BaseValue.edit(chain.points, list -> list.getAllTyped().removeIf(value -> value.getId().equals(point.getId())));
        this.updateFields();
    }

    private int pointIndex()
    {
        SplineIK chain = this.chain();
        if (chain != null)
        {
            List<SplinePoint> points = chain.points.getAllTyped();
            for (int i = 0; i < points.size(); i++) if (points.get(i).getId().equals(this.pointId)) return i;
        }
        return -1;
    }

    private int pointListHeight()
    {
        return Math.max(1, Math.min(6, this.points.getList().size())) * this.points.rowHeight();
    }

    private void movePoint(int offset)
    {
        SplineIK chain = this.chain();
        if (chain == null) return;
        int from = this.pointIndex();
        int to = from + offset;
        if (from < 0 || to < 0 || to >= chain.points.getAllTyped().size()) return;
        this.endPointEdit();
        BaseValue.edit(chain.points, list -> Collections.swap(list.getAllTyped(), from, to));
        this.updateFields();
    }

    @Override
    public UIPropTransform getGizmoTransform()
    {
        if (this.point() == null || !this.chain().enabled.get()) return null;
        ModelInstance model = ModelFormRenderer.getModel(this.form);
        return model == null || model.model == null || model.model.getBone(this.chain().root.get()) == null ? null : this.position;
    }

    @Override
    public Matrix4f getGizmoOrigin(float transition, TransformSpace space)
    {
        if (this.getGizmoTransform() == null || this.editor.editor == null) return null;
        Matrix4f parent = SplineEditorUtils.parentMatrix(FormUtils.getRoot(this.form), this.editor.editor.renderer.getTargetEntity(), transition, this.form, this.chain());
        return parent == null ? null : parent.translate(this.point().position.get().translate);
    }

    @Override
    public void render(UIContext context)
    {
        this.debug.setValue(BBSSettings.ikDebug.enabled.get());
        int height = this.pointListHeight();
        if (this.points.getFlex().getH() != height)
        {
            this.points.h(height);
            this.options.resize();
        }
        SplinePoint point = this.point();
        if (point != null && this.chain().enabled.get() && !this.position.isUserEditing()) this.position.setTransform(point.position.get());
        super.render(context);
    }
}

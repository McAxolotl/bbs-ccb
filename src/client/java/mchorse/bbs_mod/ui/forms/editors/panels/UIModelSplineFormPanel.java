package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.api.client.editor.FormEditorTool;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.spline.ModelSplineRuntime;
import mchorse.bbs_mod.cubic.spline.SplineIK;
import mchorse.bbs_mod.cubic.spline.SplineCurve;
import mchorse.bbs_mod.cubic.spline.SplinePoint;
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
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UISliderTrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp;
import mchorse.bbs_mod.ui.framework.elements.input.drag.TransformSpace;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.SplineEditorUtils;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.bones.UIBonePicker;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Spline controls are form values: fields, gestures, undo and animation share their identity. */
public class UIModelSplineFormPanel extends UIFormPanel<ModelForm> implements FormEditorTool
{
    public final UIStringList chains;
    public final UIStringList points;
    public final UIPropTransform position;
    private final UITextbox name;
    private final UIBonePicker root;
    private final UIBonePicker tip;
    private final UIToggle enabled;
    private final UIToggle fit;
    private final UIToggle moveModel;
    private final UISliderTrackpad influence;
    private final UITrackpad progress;
    private final UITrackpad twist;
    private final UILabel status;
    private final UIElement fields;
    private final UIButton reset;
    private String chainId = "";
    private String pointId = "";

    public static IKey key(String name)
    {
        return L10n.lang("bbs.ui.forms.editors.model.spline." + name);
    }

    public UIModelSplineFormPanel(UIForm editor)
    {
        super(editor);
        this.chains = new UIStringList(list -> this.select(list.isEmpty() ? "" : list.get(0), ""))
        {
            @Override
            protected String elementToString(UIContext context, int i, String id)
            {
                SplineIK chain = UIModelSplineFormPanel.this.form == null ? null : UIModelSplineFormPanel.this.form.splines.get(id);
                return chain == null ? id : chain.name.get();
            }
        };
        this.chains.background().h(72);
        this.points = new UIStringList(list -> this.select(this.chainId, list.isEmpty() ? "" : list.get(0)))
        {
            @Override
            protected String elementToString(UIContext context, int i, String id)
            {
                return key("point").format(i + 1).get();
            }
        };
        this.points.background().h(88);
        this.name = new UITextbox(value -> { if (this.chain() != null) this.chain().name.set(value); });
        this.root = this.bonePicker(true);
        this.tip = this.bonePicker(false);
        this.enabled = new UIToggle(key("enabled"), b -> { if (this.chain() != null) this.chain().enabled.set(b.getValue()); this.refresh(); });
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
        this.reset = new UIButton(key("reset"), b -> this.resetCurve());
        this.status = UI.label(IKey.EMPTY, UIConstants.CONTROL_HEIGHT, Colors.ORANGE);
        this.status.labelAnchor(0, 0.5F);
        this.fields = UI.column(UIConstants.MARGIN,
            UI.labelRow(key("name"), this.name), this.enabled,
            UI.labelRow(key("root"), this.root), UI.labelRow(key("tip"), this.tip),
            this.status, this.reset,
            UI.labelRow(key("influence"), this.influence), UI.labelRow(key("progress"), this.progress),
            this.moveModel, UI.labelRow(key("twist"), this.twist), this.fit,
            UI.label(key("points")), this.points,
            UI.row(new UIIcon(Icons.ADD, b -> this.addPoint()).tooltip(UIKeys.GENERAL_ADD),
                new UIIcon(Icons.REMOVE, b -> this.removePoint()).tooltip(UIKeys.GENERAL_REMOVE),
                new UIIcon(Icons.MOVE_UP, b -> this.movePoint(-1)).tooltip(key("up")),
                new UIIcon(Icons.MOVE_DOWN, b -> this.movePoint(1)).tooltip(key("down"))),
            this.position);
        this.options.add(UI.label(key("title")), this.chains,
            UI.row(new UIButton(UIKeys.GENERAL_ADD, b -> this.addChain()), new UIButton(UIKeys.GENERAL_REMOVE, b -> this.removeChain())), this.fields);
    }

    private UIBonePicker bonePicker(boolean first)
    {
        UIBonePicker picker = new UIBonePicker(bone ->
        {
            SplineIK chain = this.chain();
            if (chain == null) return;
            (first ? chain.root : chain.tip).set(bone);
            this.refresh();
        });
        picker.menu(menu ->
        {
            ModelInstance model = this.form == null ? null : ModelFormRenderer.getModel(this.form);
            SplineIK chain = this.chain();
            if (model == null || model.model == null || chain == null) return;
            menu.bones(model.model, Set.of()).set(first ? chain.root.get() : chain.tip.get());
            if (!first && !chain.root.get().isEmpty())
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
        super.startEdit(form);
        this.refresh();
    }

    public SplineIK chain() { return this.form == null ? null : this.form.splines.get(this.chainId); }
    public SplinePoint point() { SplineIK chain = this.chain(); return chain == null ? null : chain.points.get(this.pointId); }
    public String getChainId() { return this.chainId; }
    public String getPointId() { return this.pointId; }
    public ModelForm getModelForm() { return this.form; }

    public void select(String chain, String point)
    {
        if (this.position.isEditing())
        {
            this.position.endGesture();
            this.position.setTransform(null);
        }
        this.chainId = chain;
        this.pointId = point;
        this.refresh();
    }

    private void refresh()
    {
        List<String> ids = new ArrayList<>();
        if (this.form != null) for (SplineIK chain : this.form.splines.getAllTyped()) ids.add(chain.getId());
        if (!ids.contains(this.chainId)) this.chainId = ids.isEmpty() ? "" : ids.get(0);
        this.chains.setList(ids);
        this.chains.setCurrent(this.chainId);
        SplineIK chain = this.chain();
        this.fields.setVisible(chain != null);
        if (chain != null)
        {
            this.name.setText(chain.name.get());
            this.root.setLabel(IKey.constant(chain.root.get().isEmpty() ? "—" : chain.root.get()));
            this.tip.setLabel(IKey.constant(chain.tip.get().isEmpty() ? "—" : chain.tip.get()));
            this.enabled.setValue(chain.enabled.get());
            this.fit.setValue(chain.fit.get());
            this.influence.setValue(chain.influence.get());
            this.progress.setValue(chain.progress.get());
            this.moveModel.setValue(chain.moveModel.get());
            this.twist.setValue(chain.twist.get());
            String reason = ModelSplineRuntime.validate(this.form, chain);
            boolean valid = reason == null;
            this.status.label = valid ? IKey.EMPTY : key("error." + reason);
            this.status.tooltip(this.status.label);
            this.status.setVisible(!valid);
            this.reset.setEnabled(ModelSplineRuntime.getChain(this.form, chain.root.get(), chain.tip.get()).size() >= 2);
            List<String> pointIds = new ArrayList<>();
            for (SplinePoint point : chain.points.getAllTyped()) pointIds.add(point.getId());
            if (!pointIds.contains(this.pointId)) this.pointId = pointIds.isEmpty() ? "" : pointIds.get(0);
            this.points.setList(pointIds);
            this.points.setCurrent(this.pointId);
        }
        this.position.setVisible(this.point() != null);
        this.position.setTransform(this.point() == null ? null : this.point().position.get());
        this.options.resize();
    }

    private void addChain()
    {
        if (this.form == null) return;
        SplineIK chain = new SplineIK("");
        chain.name.set(key("title").get() + " " + (this.form.splines.getAllTyped().size() + 1));
        BaseValue.edit(this.form.splines, list -> list.add(chain));
        this.select(chain.getId(), "");
    }

    private void removeChain()
    {
        SplineIK chain = this.chain();
        if (chain == null) return;
        BaseValue.edit(this.form.splines, list -> list.getAllTyped().removeIf(value -> value.getId().equals(chain.getId())));
        this.refresh();
    }

    private void resetCurve()
    {
        SplineIK chain = this.chain();
        if (chain == null) return;
        List<Vector3f> reference = ModelSplineRuntime.createPoints(this.form, chain.root.get(), chain.tip.get());
        if (reference.size() < 2) return;
        /* A reset preserves the animator's control count and IDs, including keyed points. */
        int count = Math.max(2, chain.points.getAllTyped().size());
        List<Vector3f> points = new ArrayList<>();
        if (chain.points.getAllTyped().size() < 2) points.addAll(reference);
        else for (int i = 0; i < count; i++) points.add(SplineCurve.evaluate(reference, i / (float) (count - 1)));
        BaseValue.edit(chain.points, list ->
        {
            /* Preserve existing point IDs so resetting a curve does not orphan animation. */
            while (list.getAllTyped().size() < points.size()) list.add(new SplinePoint(""));
            while (list.getAllTyped().size() > points.size()) list.getAllTyped().remove(list.getAllTyped().size() - 1);
            for (int i = 0; i < points.size(); i++) list.getAllTyped().get(i).position.get().translate.set(points.get(i));
        });
        this.refresh();
    }

    private void addPoint()
    {
        SplineIK chain = this.chain();
        if (chain == null) return;
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
        if (chain == null || point == null || chain.points.getAllTyped().size() <= 2) return;
        BaseValue.edit(chain.points, list -> list.getAllTyped().removeIf(value -> value.getId().equals(point.getId())));
        this.refresh();
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

    private void movePoint(int offset)
    {
        SplineIK chain = this.chain();
        if (chain == null) return;
        int from = this.pointIndex();
        int to = from + offset;
        if (from < 0 || to < 0 || to >= chain.points.getAllTyped().size()) return;
        BaseValue.edit(chain.points, list -> Collections.swap(list.getAllTyped(), from, to));
        this.refresh();
    }

    @Override
    public UIPropTransform getGizmoTransform()
    {
        if (this.point() == null) return null;
        ModelInstance model = ModelFormRenderer.getModel(this.form);
        return model == null || model.model == null || model.model.getBone(this.chain().root.get()) == null ? null : this.position;
    }

    @Override
    public Matrix4f getGizmoOrigin(float transition, TransformSpace space)
    {
        if (this.point() == null || this.editor.editor == null) return null;
        Matrix4f parent = SplineEditorUtils.parentMatrix(FormUtils.getRoot(this.form), this.editor.editor.renderer.getTargetEntity(), transition, this.form, this.chain());
        return parent == null ? null : parent.translate(this.point().position.get().translate);
    }

    @Override
    public void render(UIContext context)
    {
        SplinePoint point = this.point();
        if (point != null && !this.position.isUserEditing()) this.position.setTransform(point.position.get());
        super.render(context);
    }
}

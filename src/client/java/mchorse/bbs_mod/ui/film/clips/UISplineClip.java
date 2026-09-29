package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.SplineClientClip;
import mchorse.bbs_mod.camera.clips.misc.TrackerFrame;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.SplineForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.clips.modules.UIPointModule;
import mchorse.bbs_mod.ui.film.clips.widgets.UIBitToggle;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;
import mchorse.bbs_mod.ui.framework.tooltips.InterpolationTooltip;
import mchorse.bbs_mod.ui.utils.SplineEditorUtils;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIPathFields;
import mchorse.bbs_mod.ui.utils.bones.UIBonePickerContextMenu;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Set;

/** Ordinary clip parameters: endpoints and interpolation, with no implicit key insertion. */
public class UISplineClip extends UIClip<SplineClientClip>
{
    private UIButton selector;
    private UIButton group;
    private UITrackpad start;
    private UITrackpad end;
    private UIButton interp;
    private UIButton rotation;
    private UIPointModule offset;
    private UIPointModule angle;
    private UITrackpad fov;
    private UIBitToggle active;

    public UISplineClip(SplineClientClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();
        this.selector = new UIButton(UIKeys.CAMERA_PANELS_TARGET_TITLE, button ->
        {
            UIFilmPanel panel = this.getParent(UIFilmPanel.class);
            if (panel == null) return;
            UIAnchorKeyframeFactory.displayActors(this.getContext(), panel.getController().getEntities(), this.clip.selector.get(),
                id -> BaseValue.edit(this.clip, clip ->
                {
                    clip.selector.set(id);
                    clip.group.set("");
                }));
        });
        this.selector.tooltip(UIKeys.CAMERA_PANELS_TARGET_TOOLTIP);
        this.group = new UIButton(UIPathFields.key("title"), button -> this.pickPath());
        this.start = this.trackpad(this.clip.start);
        this.end = this.trackpad(this.clip.end);
        this.start.tooltip(UIPathFields.key("start"));
        this.end.tooltip(UIPathFields.key("end"));
        this.interp = new UIButton(UIKeys.CAMERA_PANELS_INTERPOLATION,
            button -> this.getContext().replaceContextMenu(new UIInterpolationContextMenu(this.clip.interp)));
        this.interp.tooltip(new InterpolationTooltip(1F, 0.5F, () -> this.clip.interp));
        this.rotation = UIPathFields.rotationButton(() -> this.clip.pathRotation.get(),
            mode -> this.editor.editMultiple(this.clip.pathRotation, value -> value.set(mode)));
        this.offset = this.bind(new UIPointModule(this.editor, UIKeys.CAMERA_PANELS_OFFSET).contextMenu(),
            () -> this.offset.fill(this.clip.offset));
        this.angle = this.bind(new UIPointModule(this.editor, UIKeys.CAMERA_PANELS_ANGLE).contextMenu(),
            () -> this.angle.fill(this.clip.angle));
        this.fov = this.trackpad(this.clip.fov);
        this.fov.tooltip(UIKeys.CAMERA_PANELS_FOV);
        this.active = this.bind(new UIBitToggle(value -> this.editor.editMultiple(this.clip.active, field -> field.set(value))).all(),
            () -> this.active.setValue(this.clip.active.get()));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();
        this.panels.add(this.section(UIKeys.CAMERA_PANELS_TARGET, this.selector, this.group));
        this.panels.add(this.section(UIPathFields.key("progress"),
            UI.row(this.start, this.end), this.interp, this.rotation));
        this.panels.add(this.offset, this.angle, this.fov, this.active);
    }

    private void pickPath()
    {
        UIFilmPanel panel = this.getParent(UIFilmPanel.class);
        var entity = panel == null ? null : panel.getController().getEntities().get(this.clip.selector.get());
        if (entity == null || entity.getForm() == null) return;
        Set<String> paths = new HashSet<>();
        for (var source : SplineEditorUtils.sourcesInTree(entity.getForm()))
        {
            if (source instanceof SplineForm spline) paths.add(FormUtils.getPath(spline));
        }
        UIBonePickerContextMenu picker = new UIBonePickerContextMenu(path -> this.clip.group.set(path));
        picker.attachments(entity.getForm(), paths).set(this.clip.group.get());
        this.getContext().replaceContextMenu(picker);
    }

    @Override
    public void render(UIContext context)
    {
        UIFilmPanel panel = this.getParent(UIFilmPanel.class);
        var replay = panel == null ? null : panel.getData().replays.getById(this.clip.selector.get());
        this.selector.label = replay == null ? UIKeys.CAMERA_PANELS_TARGET_TITLE : IKey.constant(replay.getName());
        var entity = panel == null ? null : panel.getController().getEntities().get(this.clip.selector.get());
        var form = entity == null || entity.getForm() == null ? null : FormUtils.getForm(entity.getForm(), this.clip.group.get());
        this.group.setEnabled(entity != null && entity.getForm() != null);
        this.group.label = form instanceof SplineForm && FormUtils.getPath(form).equals(this.clip.group.get())
            ? IKey.constant(form.getDisplayName()) : UIPathFields.key("missing");
        super.render(context);
    }

    @Override
    public void editClip(Position position)
    {
        UIFilmPanel panel = this.getParent(UIFilmPanel.class);
        UIContext context = this.getContext();
        if (panel != null && context != null)
        {
            float tick = this.editor.getKeyframeCursor(context.getTransition()) - this.clip.tick.get();
            TrackerFrame frame = TrackerFrame.resolvePath(panel.getController().getEntities(), this.clip, tick,
                position.point.x, position.point.y, position.point.z, context.getTransition());
            if (frame != null) this.applyCameraPosition(frame, position);
        }
        super.editClip(position);
    }

    private void applyCameraPosition(TrackerFrame frame, Position position)
    {
        if (this.isActive(0, 1, 2))
        {
            Vector3d current = frame.position(this.clip.offset.get());
            Point solved = frame.solveOffset(
                this.clip.isActive(0) ? position.point.x : current.x,
                this.clip.isActive(1) ? position.point.y : current.y,
                this.clip.isActive(2) ? position.point.z : current.z);
            if (solved != null) this.clip.offset.set(solved);
        }
        if (this.isActive(3, 4, 5))
        {
            Angle current = frame.angles(this.clip.angle.get());
            this.clip.angle.set(frame.solveAngles(
                this.clip.isActive(3) ? position.angle.yaw : current.yaw,
                this.clip.isActive(4) ? position.angle.pitch : current.pitch,
                this.clip.isActive(5) ? position.angle.roll : current.roll));
        }
        if (this.clip.isActive(6) && this.clip.fov.get() != position.angle.fov) this.clip.fov.set(position.angle.fov);
    }

    private boolean isActive(int... bits)
    {
        for (int bit : bits) if (this.clip.isActive(bit)) return true;
        return false;
    }
}

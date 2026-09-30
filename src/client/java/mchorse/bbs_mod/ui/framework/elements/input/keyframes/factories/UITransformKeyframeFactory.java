package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.function.Consumer;

public class UITransformKeyframeFactory extends UIKeyframeFactory<Transform>
{
    @Override public Transform getGizmoTransform(Transform value) { return value; }

    public UIPropTransform transform;

    public UITransformKeyframeFactory(UITrackValue<Transform> track, UIKeyframes editor)
    {
        super(track, editor);

        UIKeyframeSheet sheet = track.sheet;
        boolean point = sheet != null && mchorse.bbs_mod.ui.utils.SplineEditorUtils.isPoint(mchorse.bbs_mod.film.replays.tracks.TrackId.parse(sheet.id));
        this.transform = point ? new UIPointTransform(this) : new UIPoseTransforms(this);
        this.transform.enableHotkeys(() -> true, op -> !point || op == mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp.TRANSLATE);
        this.transform.setTransform(track.getValue());

        this.scroll.add(this.transform);
    }

    @Override
    public void update()
    {
        if (!this.transform.isUserEditing()) this.transform.setTransform(this.getDisplayValue());
    }

    /** A point has a position; hidden rotation/scale channels must not accept gestures. */
    private static class UIPointTransform extends UIPoseTransforms
    {
        public UIPointTransform(UITransformKeyframeFactory editor)
        {
            super(editor);
            this.noScale();
            this.rotateRow.setVisible(false);
            this.h(2 * mchorse.bbs_mod.ui.utils.UIConstants.CONTROL_HEIGHT);
        }
    }

    public static class UIPoseTransforms extends UIKeyframePropTransform
    {
        private UITransformKeyframeFactory editor;

        public UIPoseTransforms(UITransformKeyframeFactory editor)
        {
            this.editor = editor;
        }

        @Override
        protected UIKeyframes getKeyframes()
        {
            return this.editor.editor;
        }

        @Override
        protected void applyToSelection(Consumer<Transform> consumer)
        {
            apply(this.editor.track, consumer);
        }

        public static void apply(UITrackValue<Transform> track, Consumer<Transform> consumer)
        {
            track.edit(consumer);
        }
    }
}

package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.function.Consumer;

public class UITransformKeyframeFactory extends UIKeyframeFactory<Transform>
{
    public UIPropTransform transform;

    public UITransformKeyframeFactory(Keyframe<Transform> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);
        boolean point = sheet != null && mchorse.bbs_mod.ui.utils.SplineEditorUtils.isPoint(mchorse.bbs_mod.film.replays.tracks.TrackId.parse(sheet.id));
        this.transform = point ? new UIPointTransform(this) : new UIPoseTransforms(this);
        this.transform.enableHotkeys(() -> true, op -> !point || op == mchorse.bbs_mod.ui.framework.elements.input.drag.TransformOp.TRANSLATE);
        this.transform.setTransform(keyframe.getValue());

        this.scroll.add(this.transform);
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
            apply(this.editor.editor, this.editor.keyframe, consumer);
        }

        @Override
        protected Transform getAutoKeyTransform(float tick)
        {
            UIKeyframeSheet sheet = this.editor.editor.getGraph().getSheet(this.editor.keyframe);
            Keyframe<Transform> target = sheet == null ? null : sheet.ensureKeyframe(tick);

            return target == null ? null : target.getValue();
        }

        public static void apply(UIKeyframes editor, Keyframe keyframe, Consumer<Transform> consumer)
        {
            UIReplaysEditorUtils.forEachSelectedKeyframe(editor, keyframe, (selected) ->
            {
                Transform transform = (Transform) selected.getValue();

                selected.preNotify();
                consumer.accept(transform);
                selected.postNotify();
            });
        }
    }
}

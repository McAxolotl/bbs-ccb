package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.RigBone;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.spline.SplineIK;
import mchorse.bbs_mod.cubic.spline.ModelSplineRuntime;
import mchorse.bbs_mod.cubic.spline.SplinePoint;
import mchorse.bbs_mod.film.replays.tracks.TrackId;
import mchorse.bbs_mod.film.replays.tracks.TrackKind;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import org.joml.Matrix4f;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UISplineKeyframeFactory;
import mchorse.bbs_mod.utils.Axis;

/** Point addresses and frames shared by all three editors. No synthetic bone names. */
public class SplineEditorUtils
{
    public static final Gizmo.HandleMask HANDLES = Gizmo.HandleMask.of(
        java.util.EnumSet.of(Gizmo.Op.MOVE, Gizmo.Op.SCREEN), java.util.EnumSet.noneOf(Axis.class));

    public record Point(ModelForm form, SplineIK chain, SplinePoint point, TrackId track) {}

    public static String selectedPath(UIKeyframeEditor editor)
    {
        if (editor == null || editor.editor == null) return null;
        if (editor.editor instanceof UISplineKeyframeFactory spline)
            return spline.pointPath();
        var sheet = editor.getSheet(editor.editor.getKeyframe());
        if (sheet == null) return null;
        TrackId id = TrackId.parse(sheet.id);
        return isPoint(id) ? sheet.id : null;
    }

    public static String compoundPath(String pointPath)
    {
        TrackId id = TrackId.parse(pointPath);
        return isPoint(id) ? TrackId.property(id.formPath(), "spline_ik").toKey() : pointPath;
    }

    public static void selectPoint(UIKeyframeEditor editor, String pointPath)
    {
        TrackId id = TrackId.parse(pointPath);
        if (isPoint(id) && editor.editor instanceof UISplineKeyframeFactory spline)
        {
            String[] parts = id.subject().split("/");
            spline.select(parts[1], parts[3]);
        }
    }

    public static boolean isPoint(TrackId id)
    {
        if (id == null || id.kind() != TrackKind.PROPERTY) return false;
        String[] parts = id.subject().split("/");
        return parts.length == 5 && parts[0].equals("splines") && parts[2].equals("points") && parts[4].equals("position");
    }

    public static Point resolve(Form root, String path)
    {
        if (root == null || path == null) return null;
        TrackId id = TrackId.parse(path);
        if (!isPoint(id)) return null;
        var value = FormUtils.getProperty(root, path);
        if (value == null || !(FormUtils.getForm(value) instanceof ModelForm form)) return null;
        String[] parts = id.subject().split("/");
        SplineIK chain = form.splines.get(parts[1]);
        if (chain == null) return null;
        SplinePoint point = chain.points.get(parts[3]);
        return point == null ? null : new Point(form, chain, point, id);
    }

    public static Matrix4f parentMatrix(Form root, IEntity entity, float transition, ModelForm form, SplineIK chain)
    {
        MatrixCache cache = FormUtilsClient.getRenderer(root).collectMatrices(entity, transition);
        ModelInstance instance = ModelFormRenderer.getModel(form);
        RigBone bone = instance == null ? null : instance.model.getBone(ModelSplineRuntime.getRoot(form, chain));
        if (bone == null) return null;
        String path = FormUtils.getPath(form);
        var renderer = (ModelFormRenderer) FormUtilsClient.getRenderer(form);
        var motion = renderer.getSplineMotion();
        if (motion != null && motion.chainId().equals(chain.getId()))
        {
            /* The driver path stays in the original model frame while the model travels.
             * Bone attachment matrices already contain that travel and cannot anchor it. */
            Matrix4f formMatrix = cache.get(path).matrix();
            return formMatrix == null ? null : new Matrix4f(formMatrix).rotateY((float) Math.PI).mul(motion.parentFrame());
        }
        RigBone parent = bone.getParentBone();
        String key = parent == null ? path : path.isEmpty() ? parent.getBoneName() : path + "/" + parent.getBoneName();
        Matrix4f matrix = cache.get(key).matrix();
        if (matrix == null) return null;
        Matrix4f result = parentFrame(matrix, parent);
        if (parent == null && motion != null) result.mul(motion.matrix());
        return result;
    }

    /** Convert the attachment cache to the cubic frame in which spline controls are stored. */
    public static Matrix4f parentFrame(Matrix4f attachmentMatrix, RigBone parent)
    {
        Matrix4f result = new Matrix4f(attachmentMatrix);
        if (parent == null)
        {
            return result.rotateY((float) Math.PI);
        }
        if (parent instanceof ModelGroup group)
        {
            /* captureMatrices appends T(pivot) * Ry(PI) for bone attachments.
             * Controls use the render frame before that suffix, including the parent's scale. */
            result.rotateY(-(float) Math.PI).translate(
                -group.initial.translate.x / 16F,
                -group.initial.translate.y / 16F,
                -group.initial.translate.z / 16F);
        }
        return result;
    }

    public static Matrix4f pointMatrix(Form root, IEntity entity, float transition, Point point)
    {
        if (point == null) return null;
        Matrix4f parent = parentMatrix(root, entity, transition, point.form, point.chain);
        return parent == null ? null : parent.translate(point.form.splineIK.get().get(point.chain.getId()).point(point.point.getId()).translate);
    }
}

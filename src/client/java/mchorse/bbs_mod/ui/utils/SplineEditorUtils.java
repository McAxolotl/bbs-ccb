package mchorse.bbs_mod.ui.utils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import mchorse.bbs_mod.settings.values.base.BaseValue;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.RigBone;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.spline.SplineIK;
import mchorse.bbs_mod.cubic.spline.SplineSource;
import mchorse.bbs_mod.forms.forms.SplineForm;
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
import org.joml.Vector3f;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.ui.framework.UIContext;
import java.util.Set;

/** Point addresses and frames shared by all three editors. No synthetic bone names. */
public class SplineEditorUtils
{
    /** A standalone spline owns its controls; an IK chain belongs to its enclosing model. */
    public static Form owner(SplineSource source)
    {
        return source instanceof Form form ? form : FormUtils.getForm((BaseValue) source);
    }

    public static List<SplineSource> sources(Form form)
    {
        if (form instanceof SplineForm spline) return List.of(spline);
        return form instanceof ModelForm model ? new ArrayList<>(model.splines.getAllTyped()) : List.of();
    }

    public static List<SplineSource> sourcesInTree(Form form)
    {
        List<SplineSource> result = new ArrayList<>(sources(form));
        if (form != null) for (var part : form.parts.getAllTyped())
            if (part.getForm() != null) result.addAll(sourcesInTree(part.getForm()));
        return result;
    }

    public static final Gizmo.HandleMask HANDLES = Gizmo.HandleMask.of(
        EnumSet.of(Gizmo.Op.MOVE, Gizmo.Op.SCREEN), EnumSet.noneOf(Axis.class));

    public record Point(Form form, SplineSource chain, SplinePoint point, TrackId track) {}

    public static Set<String> selectedPoints(UIKeyframeEditor editor, SplineSource chain)
    {
        if (editor == null) return Set.of();
        if (editor.editor instanceof SplineKeyframeEditor spline) return spline.selectedPoints(chain);
        TrackId id = TrackId.parse(selectedPath(editor));
        if (!isPoint(id)) return Set.of();
        String[] parts = id.subject().split("/");
        return parts.length == 5 && chain instanceof SplineIK ik && FormUtils.getPath(owner(chain)).equals(id.formPath()) && ik.getId().equals(parts[1]) ? Set.of(parts[3]) : Set.of();
    }

    public static String hoveredPoint(UIKeyframeEditor editor, UIContext context, SplineSource chain)
    {
        return editor != null && editor.editor instanceof SplineKeyframeEditor spline ? spline.hoveredPoint(context, chain) : "";
    }

    public static void viewportHover(UIKeyframeEditor editor, SplineOverlay.Hit hit)
    {
        if (editor != null && editor.editor instanceof SplineKeyframeEditor spline)
            spline.viewportHover(hit == null ? null : hit.chain(), hit == null ? "" : hit.point().getId());
    }

    public static String selectedPath(UIKeyframeEditor editor)
    {
        if (editor == null || editor.editor == null) return null;
        if (editor.editor instanceof SplineKeyframeEditor spline)
            return spline.pointPath();
        var sheet = editor.editor.getSheet();
        if (sheet == null) return null;
        TrackId id = TrackId.parse(sheet.id);
        return isPoint(id) ? sheet.id : null;
    }

    public static String compoundPath(String pointPath)
    {
        TrackId id = TrackId.parse(pointPath);
        return isPoint(id) ? TrackId.property(id.formPath(), id.subject().startsWith("points/") ? "curve" : "spline_ik").toKey() : pointPath;
    }

    public static void selectPoint(UIKeyframeEditor editor, String pointPath)
    {
        TrackId id = TrackId.parse(pointPath);
        if (isPoint(id) && editor.editor instanceof SplineKeyframeEditor spline)
        {
            spline.selectPoint(pointPath);
        }
    }

    public static boolean isPoint(TrackId id)
    {
        if (id == null || id.kind() != TrackKind.PROPERTY) return false;
        String[] parts = id.subject().split("/");
        return (parts.length == 3 && parts[0].equals("points") && parts[2].equals("position")) || parts.length == 5 && parts[0].equals("splines") && parts[2].equals("points") && parts[4].equals("position");
    }

    public static Point resolve(Form root, String path)
    {
        if (root == null || path == null) return null;
        TrackId id = TrackId.parse(path);
        if (!isPoint(id)) return null;
        var value = FormUtils.getProperty(root, path);
        if (value == null) return null;
        if (FormUtils.getForm(value) instanceof SplineForm spline)
        {
            SplinePoint point = spline.points.get(id.subject().split("/")[1]);
            return point == null ? null : new Point(spline, spline, point, id);
        }
        if (!(FormUtils.getForm(value) instanceof ModelForm form)) return null;
        String[] parts = id.subject().split("/");
        SplineIK chain = form.splines.get(parts[1]);
        if (chain == null) return null;
        SplinePoint point = chain.points.get(parts[3]);
        return point == null ? null : new Point(form, chain, point, id);
    }

    public static Matrix4f parentMatrix(Form root, IEntity entity, float transition, Form owner, SplineSource source)
    {
        MatrixCache cache = FormUtilsClient.getRenderer(root).collectMatrices(entity, transition);
        if (owner instanceof SplineForm)
        {
            Matrix4f matrix = cache.get(FormUtils.getPath(owner)).matrix();
            return matrix == null ? null : new Matrix4f(matrix);
        }
        if (!(owner instanceof ModelForm form) || !(source instanceof SplineIK chain)) return null;
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
        return parent == null ? null : parent.translate(point.chain.position(point.point.getId()).translate);
    }

    /** Root pivot in the same parent frame as the authored control points. */
    public static Vector3f rootPosition(Form root, IEntity entity, float transition, ModelForm form, SplineIK chain)
    {
        Matrix4f parent = parentMatrix(root, entity, transition, form, chain);
        if (parent == null || Math.abs(parent.determinant()) < 1E-8F) return null;
        String bone = ModelSplineRuntime.getRoot(form, chain);
        String path = FormUtils.getPath(form);
        Matrix4f matrix = FormUtilsClient.getRenderer(root).collectMatrices(entity, transition)
            .get(path.isEmpty() ? bone : path + "/" + bone).matrix();
        if (matrix == null) return null;
        Vector3f position = parent.invert().transformPosition(matrix.getTranslation(new Vector3f()));
        return position.isFinite() ? position : null;
    }
}

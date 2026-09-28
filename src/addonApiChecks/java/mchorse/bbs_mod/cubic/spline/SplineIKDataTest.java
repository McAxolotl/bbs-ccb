package mchorse.bbs_mod.cubic.spline;

import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.replays.FormProperties;
import mchorse.bbs_mod.film.replays.tracks.TrackContext;
import mchorse.bbs_mod.film.replays.tracks.TrackId;
import mchorse.bbs_mod.film.replays.tracks.TrackKind;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.states.AnimationState;
import mchorse.bbs_mod.forms.states.StatePlayer;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.StableIds;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.Collections;

/** Data/playback checks run by the existing Fabric prelaunch harness (Minecraft requires its access transforms). */
public class SplineIKDataTest
{
    private static int checks;

    public static void main(String[] args) throws Exception
    {
        net.minecraft.SharedConstants.createGameVersion();
        net.minecraft.Bootstrap.initialize();
        KeyframeFactories.setup();
        ModelForm form = new ModelForm();
        form.setId("form");
        check(form.splines.isDefault(), "Old forms have no spline constraints");
        form.fromData(new MapType());
        check(form.splines.getList().isEmpty(), "Old data remains neutral");

        SplineIK spline = new SplineIK("");
        MapType legacySpline = spline.toData().asMap();
        legacySpline.remove("progress");
        legacySpline.remove("moveModel");
        spline.fromData(legacySpline);
        near(spline.progress.get(), 0, "Legacy spline data without progress remains neutral");
        check(!spline.moveModel.get(), "Legacy spline data keeps whole-model motion disabled");
        spline.progress.set(137.5F);
        spline.moveModel.set(true);
        spline.name.set("Snake");
        spline.chainLength.set(2);
        spline.tip.set("bone8");
        form.splines.add(spline);
        SplinePoint first = point(1), second = point(3), third = point(5);
        spline.points.add(first);
        spline.points.add(second);
        spline.points.add(third);
        check(StableIds.isStableId(spline.getId()) && StableIds.isStableId(second.getId()), "Chains and points receive stable ids");
        String path = FormUtils.getPropertyPath(second.position);
        TrackId id = TrackId.parse(path);
        check(path.equals("splines/" + spline.getId() + "/points/" + second.getId() + "/position"), "Nested property address");
        check(id.kind() == TrackKind.PROPERTY && id.formPath().isEmpty(), "Spline ids are not mistaken for body parts");
        check(FormUtils.getProperty(form, path) == second.position, "Resolver finds point");
        check(FormUtils.getForm(second.position) == form, "Nested property reports owner form");
        check(FormUtils.getProperty(form, path + "/invalid") == null, "Malformed paths do not match a prefix");
        check(FormUtils.collectPropertyPaths(form).contains("spline_ik") && !FormUtils.collectPropertyPaths(form).contains(path), "Only the compound spline property is offered");
        check(!spline.name.isVisible() && second.position.isVisible(), "Only supported settings offer tracks");

        BaseType saved = DataStorageUtils.readFromBytes(DataStorageUtils.writeToBytes(form.toData()));
        ModelForm loaded = new ModelForm();
        loaded.fromData(saved);
        check(loaded.splines.toData().equals(form.splines.toData()), "Binary form save/load preserves settings and identities");
        near(loaded.splines.get(spline.getId()).progress.get(), 137.5F, "Progress beyond 100 survives binary save/load");
        check(loaded.splines.get(spline.getId()).moveModel.get(), "Whole-model motion survives binary save/load");
        check(FormUtils.getProperty(loaded, path) != null, "Saved animation address resolves after load");
        ModelForm copied = new ModelForm();
        copied.copy(form);
        check(copied.splines.get(spline.getId()).moveModel.get(), "Form copy retains whole-model motion");
        near(copied.splines.get(spline.getId()).progress.get(), 137.5F, "Form copy retains progress");
        copied.splines.get(spline.getId()).progress.set(-25F);
        near(copied.splines.get(spline.getId()).progress.get(), -25, "Progress accepts negative percentages");
        near(spline.progress.get(), 137.5F, "Copied progress is independent of its source");
        SplinePoint copiedPoint = copied.splines.get(spline.getId()).points.get(second.getId());
        near(copiedPoint.position.get().translate.x, 3, "Copied form retains curve");
        copiedPoint.position.get().translate.x = 42;
        near(second.position.get().translate.x, 3, "Form copies never share point transforms");

        Collections.swap(spline.points.getAllTyped(), 0, 1);
        check(FormUtils.getProperty(form, path) == second.position, "Reordering keeps the same target");
        spline.points.add(0, point(-1));
        check(FormUtils.getProperty(form, path) == second.position, "Insertion keeps the same target");
        BaseType beforeRemoval = spline.points.toData();
        spline.points.getAllTyped().remove(second);
        check(FormUtils.getProperty(form, path) == null, "Removed targets become orphaned");
        spline.points.fromData(beforeRemoval);
        second = spline.points.get(second.getId());
        check(FormUtils.getProperty(form, path) == second.position, "Undo data restores the original target");

        SplineIK duplicate = new SplineIK(spline.getId());
        duplicate.fromData(spline.toData());
        form.splines.add(duplicate);
        check(!duplicate.getId().equals(spline.getId()), "Duplicated chains receive independent identities");

        int[] notifications = new int[2];
        form.preCallback((value, flag) -> notifications[0]++);
        form.postCallback((value, flag) -> notifications[1]++);
        second.position.set(transform(3));
        check(notifications[0] == 1 && notifications[1] == 1, "Point edits notify the owner for undo and cache invalidation");
        check(form.findRecursively(second.position.getPath()) == second.position, "Undo data path resolves nested stable ids");

        FormProperties film = new FormProperties("properties");
        check(film.getOrCreate(form, TrackId.parse(FormUtils.getPropertyPath(spline.moveModel))) == null,
            "Whole-model motion is a static setting, not an animatable track");
        TrackId progressId = TrackId.parse(FormUtils.getPropertyPath(spline.progress));
        KeyframeChannel<Float> progress = film.register(progressId, KeyframeFactories.FLOAT);
        check(progress != null && progress.getFactory() == KeyframeFactories.FLOAT, "Progress creates an ordinary float property track");
        progress.insert(0, -50F);
        progress.insert(10, 150F);
        KeyframeChannel<Transform> channel = film.register(id, KeyframeFactories.TRANSFORM);
        check(channel != null && channel.getFactory() == KeyframeFactories.TRANSFORM, "Nested point creates its ordinary transform track");
        channel.insert(0, transform(0));
        channel.insert(10, transform(10));
        film.apply(TrackContext.frame(form, 0, null), 5, 1);
        near(spline.progress.get(), 50, "Film interpolates progress between negative and beyond-100 keys");
        near(spline.progress.getOriginalValue(), 137.5F, "Progress playback preserves its authored value");
        near(second.position.get().translate.x, 5, "Film interpolates point position");
        near(second.position.getOriginalValue().translate.x, 3, "Playback leaves authored point unchanged");
        film.resetProperties(form);
        near(spline.progress.get(), 137.5F, "Film reset releases progress override");
        near(second.position.get().translate.x, 3, "Reset releases point runtime value");

        AnimationState state = new AnimationState("");
        state.properties.fromData(film.toData());
        StatePlayer player = new StatePlayer(state);
        for (int i = 0; i < 5; i++) player.update();
        player.assignValues(form, 0);
        near(spline.progress.get(), 50, "Animation state interpolates progress with solvers=false");
        near(second.position.get().translate.x, 5, "Animation state applies points with solvers=false");
        player.resetValues(form);
        near(spline.progress.get(), 137.5F, "Ending state restores authored progress");
        near(second.position.get().translate.x, 3, "Ending state restores authored position");
        state.properties.applyProperties(form, 5, 0.5F);
        near(spline.progress.get(), 93.75F, "State fade blends progress from its authored percentage");
        near(second.position.get().translate.x, 4, "State fade blends from current value");
        state.properties.resetProperties(form);

        TrackId influenceId = TrackId.parse(FormUtils.getPropertyPath(spline.influence));
        TrackId twistId = TrackId.parse(FormUtils.getPropertyPath(spline.twist));
        KeyframeChannel<Float> influence = film.register(influenceId, KeyframeFactories.FLOAT);
        KeyframeChannel<Float> twist = film.register(twistId, KeyframeFactories.FLOAT);
        influence.insert(0, 0F);
        influence.insert(10, 1F);
        twist.insert(0, 0F);
        twist.insert(10, 180F);
        film.applyProperties(form, 5);
        near(spline.influence.get(), 0.5F, "State path interpolates influence");
        near(spline.twist.get(), 90F, "State path interpolates twist");
        film.resetProperties(form);
        near(spline.influence.get(), 1, "Reset releases influence");
        near(spline.twist.get(), 0, "Reset releases twist");
        channel.reset();
        second.position.setRuntimeValue(transform(99));
        film.applyProperties(form, 5);
        near(second.position.get().translate.x, 3, "An empty channel releases its old override");
        film.resetProperties(form);

        BodyPart part = new BodyPart("");
        ModelForm outer = new ModelForm();
        outer.setId("form");
        outer.parts.add(part);
        part.setForm(form);
        String nestedPath = FormUtils.getPropertyPath(second.position);
        check(nestedPath.equals(part.getId() + "/" + path), "Body-part prefix is independent of curve identity");
        check(FormUtils.getProperty(outer, nestedPath) == second.position, "Nested form point resolves from outer root");
        check(TrackId.parse(nestedPath).formPath().equals(part.getId()), "Track separates owning body part from spline subject");
        FormProperties nested = new FormProperties("properties");
        check(nested.getOrCreate(outer, TrackId.property(part.getId(), "spline_ik")) != null, "Nested form offers compound spline channel");

        if (args.length > 0 && args[0].equals("client"))
        {
            checkUndo(outer, spline, second);
        }

        System.out.println("Spline IK data: " + checks + " checks passed");
    }

    private static void checkUndo(ModelForm outer, SplineIK spline, SplinePoint point) throws Exception
    {
        Class<?> type = Class.forName("mchorse.bbs_mod.ui.film.utils.undo.ValueChangeUndo");
        BaseType before = point.position.toData();
        point.position.set(transform(12));
        Object undo = type.getConstructor(mchorse.bbs_mod.utils.DataPath.class, BaseType.class, BaseType.class)
            .newInstance(point.position.getPath(), before, point.position.toData());
        type.getMethod("undo", ValueGroup.class).invoke(undo, outer);
        near(point.position.get().translate.x, 3, "Real undo restores nested point value");
        type.getMethod("redo", ValueGroup.class).invoke(undo, outer);
        near(point.position.get().translate.x, 12, "Real redo restores nested point value");

        before = spline.points.toData();
        BaseValue.edit(spline.points, points -> points.getAllTyped().remove(point));
        Object listUndo = type.getConstructor(mchorse.bbs_mod.utils.DataPath.class, BaseType.class, BaseType.class)
            .newInstance(spline.points.getPath(), before, spline.points.toData());
        type.getMethod("undo", ValueGroup.class).invoke(listUndo, outer);
        check(spline.points.get(point.getId()) != null, "Real undo restores deleted point identity");
        type.getMethod("undo", ValueGroup.class).invoke(undo, outer);
        near(spline.points.get(point.getId()).position.get().translate.x, 3, "Earlier point edit undo resolves replacement instance by stable id");
        type.getMethod("redo", ValueGroup.class).invoke(listUndo, outer);
        check(spline.points.get(point.getId()) == null, "Real redo removes the same point");
    }

    private static SplinePoint point(float x)
    {
        SplinePoint point = new SplinePoint("");
        point.position.set(transform(x));
        return point;
    }

    private static Transform transform(float x)
    {
        Transform transform = new Transform();
        transform.translate.x = x;
        return transform;
    }

    private static void near(float actual, float expected, String message)
    {
        check(Math.abs(actual - expected) < 0.0001F, message + " (" + actual + " != " + expected + ")");
    }

    private static void check(boolean condition, String message)
    {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}

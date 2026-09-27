package mchorse.bbs_mod.cubic.spline;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;

/** Run main with JOML on the classpath; deliberately independent of the game and GL. */
public class SplineIKSolverTest
{
    private static int checks;

    public static void main(String[] args)
    {
        List<Vector3f> line = List.of(new Vector3f(2, 3, -4), new Vector3f(2, 3, 6));
        close(SplineCurve.evaluate(line, 0.3F), new Vector3f(2, 3, -1), "Two points interpolate linearly");
        float[] lengths = {1F, 2F, 3F};
        SplineIKSolver.Result straight = SplineIKSolver.solve(line, lengths, false);
        close(straight.joints()[3], new Vector3f(2, 3, 2), "Preserve length stops before a long curve ends");
        lengths(straight, lengths);

        List<Vector3f> shortLine = List.of(new Vector3f(), new Vector3f(0, 0, 0.2F));
        SplineIKSolver.Result extended = SplineIKSolver.solve(shortLine, lengths, false);
        close(extended.joints()[3], new Vector3f(0, 0, 6), "Short curves extend the final tangent");
        lengths(extended, lengths);
        SplineIKSolver.Result fit = SplineIKSolver.solve(shortLine, lengths, true);
        close(fit.joints()[3], shortLine.get(1), "Fit reaches the curve end");

        List<Vector3f> curved = List.of(new Vector3f(), new Vector3f(1, 2, 0), new Vector3f(3, -1, 2), new Vector3f(5, 0, 0));
        float[] small = new float[30];
        Arrays.fill(small, 0.4F);
        SplineIKSolver.Result a = SplineIKSolver.solve(curved, small, false);
        lengths(a, small);
        SplineIKSolver.Result b = SplineIKSolver.solve(curved, small, false);
        for (int i = 0; i < a.joints().length; i++) close(a.joints()[i], b.joints()[i], "Repeated solves have no history");

        List<Vector3f> duplicates = List.of(new Vector3f(), new Vector3f(), new Vector3f(1, 0, 0), new Vector3f(1, 0, 0));
        lengths(SplineIKSolver.solve(duplicates, small, false), small);
        check(SplineIKSolver.solve(List.of(new Vector3f(), new Vector3f()), lengths, false) == null, "Collapsed curves reject safely");
        check(SplineIKSolver.solve(List.of(new Vector3f(Float.NaN), new Vector3f()), lengths, false) == null, "Non-finite controls reject safely");
        check(SplineIKSolver.solve(line, new float[] {0F}, false) == null, "Zero-length bones reject safely");

        Vector3f previous = new Vector3f(0, 0, 1);
        Vector3f normal = SplineIKSolver.perpendicular(previous);
        for (Vector3f tangent : a.tangents())
        {
            normal = SplineIKSolver.transport(normal, previous, tangent);
            check(normal.isFinite() && Math.abs(normal.dot(tangent)) < 1E-4F, "Transport normal is perpendicular and finite");
            Quaternionf frame = SplineIKSolver.frame(tangent, normal);
            close(frame.transform(new Vector3f(0, 0, 1)), tangent, "Frame points along tangent");
            previous = tangent;
        }
        Vector3f reversed = new Vector3f(previous).negate();
        normal = SplineIKSolver.transport(normal, previous, reversed);
        check(normal.isFinite() && Math.abs(normal.dot(reversed)) < 1E-4F, "Exact reversal has stable normal");
        progress(line, lengths, straight);
        System.out.println("SplineIKSolverTest: " + checks + " checks passed");
    }

    private static void progress(List<Vector3f> line, float[] lengths, SplineIKSolver.Result original)
    {
        SplineIKSolver.Result positive = SplineIKSolver.solve(line, lengths, false, 0.5F);
        close(positive.joints()[0], new Vector3f(2, 3, 1), "Progress places root at half the curve's arc length");
        close(positive.joints()[3], new Vector3f(2, 3, 7), "The entire chain advances beyond the end without clamping");
        lengths(positive, lengths);

        SplineIKSolver.Result negative = SplineIKSolver.solve(line, lengths, false, -0.2F);
        close(negative.joints()[0], new Vector3f(2, 3, -6), "Negative progress extends the initial tangent");
        close(negative.joints()[3], new Vector3f(2, 3, 0), "A chain entering the curve keeps moving through its start");
        lengths(negative, lengths);
        SplineIKSolver.Result beyond = SplineIKSolver.solve(line, lengths, false, 1.2F);
        close(beyond.joints()[0], new Vector3f(2, 3, 8), "Progress above one extends the final tangent");
        close(beyond.joints()[3], new Vector3f(2, 3, 14), "Progress above one does not loop back");
        lengths(beyond, lengths);
        SplineIKSolver.Result before = SplineIKSolver.solve(line, lengths, false, -1F);
        close(before.joints()[3], new Vector3f(2, 3, -8), "A whole chain may remain before the curve");
        close(before.tangents()[3], new Vector3f(0, 0, 1), "Terminal before the curve follows its initial tangent");

        SplineIKSolver.Result fitBase = SplineIKSolver.solve(line, lengths, true);
        for (float offset : new float[] {-0.25F, 0.25F})
        {
            SplineIKSolver.Result fitMoved = SplineIKSolver.solve(line, lengths, true, offset);
            for (int i = 0; i < fitMoved.joints().length; i++)
                close(fitMoved.joints()[i], new Vector3f(fitBase.joints()[i]).add(0, 0, offset * 10F), "Fit shifts its full interval without compressing it");
        }

        List<Vector3f> symmetric = List.of(new Vector3f(-2, 0, 0), new Vector3f(0, 2, 0), new Vector3f(2, 0, 0));
        float[] shortLengths = {0.4F, 0.4F, 0.4F};
        SplineIKSolver.Result curved = SplineIKSolver.solve(symmetric, shortLengths, false, 0.5F);
        close(curved.joints()[0], symmetric.get(1), "Arc midpoint of a symmetric curved spline is its middle control");
        lengths(curved, shortLengths);
        SplineIKSolver.solve(symmetric, shortLengths, false, -0.3F);
        SplineIKSolver.Result repeated = SplineIKSolver.solve(symmetric, shortLengths, false, 0.5F);
        for (int i = 0; i < curved.joints().length; i++) close(repeated.joints()[i], curved.joints()[i], "Progress can scrub backward and forward without history");

        SplineIKSolver.Result zero = SplineIKSolver.solve(line, lengths, false, 0F);
        for (int i = 0; i < original.joints().length; i++) close(zero.joints()[i], original.joints()[i], "Returning progress to zero restores the original pose");
        SplineIKSolver.Result atEnd = SplineIKSolver.solve(line, lengths, false, 1F);
        SplineIKSolver.Result afterEnd = SplineIKSolver.solve(line, lengths, false, 1.000005F);
        check(atEnd.joints()[0].distance(afterEnd.joints()[0]) < 1E-4F, "Crossing progress one is continuous");
        check(SplineIKSolver.solve(line, lengths, false, Float.NaN) == null, "NaN progress rejects safely");
        check(SplineIKSolver.solve(line, lengths, true, Float.POSITIVE_INFINITY) == null, "Infinite progress rejects safely");
    }

    private static void lengths(SplineIKSolver.Result result, float[] lengths)
    {
        check(result != null, "Valid curve solves");
        for (int i = 0; i < lengths.length; i++) check(Math.abs(result.joints()[i].distance(result.joints()[i + 1]) - lengths[i]) < 1E-4F, "Chord length preserved at " + i);
    }

    private static void close(Vector3f actual, Vector3f expected, String message)
    {
        check(actual.distance(expected) < 1E-4F, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean condition, String message)
    {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}

package mchorse.bbs_mod.cubic.data.model;

import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import org.joml.Vector3f;

/** Standalone save/load and undo snapshot checks; no Minecraft client required. */
public class ModelMeshSerializationTest
{
    public static void main(String[] args)
    {
        for (Vector3f origin : new Vector3f[] {
            new Vector3f(), new Vector3f(10, 20, 30), new Vector3f(-7, 12, -23),
            new Vector3f(0.25F, -1.5F, 3.75F)
        })
        {
            MapType input = new MapType();
            input.put("origin", numbers(origin.x, origin.y, origin.z));
            input.put("rotate", numbers(15, -30, 45));
            input.put("vertices", numbers(1, 2, 3, 2, 2, 3, 1, 3, 3));
            input.put("uvs", numbers(0, 0, 1, 0, 0, 1));
            input.putString("material", "detail");

            ModelMesh mesh = new ModelMesh();
            mesh.fromData(input);
            Vector3f firstVertex = new Vector3f(mesh.baseData.vertices.get(0));
            MapType before = mesh.toData();
            check(mesh.baseData.vertices.get(0).equals(firstVertex), "Saving mutated the live mesh");
            check(before.equals(input), "Saved vertices must be relative to the pivot: " + origin);

            for (int i = 0; i < 5; i++)
            {
                ModelMesh loaded = new ModelMesh();
                loaded.fromData(mesh.toData());
                check(loaded.baseData.vertices.equals(mesh.baseData.vertices), "Reload moved vertices");
                check(loaded.baseData.uvs.equals(mesh.baseData.uvs), "Reload changed UVs");
                check(loaded.baseData.normals.equals(mesh.baseData.normals), "Reload changed normals");
                check(loaded.origin.equals(mesh.origin), "Reload moved the pivot");
                check(loaded.rotate.equals(mesh.rotate), "Reload changed rotation");
                check(loaded.material.equals(mesh.material), "Reload changed material");
                mesh = loaded;
            }

            /* The editor's undo/redo restores serialized model data. Change the pivot and
             * geometry independently, then restore both snapshots on the same mesh. */
            mesh.origin.add(2, -3, 4);
            for (Vector3f vertex : mesh.baseData.vertices) vertex.add(5, 6, -7);
            Vector3f editedVertex = new Vector3f(mesh.baseData.vertices.get(0));
            Vector3f editedPivot = new Vector3f(mesh.origin);
            MapType after = mesh.toData();
            mesh.fromData(before);
            check(mesh.baseData.vertices.get(0).equals(firstVertex), "Undo moved vertices");
            check(mesh.origin.equals(origin), "Undo moved the pivot");
            mesh.fromData(after);
            check(mesh.baseData.vertices.get(0).equals(editedVertex), "Redo moved vertices");
            check(mesh.origin.equals(editedPivot), "Redo moved the pivot");
        }

        System.out.println("Mesh save/load and undo snapshot checks passed");
    }

    private static ListType numbers(float... values)
    {
        ListType list = new ListType();
        for (float value : values) list.addFloat(value);
        return list;
    }

    private static void check(boolean condition, String message)
    {
        if (!condition) throw new AssertionError(message);
    }
}
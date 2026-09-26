package mchorse.bbs_mod.utils.iris;

import net.irisshaders.iris.Iris;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Called only when Iris is installed. No Iris types cross the UI boundary. */
public final class IrisShaderPacks
{
    private IrisShaderPacks() {}

    public static List<String> list() throws IOException
    {
        List<String> packs = new ArrayList<>(Iris.getShaderpacksDirectoryManager().enumerate());
        packs.sort(String.CASE_INSENSITIVE_ORDER);
        return packs;
    }

    public static File folder()
    {
        return Iris.getShaderpacksDirectory().toFile();
    }

    public static String current()
    {
        return Iris.getIrisConfig().areShadersEnabled() && Iris.getCurrentPack().isPresent()
            ? Iris.getIrisConfig().getShaderPackName().orElse("") : "";
    }

    public static void select(String pack) throws IOException
    {
        String previous = Iris.getIrisConfig().getShaderPackName().orElse(null);
        boolean enabled = Iris.getIrisConfig().areShadersEnabled();
        try
        {
            Iris.clearShaderPackOptionQueue();
            if (!pack.isEmpty()) Iris.getIrisConfig().setShaderPackName(pack);
            Iris.getIrisConfig().setShadersEnabled(!pack.isEmpty());
            Iris.getIrisConfig().save();
            Iris.reload();
            if (!pack.isEmpty() && (Iris.getCurrentPack().isEmpty() || Iris.getStoredError().isPresent()))
            {
                throw new IOException("Could not load shader pack: " + pack, Iris.getStoredError().orElse(null));
            }
        }
        catch (Exception error)
        {
            Iris.getIrisConfig().setShaderPackName(previous);
            Iris.getIrisConfig().setShadersEnabled(enabled);
            try
            {
                Iris.getIrisConfig().save();
                Iris.reload();
            }
            catch (Exception restoreError)
            {
                error.addSuppressed(restoreError);
            }
            throw new IOException("Could not switch shader pack", error);
        }
    }
}

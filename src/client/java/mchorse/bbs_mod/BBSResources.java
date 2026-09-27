package mchorse.bbs_mod;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.forms.structure.StructureManager;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.ExternalAssetsSourcePack;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.utils.resources.MultiLinkThread;
import mchorse.bbs_mod.utils.watchdog.WatchDog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class BBSResources
{
    private static volatile List<WatchDog> watchDogs = List.of();
    private static volatile long watchDogGeneration;

    /**
     * Bumped on every change the watchdog sees in the assets folder. A browser over the
     * assets compares it against what it last saw and relists — no per-instance listener
     * to register and forget, no polling of the disk.
     */
    private static int assetsVersion;

    public static void init()
    {
        setupWatchdog();

        BBSModClient.getFormCategories().setup();
    }

    public static synchronized void setupWatchdog()
    {
        stopWatchdog();

        File assetsFolder = BBSMod.getAssetsFolder();
        File sharedFolder = BBSMod.getOriginalSourcePack().getFolder();
        List<WatchDog> watchers = new ArrayList<>();

        watchers.add(createWatchdog(assetsFolder));

        if (!assetsFolder.equals(sharedFolder))
        {
            watchers.add(createWatchdog(sharedFolder));
        }

        watchDogs = List.copyOf(watchers);

        for (WatchDog watcher : watchDogs)
        {
            watcher.start();
        }
    }

    private static WatchDog createWatchdog(File folder)
    {
        long generation = watchDogGeneration;
        WatchDog watchDog = new WatchDog(folder, false, (runnable) -> MinecraftClient.getInstance().execute(() ->
        {
            if (generation == watchDogGeneration)
            {
                runnable.run();
            }
        }));
        watchDog.getProxy().register(BBSModClient.getTextures());
        watchDog.getProxy().register(BBSModClient.getModels());
        watchDog.getProxy().register(BBSModClient.getSounds());
        watchDog.getProxy().register(BBSModClient.getFonts());
        watchDog.getProxy().register(BBSModClient.getFormCategories());
        watchDog.getProxy().register((path, event) -> assetsVersion += 1);

        return watchDog;
    }

    public static int getAssetsVersion()
    {
        return assetsVersion;
    }

    /** For code that changed the assets itself and wants browsers to relist right away. */
    public static void markAssetsChanged()
    {
        assetsVersion += 1;
    }

    public static synchronized void stopWatchdog()
    {
        watchDogGeneration += 1;

        for (WatchDog watchDog : watchDogs)
        {
            watchDog.stop();
        }

        watchDogs = List.of();
    }

    public static void enterWorld()
    {
        var server = MinecraftClient.getInstance().getServer();
        File folder = null;

        if (server != null)
        {
            try
            {
                folder = server.getSavePath(WorldSavePath.ROOT).resolve("bbs/assets").toFile();

                for (String path : List.of("models", "textures", "audio", "video", "fonts", "particles", "structures"))
                {
                    Files.createDirectories(folder.toPath().resolve(path));
                }
            }
            catch (IOException e)
            {
                /* Do not quietly let editors write to the shared library on a failed world mount. */
                e.printStackTrace();
                MinecraftClient.getInstance().getNetworkHandler().getConnection()
                    .disconnect(Text.literal(UIKeys.WORLD_ASSETS_ERROR.get()));

                return;
            }
        }

        useAssetsFolder(folder);
    }

    public static void leaveWorld()
    {
        useAssetsFolder(null);
    }

    private static void useAssetsFolder(File folder)
    {
        RenderSystem.assertOnRenderThread();

        File target = folder == null ? BBSMod.getOriginalSourcePack().getFolder() : folder;

        if (BBSMod.getAssetsFolder().toPath().toAbsolutePath().normalize().equals(target.toPath().toAbsolutePath().normalize()))
        {
            return;
        }

        stopWatchdog();

        MultiLinkThread.clear();

        /* Wait for the current background model load before replacing its provider's source. */
        synchronized (BBSModClient.getModels())
        {
            BBSMod.getDynamicSourcePack().setSecondary(folder == null ? null :
                new ExternalAssetsSourcePack(Link.ASSETS, folder).providesFiles());
            BBSModClient.getModels().reload();
        }

        BBSModClient.getVideos().delete();
        BBSModClient.getSounds().deleteSounds();
        BBSModClient.getFonts().delete();
        BBSModClient.getTextures().delete();
        StructureManager.invalidate();
        BBSModClient.reloadLanguage(BBSModClient.getLanguageKey());
        BBSModClient.getFormCategories().reloadAssets();
        markAssetsChanged();
        setupWatchdog();
    }

    public static void tick()
    {
        for (WatchDog current : watchDogs)
        {
            current.getProxy().tick();
        }
    }
}

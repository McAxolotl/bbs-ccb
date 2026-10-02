package mchorse.bbs_mod.ui.framework.elements.input.list;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.audio.AudioCacheManager;
import mchorse.bbs_mod.audio.SoundLikeManager;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.RowStyle;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * List component for Minecraft vanilla sound effects with preview and download support
 */
public class UIVanillaSoundList extends UIStringList
{
    private final Map<String, VanillaSoundAsset> soundAssetMap = new HashMap<>();
    private UIIcon likeButton;
    private UIIcon downloadButton;
    private Consumer<String> downloadCallback;
    private Runnable likeToggleCallback;
    private SoundLikeManager likeManager;
    private boolean loaded = false;
    private JsonObject cachedSoundsJson = null;

    /** Guards against cyclic "type": "event" references while resolving sound entries. */
    private static final int MAX_EVENT_DEPTH = 16;

    public UIVanillaSoundList(Consumer<List<String>> callback, SoundLikeManager likeManager)
    {
        super(callback);
        this.likeButton = new UIIcon(Icons.LIKE, null);
        this.downloadButton = new UIIcon(Icons.DOWNLOAD, null);
        this.likeManager = likeManager;
    }

    /**
     * Ensure list is loaded (lazy loading)
     */
    private void ensureLoaded()
    {
        if (!this.loaded)
        {
            this.loadVanillaSoundFiles();
            this.populateList();

            this.loaded = true;
        }
    }

    /**
     * Set download callback
     */
    public void setDownloadCallback(Consumer<String> callback)
    {
        this.downloadCallback = callback;
    }

    /**
     * Set like toggle callback
     */
    public void setLikeToggleCallback(Runnable callback)
    {
        this.likeToggleCallback = callback;
    }

    /**
     * Detect sound category from path
     */
    private String detectSoundCategory(String soundPath)
    {
        if (soundPath.startsWith("record."))
        {
            return "Record";
        }
        else if (soundPath.startsWith("music."))
        {
            return "Music";
        }
        else if (soundPath.startsWith("ambient."))
        {
            return "Ambient";
        }
        else if (soundPath.startsWith("block."))
        {
            return "Block";
        }
        else if (soundPath.startsWith("entity."))
        {
            return "Entity";
        }
        else if (soundPath.startsWith("item."))
        {
            return "Item";
        }
        else if (soundPath.startsWith("ui."))
        {
            return "UI";
        }
        else if (soundPath.startsWith("weather."))
        {
            return "Weather";
        }
        else
        {
            return "Other";
        }
    }

    /**
     * Load vanilla sound files using Fabric API
     */
    private void loadVanillaSoundFiles()
    {
        this.soundAssetMap.clear();

        Set<String> existingDisplayNames = new HashSet<>();

        try
        {
            MinecraftClient client = MinecraftClient.getInstance();
            ResourceManager resourceManager = client.getResourceManager();

            if (this.cachedSoundsJson == null)
            {
                this.cachedSoundsJson = this.loadSoundsJson(resourceManager);
            }

            Registry<SoundEvent> soundRegistry = Registries.SOUND_EVENT;

            for (Identifier soundId : soundRegistry.getIds())
            {
                if (soundId.getNamespace().equals("minecraft"))
                {
                    String soundPath = soundId.getPath();
                    String displayName = soundPath;
                    int suffix = 1;
                    String category = this.detectSoundCategory(soundPath);

                    while (existingDisplayNames.contains(displayName))
                    {
                        displayName = soundPath + "_" + suffix++;
                    }

                    existingDisplayNames.add(displayName);

                    List<String> actualSoundPaths = this.findAllSoundFilesFromCache(soundId);

                    if (actualSoundPaths != null && !actualSoundPaths.isEmpty())
                    {
                        for (int i = 0; i < actualSoundPaths.size(); i++)
                        {
                            String soundPathFull = actualSoundPaths.get(i);

                            /* toResourcePath() always yields an .ogg path */
                            String flatDisplayName = soundPathFull.substring(0, soundPathFull.length() - 4).replace("/", "_");
                            
                            String uniqueKey = flatDisplayName;
                            int uniqueSuffix = 1;

                            while (existingDisplayNames.contains(uniqueKey))
                            {
                                uniqueKey = flatDisplayName + "_" + uniqueSuffix++;
                            }
                            
                            existingDisplayNames.add(uniqueKey);
                            
                            List<String> singlePath = new ArrayList<>();
                            singlePath.add(soundPathFull);
                            
                            this.soundAssetMap.put(uniqueKey, new VanillaSoundAsset(uniqueKey, soundId.toString(), singlePath, category));
                        }
                    }
                    else
                    {
                        String flatDisplayName = soundPath.replace(".", "_");

                        this.soundAssetMap.put(flatDisplayName, new VanillaSoundAsset(flatDisplayName, soundId.toString(), new ArrayList<>(), category));
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    /**
     * Load and cache the merged sounds.json.
     *
     * <p>Vanilla merges {@code sounds.json} from every resource pack (lowest priority first,
     * highest last). A mod like VanillaBackport ships a partial {@code minecraft:sounds.json}
     * holding only the sounds it backports, so reading just {@link ResourceManager#getResource}
     * would return that partial file and hide every vanilla sound it doesn't list.
     *
     * <p>Per entry this mirrors vanilla's {@code SoundList#register}: a later pack appends its
     * sounds to the ones already defined for the same event, unless the entry carries
     * {@code "replace": true}, which discards everything registered for that event before it.
     */
    private JsonObject loadSoundsJson(ResourceManager resourceManager)
    {
        Identifier soundsJsonId = new Identifier("minecraft", "sounds.json");
        JsonObject merged = new JsonObject();

        /* getAllResources returns packs lowest priority first, top pack last - the same
         * order vanilla's SoundManager registers them in. */
        for (Resource resource : resourceManager.getAllResources(soundsJsonId))
        {
            try (InputStream inputStream = resource.getInputStream())
            {
                String jsonContent = IOUtils.readText(inputStream);
                JsonObject parsed = JsonParser.parseString(jsonContent).getAsJsonObject();

                for (String key : parsed.keySet())
                {
                    this.mergeSoundEntry(merged, key, parsed.get(key));
                }
            }
            catch (Exception e)
            {
                /* One broken pack (invalid JSON, unreadable entry) must not hide every other
                 * pack's sounds; vanilla logs the bad file and keeps going. */
                e.printStackTrace();
            }
        }

        return merged;
    }

    /**
     * Merge one sound event entry into the accumulated map, following vanilla's
     * replace-or-append rule for duplicate event names across resource packs.
     */
    private void mergeSoundEntry(JsonObject merged, String key, JsonElement entry)
    {
        if (!merged.has(key) || !entry.isJsonObject() || this.isReplace(entry.getAsJsonObject()))
        {
            merged.add(key, entry);

            return;
        }

        JsonElement existing = merged.get(key);

        if (!existing.isJsonObject())
        {
            merged.add(key, entry);

            return;
        }

        JsonObject existingObj = existing.getAsJsonObject();
        JsonObject entryObj = entry.getAsJsonObject();

        if (!entryObj.has("sounds") || !entryObj.get("sounds").isJsonArray())
        {
            /* Nothing to append; the entry that was registered first stays */
            return;
        }

        if (!existingObj.has("sounds") || !existingObj.get("sounds").isJsonArray())
        {
            /* The event was first registered without a sounds array; vanilla keeps that
             * registration (and its subtitle) and only adds the new sounds to it. */
            JsonObject combined = existingObj.deepCopy();

            combined.add("sounds", entryObj.getAsJsonArray("sounds"));
            merged.add(key, combined);

            return;
        }

        /* Appending keeps the first subtitle, exactly like vanilla's WeightedSoundSet. */
        JsonArray sounds = existingObj.getAsJsonArray("sounds");

        for (JsonElement sound : entryObj.getAsJsonArray("sounds"))
        {
            sounds.add(sound);
        }
    }

    private boolean isReplace(JsonObject entry)
    {
        return entry.has("replace") && entry.get("replace").isJsonPrimitive() && entry.get("replace").getAsBoolean();
    }

    /**
     * Strip a {@code minecraft:} namespace off a sound name. Names in other namespaces
     * can't be resolved from the merged {@code minecraft} sounds.json, so they return null.
     */
    private static String stripMinecraftNamespace(String name)
    {
        int colon = name.indexOf(':');

        if (colon < 0)
        {
            return name;
        }

        String namespace = name.substring(0, colon);

        return namespace.equals("minecraft") ? name.substring(colon + 1) : null;
    }

    /**
     * Resolve a concrete sound name from sounds.json into the resource path of its .ogg.
     * Names may be namespaced ({@code minecraft:block/copper_bulb/toggle}); the namespace
     * is stripped and the extension is filled in so the file can be looked up as
     * {@code minecraft:sounds/<path>}. A name in another namespace can't be resolved by this
     * minecraft-only browser, so it returns null.
     */
    private static String toResourcePath(String name)
    {
        String path = stripMinecraftNamespace(name);

        if (path == null)
        {
            return null;
        }

        return path.endsWith(".ogg") ? path : path + ".ogg";
    }

    /**
     * Find all actual sound file paths for a sound event, following "type": "event"
     * references (e.g. {@code entity.parrot.imitate.zombie_nautilus} pointing at
     * {@code entity.zombie_nautilus.ambient}) the same way vanilla's SoundList does.
     */
    private List<String> findAllSoundFilesFromCache(Identifier soundId)
    {
        List<String> actualPaths = new ArrayList<>();

        this.collectSoundFiles(soundId.getPath(), actualPaths, new ArrayDeque<>());

        return actualPaths.isEmpty() ? null : actualPaths;
    }

    /**
     * Walk a sound entry, appending concrete file paths and recursing into event
     * references. A reference cycle (mods can be sloppy) is dropped instead of
     * recursing forever.
     */
    private void collectSoundFiles(String soundPath, List<String> out, Deque<String> visiting)
    {
        if (visiting.size() >= MAX_EVENT_DEPTH || visiting.contains(soundPath))
        {
            return;
        }

        JsonObject soundEntry = this.cachedSoundsJson.getAsJsonObject(soundPath);

        if (soundEntry == null || !soundEntry.has("sounds") || !soundEntry.get("sounds").isJsonArray())
        {
            return;
        }

        visiting.push(soundPath);

        try
        {
            for (var sound : soundEntry.getAsJsonArray("sounds"))
            {
                if (sound.isJsonPrimitive())
                {
                    String path = toResourcePath(sound.getAsString());

                    if (path != null)
                    {
                        out.add(path);
                    }
                }
                else if (sound.isJsonObject())
                {
                    JsonObject soundObj = sound.getAsJsonObject();

                    if (!soundObj.has("name"))
                    {
                        continue;
                    }

                    String name = soundObj.get("name").getAsString();

                    if (soundObj.has("type") && soundObj.get("type").getAsString().equals("event"))
                    {
                        String key = stripMinecraftNamespace(name);

                        if (key != null)
                        {
                            this.collectSoundFiles(key, out, visiting);
                        }
                    }
                    else
                    {
                        String path = toResourcePath(name);

                        if (path != null)
                        {
                            out.add(path);
                        }
                    }
                }
            }
        }
        finally
        {
            visiting.pop();
        }
    }

    /**
     * Populate list data
     */
    private void populateList()
    {
        this.list.clear();

        for (String key : this.soundAssetMap.keySet())
        {
            VanillaSoundAsset asset = this.soundAssetMap.get(key);
            String prefix = "[" + asset.category + "]: ";
            String displayName = prefix + asset.displayName;

            this.list.add(displayName);
        }

        this.list.sort(String::compareToIgnoreCase);
        this.update();
    }

    @Override
    protected void renderElementPart(UIContext context, String element, int i, int x, int y, boolean hover, boolean selected)
    {
        int textWidth = context.batcher.getFont().getWidth(element);
        int buttonSpace = 40;
        int maxWidth = this.area.w - 8 - buttonSpace;

        String displayText = element;

        if (textWidth > maxWidth)
        {
            displayText = context.batcher.getFont().limitToWidth(element, maxWidth);
        }

        context.batcher.textShadow(displayText, x + 4, y + (this.scroll.scrollItemSize - context.batcher.getFont().getHeight()) / 2, RowStyle.textColor(hover || selected));

        String downloadedPath = this.findDownloadedSound(element);
        boolean isDownloaded = downloadedPath != null;

        int currentIconX = this.area.x + this.area.w - 20;
        int iconY = y + (this.scroll.scrollItemSize - 16) / 2;

        if (!isDownloaded)
        {
            boolean isHoverOnDownload = this.area.isInside(context) && context.mouseX >= currentIconX && context.mouseX < currentIconX + 16 &&
                    context.mouseY >= iconY && context.mouseY < iconY + 16;

            this.downloadButton.iconColor(isHoverOnDownload ? Colors.WHITE : Colors.GRAY);
            this.downloadButton.area.set(currentIconX, iconY, 16, 16);
            this.downloadButton.render(context);

            currentIconX -= 20;
        }

        boolean isHoverOnLike = this.area.isInside(context)
            && context.mouseX >= currentIconX
            && context.mouseX < currentIconX + 16
            && context.mouseY >= iconY
            && context.mouseY < iconY + 16;
        boolean isLiked = isDownloaded && this.likeManager.isSoundLiked(downloadedPath);

        this.likeButton.both(isLiked ? Icons.DISLIKE : Icons.LIKE);
        this.likeButton.iconColor(isHoverOnLike || isLiked ? Colors.WHITE : Colors.GRAY);
        this.likeButton.area.set(currentIconX, iconY, 16, 16);
        this.likeButton.render(context);
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            int scrollIndex = this.scroll.getIndex(context.mouseX, context.mouseY);

            String element = this.getElementAt(scrollIndex);

            if (element != null)
            {
                int y = this.area.y + scrollIndex * this.scroll.scrollItemSize - (int) this.scroll.getScroll();
                int iconY = y + (this.scroll.scrollItemSize - 16) / 2;

                String downloadedPath = this.findDownloadedSound(element);
                boolean isDownloaded = downloadedPath != null;

                int currentIconX = this.area.x + this.area.w - 20;

                if (!isDownloaded)
                {
                    if (
                        context.mouseX >= currentIconX &&
                        context.mouseX < currentIconX + 16 &&
                        context.mouseY >= iconY &&
                        context.mouseY < iconY + 16
                    ) {
                        this.downloadSound(element);

                        return true;
                    }

                    currentIconX -= 20;
                }

                if (
                    context.mouseX >= currentIconX &&
                    context.mouseX < currentIconX + 16 &&
                    context.mouseY >= iconY &&
                    context.mouseY < iconY + 16
                ) {
                    this.toggleLikeWithDownload(element);

                    return true;
                }
            }
        }

        return super.subMouseClicked(context);
    }

    /**
     * Toggle like status, download first if not downloaded
     */
    private void toggleLikeWithDownload(String displayName)
    {
        String originalName = this.removePrefix(displayName);
        
        VanillaSoundAsset asset = this.soundAssetMap.get(originalName);
        if (asset == null)
        {
            return;
        }

        try
        {
            String downloadedPath = this.findDownloadedSound(displayName);

            if (downloadedPath != null)
            {
                this.likeManager.toggleSoundLiked(downloadedPath, displayName);

                if (this.likeToggleCallback != null)
                {
                    this.likeToggleCallback.run();
                }

                this.update();
            }
            else
            {
                String finalName = this.copyToAssetsDirectoryWithOriginalName(originalName, asset);
                
                if (finalName != null)
                {
                    String assetsPath = "assets:audio/" + finalName + ".ogg";
                    this.likeManager.setSoundLiked(assetsPath, displayName, true);

                    if (this.downloadCallback != null)
                    {
                        this.downloadCallback.accept(assetsPath);
                    }

                    if (this.likeToggleCallback != null)
                    {
                        this.likeToggleCallback.run();
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Remove category prefix from display name
     */
    private String removePrefix(String displayName)
    {
        int endBracket = displayName.indexOf(']');

        if (endBracket > 0 && displayName.startsWith("["))
        {
            int colonSpace = displayName.indexOf("]: ", endBracket);

            if (colonSpace > 0)
            {
                return displayName.substring(colonSpace + 3);
            }
        }

        return displayName;
    }

    /**
     * Find the downloaded file for a list entry, or null when it hasn't been downloaded.
     * Downloads are named after the flattened display name, so that is what to look for.
     */
    private String findDownloadedSound(String displayName)
    {
        try
        {
            String originalName = this.removePrefix(displayName);

            File gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().toFile();
            File audioDir = new File(gameDir, "config/bbs/assets/audio");

            if (!audioDir.exists() || !audioDir.isDirectory())
            {
                return null;
            }

            String flatFileName = originalName;

            if (!flatFileName.endsWith(".ogg"))
            {
                flatFileName += ".ogg";
            }

            File exactMatch = new File(audioDir, flatFileName);

            if (exactMatch.exists())
            {
                return "assets:audio/" + flatFileName;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
    /**
     * Copy audio to temporary file for preview
     */
    private File copyToTempFile(VanillaSoundAsset asset)
    {
        try
        {
            AudioCacheManager cacheManager = AudioCacheManager.getInstance();
            
            if (asset.actualSoundPaths != null && !asset.actualSoundPaths.isEmpty())
            {
                String soundPath = asset.actualSoundPaths.get(0);

                File cachedFile = cacheManager.getCachedFile(soundPath);

                if (cachedFile != null && cachedFile.exists())
                {
                    return cachedFile;
                }
                
                File cacheFile = cacheManager.createTempCacheFile(soundPath);

                if (cacheFile == null)
                {
                    return null;
                }
                
                Identifier soundFileId = new Identifier("minecraft", "sounds/" + soundPath);
                MinecraftClient client = MinecraftClient.getInstance();
                Optional<Resource> resource = client.getResourceManager().getResource(soundFileId);

                if (resource.isPresent())
                {
                    try (InputStream inputStream = resource.get().getInputStream())
                    {
                        Files.copy(inputStream, cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    return cacheFile;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Download audio to assets directory
     */
    private void downloadSound(String displayName)
    {
        String originalName = this.removePrefix(displayName);
        VanillaSoundAsset asset = this.soundAssetMap.get(originalName);

        if (asset == null)
        {
            return;
        }

        try
        {
            String finalName = this.copyToAssetsDirectory(displayName, asset);
            
            if (finalName != null)
            {
                String fullPath = "assets:audio/" + finalName + ".ogg";
                
                if (this.downloadCallback != null)
                {
                    this.downloadCallback.accept(fullPath);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Copy audio file to assets directory
     */
    private String copyToAssetsDirectory(String displayName, VanillaSoundAsset asset)
    {
        String originalName = this.removePrefix(displayName);

        return this.copyToAssetsDirectoryWithOriginalName(originalName, asset);
    }
    /**
     * Copy audio file to assets directory using original name
     */
    private String copyToAssetsDirectoryWithOriginalName(String originalName, VanillaSoundAsset asset)
    {
        try
        {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File audioDir = new File(gameDir, "config/bbs/assets/audio");
            
            if (!audioDir.exists())
            {
                audioDir.mkdirs();
            }

            if (asset.actualSoundPaths != null && !asset.actualSoundPaths.isEmpty())
            {
                String soundPath = asset.actualSoundPaths.get(0);

                Identifier soundFileId = new Identifier("minecraft", "sounds/" + soundPath);
                MinecraftClient client = MinecraftClient.getInstance();
                Optional<Resource> resource = client.getResourceManager().getResource(soundFileId);

                if (resource.isPresent())
                {
                    String newSoundName = this.generateSoundName(originalName, audioDir);
                    File targetFile = new File(audioDir, newSoundName + ".ogg");

                    try (InputStream inputStream = resource.get().getInputStream())
                    {
                        Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }

                    return newSoundName;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Generate unique sound file name
     */
    private String generateSoundName(String originalName, File audioDir)
    {
        File originalFile = new File(audioDir, originalName + ".ogg");

        if (!originalFile.exists())
        {
            return originalName;
        }

        Pattern pattern = Pattern.compile("(.*)_(\\d+)$");
        Matcher matcher = pattern.matcher(originalName);

        if (matcher.matches())
        {
            String baseName = matcher.group(1);
            int currentNumber = Integer.parseInt(matcher.group(2));

            return this.findAvailableFileName(baseName, currentNumber + 1, audioDir);
        }

        return this.findAvailableFileName(originalName, 1, audioDir);
    }

    /**
     * Find available file name
     */
    private String findAvailableFileName(String baseName, int startNumber, File audioDir)
    {
        int number = startNumber;
        String candidateName;
        File candidateFile;

        do
        {
            candidateName = baseName + "_" + number;
            candidateFile = new File(audioDir, candidateName + ".ogg");
            number += 1;
        }
        while (candidateFile.exists());

        return candidateName;
    }

    /**
     * Get temporary cache file for audio preview
     */
    public File getTemporaryFileForSound(String displayName)
    {
        String originalName = this.removePrefix(displayName);
        VanillaSoundAsset asset = this.soundAssetMap.get(originalName);
        
        if (asset == null || asset.actualSoundPaths == null || asset.actualSoundPaths.isEmpty())
        {
            return null;
        }

        try
        {
            return this.copyToTempFile(asset);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public void refresh()
    {
        this.ensureLoaded();
    }

    /**
     * Minecraft sound resource info
     */
    private static class VanillaSoundAsset
    {
        final String displayName;
        final String resourcePath;
        final List<String> actualSoundPaths;
        final String category;

        VanillaSoundAsset(String displayName, String resourcePath, List<String> actualSoundPaths, String category)
        {
            this.displayName = displayName;
            this.resourcePath = resourcePath;
            this.actualSoundPaths = actualSoundPaths;
            this.category = category;
        }
    }
}

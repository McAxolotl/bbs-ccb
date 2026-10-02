package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.audio.AudioCacheManager;
import mchorse.bbs_mod.audio.AudioReader;
import mchorse.bbs_mod.audio.ColorCode;
import mchorse.bbs_mod.audio.SoundLikeManager;
import mchorse.bbs_mod.audio.SoundLikeManager.LikedSound;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.audio.ogg.VorbisReader;
import mchorse.bbs_mod.audio.wav.WaveReader;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.screenplay.UIAudioPlayer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UILikeableStringList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UILikedSoundList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIVanillaSoundList;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Overlay panel for picking sounds, importing vanilla sounds, and browsing liked sounds.
 * Split into three tabs represented by left-hand side buttons.
 */
public class UISoundOverlayPanel extends UIStringOverlayPanel
{
    private static final int PLAYER_HEIGHT = 24;

    /** Formats the picker offers; a user track keeps its own when renamed. */
    private static final String[] AUDIO_EXTENSIONS = {".ogg", ".wav"};

    public UIAudioPlayer player;

    private final SoundLikeManager likeManager;
    private final UIIcon folderButton;
    private final UIIcon addButton;
    private final UIIcon likeButton;

    private UISearchList<String> vanillaSounds;
    private UIVanillaSoundList vanillaSoundList;
    private UISearchList<LikedSound> likedSounds;
    private UILikedSoundList likedSoundList;

    private ViewMode currentMode = null;
    private final UIContext context;
    private final Consumer<Link> originalCallback;
    private BiConsumer<String, String> renameCallback;
    private Consumer<String> removeCallback;
    private String selectedSound;

    public UISoundOverlayPanel(Consumer<Link> callback)
    {
        this(callback, null);
    }

    public UISoundOverlayPanel(Consumer<Link> callback, UIContext context)
    {
        super(UIKeys.OVERLAYS_SOUNDS_MAIN, getSoundEvents(), null);

        AudioCacheManager.getInstance().clearAllCache();

        this.context = context;
        this.originalCallback = callback;
        this.likeManager = new SoundLikeManager();

        /* Replace the default list with the like-aware list */
        this.content.remove(this.strings);

        UILikeableStringList likeableList = new UILikeableStringList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickAudio(list.get(0));
            }
        }, this.likeManager);

        likeableList.scroll.scrollSpeed *= 2;
        likeableList.setEditCallback((soundName) -> {
            if (this.context == null)
            {
                return;
            }
            
            UIPromptOverlayPanel renamePanel = new UIPromptOverlayPanel(
                UIKeys.GENERAL_RENAME,
                UIKeys.GENERAL_RENAME,
                (newName) -> this.renameAudio(soundName, newName)
            );
            renamePanel.text.setText(soundName);
            renamePanel.text.filename();
            
            UIOverlay.addOverlay(this.context, renamePanel);
        });
        
        likeableList.setRemoveCallback((soundName) ->
        {
            if (this.context == null)
            {
                return;
            }
            
            UIConfirmOverlayPanel confirmPanel = new UIConfirmOverlayPanel(
                UIKeys.GENERAL_REMOVE,
                UIKeys.GENERAL_REMOVE,
                (confirmed) ->
                {
                    if (confirmed)
                    {
                        this.deleteAudio(soundName);
                    }
                }
            );
            
            UIOverlay.addOverlay(this.context, confirmPanel);
        });

        likeableList.setRefreshCallback(this::refreshLikedList);

        this.strings = new UISearchList<>(likeableList);
        this.strings.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.content.add(this.strings);

        this.player = new UIAudioPlayer();
        this.content.add(this.player);
        this.player.relative(this.content).x(6).w(1F, -12).h(PLAYER_HEIGHT).y(0);

        this.vanillaSoundList = new UIVanillaSoundList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickAudio(list.get(0));
            }
        }, this.likeManager);
        this.vanillaSoundList.setDownloadCallback((soundLink) ->
        {
            this.refreshSoundList();
            this.refreshVanillaSoundList();
            this.refreshLikedList();
        });
        this.vanillaSoundList.setLikeToggleCallback(() ->
        {
            this.refreshVanillaSoundList();
            this.refreshLikedList();
        });

        this.vanillaSounds = new UISearchList<>(this.vanillaSoundList);
        this.vanillaSounds.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.vanillaSounds.setVisible(false);
        this.content.add(this.vanillaSounds);

        this.likedSoundList = new UILikedSoundList((list) ->
        {
            if (!list.isEmpty())
            {
                this.pickAudio(list.get(0).getPath());
            }
        });
        this.likedSoundList.setUnlikeCallback((sound) ->
        {
            this.likeManager.setSoundLiked(sound.getPath(), sound.getDisplayName(), false);
            this.refreshLikedList();
        });
        this.likedSounds = new UISearchList<>(this.likedSoundList);
        this.likedSounds.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        this.likedSounds.setVisible(false);
        this.content.add(this.likedSounds);

        this.strings.y(PLAYER_HEIGHT).h(1F, -PLAYER_HEIGHT);
        this.vanillaSounds.y(PLAYER_HEIGHT).h(1F, -PLAYER_HEIGHT);
        this.likedSounds.y(PLAYER_HEIGHT).h(1F, -PLAYER_HEIGHT);

        this.folderButton = new UIIcon(Icons.FOLDER, (b) -> this.switchToMode(ViewMode.FOLDER));
        this.folderButton.tooltip(UIKeys.OVERLAYS_SOUNDS_FOLDER_MODE);
        this.folderButton.highlight(() -> this.currentMode == ViewMode.FOLDER, Direction.LEFT);

        this.addButton = new UIIcon(Icons.ADD, (b) -> this.switchToMode(ViewMode.ADD));
        this.addButton.tooltip(UIKeys.OVERLAYS_SOUNDS_ADD_MODE);
        this.addButton.highlight(() -> this.currentMode == ViewMode.ADD, Direction.LEFT);

        this.likeButton = new UIIcon(Icons.HEART_ALT, (b) -> this.switchToMode(ViewMode.LIKE));
        this.likeButton.tooltip(UIKeys.OVERLAYS_SOUNDS_LIKE_MODE);
        this.likeButton.highlight(() -> this.currentMode == ViewMode.LIKE, Direction.LEFT);

        this.icons.add(this.folderButton, this.addButton, this.likeButton);

        this.callback(this::pickAudio);

        this.refreshSoundList();
        this.refreshVanillaSoundList();
        this.refreshLikedList();
        this.switchToMode(ViewMode.FOLDER);
    }

    /** Called with the old and new link after a user track was renamed on disk. */
    public void setRenameCallback(BiConsumer<String, String> callback)
    {
        this.renameCallback = callback;
    }

    /** Called with the link of a user track that was removed from disk. */
    public void setRemoveCallback(Consumer<String> callback)
    {
        this.removeCallback = callback;
    }

    /** Every sound file the picker offers; the audio editor's landing screen goes by the same list. */
    public static Set<String> getSoundEvents()
    {
        Set<String> locations = new HashSet<>();

        for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("audio")))
        {
            String pathLower = link.path.toLowerCase();
            boolean supported = pathLower.endsWith(".wav") || pathLower.endsWith(".ogg");

            if (supported)
            {
                locations.add(link.toString());
            }
        }

        return locations;
    }

    private void switchToMode(ViewMode mode)
    {
        if (this.currentMode == mode)
        {
            return;
        }

        this.stopCurrentPlayback();

        this.currentMode = mode;

        switch (mode)
        {
            case FOLDER:
                this.showFolderMode();
            break;

            case ADD:
                this.showAddMode();
            break;

            case LIKE:
                this.showLikeMode();
            break;
        }

        this.updateListSelections();
    }

    private void showFolderMode()
    {
        this.strings.setVisible(true);
        this.vanillaSounds.setVisible(false);

        if (this.likedSounds != null)
        {
            this.likedSounds.setVisible(false);
        }

        UILikeableStringList list = (UILikeableStringList) this.strings.list;

        list.setShowOnlyLiked(false);
        list.setShowEditRemoveButtons(true);

        this.refreshSoundList();
        list.update();
    }

    private void showAddMode()
    {
        this.strings.setVisible(false);
        this.vanillaSounds.setVisible(true);

        if (this.likedSounds != null)
        {
            this.likedSounds.setVisible(false);
        }

        this.vanillaSounds.resize();
        this.content.resize();

        this.refreshVanillaSoundList();
    }

    private void showLikeMode()
    {
        this.strings.setVisible(false);
        this.vanillaSounds.setVisible(false);

        if (this.likedSounds != null)
        {
            this.likedSounds.setVisible(true);
        }

        UILikeableStringList list = (UILikeableStringList) this.strings.list;

        list.setShowOnlyLiked(true);
        list.setShowEditRemoveButtons(false);

        this.refreshLikedList();
        list.update();
    }

    private void refreshVanillaSoundList()
    {
        if (this.vanillaSoundList == null)
        {
            return;
        }

        this.vanillaSoundList.refresh();

        if (this.vanillaSounds != null)
        {
            String filter = this.vanillaSounds.search.getText();

            this.vanillaSounds.filter(filter, true);
            this.vanillaSounds.resize();
        }
    }

    private void refreshLikedList()
    {
        if (this.likedSoundList == null)
        {
            return;
        }

        this.likedSoundList.setSounds(this.likeManager.getLikedSounds());

        if (this.likedSounds != null)
        {
            String filter = this.likedSounds.search.getText();

            this.likedSounds.filter(filter, true);
            this.likedSounds.resize();
        }
    }

    public void refreshSoundList()
    {
        Set<String> soundEvents = getSoundEvents();

        UILikeableStringList list = (UILikeableStringList) this.strings.list;
        list.getList().clear();

        List<String> sorted = new ArrayList<>(soundEvents);

        sorted.sort(null);
        list.getList().addAll(sorted);
        list.getList().add(0, UIKeys.GENERAL_NONE.get());
        list.update();
        list.sort();

        String filter = this.strings.search.getText();

        this.strings.filter(filter, true);
        this.strings.resize();
        this.refreshLikedList();
    }

    private void pickAudio(String audio)
    {
        this.selectedSound = audio;

        if (audio == null || audio.isEmpty() || audio.equals(UIKeys.GENERAL_NONE.get()))
        {
            if (this.originalCallback != null)
            {
                this.originalCallback.accept(null);
            }

            return;
        }

        try
        {
            SoundManager sounds = BBSModClient.getSounds();
            Link link = null;
            Wave wave = null;

            SoundPlayer current = this.player.getPlayer();

            if (current != null)
            {
                current.stop();
            }

            if (this.currentMode == ViewMode.ADD && !audio.startsWith("assets:"))
            {
                File tempFile = this.vanillaSoundList.getTemporaryFileForSound(audio);

                if (tempFile != null && tempFile.exists())
                {
                    try (FileInputStream fis = new FileInputStream(tempFile))
                    {
                        String pathLower = tempFile.getName().toLowerCase();
                        
                        if (pathLower.endsWith(".wav"))
                        {
                            wave = new WaveReader().read(fis);
                        }
                        else if (pathLower.endsWith(".ogg"))
                        {
                            Link tempLink = new Link("cache", tempFile.getName());

                            wave = VorbisReader.read(tempLink, fis);
                        }
                    }
                }
            }
            else
            {
                link = Link.create(audio);

                if (BBSMod.getProvider().getFile(link) == null)
                {
                    return;
                }

                wave = AudioReader.read(BBSMod.getProvider(), link);
            }

            if (wave != null)
            {
                List<ColorCode> colorCodes = link != null ? sounds.readColorCodes(link) : new ArrayList<>();

                if (wave.getBytesPerSample() > 2)
                {
                    wave = wave.convertTo16();
                }

                this.player.loadAudio(wave, colorCodes);

                SoundPlayer newPlayer = this.player.getPlayer();

                if (newPlayer != null)
                {
                    BBSModClient.getSounds().deleteSounds();
                    newPlayer.play();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.updateListSelections();

        if (this.originalCallback != null)
        {
            if (audio.startsWith("assets:"))
            {
                this.originalCallback.accept(Link.create(audio));
            }
            else
            {
                String downloaded = this.findDownloadedSoundInAddMode(audio);

                if (downloaded != null)
                {
                    this.originalCallback.accept(Link.create(downloaded));
                }
            }
        }
    }

    private void updateListSelections()
    {
        if (this.selectedSound == null)
        {
            return;
        }

        if (this.strings.list instanceof UILikeableStringList list)
        {
            int index = list.getList().indexOf(this.selectedSound);

            if (index >= 0)
            {
                list.setIndex(index);
            }
        }

        if (this.vanillaSoundList != null)
        {
            int index = this.vanillaSoundList.getList().indexOf(this.selectedSound);

            if (index >= 0)
            {
                this.vanillaSoundList.setIndex(index);
            }
        }

        if (this.likedSoundList != null)
        {
            List<LikedSound> liked = this.likedSoundList.getSounds();

            for (int i = 0; i < liked.size(); i++)
            {
                if (liked.get(i).getPath().equals(this.selectedSound))
                {
                    this.likedSoundList.setIndex(i);

                    break;
                }
            }
        }
    }

    private String findDownloadedSoundInAddMode(String displayName)
    {
        /* The list owns the naming scheme for downloads; ask it so the two can't drift. */
        return this.vanillaSoundList == null ? null : this.vanillaSoundList.findDownloadedSound(displayName);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        switch (this.currentMode)
        {
            case FOLDER:
                this.renderFolderEmptyState(context);
            break;

            case ADD:
                this.renderAddEmptyState(context);
            break;

            case LIKE:
                this.renderLikeEmptyState(context);
            break;
        }
    }

    private void renderFolderEmptyState(UIContext context)
    {}

    private void renderAddEmptyState(UIContext context)
    {}

    private void renderLikeEmptyState(UIContext context)
    {}

    private void renameAudio(String oldName, String newName)
    {
        if (oldName == null || newName == null || oldName.equals(newName))
        {
            return;
        }

        File oldFile = findAudioFile(oldName);

        if (oldFile == null)
        {
            return;
        }

        /* Keep the format: renaming a .wav must not turn it into an .ogg */
        String extension = oldFile.getName().toLowerCase().endsWith(".wav") ? ".wav" : ".ogg";
        String newFileName = stripExtension(toRelativePath(newName));
        File newFile = new File(getAudioDir(), newFileName + extension);
        File newParent = newFile.getParentFile();

        if (newFile.exists())
        {
            return;
        }

        if (newParent != null && !newParent.isDirectory())
        {
            newParent.mkdirs();
        }

        if (oldFile.renameTo(newFile))
        {
            String newLink = "assets:audio/" + newFileName + extension;

            if (this.likeManager.isSoundLiked(oldName))
            {
                this.likeManager.removeSound(oldName);
                this.likeManager.setSoundLiked(newLink, newFileName, true);
            }

            if (this.renameCallback != null)
            {
                this.renameCallback.accept(oldName, newLink);
            }

            this.refreshSoundList();
            this.refreshVanillaSoundList();
            this.refreshLikedList();
        }
    }

    private void deleteAudio(String soundName)
    {
        if (soundName == null)
        {
            return;
        }

        File audioFile = findAudioFile(soundName);

        if (audioFile != null && audioFile.delete())
        {
            this.likeManager.removeSound(soundName);

            if (BBSModClient.getSounds() != null)
            {
                BBSModClient.getSounds().stop(Link.assets("audio/" + toRelativePath(soundName)));
            }

            if (this.removeCallback != null)
            {
                this.removeCallback.accept(soundName);
            }

            this.refreshSoundList();
            this.refreshVanillaSoundList();
            this.refreshLikedList();
        }
    }

    private static File getAudioDir()
    {
        return new File(BBSMod.getAssetsFolder(), "audio");
    }

    /** Path of a list entry relative to the audio folder, e.g. {@code foo.wav}. */
    private static String toRelativePath(String link)
    {
        return link.startsWith("assets:audio/") ? link.substring("assets:audio/".length()) : link;
    }

    /** The name without any supported audio extension. */
    private static String stripExtension(String path)
    {
        String lower = path.toLowerCase();

        for (String extension : AUDIO_EXTENSIONS)
        {
            if (lower.endsWith(extension))
            {
                return path.substring(0, path.length() - extension.length());
            }
        }

        return path;
    }

    /**
     * Resolve a list entry to the file on disk. User tracks can be .ogg or .wav, and the
     * entry may or may not carry the extension, so both are tried.
     */
    private static File findAudioFile(String link)
    {
        String path = stripExtension(toRelativePath(link));
        File audioDir = getAudioDir();

        for (String extension : AUDIO_EXTENSIONS)
        {
            File file = new File(audioDir, path + extension);

            if (file.exists())
            {
                return file;
            }
        }

        return null;
    }

    private void stopCurrentPlayback()
    {
        if (this.player != null)
        {
            SoundPlayer current = this.player.getPlayer();

            if (current != null)
            {
                current.stop();
            }
        }

        AudioCacheManager.getInstance().cleanupInvalidCache();
    }

    @Override
    public void onClose()
    {
        super.onClose();

        this.stopCurrentPlayback();
        AudioCacheManager.getInstance().clearAllCache();
    }

    private enum ViewMode
    {
        FOLDER, ADD, LIKE;
    }
}

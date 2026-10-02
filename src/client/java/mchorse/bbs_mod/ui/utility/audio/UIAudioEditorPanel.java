package mchorse.bbs_mod.ui.utility.audio;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.onboarding.Onboarding;
import mchorse.bbs_mod.ui.onboarding.TourAnchors;
import mchorse.bbs_mod.ui.dashboard.panels.UIEditorDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UISoundOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.io.File;

public class UIAudioEditorPanel extends UIEditorDashboardPanel
{
    /** Under which key the sounds opened here are kept in the settings. */
    private static final String RECENT = "audio";

    public UIIcon pickAudio;
    public UIIcon plause;
    public UIIcon saveColors;
    public UIAudioEditor audioEditor;

    public UIAudioEditorPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.pickAudio = new UIIcon(Icons.MORE, (b) -> this.openDataManager());
        this.pickAudio.tooltip(UIKeys.PANELS_KEYS_OPEN_DATA_MANAGER);
        this.plause = new UIIcon(() ->
        {
            SoundPlayer player = this.audioEditor.getPlayer();

            if (player == null)
            {
                return Icons.STOP;
            }

            return player.isPlaying() ? Icons.PAUSE : Icons.PLAY;
        }, (b) -> this.audioEditor.togglePlayback());
        this.saveColors = new UIIcon(Icons.SAVED, (b) -> this.saveColors());
        this.saveColors.tooltip(UIKeys.GENERAL_SAVE);
        this.audioEditor = new UIAudioEditor();
        this.audioEditor.full(this.editor);

        this.actions().action(this.plause).common(this.pickAudio).common(this.saveColors);
        this.editor.add(this.audioEditor);

        this.mountLanding();

        this.openAudio(null);

        this.keys().register(Keys.PLAUSE, this.audioEditor::togglePlayback);
        this.keys().register(Keys.SAVE, this::saveColors);
        this.keys().register(Keys.OPEN_DATA_MANAGER, this.pickAudio::clickItself);

        /* What the tour of this panel points at */
        TourAnchors.register("audio.waveform", () -> this.audioEditor);
        TourAnchors.register("audio.play", () -> this.plause);
        TourAnchors.register("audio.save", () -> this.saveColors);
    }

    /* Tabs — a tab holds the link of an open sound */

    @Override
    protected void showTab(String id)
    {
        this.setAudio(id == null ? null : Link.create(id));
    }

    @Override
    public String getOpenId()
    {
        Link audio = this.audioEditor.getAudio();

        return audio == null ? null : audio.toString();
    }

    @Override
    public Icon getTabIcon(String id)
    {
        return id == null ? Icons.SEARCH : Icons.SOUND;
    }

    /** Picked from the sound overlay: goes into the current tab, like picking data does elsewhere. */
    private void openAudio(Link link)
    {
        this.pickData(link == null ? null : link.toString());
    }

    private void setAudio(Link link)
    {
        this.audioEditor.setup(link);
        this.editor.setVisible(link != null);
        this.saveColors.setEnabled(this.audioEditor.isEditing());

        if (link != null)
        {
            BBSSettings.recentData.touch(RECENT, link.toString());
            Onboarding.dataOpened(this);
        }
    }

    private void saveColors()
    {
        Link audio = this.audioEditor.getAudio();

        if (audio == null)
        {
            return;
        }

        SoundManager sounds = BBSModClient.getSounds();

        sounds.saveColorCodes(new Link(audio.source, audio.path + ".json"), this.audioEditor.getColorCodes());
        sounds.deleteSound(audio);
    }

    /* ILandingHost — the empty tab shows the sounds opened last */

    @Override
    public IKey getTitle()
    {
        return UIKeys.AUDIO_TITLE;
    }

    @Override
    public String getRecentType()
    {
        return RECENT;
    }

    @Override
    public IKey getListLabel()
    {
        return UIKeys.AUDIO_LANDING_LIST;
    }

    @Override
    public void openDataManager()
    {
        UIOverlay.addOverlay(this.getContext(), this.buildSoundPanel());
    }

    @Override
    public void showInList(String id)
    {
        UISoundOverlayPanel panel = this.buildSoundPanel();

        UIOverlay.addOverlay(this.getContext(), panel);
        panel.set(id);
    }

    /**
     * The picker renames and deletes files behind the panel's back, so the open tabs and the
     * recent list have to be told - otherwise a tab keeps pointing at a link that no longer
     * resolves and fails to load.
     */
    private UISoundOverlayPanel buildSoundPanel()
    {
        UISoundOverlayPanel panel = new UISoundOverlayPanel(this::openAudio, this.getContext());

        panel.setRenameCallback((from, to) ->
        {
            this.tabs.renameId(from, to);
            BBSSettings.recentData.rename(RECENT, from, to);

            if (from.equals(this.getOpenId()))
            {
                this.setAudio(Link.create(to));
            }
        });
        panel.setRemoveCallback((id) ->
        {
            this.tabs.forgetId(id);
            BBSSettings.recentData.forget(RECENT, id);

            if (id.equals(this.getOpenId()))
            {
                this.setAudio(null);
            }
        });

        return panel;
    }

    @Override
    public File getDataFolder()
    {
        return BBSMod.getAudioFolder();
    }

    /**
     * Sounds live in files rather than in a repository, so the answer is here right away: whatever
     * the picker offers is what the landing screen keeps in its list.
     */
    @Override
    public void requestNames()
    {
        this.fillNames(UISoundOverlayPanel.getSoundEvents());
    }
}

package mchorse.bbs_mod.camera.runtime;

import com.mojang.logging.LogUtils;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.clips.overwrite.DimensionClip;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.WorldFilmController;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.controller.FilmEditorController;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

import java.util.List;

/**
 * Dimension fixture runtime controller.
 *
 * <p>Responsible for evaluating active {@link DimensionClip}s on the camera timeline during playback
 * and scrubbing, smoothly transitioning the client world dimension, and safely restoring the player
 * to their initial dimension upon film completion.</p>
 */
public class DimensionRuntimeController
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Debounce delay in milliseconds during paused scrubbing */
    private static final long DEBOUNCE_MS = 250L;

    /** Player's original world dimension when film playback/preview began */
    private static Identifier initialDimension;

    /** Player's original spatial coordinates and orientation */
    private static double initialX;
    private static double initialY;
    private static double initialZ;
    private static float initialYaw;
    private static float initialPitch;

    /** Whether any dimension switch occurred in this film session */
    private static boolean didSwitchDimension = false;

    /** Last detected target dimension on timeline */
    private static Identifier lastTargetDimension;

    /** Last requested dimension sent over the network */
    private static Identifier lastRequestedDimension;

    /** Timestamp of last target dimension change */
    private static long lastChangeTime;

    /** Whether a cross-dimension switch is currently in flight */
    private static volatile boolean switchingDimension = false;

    /** Timestamp when switch started */
    private static long switchStartTime = 0L;

    /** Timeout duration for dimension switch */
    private static final long SWITCH_TIMEOUT_MS = 10000L;

    public static boolean isSwitchingDimension()
    {
        if (switchingDimension)
        {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && lastRequestedDimension != null
                && client.world.getRegistryKey().getValue().equals(lastRequestedDimension)
                && client.player != null)
            {
                switchingDimension = false;
            }
            else if (System.currentTimeMillis() - switchStartTime > SWITCH_TIMEOUT_MS)
            {
                LOGGER.warn("[BBS] Dimension switch timed out for target: {}, resetting state", lastRequestedDimension);
                switchingDimension = false;
            }
        }

        return switchingDimension;
    }

    public static boolean hasActiveDimensionClips(Film film)
    {
        if (film == null || film.camera == null)
        {
            return false;
        }

        for (DimensionClip clip : film.camera.getClips(DimensionClip.class))
        {
            if (clip.enabled.get() && clip.dimension.get() != null)
            {
                return true;
            }
        }

        return false;
    }

    private static boolean isEligibleController(BaseFilmController controller)
    {
        if (controller == null || controller instanceof Recorder)
        {
            return false;
        }

        if (BBSModClient.getFilms() != null && BBSModClient.getFilms().getRecorder() != null)
        {
            return false;
        }

        if (controller instanceof WorldFilmController)
        {
            ICameraController current = BBSModClient.getCameraController().getCurrent();
            if (!(current instanceof PlayCameraController play && play.getContext().clips == controller.film.camera))
            {
                return false;
            }
        }

        return true;
    }

    private static void captureInitialSnapshot(MinecraftClient client)
    {
        initialDimension = client.world.getRegistryKey().getValue();
        initialX = client.player.getX();
        initialY = client.player.getY();
        initialZ = client.player.getZ();
        initialYaw = client.player.getYaw();
        initialPitch = client.player.getPitch();

        lastTargetDimension = initialDimension;
        lastRequestedDimension = null;
        lastChangeTime = 0L;
    }

    public static void onCreated(BaseFilmController controller)
    {
        if (controller instanceof Recorder)
        {
            return;
        }

        Film film = controller.film;
        if (!hasActiveDimensionClips(film))
        {
            resetState();
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null && client.player != null)
        {
            captureInitialSnapshot(client);
        }
    }

    public static void onTick(BaseFilmController controller, int tick)
    {
        if (!isEligibleController(controller))
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null)
        {
            return;
        }

        Film film = controller.film;
        if (film == null || film.camera == null)
        {
            return;
        }

        if (!hasActiveDimensionClips(film))
        {
            if (didSwitchDimension && initialDimension != null)
            {
                Identifier currentDim = client.world.getRegistryKey().getValue();
                if (!currentDim.equals(initialDimension))
                {
                    LOGGER.info("Film dimension clips removed; safely restoring player to initial dimension: {}", initialDimension);
                    requestDimensionSwitch(initialDimension, initialX, initialY, initialZ, initialYaw, initialPitch);
                }
            }
            resetState();
            return;
        }

        if (initialDimension == null)
        {
            captureInitialSnapshot(client);
        }

        List<Clip> clips = film.camera.getClips(tick);
        Identifier targetDim = initialDimension;

        for (int i = clips.size() - 1; i >= 0; i--)
        {
            Clip clip = clips.get(i);
            if (clip.enabled.get() && clip instanceof DimensionClip dimClip)
            {
                Link link = dimClip.dimension.get();

                if (link != null)
                {
                    Identifier parsed = Identifier.tryParse(link.toString());
                    if (parsed != null)
                    {
                        targetDim = parsed;
                        break;
                    }
                }
            }
        }

        Identifier currentDim = client.world.getRegistryKey().getValue();
        long now = System.currentTimeMillis();

        if (!targetDim.equals(currentDim))
        {
            boolean isPlaying = false;

            if (controller instanceof FilmEditorController editorController)
            {
                isPlaying = editorController.controller.isPlaying() || BBSModClient.getVideoRecorder().isRecording();
            }
            else if (controller instanceof WorldFilmController)
            {
                isPlaying = true;
            }

            if (!targetDim.equals(lastTargetDimension))
            {
                lastTargetDimension = targetDim;
                lastChangeTime = now;
            }

            if (isPlaying || (now - lastChangeTime >= DEBOUNCE_MS))
            {
                boolean inFlight = isSwitchingDimension() && targetDim.equals(lastRequestedDimension);
                if (!inFlight)
                {
                    boolean hasActiveCameraClip = false;
                    for (Clip clip : clips)
                    {
                        if (clip.enabled.get() && clip instanceof CameraClip && !(clip instanceof DimensionClip))
                        {
                            hasActiveCameraClip = true;
                            break;
                        }
                    }

                    Camera camera = BBSModClient.getCameraController().camera;

                    double x;
                    double y;
                    double z;
                    float yaw;
                    float pitch;

                    if (hasActiveCameraClip && camera != null)
                    {
                        x = camera.position.x;
                        y = camera.position.y;
                        z = camera.position.z;
                        yaw = (float) Math.toDegrees(camera.rotation.y);
                        pitch = (float) Math.toDegrees(camera.rotation.x);
                    }
                    else if (targetDim.equals(initialDimension))
                    {
                        x = initialX;
                        y = initialY;
                        z = initialZ;
                        yaw = initialYaw;
                        pitch = initialPitch;
                    }
                    else
                    {
                        x = camera != null ? camera.position.x : client.player.getX();
                        y = camera != null ? camera.position.y : client.player.getY();
                        z = camera != null ? camera.position.z : client.player.getZ();
                        yaw = camera != null ? (float) Math.toDegrees(camera.rotation.y) : client.player.getYaw();
                        pitch = camera != null ? (float) Math.toDegrees(camera.rotation.x) : client.player.getPitch();
                    }

                    requestDimensionSwitch(targetDim, x, y, z, yaw, pitch);
                }
            }
        }
        else
        {
            switchingDimension = false;
            lastTargetDimension = currentDim;
            lastRequestedDimension = currentDim;
        }
    }

    public static void onShutdown(BaseFilmController controller)
    {
        if (controller instanceof Recorder)
        {
            resetState();
            return;
        }

        if (didSwitchDimension && initialDimension != null)
        {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.player != null)
            {
                Identifier currentDim = client.world.getRegistryKey().getValue();
                if (!currentDim.equals(initialDimension))
                {
                    LOGGER.info("Film shutdown; safely restoring player to initial dimension: {}", initialDimension);
                    requestDimensionSwitch(
                        initialDimension,
                        initialX, initialY, initialZ,
                        initialYaw, initialPitch
                    );
                }
            }
        }

        resetState();
    }

    public static void resetState()
    {
        initialDimension = null;
        initialX = 0;
        initialY = 0;
        initialZ = 0;
        initialYaw = 0;
        initialPitch = 0;
        didSwitchDimension = false;
        lastTargetDimension = null;
        lastRequestedDimension = null;
        lastChangeTime = 0L;
        switchingDimension = false;
    }

    public static void requestDimensionSwitch(Identifier targetDim, double x, double y, double z, float yaw, float pitch)
    {
        if (initialDimension != null)
        {
            didSwitchDimension = true;
        }
        lastRequestedDimension = targetDim;
        lastTargetDimension = targetDim;
        switchingDimension = true;
        switchStartTime = System.currentTimeMillis();

        ClientNetwork.sendDimensionSwitch(targetDim, x, y, z, yaw, pitch);
    }
}

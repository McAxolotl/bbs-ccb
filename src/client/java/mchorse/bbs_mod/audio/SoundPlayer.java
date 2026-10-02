package mchorse.bbs_mod.audio;

import com.mojang.logging.LogUtils;
import mchorse.bbs_mod.utils.MathUtils;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.SOFTGainClampEx;
import org.slf4j.Logger;

public class SoundPlayer
{
    private static final Logger LOGGER = LogUtils.getLogger();

    private static float gainLimit = -1F;

    /**
     * How far the smoothed position may drift from OpenAL's reported offset before it
     * snaps back. AL_SEC_OFFSET is only refreshed once per mixer period, so a playhead
     * driven straight from it steps - and, because the refresh doesn't line up with the
     * render frames, visibly wobbles. While the source keeps playing the position is
     * extrapolated from the last reported value and corrected on real drift.
     */
    private static final float POSITION_SNAP_THRESHOLD = 0.05F;

    /** Longest gap that still extrapolates; a bigger one (hidden UI, a stall) resyncs. */
    private static final long POSITION_MAX_EXTRAPOLATION_NS = 250_000_000L;

    /** Smoothed playback position (seconds) and when it was last updated. */
    private float smoothedPosition;
    private long smoothedPositionTime;
    private boolean positionInitialized;

    private int source;
    private SoundBuffer buffer;
    private boolean unique;

    /** Who this source belongs to, for the players handed out per owner - see the sound manager. */
    private Object owner;

    /** When the owner last asked for this source, for pruning the ones whose owner is gone. */
    private long lastUsed = System.currentTimeMillis();

    public SoundPlayer(SoundBuffer buffer)
    {
        this.buffer = buffer;
        this.source = AL10.alGenSources();

        AL10.alSourcei(this.source, AL10.AL_BUFFER, buffer.getBuffer());
        AL10.alSourcef(this.source, AL10.AL_MAX_DISTANCE, 60);

        this.setRelative(false);
    }

    public SoundPlayer unique()
    {
        this.unique = true;

        return this;
    }

    public SoundPlayer owner(Object owner)
    {
        this.owner = owner;

        return this;
    }

    public Object getOwner()
    {
        return this.owner;
    }

    public void refresh()
    {
        this.lastUsed = System.currentTimeMillis();
    }

    public long getLastUsed()
    {
        return this.lastUsed;
    }

    public int getSource()
    {
        return this.source;
    }

    public SoundBuffer getBuffer()
    {
        return this.buffer;
    }

    public boolean isUnique()
    {
        return this.unique;
    }

    public boolean canBeRemoved()
    {
        return !this.unique && this.isStopped();
    }

    /* Properties */

    public static float getGainLimit()
    {
        if (gainLimit < 0F)
        {
            ALCapabilities capabilities = AL.getCapabilities();

            gainLimit = capabilities != null && capabilities.AL_SOFT_gain_clamp_ex
                ? Math.max(AL10.alGetFloat(SOFTGainClampEx.AL_GAIN_LIMIT_SOFT), 1F)
                : 1F;

            if (gainLimit <= 1F)
            {
                LOGGER.warn("AL_SOFT_gain_clamp_ex is missing, sounds can't be played louder than 100%");
            }
        }

        return gainLimit;
    }

    public void setVolume(float volume)
    {
        float gain = MathUtils.clamp(volume, 0F, getGainLimit());

        /* AL_MAX_GAIN has to be lifted first, or the gain below gets clamped back to it */
        if (gain > 1F)
        {
            AL10.alSourcef(this.source, AL10.AL_MAX_GAIN, gain);
        }

        AL10.alSourcef(this.source, AL10.AL_GAIN, gain);
    }

    public void setPitch(float pitch)
    {
        AL10.alSourcef(this.source, AL10.AL_PITCH, pitch);
    }

    public void setRelative(boolean relative)
    {
        AL10.alSourcei(this.source, AL10.AL_SOURCE_RELATIVE, relative ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    public void setLooping(boolean looping)
    {
        AL10.alSourcei(this.source, AL10.AL_LOOPING, looping ? AL10.AL_TRUE : AL10.AL_FALSE);
    }

    public void setPosition(Vector3f vector)
    {
        this.setPosition(vector.x, vector.y, vector.z);
    }

    public void setPosition(float x, float y, float z)
    {
        AL10.alSource3f(this.source, AL10.AL_POSITION, x, y, z);
    }

    public void setVelocity(Vector3f vector)
    {
        this.setVelocity(vector.x, vector.y, vector.z);
    }

    public void setVelocity(float x, float y, float z)
    {
        AL10.alSource3f(this.source, AL10.AL_VELOCITY, x, y, z);
    }

    /* Playback */

    public void play()
    {
        AL10.alSourcePlay(this.source);
    }

    public void pause()
    {
        AL10.alSourcePause(this.source);
    }

    public void stop()
    {
        AL10.alSourceStop(this.source);
    }

    public int getSourceState()
    {
        return AL10.alGetSourcei(this.source, AL10.AL_SOURCE_STATE);
    }

    public boolean isPlaying()
    {
        return this.getSourceState() == AL10.AL_PLAYING;
    }

    public boolean isPaused()
    {
        return this.getSourceState() == AL10.AL_PAUSED;
    }

    public boolean isStopped()
    {
        if (this.source == -1)
        {
            return true;
        }

        int state = this.getSourceState();

        return state == AL10.AL_STOPPED || state == AL10.AL_INITIAL;
    }

    public float getPlaybackPosition()
    {
        float raw = AL10.alGetSourcef(this.source, AL11.AL_SEC_OFFSET);
        long now = System.nanoTime();
        boolean playing = this.isPlaying();

        if (!this.positionInitialized)
        {
            this.smoothedPosition = raw;
            this.positionInitialized = true;
        }
        else if (playing)
        {
            long elapsed = Math.min(now - this.smoothedPositionTime, POSITION_MAX_EXTRAPOLATION_NS);

            this.smoothedPosition += elapsed / 1_000_000_000F;

            /* OpenAL refreshes the offset once per mixer period; pull the smoothed value
             * to it only when it has really drifted, so its jitter stays invisible. */
            if (Math.abs(raw - this.smoothedPosition) > POSITION_SNAP_THRESHOLD)
            {
                this.smoothedPosition = raw;
            }
        }
        else
        {
            this.smoothedPosition = raw;
        }

        this.smoothedPositionTime = now;

        float duration = this.buffer == null ? 0F : this.buffer.getDuration();

        if (duration > 0F)
        {
            this.smoothedPosition = MathUtils.clamp(this.smoothedPosition, 0F, duration);
        }

        return this.smoothedPosition;
    }

    public void setPlaybackPosition(float seconds)
    {
        seconds = MathUtils.clamp(seconds, 0, this.buffer.getDuration());

        AL10.alSourcef(this.source, AL11.AL_SEC_OFFSET, seconds);

        this.smoothedPosition = seconds;
        this.smoothedPositionTime = System.nanoTime();
        this.positionInitialized = true;
    }

    public void delete()
    {
        AL10.alDeleteSources(this.source);

        this.source = -1;
        this.buffer = null;
    }
}

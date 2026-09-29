package mchorse.bbs_mod.film.replays.tracks.compatibility;

import mchorse.bbs_mod.film.replays.tracks.TrackId;
import mchorse.bbs_mod.film.replays.tracks.TrackKind;

/** Ordering when a film mixes authored compound tracks and untouched independent curves. */
public final class LegacyTrackCompatibility
{
    private LegacyTrackCompatibility() {}

    public static int order(TrackId track)
    {
        if (track.kind().isWholeForm()) return 0;
        if (track.kind() == TrackKind.PROPERTY && track.subject().startsWith("splines/")) return 2;
        return 1;
    }
}

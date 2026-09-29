package mchorse.bbs_mod.ui.film.clips.renderer;

import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.overwrite.DimensionClip;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Dimension clip timeline renderer.
 *
 * <p>Renders timeline strips with dynamic dimension theme colors (Overworld green,
 * Nether red, End purple, custom cyan), formatted label display, and drop shadows.</p>
 */
public class UIDimensionClipRenderer extends UIClipRenderer<DimensionClip>
{
    public static int getDimensionColor(Link link)
    {
        if (link == null)
        {
            return 0x3498db;
        }

        return switch (link.toString())
        {
            case "minecraft:overworld" -> 0x27ae60;
            case "minecraft:the_nether" -> 0xc0392b;
            case "minecraft:the_end" -> 0x8e44ad;
            default -> 0x2980b9;
        };
    }

    @Override
    public String getDefaultLabel(UIClips clips, DimensionClip clip)
    {
        Link link = clip.dimension.get();
        if (link == null)
        {
            return "Dimension";
        }

        return switch (link.toString())
        {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "The End";
            default -> formatPath(link.path);
        };
    }

    private static String formatPath(String path)
    {
        if (path == null || path.isEmpty())
        {
            return "Dimension";
        }

        StringBuilder sb = new StringBuilder();
        for (String part : path.split("_"))
        {
            if (!part.isEmpty())
            {
                if (sb.length() > 0)
                {
                    sb.append(" ");
                }
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1)
                {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : path;
    }

    @Override
    public void renderClip(UIContext context, UIClips clips, DimensionClip clip, Area area, boolean selected, boolean current)
    {
        int y = area.y;
        int h = area.h;
        int left = area.x;
        int right = area.ex();

        ClipFactoryData data = clips.getFactory().getData(clip);
        int dimColor = getDimensionColor(clip.dimension.get());
        int color = Colors.A100 | dimColor;

        if (current)
        {
            int shadow = dimColor & Colors.RGB;
            context.batcher.dropShadow(left + 2, y + 2, right - 2, y + h - 2, 8, Colors.A75 | shadow, shadow);
        }

        if (clip.enabled.get())
        {
            this.renderBackground(context, color, clip, area, selected, current);
        }
        else
        {
            context.batcher.iconArea(Icons.DISABLED, color, left, y, (right - left), h);
        }

        context.batcher.outline(left, y, right, y + h, selected ? Colors.WHITE : Colors.A50);

        FontRenderer font = context.batcher.getFont();
        boolean hasIcon = right - left >= 20;
        int labelX = left + (hasIcon ? 20 : 5);
        String label = font.limitToWidth(clips.getClipDisplayName(clip), right - 2 - labelX);

        boolean alignTop = h >= 28;
        int labelY = alignTop ? y + 3 : y + (h - font.getHeight()) / 2;
        float iconAnchorY = alignTop ? 0F : 0.5F;
        int iconY = alignTop ? y + 3 : y + h / 2;

        if (hasIcon)
        {
            context.batcher.icon(data.icon, left + 2, iconY, 0F, iconAnchorY);
        }

        if (!label.isEmpty())
        {
            context.batcher.textShadow(label, labelX, labelY);
        }
    }
}

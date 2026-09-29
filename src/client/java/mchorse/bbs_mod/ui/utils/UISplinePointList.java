package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.cubic.spline.SplinePoint;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;

import java.util.List;
import java.util.function.Consumer;

/** The same point selection and visual identity in the rig and keyframe editors. */
public class UISplinePointList extends UIStringList
{
    public String viewportHover = "";

    public UISplinePointList(Consumer<List<String>> callback)
    {
        super(callback);
        this.background().multi();
    }

    public String hoveredPoint(UIContext context)
    {
        int index = this.area.isInside(context) ? this.getIndexAtCursor(context) : -1;
        return index >= 0 && index < this.getList().size() ? this.getList().get(index) : "";
    }

    @Override
    protected String elementToString(UIContext context, int index, String id)
    {
        return SplinePoint.displayName(index + 1);
    }

    @Override
    public void renderListElement(UIContext context, String id, int index, int x, int y, boolean hover, boolean selected)
    {
        super.renderListElement(context, id, index, x, y, hover || id.equals(this.viewportHover), selected);
    }
}

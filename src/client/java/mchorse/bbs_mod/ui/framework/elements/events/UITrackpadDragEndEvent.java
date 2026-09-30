package mchorse.bbs_mod.ui.framework.elements.events;

import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;

public class UITrackpadDragEndEvent extends UIEvent<UINumericInput<?>>
{
    public final boolean cancelled;

    public UITrackpadDragEndEvent(UINumericInput<?> element)
    {
        this(element, false);
    }

    public UITrackpadDragEndEvent(UINumericInput<?> element, boolean cancelled)
    {
        super(element);
        this.cancelled = cancelled;
    }
}

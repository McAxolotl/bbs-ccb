package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;

public class UILongKeyframeFactory extends UIKeyframeFactory<Long>
{
    private final UITextbox value;
    private long displayedValue;

    public UILongKeyframeFactory(UITrackValue<Long> track, UIKeyframes editor)
    {
        super(track, editor);

        /* A double-based trackpad cannot represent every long exactly. */
        this.value = new UITextbox(20, text ->
        {
            try
            {
                long value = Long.parseLong(text);
                this.track.setValue(value, this.displayedValue);
                this.displayedValue = value;
            }
            catch (NumberFormatException ignored)
            {}
        });
        this.scroll.add(this.value);
        this.update();
    }

    @Override
    public void update()
    {
        if (!this.value.isFocused())
        {
            this.displayedValue = this.getDisplayValue();
            this.value.setText(Long.toString(this.displayedValue));
        }
    }
}

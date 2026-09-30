package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import net.minecraft.item.ItemStack;

public class UIItemStackKeyframeFactory extends UIKeyframeFactory<ItemStack>
{
    private UIItemStack editor;

    public UIItemStackKeyframeFactory(UITrackValue<ItemStack> track, UIKeyframes editor)
    {
        super(track, editor);

        this.editor = new UIItemStack(this::setValue);
        this.editor.setStack(track.getValue());

        this.scroll.add(this.editor);
    }
    @Override
    public void update()
    {
        this.editor.setStack(this.getDisplayValue());
    }

}
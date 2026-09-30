package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UITrackValue;
import org.lwjgl.glfw.GLFW;

/**
 * Base class for numeric track factories (Double, Float, Integer).
 */
public abstract class UINumericKeyframeFactory <T extends Number> extends UIKeyframeFactory<T>
{
    protected UITrackpad value;

    private int lastMouseX;
    private boolean editingMode;
    private double editingInitialValue;
    private T displayedValue;

    public UINumericKeyframeFactory(UITrackValue<T> track, UIKeyframes editor)
    {
        super(track, editor);

        this.value = new UITrackpad((v) -> this.setNumericValue(v));
        this.displayedValue = track.getValue();
        this.value.setValue(this.getNumericValue(this.displayedValue));

        this.keys().register(Keys.TRANSFORMATIONS_TRANSLATE, this::startEditingMode).category(UIKeys.TRANSFORMS_KEYS_CATEGORY);
        this.scroll.add(this.value);
    }

    /**
     * Convert typed value to double for trackpad display.
     */
    protected abstract double getNumericValue(T value);

    /**
     * Convert double value back to typed value and update the given track.
     */
    protected abstract T convertValue(double value);

    /**
     * Convert the trackpad's Double before writing to a typed numeric track. Keep the name
     * distinct from setValue(Object), which would accept a boxed Double without conversion.
     */
    private void setNumericValue(double value)
    {
        T converted = this.convertValue(value);
        T before = this.displayedValue;
        this.displayedValue = converted;
        this.track.setValue(converted, before);
    }

    private void startEditingMode()
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        this.editor.beginValueGesture();
        this.editingInitialValue = this.value.getValue();
        this.lastMouseX = context.mouseX;
        this.editingMode = true;
    }

    private void stopEditingMode(boolean accept)
    {
        if (!this.editingMode)
        {
            return;
        }

        this.editingMode = false;

        if (accept) this.editor.endValueGesture();

        if (!accept)
        {
            this.value.setValue(this.editingInitialValue);
            this.editor.cancelValueGesture();
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.editingMode)
        {
            if (context.mouseButton == 0)
            {
                this.stopEditingMode(true);

                return true;
            }
            else if (context.mouseButton == 1)
            {
                this.stopEditingMode(false);

                return true;
            }
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.editingMode)
        {
            if (context.isPressed(GLFW.GLFW_KEY_ENTER))
            {
                this.stopEditingMode(true);

                return true;
            }
            else if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
            {
                this.stopEditingMode(false);

                return true;
            }
        }

        return super.subKeyPressed(context);
    }

    /** Nothing is refreshed under the user's hands: not while typing, dragging or grabbing. */
    private boolean isBusy()
    {
        return this.editingMode || this.value.isDragging() || this.value.textbox.isFocused();
    }

    @Override
    public boolean isUserEditing()
    {
        return this.editingMode || super.isUserEditing();
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (this.editingMode)
        {
            int dx = context.mouseX - this.lastMouseX;

            if (dx != 0)
            {
                double modifier = this.value.getValueModifier();
                double newValue = MathUtils.clamp(this.value.getValue() + dx * modifier, this.value.min, this.value.max);

                if (this.value.integer)
                {
                    newValue = (int) newValue;
                }

                this.value.setValue(newValue);
                this.setNumericValue(newValue);
                this.lastMouseX = context.mouseX;
            }

            String label = UIKeys.TRANSFORMS_EDITING.get();
            FontRenderer font = context.batcher.getFont();
            int x = this.area.mx(font.getWidth(label));
            int y = this.area.my(font.getHeight());

            context.batcher.textCard(label, x, y, Colors.WHITE, Colors.A50);
        }
    }

    @Override
    public void update()
    {
        super.update();

        if (!this.isBusy())
        {
            this.displayedValue = this.getDisplayValue();
            this.value.setValue(this.getNumericValue(this.displayedValue));
        }

    }
}

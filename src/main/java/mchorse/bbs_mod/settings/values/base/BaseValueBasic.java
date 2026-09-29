package mchorse.bbs_mod.settings.values.base;

import mchorse.bbs_mod.settings.values.IValueListener;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseValueBasic <T> extends BaseValue
{
    protected T value;
    protected T runtimeValue;

    private BaseValueBasic<?> storageOwner;
    private Supplier<T> stored;
    private Supplier<T> evaluated;
    private Consumer<T> writeStored;

    /** A field editor/API view into a compound value, with no second authored payload. */
    public void bind(BaseValueBasic<?> owner, Supplier<T> stored, Supplier<T> evaluated, Consumer<T> write)
    {
        this.storageOwner = owner;
        this.stored = stored;
        this.evaluated = evaluated;
        this.writeStored = write;
        this.value = null;
    }

    public boolean isBound() { return this.storageOwner != null; }

    public BaseValueBasic<?> animationProperty()
    {
        return this.storageOwner == null ? this : this.storageOwner.animationProperty();
    }

    protected void setOriginalValue(T value)
    {
        if (this.writeStored == null) this.value = value;
        else this.writeStored.accept(value);
    }

    @Override
    public void preNotify(int flag)
    {
        if (this.storageOwner == null) super.preNotify(flag);
        else this.storageOwner.preNotify(flag);
    }

    @Override
    public void postNotify(int flag)
    {
        if (this.storageOwner == null) super.postNotify(flag);
        else this.storageOwner.postNotify(flag);
    }

    /**
     * What the constructor declared, kept aside so {@link #reset()} has
     * something to go back to. Snapshotted rather than aliased — see
     * {@link #copyValue(Object)} — since plenty of payloads here get edited in
     * place and would otherwise drag the default along with them.
     */
    private T defaultValue;

    public BaseValueBasic(String id, T value)
    {
        super(id);

        this.value = value;

        this.captureDefault();
    }

    /**
     * Take the snapshot of the current value as this property's default.
     *
     * <p>Called from the constructor, and again by subclasses that only learn
     * how to copy their payload after {@code super()} has run — see
     * {@link BaseKeyframeFactoryValue}, whose factory is the thing doing the
     * copying.</p>
     */
    protected final void captureDefault()
    {
        this.defaultValue = this.copyValue(this.value);
    }

    /**
     * Detach a value from whoever holds it. Immutable payloads — numbers,
     * strings, enums — are their own copies; everything editable in place must
     * override this.
     */
    protected T copyValue(T value)
    {
        return value;
    }

    /**
     * How two values of this kind are told apart. Split out from
     * {@code equals()} because a few payloads compare by tolerance rather than
     * by identity.
     */
    protected boolean compareValue(T a, T b)
    {
        return Objects.equals(a, b);
    }

    public T getDefaultValue()
    {
        return this.defaultValue;
    }

    @Override
    public void reset()
    {
        this.set(this.copyValue(this.defaultValue));
    }

    @Override
    public boolean isDefault()
    {
        return this.compareValue(this.getOriginalValue(), this.defaultValue);
    }

    public T get()
    {
        if (this.runtimeValue != null)
        {
            return this.runtimeValue;
        }

        return this.evaluated == null ? this.value : this.evaluated.get();
    }

    public T getOriginalValue()
    {
        return this.stored == null ? this.value : this.stored.get();
    }

    public T getRuntimeValue()
    {
        return this.runtimeValue;
    }

    public void set(T value)
    {
        this.set(value, IValueListener.FLAG_DEFAULT);
    }

    public void set(T value, int flag)
    {
        this.preNotify(flag);
        this.setOriginalValue(value);
        this.postNotify(flag);
    }

    public void setRuntimeValue(T value)
    {
        this.runtimeValue = value;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }

        if (obj instanceof BaseValueBasic)
        {
            BaseValueBasic baseValue = (BaseValueBasic) obj;

            return Objects.equals(this.getOriginalValue(), baseValue.getOriginalValue());
        }

        return false;
    }
}

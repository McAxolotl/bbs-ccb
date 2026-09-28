package mchorse.bbs_mod.settings.values.core;

import mchorse.bbs_mod.cubic.chains.ChainControl;
import mchorse.bbs_mod.cubic.chains.ChainControls;
import mchorse.bbs_mod.settings.values.base.BaseKeyframeFactoryValue;
import mchorse.bbs_mod.utils.keyframes.factories.IKeyframeFactory;

/** Authored solver state and its evaluated override. Rig topology belongs to the form. */
public class ValueChainControls<C extends ChainControl<C>, S extends ChainControls<C, S>> extends BaseKeyframeFactoryValue<S>
{
    public ValueChainControls(String id, IKeyframeFactory<S> factory)
    {
        super(id, factory, factory.createEmpty());
        this.animatable(true);
    }

    @Override
    public void setRuntimeValue(S value)
    {
        if (value == null)
        {
            super.setRuntimeValue(null);
            return;
        }

        S resolved = this.getFactory().withDefaults(value, this.getOriginalValue());
        super.setRuntimeValue(resolved);
    }
}

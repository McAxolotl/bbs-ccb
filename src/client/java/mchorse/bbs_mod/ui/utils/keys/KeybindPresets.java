package mchorse.bbs_mod.ui.utils.keys;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.value.ValueKeyCombo;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Named snapshots of the registered key combos, kept separate from active keybinds.json. */
public class KeybindPresets
{
    private static File file()
    {
        return new File(BBSMod.getSettingsFolder(), "keybind-presets.json");
    }

    private static MapType read() throws IOException
    {
        if (!file().isFile()) return new MapType();

        BaseType data = DataToString.read(file());

        if (data == null || !data.isMap()) throw new IOException("Invalid keybind presets");

        return data.asMap();
    }

    private static void write(MapType data) throws IOException
    {
        IOUtils.writeText(file(), DataToString.toString(data, true));
    }

    public static List<String> names() throws IOException
    {
        List<String> names = new ArrayList<>(read().keys());

        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);

        return names;
    }

    public static boolean exists(String name) throws IOException
    {
        return read().has(name);
    }

    public static void save(String name, Settings settings) throws IOException
    {
        MapType presets = read();
        MapType snapshot = new MapType();

        for (Map.Entry<String, ValueGroup> category : settings.categories.entrySet())
        {
            MapType values = new MapType();

            for (BaseValue value : category.getValue().getAll())
            {
                if (value instanceof ValueKeyCombo) values.put(value.getId(), value.toData());
            }

            if (!values.isEmpty()) snapshot.put(category.getKey(), values);
        }

        presets.put(name, snapshot);
        write(presets);
    }

    public static boolean load(String name, Settings settings) throws IOException
    {
        BaseType preset = read().get(name);

        if (preset == null || !preset.isMap()) return false;

        MapType categories = preset.asMap();

        for (Map.Entry<String, ValueGroup> category : settings.categories.entrySet())
        {
            MapType values = categories.getMap(category.getKey());

            for (BaseValue value : category.getValue().getAll())
            {
                BaseType keys = values.get(value.getId());

                if (value instanceof ValueKeyCombo && keys != null && keys.isList()) value.fromData(keys);
            }
        }

        settings.save();

        return true;
    }

    public static void delete(String name) throws IOException
    {
        MapType presets = read();

        presets.remove(name);
        write(presets);
    }
}

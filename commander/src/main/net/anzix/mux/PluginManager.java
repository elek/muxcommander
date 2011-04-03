package net.anzix.mux;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class PluginManager {

    private static PluginManager INSTANCE = new PluginManager();

    public static PluginManager getInstace() {
        return INSTANCE;
    }

    public <T> List<T> getPlugins(Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        ServiceLoader<T> loader = ServiceLoader.load(clazz);
        Iterator<T> it = loader.iterator();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }
}

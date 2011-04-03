package hu.pagavcs.mug.findfile;

import com.mucommander.ui.action.ActionDescriptor;
import com.mucommander.ui.action.ActionFactory;
import net.anzix.mux.hooks.ActionHook;

/**
 * Hook to the plugin system.
 * 
 * @author elek
 */

public class FindFileActionHook implements ActionHook {

    @Override
    public ActionDescriptor createDescriptor() {
        return new FindFileAction.Descriptor();
    }

    @Override
    public ActionFactory createFactory() {
        return new FindFileAction.Factory();
    }
}

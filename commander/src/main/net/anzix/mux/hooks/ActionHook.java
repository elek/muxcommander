package net.anzix.mux.hooks;

import com.mucommander.ui.action.ActionDescriptor;
import com.mucommander.ui.action.ActionFactory;

public interface ActionHook {
	public ActionDescriptor createDescriptor();

	public ActionFactory createFactory();
}

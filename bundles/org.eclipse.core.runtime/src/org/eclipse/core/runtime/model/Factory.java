package org.eclipse.core.runtime.model;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
/**
 * An object which can create plug-in related model objects (typically when
 * parsing plug-in manifest files).
 * <p>
 * This class may be instantiated, or further subclassed.
 * </p>
 */
import org.eclipse.core.runtime.*;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.runtime.model.*;
import org.eclipse.core.internal.plugins.RegistryResolver;

public class Factory {
	private MultiStatus status;
/**
 * Creates a factory which can be used to create plug-in model objects.
 * Errors and warnings during parsing etc. can be logged to the given 
 * status via the <code>error</code> method.
 *
 * @param status the status to which errors should be logged
 */
public Factory(MultiStatus status) {
	super();
	this.status = status;
}
/**
 * Returns a new configuration element model which is not initialized.
 *
 * @return a new configuration element model
 */
public ConfigurationElementModel createConfigurationElement() {
	return new ConfigurationElementModel();
}
/**
 * Returns a new configuration property model which is not initialized.
 *
 * @return a new configuration property model
 */
public ConfigurationPropertyModel createConfigurationProperty() {
	return new ConfigurationPropertyModel();
}
/**
 * Returns a new extension model which is not initialized.
 *
 * @return a new extension model
 */
public ExtensionModel createExtension() {
	return new ExtensionModel();
}
/**
 * Returns a new extension point model which is not initialized.
 *
 * @return a new extension point model
 */
public ExtensionPointModel createExtensionPoint() {
	return new ExtensionPointModel();
}
/**
 * Returns a new library model which is initialized to not export any
 * of its code.
 *
 * @return a new library model
 */
public LibraryModel createLibrary() {
	return new LibraryModel();
}
/**
 * Returns a new plug-in descriptor model which is not initialized.
 *
 * @return a new plug-in descriptor model
 */
public PluginDescriptorModel createPluginDescriptor() {
	return new PluginDescriptorModel();
}
/**
 * Returns a new plug-in prerequisite model which is initialized to
 * not export its code and to not require an exact match.
 *
 * @return a new plug-in prerequisite model
 */
public PluginPrerequisiteModel createPluginPrerequisite() {
	return new PluginPrerequisiteModel();
}
/**
 * Returns a new plug-in registry model with an empty plug-in table.
 *
 * @return a new plug-in registry model
 */
public PluginRegistryModel createPluginRegistry() {
	return new PluginRegistryModel();
}
/**
 * Handles an error state specified by the status.  The collection of all logged status
 * objects can be accessed using <code>getStatus()</code>.
 *
 * @param error a status detailing the error condition
 */
public void error(IStatus error) {
	status.add(error);
	if (InternalPlatform.DEBUG && InternalPlatform.DEBUG_PLUGINS)
		System.out.println(error.toString());
}
/**
 * Returns all of the status objects logged thus far by this factory.
 *
 * @return a multi-status containing all of the logged status objects
 */
public MultiStatus getStatus() {
	return status;
}
}

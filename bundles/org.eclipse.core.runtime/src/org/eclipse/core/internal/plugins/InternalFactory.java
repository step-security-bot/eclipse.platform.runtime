package org.eclipse.core.internal.plugins;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.core.internal.runtime.InternalPlatform;

public class InternalFactory extends Factory {
public InternalFactory(MultiStatus status) {
	super(status);
}
public ConfigurationElementModel createConfigurationElement() {
	return new ConfigurationElement();
}
public ConfigurationPropertyModel createConfigurationProperty() {
	return new ConfigurationProperty();
}
public ExtensionModel createExtension() {
	return new Extension();
}
public ExtensionPointModel createExtensionPoint() {
	return new ExtensionPoint();
}
public LibraryModel createLibrary() {
	return new Library();
}
public PluginDescriptorModel createPluginDescriptor() {
	return new PluginDescriptor();
}
public PluginPrerequisiteModel createPluginPrerequisite() {
	return new PluginPrerequisite();
}
public PluginRegistryModel createPluginRegistry() {
	return new PluginRegistry();
}
}

package org.eclipse.core.runtime.model;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 2000
 */

/**
 * An object which represents the user-defined extension in a 
 * plug-in manifest.  
 * <p>
 * This class may be instantiated, or further subclassed.
 * </p>
 */
public class ExtensionModel extends PluginModelObject {

	// DTD properties (included in plug-in manifest)
	private String extensionPoint = null;
	private String id = null;
	private ConfigurationElementModel[] elements = null;

	// transient properties (not included in plug-in manifest)
	private PluginDescriptorModel plugin = null; // declaring plugin
/**
 * Creates a new extension model in which all fields
 * are <code>null</code>.
 */
public ExtensionModel() {}
/**
 * Returns the extension point with which this extension is associated.
 *
 * @return the extension point with which this extension is associated
 *  or <code>null</code>
 */
public String getExtensionPoint() {
	return extensionPoint;
}
/**
 * Returns the simple identifier of this extension, or <code>null</code>
 * if this extension does not have an identifier.
 * This identifier is specified in the plug-in manifest as a non-empty
 * string containing no period characters (<code>'.'</code>) and 
 * must be unique within the defining plug-in.
 *
 * @return the simple identifier of the extension (e.g. <code>"main"</code>)
 *  or <code>null</code>
 */
public String getId() {
	return id;
}
/**
 * Returns the plug-in descriptor in which this extension is declared.
 *
 * @return the plug-in descriptor in which this extension is declared
 *  or <code>null</code>
 */
public PluginDescriptorModel getParentPluginDescriptor() {
	return plugin;
}
/**
 * Returns the configuration element children of this extension.
 *
 * @return the configuration elements in this extension
 *  or <code>null</code>
 */
public ConfigurationElementModel[] getSubElements() {
	return elements;
}
/**
 * Sets this model object and all of its descendents to be read-only.
 * Subclasses may extend this implementation.
 *
 * @see #isReadOnly
 */
public void markReadOnly() {
	super.markReadOnly();
	if (elements != null)
		for (int i = 0; i < elements.length; i++)
			elements[i].markReadOnly();
}
/**
 * Set the extension point with which this extension is associated.
 * This object must not be read-only.
 *
 * @return the extension point with which this extension is associated.  
 *		May be <code>null</code>.
 */
public void setExtensionPoint(String value) {
	assertIsWriteable();
	extensionPoint = value;
}
/**
 * Sets the simple identifier of this extension, or <code>null</code>
 * if this extension does not have an identifier.
 * This identifier is specified in the plug-in manifest as a non-empty
 * string containing no period characters (<code>'.'</code>) and 
 * must be unique within the defining plug-in.
 * This object must not be read-only.
 *
 * @param value the simple identifier of the extension (e.g. <code>"main"</code>).
 *		May be <code>null</code>.
 */
public void setId(String value) {
	assertIsWriteable();
	id = value;
}
/**
 * Sets the plug-in descriptor in which this extension is declared.
 * This object must not be read-only.
 *
 * @param descriptor the plug-in descriptor in which this extension is declared.  
 *		May be <code>null</code>.
 */
public void setParentPluginDescriptor(PluginDescriptorModel value) {
	assertIsWriteable();
	plugin = value;
}
/**
 * Sets the configuration element children of this extension.
 * This object must not be read-only.
 *
 * @param value the configuration elements in this extension.  
 *		May be <code>null</code>.
 */
public void setSubElements(ConfigurationElementModel[] value) {
	assertIsWriteable();
	elements = value;
}
}

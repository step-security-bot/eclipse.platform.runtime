package org.eclipse.core.internal.plugins;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.*;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.internal.runtime.InternalPlatform;
import org.eclipse.core.internal.runtime.Policy;

public class RegistryResolver {

	private Map idmap;
	private int size;
	private PluginRegistryModel reg;
	private MultiStatus status;
	private boolean trimPlugins = true;
	private boolean crossLinking = true;

	public static final int MATCH_EXACT = 0;
	public static final int MATCH_COMPATIBLE = 1;
	public static final int MATCH_LATEST = 2;

	private boolean DEBUG_RESOLVE = false;
	private static final String OPTION_DEBUG_RESOLVE = "org.eclipse.core.runtime/registry/debug/resolve";

	// constraint entry
	private class Constraint {
		private PluginDescriptorModel parent;
		private PluginPrerequisiteModel prq;
		private PluginVersionIdentifier ver;
		private int type = MATCH_LATEST;
		private ConstraintsEntry cEntry = null;

		private Constraint(PluginDescriptorModel parent, PluginPrerequisiteModel prq) {
			this.parent = parent;
			this.prq = prq;
			if (prq != null) {
				ver = RegistryResolver.this.getVersionIdentifier(prq);
				if (ver == null)
					type = MATCH_LATEST;
				else
					if (prq.getMatch())
						type = MATCH_EXACT;
					else
						type = MATCH_COMPATIBLE;
			}
		}

		private int getMatchType() {
			return type;
		}

		private ConstraintsEntry getConstraintsEntry() {
			return cEntry;
		}

		private void setConstraintsEntry(ConstraintsEntry entry) {
			cEntry = entry;
		}

		private PluginDescriptorModel getParent() {
			return parent;
		}

		private PluginPrerequisiteModel getPrerequisite() {
			return prq;
		}

		private PluginVersionIdentifier getVersionIdentifier() {
			return ver;
		}

		public String toString() {
			if (prq == null)
				return "(null)";
			String s = parent.toString() + "->" + prq.getPlugin();
			String v = prq.getVersion();
			s += v == null ? "(any)" : (prq.getMatch() ? "(" + v + ",exact)" : "(" + v + ",compatible)");
			return s;
		}
	}

	// constraint index structure
	private class ConstraintsEntry {
		private IndexEntry parent;
		private List constraintList = new LinkedList();
		private PluginDescriptorModel lastResolved = null;
		private boolean isResolved = false;
		private PluginDescriptorModel bestMatch = null;
		private boolean bestMatchEnabled = false;

		private ConstraintsEntry(IndexEntry parent) {
			this.parent = parent;
		}

		private int constraintCount() {
			return constraintList.size();
		}

		private PluginDescriptorModel addConstraint(Constraint c) {
			constraintList.add(c);
			c.setConstraintsEntry(this);
			List constrained = getMatchingDescriptors();
			if (constrained.size() <= 0) {
				constraintList.remove(c);
				c.setConstraintsEntry(null);
				return null;
			} else {
				PluginDescriptorModel match = (PluginDescriptorModel) constrained.get(0);
				if (!match.equals(lastResolved)) {
					lastResolved = match;
					isResolved = false;
				}
				return match;
			}
		}

		private void removeConstraint(Constraint c) {
			if (DEBUG_RESOLVE)
				debug("removing constraint " + c.toString());
			constraintList.remove(c);
			c.setConstraintsEntry(null);
			lastResolved = null;
			isResolved = false;
		}

		private void removeConstraintFor(PluginPrerequisiteModel prereq) {
			List remove = new ArrayList();
			for (Iterator list = constraintList.iterator(); list.hasNext();) {
				Constraint c = (Constraint) list.next();
				if (c.getPrerequisite() == prereq)
					remove.add(c);
			}
			for (Iterator list = remove.iterator(); list.hasNext();)
				removeConstraint((Constraint) list.next());
		}

		private PluginDescriptorModel getMatchingDescriptor() {
			List constrained = getMatchingDescriptors();
			if (constrained.size() <= 0)
				return null;
			else
				return (PluginDescriptorModel) constrained.get(0);
		}

		private List getMatchingDescriptors() {
			List constrained = new LinkedList();

			for (Iterator list = parent.versions().iterator(); list.hasNext();) {
				PluginDescriptorModel pd = (PluginDescriptorModel) list.next();
				if (pd.getEnabled())
					constrained.add(pd);
			}

			for (Iterator list = constraintList.iterator(); list.hasNext();) {
				Constraint c = (Constraint) list.next();
				if (c.getMatchType() == MATCH_LATEST)
					continue;
				for (Iterator list2 = parent.versions().iterator(); list2.hasNext();) {
					PluginDescriptorModel pd = (PluginDescriptorModel) list2.next();
					if (!pd.getEnabled())
						continue;
					if (c.getMatchType() == MATCH_EXACT) {
						if (!getVersionIdentifier(pd).isEquivalentTo(c.getVersionIdentifier()))
							constrained.remove(pd);
					} else {
						if (!getVersionIdentifier(pd).isCompatibleWith(c.getVersionIdentifier()))
							constrained.remove(pd);
					}
				}
			}

			return constrained;
		}

		private void preresolve(List roots) {

			if (constraintList.size() <= 0) {
				if (roots.contains(parent.getId())) {
					bestMatch = (PluginDescriptorModel) parent.versions().get(0);
					if (bestMatch == null) {
						if (DEBUG_RESOLVE)
							debug("*ERROR* no resolved descriptor for " + parent.getId());
					} else
						bestMatchEnabled = bestMatch.getEnabled();
				}
			} else {
				bestMatch = getMatchingDescriptor();
				if (bestMatch == null) {
					if (DEBUG_RESOLVE)
						debug("*ERROR* no resolved descriptor for " + parent.getId());
				} else
					bestMatchEnabled = true;
			}
		}

		private void resolve() {
			if (bestMatch != null) {
				bestMatch.setEnabled(bestMatchEnabled);
				if (bestMatchEnabled) {
					if (DEBUG_RESOLVE)
						debug("configured " + bestMatch.toString());
					if (constraintList.size() > 0) {
						for (int i = 0; i < constraintList.size(); i++) {
							PluginPrerequisiteModel prq = (PluginPrerequisiteModel) ((Constraint) constraintList.get(i)).getPrerequisite();
							prq.setResolvedVersion(getVersionIdentifier(bestMatch).toString());
						}
					}
				}
			}
		}

		private boolean isResolved() {
			return this.isResolved;
		}
		private void isResolved(boolean isResolved) {
			this.isResolved = isResolved;
		}
	}

	// plugin descriptor index structure
	private class IndexEntry {
		private String id;
		private List verList = new LinkedList();
		private List concurrentList = new ArrayList();

		private IndexEntry(String id) {
			this.id = id;
			concurrentList.add(new ConstraintsEntry(this));
		}

		private String getId() {
			return id;
		}

		private ConstraintsEntry getConstraintsEntryFor(Constraint c) {
			ConstraintsEntry ce = c.getConstraintsEntry();
			if (ce != null)
				return ce;
			ce = (ConstraintsEntry) concurrentList.get(0);
			if (c.getPrerequisite() == null)
				c.setConstraintsEntry(ce);
			return ce;
		}

		private PluginDescriptorModel addConstraint(Constraint c) {
			int concurrentCount = concurrentList.size();

			// try to find constraits entry that can accommodate new constraint
			for (Iterator list = concurrentList.iterator(); list.hasNext();) {
				ConstraintsEntry cie = (ConstraintsEntry) list.next();
				PluginDescriptorModel pd = cie.addConstraint(c);
				if (pd != null) {

					// constraint added OK and no concurrency
					if (concurrentCount <= 1)
						return pd;

					// constraint added OK but have concurrency
					if (allowConcurrencyFor(pd))
						return pd;
					else {
						cie.removeConstraint(c); // cannot be concurrent
						return null;
					}
				}
			}

			// attempt to create new constraints entry
			ConstraintsEntry cie;
			PluginDescriptorModel pd;

			if (concurrentList.size() == 1) {
				// ensure base entry allows concurrency
				cie = (ConstraintsEntry) concurrentList.get(0);
				pd = cie.getMatchingDescriptor();
				if (!allowConcurrencyFor(pd))
					return null;
			}

			cie = new ConstraintsEntry(this);
			pd = cie.addConstraint(c);
			if (pd == null) {
				cie.removeConstraint(c); // no matching target
				return null;
			}
			if (!allowConcurrencyFor(pd)) {
				cie.removeConstraint(c); // cannot be concurrent
				return null;
			}
			if (DEBUG_RESOLVE)
				debug("creating new constraints list in " + id + " for " + c.toString());
			concurrentList.add(cie);
			return pd;
		}

		private boolean allowConcurrencyFor(PluginDescriptorModel pd) {
			if (pd == null)
				return false;
			if (pd.getDeclaredExtensions() != null && pd.getDeclaredExtensions().length > 0)
				return false;
			if (pd.getDeclaredExtensionPoints() != null && pd.getDeclaredExtensionPoints().length > 0)
				return false;
			return true;
		}

		private void removeConstraint(Constraint c) {
			ConstraintsEntry cie = getConstraintsEntryFor(c);
			cie.removeConstraint(c);
			if (concurrentList.get(0) != cie && cie.constraintCount() == 0)
				concurrentList.remove(cie);
		}

		private void removeConstraintFor(PluginPrerequisiteModel prereq) {
			for (Iterator list = concurrentList.iterator(); list.hasNext();)
				 ((ConstraintsEntry) list.next()).removeConstraintFor(prereq);
		}

		private PluginDescriptorModel getMatchingDescriptorFor(Constraint c) {
			ConstraintsEntry cie = getConstraintsEntryFor(c);
			return cie.getMatchingDescriptor();
		}

		private void disableAllDescriptors() {
			for (Iterator list = verList.iterator(); list.hasNext();) {
				PluginDescriptorModel pd = (PluginDescriptorModel) list.next();
				pd.setEnabled(false);
			}
		}

		private void resolveDependencies(List roots) {
			for (Iterator list = concurrentList.iterator(); list.hasNext();)
				 ((ConstraintsEntry) list.next()).preresolve(roots);
			disableAllDescriptors();
			for (Iterator list = concurrentList.iterator(); list.hasNext();)
				 ((ConstraintsEntry) list.next()).resolve();
		}

		private List versions() {
			return verList;
		}

		private boolean isResolvedFor(Constraint c) {
			ConstraintsEntry cie = getConstraintsEntryFor(c);
			return cie.isResolved();
		}

		private void isResolvedFor(Constraint c, boolean value) {
			ConstraintsEntry cie = getConstraintsEntryFor(c);
			cie.isResolved(value);
		}

	}

	// subtree resolution "cookie" (composite change list)
	private class Cookie {
		private boolean ok = true;
		private List changes = new ArrayList();

		private Cookie() {
		}

		private boolean addChange(Constraint c) {
			PluginPrerequisiteModel prereq = c.getPrerequisite();
			for (Iterator list = changes.iterator(); list.hasNext();)
				if (prereq == ((Constraint)list.next()).getPrerequisite())
					return false; // prereq loop
			changes.add(c);
			return true;
		}

		private List getChanges() {
			return changes;
		}

		private void clearChanges() {
			if (changes.size() >= 0)
				changes = new ArrayList();
		}

		private boolean isOk() {
			return ok;
		}

		private void isOk(boolean value) {
			ok = value;
		}
	}
public RegistryResolver() {	
	String debug = Platform.getDebugOption(OPTION_DEBUG_RESOLVE);
	DEBUG_RESOLVE = debug==null ? false : ( debug.equalsIgnoreCase("true") ? true : false );
}
private void add(PluginDescriptorModel pd) {

	String key = pd.getId();
	List verList;
	IndexEntry ix = (IndexEntry) idmap.get(key);

	// create new index entry if one does not exist for plugin
	if (ix == null) {
		ix = new IndexEntry(key);
		idmap.put(key, ix);
	}

	// insert plugin into list maintaining version order
	verList = ix.versions();
	int i = 0;
	for (i = 0; i < verList.size(); i++) {
		PluginDescriptorModel element = (PluginDescriptorModel) verList.get(i);
		if (getVersionIdentifier(pd).equals(getVersionIdentifier(element)))
			return; // ignore duplicates
		if (getVersionIdentifier(pd).isGreaterThan(getVersionIdentifier(element)))
			break;
	}
	verList.add(i, pd);

	// keep track of number of entries
	size++;
}
private void addAll(Collection c) {
	for (Iterator list = c.iterator(); list.hasNext();)
		add((PluginDescriptorModel) list.next());
}
private void debug(String s) {
	System.out.println("Registry Resolve: "+s);
}
private void error(String message) {
	Status error = new Status(IStatus.WARNING, Platform.PI_RUNTIME, Platform.PARSE_PROBLEM, message, null);
	status.add(error);
	if (InternalPlatform.DEBUG && DEBUG_RESOLVE)
		System.out.println(error.toString());
}
/**
 * Returns a value indicating if extensions are to be linked 
 * with their associated extension point during the resolve process.
 *
 */
public boolean getCrossLinking() {
	return crossLinking;
}
public IExtensionPoint getExtensionPoint(PluginDescriptorModel plugin, String extensionPointId) {
	if (extensionPointId == null)
		return null;
	ExtensionPointModel[] list = plugin.getDeclaredExtensionPoints();
	if (list == null)
		return null;
	for (int i = 0; i < list.length; i++) {
		if (extensionPointId.equals(list[i].getId()))
			return (IExtensionPoint) list[i];
	}
	return null;
}
/**
 * Gets the boolean value which determines if disabled plugins
 * will be removed when this resolve is done.
 *
 */
public boolean getTrimPlugins() {
	return trimPlugins;
}
private PluginVersionIdentifier getVersionIdentifier(PluginDescriptorModel descriptor) {
	String version = descriptor.getVersion();
	if (version == null)
		return new PluginVersionIdentifier("1.0.0");
	try {
		return new PluginVersionIdentifier(version);
	} catch (Throwable e) {
		return new PluginVersionIdentifier("1.0.0");
	}
}
private PluginVersionIdentifier getVersionIdentifier(PluginPrerequisiteModel prereq) {
	String version = prereq.getVersion();
	return version == null ? null : new PluginVersionIdentifier(version);
}
private void removeConstraintFor(PluginPrerequisiteModel prereq) {

	String id = prereq.getPlugin();
	IndexEntry ix = (IndexEntry) idmap.get(id);
	if (ix == null) {
		if (DEBUG_RESOLVE)
			debug("unable to locate index entry for " + id);
		return;
	}
	ix.removeConstraintFor(prereq);
}
private void resolve() {

	// resolve root descriptors
	List rd = resolveRootDescriptors();
	if (rd.size() == 0) {
		// no roots ... quit
		idmap = null;
		reg = null;
		error("Unable to resolve plugin registry");
		return;
	}

	// sort roots
	Object[] a = rd.toArray();
	Arrays.sort(a);
	ArrayList roots = new ArrayList(Arrays.asList(a));
	
	// walk the dependencies and setup constraints
	ArrayList orphans = new ArrayList();
	for (int i = 0; i < roots.size(); i++)
		resolveNode((String) roots.get(i), null, null, null, orphans);
	for (int i = 0; i < orphans.size(); i++) {
		if (!roots.contains(orphans.get(i))) {
			roots.add(orphans.get(i));
			if (DEBUG_RESOLVE)
				debug("orphan " + orphans.get(i));
		}
	}

	// resolve dependencies
	Iterator plugins = idmap.entrySet().iterator();
	while (plugins.hasNext()) {
		IndexEntry ix = (IndexEntry) ((Map.Entry) plugins.next()).getValue();
		ix.resolveDependencies(roots);
	}

	// walk down the registry structure and resolve links
	resolvePluginRegistry();

	// unhook registry and index
	idmap = null;
	reg = null;
}
public IStatus resolve(PluginRegistryModel registry) {
	status = new MultiStatus(Platform.PI_RUNTIME, IStatus.OK, "", null);
	if (registry.isResolved())
		return status;
	reg = registry;
	idmap = new HashMap();
	size = 0;
	addAll(Arrays.asList(reg.getPlugins()));
	resolve();
	registry.markResolved();
	return status;
}
private void resolveExtension(ExtensionModel ext) {

	String target = ext.getExtensionPoint();
	int ix = target.lastIndexOf(".");
	String pluginId = target.substring(0, ix);
	String extPtId = target.substring(ix + 1);
	String message;

	PluginDescriptorModel plugin = (PluginDescriptorModel) reg.getPlugin(pluginId);
	if (plugin == null) {
		message = Policy.bind("extPointUnknown", new String[] { target, ext.getParentPluginDescriptor().getId()});
		error(message);
		return;
	}
	if (!plugin.getEnabled()) {
		message = Policy.bind("extPointDisabled", new String[] { target, ext.getParentPluginDescriptor().getId()});
		error(message);
		return;
	}

	ExtensionPointModel extPt = (ExtensionPointModel) getExtensionPoint(plugin, extPtId);
	if (extPt == null) {
		message = Policy.bind("extPointUnknown", new String[] { target, ext.getParentPluginDescriptor().getId()});
		error(message);
		return;
	}

	ExtensionModel[] oldValues = extPt.getDeclaredExtensions();
	ExtensionModel[] newValues = null;
	if (oldValues == null)
		newValues = new ExtensionModel[1];
	else {
		newValues = new ExtensionModel[oldValues.length + 1];
		System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
	}
	newValues[newValues.length - 1] = ext;
	extPt.setDeclaredExtensions(newValues);
}
private Cookie resolveNode(String child, PluginDescriptorModel parent, PluginPrerequisiteModel prq, Cookie cookie, List orphans) {
	// This method is called recursively to setup dependency constraints.
	// Top invocation is passed null parent and null prerequisite.

	if (DEBUG_RESOLVE)
		debug("PUSH> " + child);

	if (cookie == null)
		cookie = new Cookie();

	// lookup child entry
	IndexEntry ix = (IndexEntry) idmap.get(child);
	if (ix == null) {
		if (parent != null)
			error(Policy.bind("pluginPrereqDisabled", new String[] { parent.getId(), child }));
		if (DEBUG_RESOLVE)
			debug("<POP  " + child + " not found");
		cookie.isOk(false);
		return cookie;
	}

	// try to add new dependency constraint
	Constraint currentConstraint = new Constraint(parent, prq);
	PluginDescriptorModel childPd = null;
	if (parent != null) {
		childPd = ix.addConstraint(currentConstraint);
		if (childPd == null) {
			error("Unable to satisfy prerequisite constraint from " + parent.getId() + " to " + child);
			if (DEBUG_RESOLVE)
				debug("<POP  " + child + " unable to satisfy constraint");
			cookie.isOk(false);
			return cookie;
		} else
			if (!cookie.addChange(currentConstraint)) {
				error("Detected prerequisite loop from " + parent.getId() + " to " + child);
				if (DEBUG_RESOLVE)
					debug("<POP  " + child + " prerequisite loop");
				cookie.isOk(false);
				return cookie;
			}
	} else {
		childPd = ix.getMatchingDescriptorFor(currentConstraint);
		if (childPd == null) {
			if (DEBUG_RESOLVE)
				debug("<POP  " + child + " not found (missing descriptor entry)");
			cookie.isOk(false);
			return cookie;
		}
	}

	// check to see if subtree is already resolved
	if (ix.isResolvedFor(currentConstraint)) {
		if (DEBUG_RESOLVE)
			debug("<POP  " + child + " already resolved");
		return cookie;
	}

	// select the subtree to resolve
	PluginPrerequisiteModel[] prereqs = childPd.getRequires();
	PluginPrerequisiteModel prereq;
	prereqs = prereqs == null ? new PluginPrerequisiteModel[0] : prereqs;
	for (int i = 0; cookie.isOk() && i < prereqs.length; i++) {
		prereq = (PluginPrerequisiteModel) prereqs[i];
		cookie = resolveNode(prereq.getPlugin(), childPd, prereq, cookie, orphans);
	}

	// if we failed, remove any constraints we added
	if (!cookie.isOk()) {
		Constraint cookieConstraint;
		for (Iterator change = cookie.getChanges().iterator(); change.hasNext();) {
			cookieConstraint = (Constraint) change.next();
			if (childPd == cookieConstraint.getParent()) {
				prereq = cookieConstraint.getPrerequisite();
				removeConstraintFor(prereq);
				if (!orphans.contains(prereq.getPlugin())) // keep track of orphaned subtrees
					orphans.add(prereq.getPlugin());
			}
		}
		if (parent != null)
			error(Policy.bind("pluginPrereqDisabled", new String[] { parent.getId(), child }));
		childPd.setEnabled(false);
		if (DEBUG_RESOLVE)
			debug("<POP  " + child + " failed to resolve subtree");
		return cookie;
	} else {
		// we're done
		ix.isResolvedFor(currentConstraint, true);
		if (DEBUG_RESOLVE)
			debug("<POP  " + child + " " + getVersionIdentifier(childPd));
		return cookie;
	}
}
private void resolvePluginDescriptor(PluginDescriptorModel pd) {
	if (getCrossLinking()) {
		ExtensionModel[] list = pd.getDeclaredExtensions();
		if (list == null || list.length == 0)
			return;
		for (int i = 0; i < list.length; i++) {
			resolveExtension((ExtensionModel) list[i]);
		}
	}
}
private void resolvePluginRegistry() {
	// filter out disabled plugins from "live" registry
	trimRegistry();

	// resolve relationships
	PluginDescriptorModel[] list = reg.getPlugins();
	for (int i = 0; i < list.length; i++)
		resolvePluginDescriptor((PluginDescriptorModel) list[i]);
}
private List resolveRootDescriptors() {

	// Determine the roots of the dependency tree. Disable all
	// but one versions of the root descriptors.

	// get list of all plugin identifiers in the registry
	List ids = new ArrayList();
	ids.addAll(idmap.keySet());

	// iterate over the list eliminating targets of <requires> entries
	Iterator p = idmap.entrySet().iterator();
	while (p.hasNext()) {
		IndexEntry ix = (IndexEntry) ((Map.Entry) p.next()).getValue();
		if (ix != null) {
			List list = ix.versions();
			if (list.size() > 0) {
				PluginDescriptorModel pd = (PluginDescriptorModel) list.get(0);
				PluginPrerequisiteModel[] prereqs = pd.getRequires();
				for (int i = 0; prereqs != null && i < prereqs.length; i++) {
					ids.remove(prereqs[i].getPlugin());
				}
			}
		}
	}

	if (ids.size() > 0) {
		// disable all but the most recent version of root descriptors
		String id;
		p = ids.iterator();
		while (p.hasNext()) {
			id = (String) p.next();
			IndexEntry ix = (IndexEntry) idmap.get(id);
			if (ix != null) {
				List list = ix.versions();
				for (int i = 0; i < list.size(); i++) {
					PluginDescriptorModel pd = (PluginDescriptorModel) list.get(i);
					if (i == 0) {
						if (DEBUG_RESOLVE)
							debug("root " + pd);
					} else {
						if (DEBUG_RESOLVE)
							debug("     " + pd + " disabled");
						pd.setEnabled(false);
					}
				}
			}
		}
	} else {
		if (DEBUG_RESOLVE)
			debug("NO ROOTS");
	}

	return ids;
}
/**
 * Allows all extensions to be linked with their associated
 * extension point during the resolve process.
 *
 */
public void setCrossLinking(boolean doCrossLinking) {
	crossLinking = doCrossLinking;
}
/**
 * Allows all disabled plugins to be removed when the resolve
 * is completed.
 *
 */
public void setTrimPlugins(boolean trimDisabledPlugins) {
	trimPlugins = trimDisabledPlugins;
}
private void trimRegistry() {

	if (getTrimPlugins()) {
		PluginDescriptorModel[] list = reg.getPlugins();
		for (int i = 0; i < list.length; i++) {
			PluginDescriptorModel pd = (PluginDescriptorModel) list[i];
			if (!pd.getEnabled()) {
				if (DEBUG_RESOLVE)
					debug("removing " + pd.toString());
				reg.removePlugin(pd.getId(), pd.getVersion());
			}
		}
	}
}
}

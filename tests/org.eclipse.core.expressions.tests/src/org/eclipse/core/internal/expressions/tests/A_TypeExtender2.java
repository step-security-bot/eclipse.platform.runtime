/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.expressions.tests;

import org.eclipse.core.runtime.Assert;

import org.eclipse.core.expressions.PropertyTester;

public class A_TypeExtender2 extends PropertyTester {

	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		if ("chaining".equals(method)) { //$NON-NLS-1$
			return "A2".equals(expectedValue); //$NON-NLS-1$
		} else if ("chainOrdering".equals(method)) { //$NON-NLS-1$
			return "A2".equals(expectedValue); //$NON-NLS-1$
		}
		Assert.isTrue(false);
		return false;
	}

}

/*******************************************************************************
 * Copyright (c) 2010 JVM Monitor project. All rights reserved. 
 * 
 * This code is distributed under the terms of the Eclipse Public License v1.0
 * which is available at http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jboss.tools.jmx.jvmmonitor.internal.ui.views;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.jboss.tools.jmx.jvmmonitor.core.IHost;
import org.jboss.tools.jmx.jvmmonitor.core.IJvm;
import org.jboss.tools.jmx.jvmmonitor.core.ISnapshot;
import org.jboss.tools.jmx.jvmmonitor.core.JvmModel;

/**
 * The content provider for JVMs tree viewer.
 */
public class JvmTreeContentProvider implements ITreeContentProvider {

    /*
     * @see ITreeContentProvider#getChildren(Object)
     */
    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof IHost) {
            return ((IHost) parentElement).getJvms().toArray(new IJvm[0]);
        } else if (parentElement instanceof IJvm) {
            return ((IJvm) parentElement).getShapshots().toArray(
                    new ISnapshot[0]);
        }
        return null;
    }

    /*
     * @see ITreeContentProvider#getParent(Object)
     */
    @Override
    public Object getParent(Object element) {
        if (element instanceof IJvm) {
            return ((IJvm) element).getHost();
        } else if (element instanceof ISnapshot) {
            return ((ISnapshot) element).getJvm();
        }
        return null;
    }

    /*
     * @see ITreeContentProvider#hasChildren(Object)
     */
    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof IHost) {
        	List<IJvm> jvms =  ((IHost) element).getJvms();
            return jvms != null && jvms.size() > 0;
        } else if (element instanceof IJvm) {
        	List<ISnapshot>  snaps = ((IJvm) element).getShapshots();
            return snaps != null && snaps.size() > 0;
        }
        return false;
    }

    /*
     * @see IStructuredContentProvider#getElements(Object)
     */
    @Override
    public Object[] getElements(Object inputElement) {
        return JvmModel.getInstance().getHosts().toArray(new IHost[0]);
    }

    /*
     * @see IContentProvider#dispose()
     */
    @Override
    public void dispose() {
        // do nothing
    }

    /*
     * @see IContentProvider#inputChanged(Viewer, Object, Object)
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // do nothing
    }
}

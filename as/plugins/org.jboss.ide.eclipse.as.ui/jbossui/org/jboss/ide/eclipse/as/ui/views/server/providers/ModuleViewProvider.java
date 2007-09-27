/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.ide.eclipse.as.ui.views.server.providers;

import java.util.Properties;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.IServerLifecycleListener;
import org.eclipse.wst.server.core.IServerListener;
import org.eclipse.wst.server.core.IServerWorkingCopy;
import org.eclipse.wst.server.core.ServerEvent;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.internal.PublishServerJob;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.ui.ServerUICore;
import org.eclipse.wst.server.ui.internal.view.servers.ModuleServer;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListener;
import org.jboss.ide.eclipse.as.core.server.UnitedServerListenerManager;
import org.jboss.ide.eclipse.as.core.util.ServerConverter;
import org.jboss.ide.eclipse.as.ui.JBossServerUISharedImages;
import org.jboss.ide.eclipse.as.ui.Messages;
import org.jboss.ide.eclipse.as.ui.views.server.extensions.ServerViewProvider;
import org.jboss.ide.eclipse.as.ui.views.server.extensions.SimplePropertiesViewExtension;

public class ModuleViewProvider extends SimplePropertiesViewExtension {

	private ModuleContentProvider contentProvider;
	private ModuleLabelProvider labelProvider;
	private Action deleteModuleAction, fullPublishModuleAction, incrementalPublishModuleAction;
	private ModuleServer selection;
	private IServerLifecycleListener serverResourceListener;
	private IServerListener serverListener;
	
	public ModuleViewProvider() {
		contentProvider = new ModuleContentProvider();
		labelProvider = new ModuleLabelProvider();
		createActions();
		addListeners();
	}

	private void createActions() {
		deleteModuleAction = new Action() {
			public void run() {
				if (MessageDialog.openConfirm(new Shell(), Messages.ServerDialogHeading, Messages.DeleteModuleConfirm)) {
					Thread t = new Thread() { public void run() { 
						try {
							IServerWorkingCopy server = selection.server.createWorkingCopy();
							
							if( ServerConverter.getDeployableServer(selection.server) != null ) {
								ServerUtil.modifyModules(server, new IModule[0], selection.module, new NullProgressMonitor());
								IServer server2 = server.save(true, null);
								ServerConverter.getDeployableServerBehavior(selection.server)
									.publishOneModule(IServer.PUBLISH_INCREMENTAL, selection.module, ServerBehaviourDelegate.REMOVED, new NullProgressMonitor());
							} else {
								ServerUtil.modifyModules(server, new IModule[0], selection.module, new NullProgressMonitor());
								IServer server2 = server.save(true, null);
								server2.publish(IServer.PUBLISH_INCREMENTAL, new NullProgressMonitor());
							}
						} catch (Exception e) {
							// ignore
						}
					}};
					t.start();
				}
			}
		};
		deleteModuleAction.setText(Messages.DeleteModuleText);
		deleteModuleAction.setDescription(Messages.DeleteModuleDescription);
		deleteModuleAction.setImageDescriptor(JBossServerUISharedImages.getImageDescriptor(JBossServerUISharedImages.UNPUBLISH_IMAGE));
		
		fullPublishModuleAction = new Action() {
			public void run() {
				actionPublish(IServer.PUBLISH_FULL);
			}
		};
		fullPublishModuleAction.setText(Messages.PublishModuleText);
		fullPublishModuleAction.setDescription(Messages.PublishModuleDescription);
		fullPublishModuleAction.setImageDescriptor(JBossServerUISharedImages.getImageDescriptor(JBossServerUISharedImages.PUBLISH_IMAGE));

	
		incrementalPublishModuleAction = new Action() {
			public void run() {
				actionPublish(IServer.PUBLISH_INCREMENTAL);
			}
		};
		incrementalPublishModuleAction.setText("Incremental Publish");
		incrementalPublishModuleAction.setDescription(Messages.PublishModuleDescription);
		incrementalPublishModuleAction.setImageDescriptor(JBossServerUISharedImages.getImageDescriptor(JBossServerUISharedImages.PUBLISH_IMAGE));
}
	
	protected void actionPublish(final int type) {
		try {
			if( ServerConverter.getDeployableServer(selection.server) != null ) {
				new Job("Publish One Module To Server") {
					protected IStatus run(IProgressMonitor monitor) {
						ServerConverter.getDeployableServerBehavior(selection.server)
						.publishOneModule(type, selection.module, 
								ServerBehaviourDelegate.CHANGED, new NullProgressMonitor());
						return Status.OK_STATUS;
					} 
				}.schedule();
			} else {
				// can't do anything special here, sadly
				new PublishServerJob(selection.server, type, true).schedule();
			}
		} catch( Exception e ) {
			// ignore
		}
	}

	
	public void fillContextMenu(Shell shell, IMenuManager menu, Object selection) {
		if( selection instanceof ModuleServer) {
			this.selection = (ModuleServer)selection;
			if( this.selection.module.length == 1 )
				menu.add(deleteModuleAction);
			menu.add(fullPublishModuleAction);
			menu.add(incrementalPublishModuleAction);
		}
	}

	public ITreeContentProvider getContentProvider() {
		return contentProvider;
	}

	public LabelProvider getLabelProvider() {
		return labelProvider;
	}

	public boolean supports(IServer server) {
		return true;
	}

	
	class ModuleContentProvider implements ITreeContentProvider {

		private IServer input;
		
		public Object[] getChildren(Object parentElement) {
			
			if (parentElement instanceof ModuleServer) {
				ModuleServer ms = (ModuleServer) parentElement;
				try {
					IModule[] children = ms.server.getChildModules(ms.module, null);
					int size = children.length;
					ModuleServer[] ms2 = new ModuleServer[size];
					for (int i = 0; i < size; i++) {
						int size2 = ms.module.length;
						IModule[] module = new IModule[size2 + 1];
						System.arraycopy(ms.module, 0, module, 0, size2);
						module[size2] = children[i];
						ms2[i] = new ModuleServer(ms.server, module);
					}
					return ms2;
				} catch (Exception e) {
					return new Object[]{};
				}
			}

			
			
			if( parentElement instanceof ServerViewProvider && input != null ) {
				IModule[] modules = input.getModules(); 
				int size = modules.length;
				ModuleServer[] ms = new ModuleServer[size];
				for (int i = 0; i < size; i++) {
					ms[i] = new ModuleServer(input, new IModule[] { modules[i] });
				}
				return ms;
			}
			return new Object[] {};
		}

		public Object getParent(Object element) {
			if( element instanceof ModuleServer ) {
				return provider;
			}
			
			return null;
		}

		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0 ? true : false; 
		}

		// unused
		public Object[] getElements(Object inputElement) {
			return null;
		}

		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			input = (IServer)newInput;
		}
		
		public IServer getServer() {
			return input;
		}
	}
	
	class ModuleLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if( obj instanceof ModuleServer ) {
				ModuleServer ms = (ModuleServer)obj;
				int size = ms.module.length;
				return ms.module[size - 1].getName();
			}

			return "garbage";
		}
		public Image getImage(Object obj) {
			if( obj instanceof ModuleServer ) {
				ModuleServer ms = (ModuleServer)obj;
				int size = ms.module.length;
				return ServerUICore.getLabelProvider().getImage(ms.module[ms.module.length - 1]);
			}
			return null;
		}

	}


	public String[] getPropertyKeys(Object selected) {
		return new String[] { Messages.ModulePropertyType, Messages.ModulePropertyProject };
	}
	
	public Properties getProperties(Object selected) {
		Properties props = new Properties();
		if( selected != null && selected instanceof ModuleServer) {
			IModule mod = ((ModuleServer)selected).module[0];
			if( mod != null && mod.getProject() != null ) {
				props.setProperty(Messages.ModulePropertyType, mod.getModuleType().getId());
				props.setProperty(Messages.ModulePropertyProject, mod.getProject().getName());
			}
		}
		return props;
	}

	private void addListeners() {
		UnitedServerListenerManager.getDefault().addListener(new UnitedServerListener() {
			public void serverChanged(ServerEvent event) {
				int eventKind = event.getKind();
				if ((eventKind & ServerEvent.MODULE_CHANGE) != 0) {
					// module change event
					if ((eventKind & ServerEvent.STATE_CHANGE) != 0 || (eventKind & ServerEvent.PUBLISH_STATE_CHANGE) != 0) {
						refreshViewer();
					} 
				}
			}			
		});
	}
}

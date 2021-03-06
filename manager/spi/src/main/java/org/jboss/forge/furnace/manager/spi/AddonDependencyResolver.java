/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.manager.spi;

import java.io.File;

import org.jboss.forge.furnace.addons.AddonId;

/**
 * A resolver that knows how to construct a graph of the requested Addon
 * 
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
public interface AddonDependencyResolver
{
   /**
    * Resolve the dependency hierarchy for use during the addon installation.
    */
   public AddonInfo resolveAddonDependencyHierarchy(final AddonId addonId);

   /**
    * Resolve an artifact given an AddonId coordinate
    */
   public File[] resolveResources(final AddonId addonId);

   /**
    * Resolve all versions of a given AddonId
    */
   public AddonId[] resolveVersions(final String addonName);
}

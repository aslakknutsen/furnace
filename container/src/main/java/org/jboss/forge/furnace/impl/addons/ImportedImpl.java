/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.forge.furnace.impl.addons;

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.addons.AddonStatus;
import org.jboss.forge.furnace.exception.ContainerException;
import org.jboss.forge.furnace.lock.LockManager;
import org.jboss.forge.furnace.lock.LockMode;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.forge.furnace.spi.ExportedInstance;
import org.jboss.forge.furnace.spi.ServiceRegistry;
import org.jboss.forge.furnace.util.Assert;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
public class ImportedImpl<T> implements Imported<T>
{
   private Map<T, ExportedInstance<T>> instanceMap = new ConcurrentHashMap<T, ExportedInstance<T>>(
            new WeakHashMap<T, ExportedInstance<T>>(new IdentityHashMap<T, ExportedInstance<T>>()));

   private AddonRegistry addonRegistry;
   private LockManager lock;
   private Class<T> type;
   private String typeName;

   public ImportedImpl(AddonRegistry addonRegistry, LockManager lock, Class<T> type)
   {
      this.addonRegistry = addonRegistry;
      this.lock = lock;
      this.type = type;
      this.typeName = type.getName();
   }

   public ImportedImpl(AddonRegistryImpl addonRegistry, LockManager lock, String typeName)
   {
      this.addonRegistry = addonRegistry;
      this.lock = lock;
      this.typeName = typeName;
   }

   @Override
   public Iterator<T> iterator()
   {
      return new ImportedIteratorImpl(this, getExportedInstances());
   }

   @Override
   public T get()
   {
      if (isAmbiguous())
         throw new IllegalStateException("Cannot resolve Ambiguous dependencies: " + toString());

      ExportedInstance<T> exported = getExportedInstance();
      if (exported != null)
      {
         T instance = exported.get();
         instanceMap.put(instance, exported);
         return instance;
      }
      else
         throw new ContainerException("No services of type [" + typeName + "] could be found in any started addons.");
   }

   @Override
   public void release(T instance)
   {
      ExportedInstance<T> exported = instanceMap.get(instance);
      if (exported != null)
      {
         instanceMap.remove(instance);
         exported.release(instance);
      }
   }

   @Override
   public T selectExact(Class<T> type)
   {
      Assert.notNull(type, "Type to select must not be null.");
      Set<ExportedInstance<T>> instances = getExportedInstances();
      for (ExportedInstance<T> instance : instances)
      {
         if (type.equals(instance.getActualType()))
         {
            T result = instance.get();
            instanceMap.put(result, instance);
            return result;
         }
      }
      throw new ContainerException("No services of type [" + type + "] could be found in any started addons.");
   }

   private ExportedInstance<T> getExportedInstance()
   {
      return lock.performLocked(LockMode.READ, new Callable<ExportedInstance<T>>()
      {
         @Override
         public ExportedInstance<T> call() throws Exception
         {
            ExportedInstance<T> result = null;

            for (Addon addon : addonRegistry.getAddons())
            {
               if (AddonStatus.STARTED.equals(addon.getStatus()))
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  if (type != null)
                  {
                     result = serviceRegistry.getExportedInstance(type);
                  }
                  else
                  {
                     result = serviceRegistry.getExportedInstance(typeName);
                  }
               }
               if (result != null)
                  break;
            }

            return result;
         }
      });

   }

   private Set<ExportedInstance<T>> getExportedInstances()
   {
      return lock.performLocked(LockMode.READ, new Callable<Set<ExportedInstance<T>>>()
      {
         @SuppressWarnings({ "unchecked", "rawtypes" })
         @Override
         public Set<ExportedInstance<T>> call() throws Exception
         {
            Set<ExportedInstance<T>> result = new HashSet<ExportedInstance<T>>();

            for (Addon addon : addonRegistry.getAddons())
            {
               if (AddonStatus.STARTED.equals(addon.getStatus()))
               {
                  ServiceRegistry serviceRegistry = addon.getServiceRegistry();
                  if (type != null)
                     result.addAll(serviceRegistry.getExportedInstances(type));
                  else
                     result.addAll((Collection) serviceRegistry.getExportedInstances(typeName));
               }
            }

            return result;
         }
      });
   }

   private class ImportedIteratorImpl implements Iterator<T>
   {
      private ImportedImpl<T> imported;
      private Iterator<ExportedInstance<T>> iterator;

      public ImportedIteratorImpl(ImportedImpl<T> imported, Set<ExportedInstance<T>> instances)
      {
         this.imported = imported;
         this.iterator = instances.iterator();
      }

      @Override
      public boolean hasNext()
      {
         return iterator.hasNext();
      }

      @Override
      public T next()
      {
         ExportedInstance<T> exported = iterator.next();
         T instance = exported.get();
         imported.instanceMap.put(instance, exported);
         return instance;
      }

      @Override
      public void remove()
      {
         throw new UnsupportedOperationException("Removal not supported.");
      }
   }

   @Override
   public String toString()
   {
      StringBuilder result = new StringBuilder();

      result.append("[");
      Iterator<ExportedInstance<T>> iterator = this.getExportedInstances().iterator();
      while (iterator.hasNext())
      {
         ExportedInstance<T> instance = iterator.next();
         result.append(instance.getActualType().getName()).append(" from addon ");
         result.append(instance.getSourceAddon().getId());
         if (iterator.hasNext())
            result.append(",\n");
      }
      result.append("]");

      return result.toString();
   }

   @Override
   public boolean isUnsatisfied()
   {
      return getExportedInstances().isEmpty();
   }

   @Override
   public boolean isAmbiguous()
   {
      return getExportedInstances().size() > 1;
   }

}

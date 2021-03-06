package org.jboss.forge.furnace.se;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Set;

import org.jboss.forge.furnace.Furnace;
import org.jboss.forge.furnace.addons.Addon;
import org.jboss.forge.furnace.addons.AddonId;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.impl.FurnaceImpl;
import org.jboss.forge.furnace.proxy.ClassLoaderAdapterCallback;
import org.jboss.forge.furnace.repositories.AddonRepositoryMode;
import org.jboss.forge.furnace.util.AddonFilters;
import org.junit.Assert;
import org.junit.Test;

public class BootstrapClassLoaderTestCase
{
   @Test
   public void shouldBeAbleToLoadEnvironment() throws Exception
   {
      final BootstrapClassLoader cl = new BootstrapClassLoader("bootpath");
      Class<?> bootstrapType = cl.loadClass("org.jboss.forge.furnace.impl.FurnaceImpl");
      Method method = bootstrapType.getMethod("startAsync", new Class<?>[] { ClassLoader.class });
      Object result = method.invoke(bootstrapType.newInstance(), cl);
      Assert.assertEquals(FurnaceImpl.class.getName(), result.getClass().getName());
   }

   @Test(expected = IllegalStateException.class)
   public void shouldBeAbleToUseFactoryDelegateTypesafely() throws Exception
   {
      Furnace instance = FurnaceFactory.getInstance();
      Assert.assertNotNull(instance);
      AddonRegistry registry = instance.getAddonRegistry();
      Assert.assertNotNull(registry);
   }

   @Test
   public void shouldBeAbleToPassPrimitivesIntoDelegate() throws Exception
   {
      Furnace instance = FurnaceFactory.getInstance();
      Assert.assertNotNull(instance);
      instance.setServerMode(false);
   }

   @Test
   public void shouldBeAbleToPassClassesIntoDelegate() throws Exception
   {
      Furnace instance = FurnaceFactory.getInstance();
      File tempDir = File.createTempFile("test", "repository");
      tempDir.delete();
      tempDir.mkdir();
      tempDir.deleteOnExit();
      instance.addRepository(AddonRepositoryMode.IMMUTABLE, tempDir);
      instance.getRepositories().get(0).getAddonResources(AddonId.from("a", "1"));
   }

   @Test(expected = IllegalStateException.class)
   public void shouldBeAbleToPassInterfacesIntoDelegate() throws Exception
   {
      Furnace instance = FurnaceFactory.getInstance();
      Set<Addon> addons = instance.getAddonRegistry().getAddons(AddonFilters.allStarted());
      Assert.assertNotNull(addons);
   }

   @Test
   public void shouldBeAbleToEnhanceAddonId() throws Exception
   {
      ClassLoader loader = AddonId.class.getClassLoader();
      AddonId enhanced = ClassLoaderAdapterCallback.enhance(loader, new URLClassLoader(
               new URL[] { new URL("file:///") }),
               AddonId.from("a", "1"), AddonId.class);
      Assert.assertNotNull(enhanced);

   }

   @Test
   public void shouldBeAbleToEnhanceAddonIdIntoDelegate() throws Exception
   {
      ClassLoader fromLoader = AddonId.class.getClassLoader();
      ClassLoader toLoader = new URLClassLoader(new URL[] { new URL("file:///") });
      ClassLoaderAdapterCallback.enhance(fromLoader, toLoader,
               AddonId.from("a", "1"), AddonId.class);
   }
}
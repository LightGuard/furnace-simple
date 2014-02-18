package org.jboss.forge.furnace.container.simple;

import java.util.Iterator;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.forge.arquillian.AddonDependency;
import org.jboss.forge.arquillian.Dependencies;
import org.jboss.forge.arquillian.archive.ForgeArchive;
import org.jboss.forge.furnace.addons.AddonRegistry;
import org.jboss.forge.furnace.container.simple.lifecycle.SimpleContainer;
import org.jboss.forge.furnace.repositories.AddonDependencyEntry;
import org.jboss.forge.furnace.services.Imported;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 */
@RunWith(Arquillian.class)
public class AddonRegistryInstanceLookupTest
{
   @Deployment(order = 2)
   @Dependencies({
            @AddonDependency(name = "org.jboss.forge.furnace.container:simple")
   })
   public static ForgeArchive getDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addAsServiceProvider(Service.class, AddonRegistryInstanceLookupTest.class)
               .addAsAddonDependencies(
                        AddonDependencyEntry.create("org.jboss.forge.furnace.container:simple"),
                        AddonDependencyEntry.create("dependency", "1")
               );

      return archive;
   }

   @Deployment(name = "dependency,1", testable = false, order = 1)
   public static ForgeArchive getDependencyDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(PublishedService.class)
               .addAsLocalServices(PublishedService.class);

      return archive;
   }

   @Deployment(name = "other,1", testable = false, order = 0)
   public static ForgeArchive getContainerDeployment()
   {
      ForgeArchive archive = ShrinkWrap.create(ForgeArchive.class)
               .addClasses(PublishedService.class)
               .addAsLocalServices(PublishedService.class);

      return archive;
   }

   @Test
   public void testMissingNamedLookupReturnsEmptyInstance() throws Exception
   {
      AddonRegistry registry = SimpleContainer.getFurnace(this.getClass().getClassLoader()).getAddonRegistry();
      Imported<PublishedService> instance = registry.getServices("org.example.blah.NotExistsBadClassThing");
      Assert.assertNotNull(instance);
      Assert.assertFalse(instance.isAmbiguous());
      Assert.assertTrue(instance.isUnsatisfied());
   }

   @Test
   public void testMissingTypedLookupReturnsEmptyInstance() throws Exception
   {
      AddonRegistry registry = SimpleContainer.getFurnace(this.getClass().getClassLoader()).getAddonRegistry();
      Imported<AddonDependencyEntry> instance = registry.getServices(AddonDependencyEntry.class);
      Assert.assertNotNull(instance);
      Assert.assertFalse(instance.isAmbiguous());
      Assert.assertTrue(instance.isUnsatisfied());
   }

   @Test
   public void testTypedLookupReturnsProperType() throws Exception
   {
      AddonRegistry registry = SimpleContainer.getFurnace(this.getClass().getClassLoader()).getAddonRegistry();
      Imported<PublishedService> instance = registry.getServices(PublishedService.class);
      Assert.assertNotNull(instance);
      PublishedService service = instance.get();
      Assert.assertNotNull(service);
   }

   @Test
   public void testTypedLookupCanBeIterated() throws Exception
   {
      AddonRegistry registry = SimpleContainer.getFurnace(this.getClass().getClassLoader()).getAddonRegistry();
      Imported<PublishedService> instance = registry.getServices(PublishedService.class);
      Assert.assertFalse(instance.isAmbiguous());
      Assert.assertFalse(instance.isUnsatisfied());
      Iterator<PublishedService> iterator = instance.iterator();
      Assert.assertTrue(iterator.hasNext());
      Assert.assertNotNull(iterator.next());
      Assert.assertFalse(iterator.hasNext());
   }

   @Test(expected = IllegalStateException.class)
   public void testGetWhenAmbiguousThrowsException() throws Exception
   {
      AddonRegistry registry = SimpleContainer.getFurnace(this.getClass().getClassLoader()).getAddonRegistry();
      Imported<PublishedService> instance = registry.getServices(PublishedService.class.getName());
      Assert.assertTrue(instance.isAmbiguous());
      instance.get();
   }

   @Test
   public void testNameLookupReturnsAllMatches() throws Exception
   {
      AddonRegistry registry = SimpleContainer.getFurnace(this.getClass().getClassLoader()).getAddonRegistry();
      Imported<PublishedService> instance = registry.getServices(PublishedService.class.getName());
      Assert.assertTrue(instance.isAmbiguous());
      Assert.assertFalse(instance.isUnsatisfied());

      Assert.assertNotNull(instance);
      Iterator<PublishedService> iterator = instance.iterator();
      Assert.assertTrue(iterator.hasNext());
      Object first = iterator.next();
      Assert.assertNotNull(first);
      Assert.assertTrue(iterator.hasNext());
      Assert.assertTrue(iterator.hasNext());
      Object second = iterator.next();
      Assert.assertNotNull(second);
      Assert.assertFalse(iterator.hasNext());

      boolean typeMatchFound = false;
      if (first instanceof PublishedService)
         typeMatchFound = true;
      if (second instanceof PublishedService)
         typeMatchFound = true;

      Assert.assertTrue(typeMatchFound);
   }

   @Test
   public void testAmbiguousSelect() throws Exception
   {
      AddonRegistry registry = SimpleContainer.getFurnace(this.getClass().getClassLoader()).getAddonRegistry();
      Imported<PublishedService> instance = registry.getServices(PublishedService.class.getName());
      Assert.assertTrue(instance.isAmbiguous());
      PublishedService service = instance.selectExact(PublishedService.class);
      Assert.assertNotNull(service);
   }

   @Test(expected = IllegalStateException.class)
   public void testAmbiguousGetThrowsException() throws Exception
   {
      AddonRegistry registry = SimpleContainer.getFurnace(this.getClass().getClassLoader()).getAddonRegistry();
      Imported<PublishedService> instance = registry.getServices(PublishedService.class.getName());
      Assert.assertTrue(instance.isAmbiguous());
      instance.get();
      Assert.fail("Should not have been able to resolve.");
   }

}
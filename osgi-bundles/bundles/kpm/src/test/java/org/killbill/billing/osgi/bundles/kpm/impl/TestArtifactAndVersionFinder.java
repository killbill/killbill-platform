/*
 * Copyright 2020-2023 Equinix, Inc
 * Copyright 2014-2023 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.osgi.bundles.kpm.impl;

import java.util.Optional;

import org.killbill.billing.osgi.bundles.kpm.PluginIdentifiersDAO;
import org.killbill.billing.osgi.bundles.kpm.impl.ArtifactAndVersionFinder.ArtifactAndVersionModel;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestArtifactAndVersionFinder {

    private ArtifactAndVersionFinder artifactAndVersionFinder;

    @BeforeMethod(groups = "fast")
    public void beforeMethod() {
        final PluginIdentifiersDAO pluginIdentifiersDAO = Mockito.mock(PluginIdentifiersDAO.class);
        final AvailablePluginsComponentsFactory factory = Mockito.mock(AvailablePluginsComponentsFactory.class);
        final ArtifactAndVersionFinder toSpy = new ArtifactAndVersionFinder(pluginIdentifiersDAO, factory);
        artifactAndVersionFinder = Mockito.spy(toSpy);
    }

    @Test(groups = "fast")
    public void findArtifactAndVersionWithNonNullInput() {
        final Optional<ArtifactAndVersionModel> result = artifactAndVersionFinder
                .findArtifactAndVersion("0.24.0", "test-plugin-key", "test-artifact-id", "1.0.0", true);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isAttributesSet());
        Assert.assertEquals(result.get().getArtifactId(), "test-artifact-id");
        Assert.assertEquals(result.get().getVersion(), "1.0.0");
    }

    @Test(groups = "fast")
    public void findArtifactAndVersionWithSearchFromPluginsIdentifier() {
        final ArtifactAndVersionModel model = new ArtifactAndVersionModel("artifact-from-plugin-identifier", "1.2.3");
        Mockito.doReturn(model).when(artifactAndVersionFinder).searchFromPluginsIdentifier("test-plugin-key", false);

        final Optional<ArtifactAndVersionModel> result = artifactAndVersionFinder.findArtifactAndVersion("0.24.0", "test-plugin-key", null, null, false);

        Mockito.verify(artifactAndVersionFinder, Mockito.times(1)).searchFromPluginsIdentifier("test-plugin-key", false);
        Mockito.verify(artifactAndVersionFinder, Mockito.never()).searchFromPluginsDirectory("0.24.0", "test-plugin-key", true);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isAttributesSet());
        Assert.assertEquals(result.get().getArtifactId(), "artifact-from-plugin-identifier");
        Assert.assertEquals(result.get().getVersion(), "1.2.3");
    }

    @Test(groups = "fast")
    public void findArtifactAndVersionWithSearchFromAvailablePlugins() {
        Mockito.doReturn(new ArtifactAndVersionModel(null, null))
               .when(artifactAndVersionFinder).searchFromPluginsIdentifier("test-plugin-key", false);
        Mockito.doReturn(new ArtifactAndVersionModel("artifact-from-available-plugin", "4.5.6"))
                .when(artifactAndVersionFinder).searchFromPluginsDirectory("0.24.0", "test-plugin-key", true);

        final Optional<ArtifactAndVersionModel> result = artifactAndVersionFinder.findArtifactAndVersion("0.24.0", "test-plugin-key", null, null, true);

        Mockito.verify(artifactAndVersionFinder, Mockito.times(1)).searchFromPluginsIdentifier("test-plugin-key", true);
        Mockito.verify(artifactAndVersionFinder, Mockito.times(1)).searchFromPluginsDirectory("0.24.0", "test-plugin-key", true);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isAttributesSet());
        Assert.assertEquals(result.get().getArtifactId(), "artifact-from-available-plugin");
        Assert.assertEquals(result.get().getVersion(), "4.5.6");
    }

    /**
     * Make sure that artifactAndVersionFinder.searchFromPluginsIdentifier() returned first even through
     * artifactAndVersionFinder.searchFromPluginsIdentifier() and artifactAndVersionFinder.searchFromPluginsDirectory()
     * contains valid return value.
     */
    @Test(groups = "fast")
    public void findArtifactAndVersionWithSearchFromPluginsIdentifierFirst() {
        // Valid return value
        Mockito.doReturn(new ArtifactAndVersionModel("artifact-plugins-id-first", "10.0.0"))
               .when(artifactAndVersionFinder).searchFromPluginsIdentifier("test-plugin-key", false);
        // This also valid return value
        Mockito.doReturn(new ArtifactAndVersionModel("artifact-plugins-id-first", "10.0.0"))
               .when(artifactAndVersionFinder).searchFromPluginsDirectory("0.24.0", "test-plugin-key", true);

        final Optional<ArtifactAndVersionModel> result = artifactAndVersionFinder.findArtifactAndVersion("0.24.0", "test-plugin-key", null, null, false);

        Mockito.verify(artifactAndVersionFinder, Mockito.times(1)).searchFromPluginsIdentifier("test-plugin-key", false);
        // searchFromPluginsIdentifier() found valid result. This should never have executed
        Mockito.verify(artifactAndVersionFinder, Mockito.never()).searchFromPluginsDirectory("0.24.0", "test-plugin-key", true);
        Assert.assertTrue(result.isPresent());
        Assert.assertTrue(result.get().isAttributesSet());
        Assert.assertEquals(result.get().getArtifactId(), "artifact-plugins-id-first");
        Assert.assertEquals(result.get().getVersion(), "10.0.0");
    }

    @Test(groups = "fast")
    public void findArtifactAndVersionWithNoSearchResult() {
        Mockito.doReturn(new ArtifactAndVersionModel(null, null))
               .when(artifactAndVersionFinder).searchFromPluginsIdentifier("test-plugin-key", false);
        Mockito.doReturn(new ArtifactAndVersionModel(null, null))
               .when(artifactAndVersionFinder).searchFromPluginsDirectory("0.24.0", "test-plugin-key", true);

        final Optional<ArtifactAndVersionModel> result = artifactAndVersionFinder.findArtifactAndVersion("0.24.0", "test-plugin-key", null, null, true);

        Mockito.verify(artifactAndVersionFinder, Mockito.times(1)).searchFromPluginsIdentifier("test-plugin-key", true);
        Mockito.verify(artifactAndVersionFinder, Mockito.times(1)).searchFromPluginsDirectory("0.24.0", "test-plugin-key", true);
        Assert.assertTrue(result.isEmpty());
    }
}

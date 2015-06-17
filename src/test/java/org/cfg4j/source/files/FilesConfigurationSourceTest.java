/*
 * Copyright 2015 Norbert Potocki (norbert.potocki@nort.pl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cfg4j.source.files;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.data.MapEntry;
import org.cfg4j.source.context.DefaultEnvironment;
import org.cfg4j.source.context.Environment;
import org.cfg4j.source.context.ImmutableEnvironment;
import org.cfg4j.source.context.MissingEnvironmentException;
import org.cfg4j.source.git.ConfigFilesProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;


@RunWith(MockitoJUnitRunner.class)
public class FilesConfigurationSourceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TempConfigurationFileRepo fileRepo;
  private ConfigFilesProvider configFilesProvider;
  private FilesConfigurationSource source;

  @Before
  public void setUp() throws Exception {
    fileRepo = new TempConfigurationFileRepo("cfg4j-test-repo");
    fileRepo.changeProperty("application.properties", "some.setting", "masterValue");
    fileRepo.changeProperty("otherConfig.properties", "otherConfig.setting", "masterValue");
    fileRepo.changeProperty("malformed.properties", "otherConfig.setting", "\\uzzzzz");
    fileRepo.changeProperty("otherApplicationConfigs/application.properties", "some.setting", "otherAppSetting");

    configFilesProvider = () -> Collections.singletonList(
        new File(fileRepo.getURI() + "/application.properties")
    );

    source = new FilesConfigurationSource(configFilesProvider);
  }

  @After
  public void tearDown() throws Exception {
    fileRepo.remove();
  }

  @Test
  public void getConfiguration2ShouldReadFromGivenPath() throws Exception {
    configFilesProvider = () -> Collections.singletonList(
        new File("application.properties")
    );

    source = new FilesConfigurationSource(configFilesProvider);

    Environment environment = new ImmutableEnvironment(fileRepo.getURI() + "/otherApplicationConfigs/");

    assertThat(source.getConfiguration(environment)).containsOnly(MapEntry.entry("some.setting", "otherAppSetting"));
  }

  @Test
  public void getConfiguration2ShouldReadFromGivenFiles() throws Exception {
    configFilesProvider = () -> Arrays.asList(
        new File(fileRepo.getURI() + "/application.properties"),
        new File(fileRepo.getURI() + "/otherConfig.properties")
    );

    source = new FilesConfigurationSource(configFilesProvider);
    assertThat(source.getConfiguration(new DefaultEnvironment())).containsOnlyKeys("some.setting", "otherConfig.setting");
  }

  @Test
  public void getConfiguration2ShouldThrowOnMissingEnvironment() throws Exception {
    expectedException.expect(MissingEnvironmentException.class);
    source.getConfiguration(new ImmutableEnvironment("awlerijawoetinawwerlkjn"));
  }

  @Test
  public void getConfiguration2ShouldThrowOnMissingConfigFile() throws Exception {
    fileRepo.deleteFile("application.properties");

    expectedException.expect(IllegalStateException.class);
    source.getConfiguration(new DefaultEnvironment());
  }

  @Test
  public void getConfiguration2ShouldThrowOnMalformedConfigFile() throws Exception {
    configFilesProvider = () -> Collections.singletonList(
        new File(fileRepo.getURI() + "/malformed.properties")
    );

    source = new FilesConfigurationSource(configFilesProvider);

    expectedException.expect(IllegalStateException.class);
    source.getConfiguration(new DefaultEnvironment());
  }

  @Test
  public void refreshShouldUpdateGetConfiguration2OnDefaultBranch() throws Exception {
    fileRepo.changeProperty("application.properties", "some.setting", "changedValue");
    source.refresh();

    assertThat(source.getConfiguration(new DefaultEnvironment())).containsOnly(MapEntry.entry("some.setting", "changedValue"));
  }
}
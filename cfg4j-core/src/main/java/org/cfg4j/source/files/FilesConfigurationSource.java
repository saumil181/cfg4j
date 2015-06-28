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

import static java.util.Objects.requireNonNull;

import org.cfg4j.source.ConfigFilesProvider;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.context.Environment;
import org.cfg4j.source.context.MissingEnvironmentException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * {@link ConfigurationSource} reading configuration from local files.
 */
public class FilesConfigurationSource implements ConfigurationSource {

  private final ConfigFilesProvider configFilesProvider;

  /**
   * Construct {@link ConfigurationSource} backed by files. Uses "application.properties" file
   * located in the path specified by the {@link Environment} provided to {@link #getConfiguration(Environment)}
   * calls (see corresponding javadoc for detail).
   */
  public FilesConfigurationSource() {
    this.configFilesProvider = () -> Collections.singletonList(
        FileSystems.getDefault().getPath("application.properties")
    );
  }

  /**
   * Construct {@link ConfigurationSource} backed by files. File paths should by provided by
   * {@link ConfigFilesProvider} and will be treated as relative paths to the environment provided in
   * {@link #getConfiguration(Environment)} calls (see corresponding javadoc for detail).
   *
   * @param configFilesProvider {@link ConfigFilesProvider} supplying a list of configuration files to use
   */
  public FilesConfigurationSource(ConfigFilesProvider configFilesProvider) {
    this.configFilesProvider = requireNonNull(configFilesProvider);
  }

  /**
   * Get configuration set for a given {@code environment} from this source in a form of {@link Properties}.
   * {@link Environment} name is prepended to all file paths from {@link ConfigFilesProvider}
   * to form an absolute configuration file path. If environment name is empty paths are treated as relative
   * to the user's home directory location.
   *
   * @param environment environment to use
   * @return configuration set for {@code environment}
   * @throws MissingEnvironmentException when requested environment couldn't be found
   * @throws IllegalStateException       when unable to fetch configuration
   */
  @Override
  public Properties getConfiguration(Environment environment) {
    Properties properties = new Properties();

    Path rootPath;
    if (environment.getName().trim().isEmpty()) {
      rootPath = FileSystems.getDefault().getPath(System.getProperty("user.home"));
    } else {
      rootPath = FileSystems.getDefault().getPath(environment.getName());
    }

    if (!rootPath.toFile().exists()) {
      throw new MissingEnvironmentException("Directory doesn't exist: " + rootPath);
    }

    List<Path> paths = StreamSupport.stream(configFilesProvider.getConfigFiles().spliterator(), false)
        .map(rootPath::resolve)
        .collect(Collectors.toList());

    for (Path path : paths) {
      try (InputStream input = new FileInputStream(path.toFile())) {
        properties.load(input);
      } catch (IOException | IllegalArgumentException e) {
        throw new IllegalStateException("Unable to load properties from application.properties file", e);
      }
    }

    return properties;
  }

  @Override
  public void reload() {
    // NOP
  }

  @Override
  public String toString() {
    return "FilesConfigurationSource{" +
        "configFilesProvider=" + configFilesProvider +
        '}';
  }
}
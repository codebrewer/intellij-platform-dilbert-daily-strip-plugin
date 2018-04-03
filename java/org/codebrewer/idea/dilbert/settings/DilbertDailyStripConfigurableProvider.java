/*
 *  Copyright 2016 Mark Scott
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.codebrewer.idea.dilbert.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import org.codebrewer.idea.dilbert.DilbertDailyStripPlugin;
import org.jetbrains.annotations.Nullable;

/**
 * <p>
 * A {@code ConfigurableProvider} registered in {@code plugin.xml} to provide a
 * {@code Configurable} implementation to the system to allow the plug-in's
 * settings to be configured by the user.
 * </p>
 *
 * @author Mark Scott
 */
public class DilbertDailyStripConfigurableProvider extends ConfigurableProvider
{
  @Nullable
  @Override
  public Configurable createConfigurable()
  {
    return ApplicationManager.getApplication().getComponent(DilbertDailyStripPlugin.class);
  }
}
/*
 *  Copyright 2005, 2007, 2008, 2018 Mark Scott
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

package org.codebrewer.intellijplatform.plugin.dilbert;

import com.intellij.openapi.diagnostic.Logger;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JComponent;
import javax.swing.event.EventListenerList;
import org.codebrewer.intellijplatform.plugin.dilbert.http.DilbertDailyStripFetcher;
import org.codebrewer.intellijplatform.plugin.dilbert.settings.ApplicationSettings;
import org.codebrewer.intellijplatform.plugin.dilbert.settings.UnattendedDownloadSettings;
import org.codebrewer.intellijplatform.plugin.dilbert.strategy.CurrentDailyStripProvider;
import org.codebrewer.intellijplatform.plugin.dilbert.strategy.DailyStripProvider;
import org.codebrewer.intellijplatform.plugin.dilbert.ui.DailyStripPresenter;
import org.codebrewer.intellijplatform.plugin.dilbert.ui.SettingsPanel;
import org.codebrewer.intellijplatform.plugin.dilbert.util.PeriodicStripFetcher;
import org.codebrewer.intellijplatform.plugin.dilbert.util.VersionInfo;
import org.codebrewer.intellijplatform.plugin.util.l10n.ResourceBundleManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * An implementation of a plugin that fetches and displays the current daily
 * cartoon strip from the dilbert.com website.
 * </p>
 *
 * @author Mark Scott
 */
public final class DilbertDailyStripPluginImpl implements DilbertDailyStripPlugin {
  /**
   * For logging messages to IDEA's log.
   */
  private static final Logger LOGGER = Logger.getInstance(DilbertDailyStripPlugin.class.getName());

  /**
   * Application-level settings for the plug-in, shared by all open projects.
   */
  private ApplicationSettings settings;

  /**
   * A UI component that permits our <code>ApplicationSettings</code> to be
   * edited.
   *
   * @noinspection InstanceVariableMayNotBeInitialized
   */
  private SettingsPanel settingsPanel;

  private DilbertDailyStrip dilbertDailyStrip;

  private final Timer backgroundTaskExecutor;
  private final PeriodicStripFetcher periodicStripFetcher;
  private final EventListenerList listenerList;

  /**
   * Constructs a plugin implementation.
   */
  public DilbertDailyStripPluginImpl() {
    LOGGER.info(
        "Dilbert Daily Strip Plug-in, version " + VersionInfo.getVersionString() + ", built " +
        VersionInfo.getBuildDate());

    dilbertDailyStrip = DilbertDailyStrip.MISSING_STRIP;
    settings = new ApplicationSettings();
    backgroundTaskExecutor = new Timer();
    periodicStripFetcher = new PeriodicStripFetcher();
    listenerList = new EventListenerList();
  }

  private void configureUnattendedDownloads() {
    final UnattendedDownloadSettings unattendedDownloadSettings;

    if (settings.getUnattendedDownloadSettings().isFetchStripAutomatically()) {
      unattendedDownloadSettings = settings.getUnattendedDownloadSettings();
    } else {
      unattendedDownloadSettings = UnattendedDownloadSettings.NO_DOWNLOAD_SETTINGS;
    }

    periodicStripFetcher.startPeriodicFetching(unattendedDownloadSettings);
  }

  // Implement DilbertDailyStripPlugin

  public void addDailyStripListener(final DailyStripListener listener) {
    if (listener != null) {
      listenerList.add(DailyStripListener.class, listener);
    }
  }

  public void fetchDailyStrip() {
    fetchDailyStrip(DilbertDailyStrip.MISSING_STRIP.getImageChecksum());
  }

  public void fetchDailyStrip(final String md5Hash) {
    if (isDisclaimerAcknowledged()) {
      LOGGER.info("disclaimer ack'd"); // NON-NLS

      backgroundTaskExecutor.schedule(new FetchDailyStripTask(md5Hash), 0);
    }
  }

  public DilbertDailyStrip getCachedDailyStrip() {
    return dilbertDailyStrip.equals(DilbertDailyStrip.MISSING_STRIP) ? null : dilbertDailyStrip;
  }

  public DailyStripProvider[] getDailyStripProviders(final DailyStripPresenter presenter) {
    return new DailyStripProvider[] { new CurrentDailyStripProvider(presenter) };
  }

  public boolean isDisclaimerAcknowledged() {
    return settings.isDisclaimerAcknowledged();
  }

  public void removeDailyStripListener(final DailyStripListener listener) {
    if (listener != null) {
      listenerList.remove(DailyStripListener.class, listener);
    }
  }

  // Implement BaseComponent

  public void disposeComponent() {
    periodicStripFetcher.stopPeriodicFetching();
    backgroundTaskExecutor.cancel();
  }

  @NotNull
  public String getComponentName() {
    return DilbertDailyStripPlugin.class.getName();
  }

  public void initComponent() {
  }

  // Implement Configurable

  public String getDisplayName() {
    final ResourceBundle resourceBundle =
        ResourceBundleManager.getResourceBundle(DilbertDailyStripPlugin.class);

    return resourceBundle.getString("plugin.name.configuration");
  }

  public String getHelpTopic() {
    return null;
  }

  // Implement JDOMExternalizable

  public void readExternal(final Element element) {
    settings.readExternal(element);
    configureUnattendedDownloads();
  }

  public void writeExternal(final Element element) {
    settings.writeExternal(element);
  }

  // Implement NamedJDOMExternalizable

  /**
   * Return the root part of the name of the file to which the plugin will save
   * its configuration data.  The value returned will have the suffix .xml
   * appended to form the full filename, and the file will be created in the
   * ${idea.config.path}/options/ directory.
   *
   * @return the root part of the configuration settings filename for the
   * plugin.
   */
  public String getExternalFileName() {
    return "dilbert.plugin"; // NON-NLS
  }

  // Implement UnnamedConfigurable

  public JComponent createComponent() {
    settingsPanel = new SettingsPanel(settings);

    return settingsPanel;
  }

  public boolean isModified() {
    boolean isModified = false;

    if (settingsPanel != null) {
      isModified = settingsPanel.isModified(settings);
    }

    return isModified;
  }

  public void apply() {
    if (settingsPanel != null) {
      // Save the current settings for future use
      //
      settings = settingsPanel.getDisplayedSettings();

      // Account for any changes made to the unattended download settings
      //
      configureUnattendedDownloads();
    }
  }

  public void reset() {
    if (settingsPanel != null) {
      settingsPanel.setSettings(settings);
    }
  }

  public void disposeUIResources() {
  }

  private class FetchDailyStripTask extends TimerTask {
    private final String md5Hash;

    private FetchDailyStripTask(final String md5Hash) {
      this.md5Hash = md5Hash;
    }

    private void fireDailyStripUpdated(final DilbertDailyStrip dailyStrip) {
      dilbertDailyStrip = dailyStrip;

      DailyStripEvent e = null;

      final Object[] listeners = listenerList.getListenerList();
      for (int i = 0; i < listeners.length; i += 2) {
        if (listeners[i] == DailyStripListener.class) {
          if (e == null) {
            e = new DailyStripEvent(this, dailyStrip);
          }
          ((DailyStripListener) listeners[i + 1]).dailyStripUpdated(e);
        }
      }
    }

    public void run() {
      try {
        final DilbertDailyStrip dailyStrip =
            new DilbertDailyStripFetcher().fetchDailyStrip(md5Hash);

        if (dailyStrip != null) {
          fireDailyStripUpdated(dailyStrip);
        }
      } catch (IOException e) {
        LOGGER.info("Error fetching current daily strip from dilbert.com", e); // NON-NLS
        fireDailyStripUpdated(DilbertDailyStrip.MISSING_STRIP);
      }
    }
  }
}

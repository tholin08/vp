package se.skl.tp.vp.hawtioauth;

import io.hawt.config.ConfigFacade;
import io.hawt.web.auth.AuthenticationConfiguration;
import java.io.File;
import java.net.URL;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import se.skl.tp.vp.constants.PropertyConstants;

@Log4j2
@Configuration
public class HawtioConfiguration {

  private static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

  @Value("${" + PropertyConstants.HAWTIO_AUTHENTICATION_ENABLED + ":#{false}}")
  private Boolean hawtioAuthenticationEnabled;

  @Value("${" + PropertyConstants.HAWTIO_EXTERNAL_LOGINFILE + ":#{null}}")
  private String externalLoginFile;

  /**
   * Configure facade to use/not use authentication.
   *
   * @return config
   * @throws Exception if an error occurs
   */
  @Bean(initMethod = "init")
  public ConfigFacade configFacade() throws Exception {
    //If key is set in custom.properties, but no value: true default value above only applies if the key is missing, not value..
    hawtioAuthenticationEnabled = hawtioAuthenticationEnabled == null ? false : hawtioAuthenticationEnabled;
    log.info("Configuring authentication for Hawtio: hawtioAuthenticationEnabled is " + hawtioAuthenticationEnabled);
    if (!hawtioAuthenticationEnabled) {
      System.setProperty(AuthenticationConfiguration.HAWTIO_AUTHENTICATION_ENABLED, "false");
      log.warn("Authentication set to false for Hawtio. Not recommended.");
    } else {
      final URL loginResource = this.getClass().getClassLoader().getResource("login.conf");
      if (loginResource != null) {
        setSystemPropertyIfNotSet(JAVA_SECURITY_AUTH_LOGIN_CONFIG, loginResource.toExternalForm());
      }
      log.info("Using loginResource " + JAVA_SECURITY_AUTH_LOGIN_CONFIG + " : "
              + System.getProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG));

      URL loginFile = null;
      if (externalLoginFile != null) {
        File f = new File(externalLoginFile);
        if (f.exists() && f.isFile() && f.canRead()) {
          loginFile = f.toURI().toURL();
        } else {
          log.error("The external loginFile for Hawtio was not found or not readable. Path was " + f.getAbsolutePath());
        }
      } else {
        loginFile = this.getClass().getClassLoader().getResource("realm.properties");
      }
      if (loginFile != null) {
        setSystemPropertyIfNotSet("hawtiologin.file", loginFile.toExternalForm());
        setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_ROLES, "user");
        setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_ROLES, "admin");
        setSystemPropertyIfNotSet(AuthenticationConfiguration.HAWTIO_REALM, "hawtio");
        setSystemPropertyIfNotSet(
            AuthenticationConfiguration.HAWTIO_ROLE_PRINCIPAL_CLASSES,
            "org.eclipse.jetty.jaas.JAASRole");
        log.info("Using loginfile for Hawtio:" + loginFile);
      } else {
        log.error("No loginFile found. Cannot set user and pw. Hawtio is NOT accessible.");
      }
      System.setProperty(AuthenticationConfiguration.HAWTIO_AUTHENTICATION_ENABLED, "true");
    }
    return new ConfigFacade();
  }

  private void setSystemPropertyIfNotSet(final String key, final String value) {
    if (System.getProperty(key) == null) {
      System.setProperty(key, value);
    }
  }
}

package seed.utilities

import seed.config.Configuration
import seed.config.IPostMarkEmailConfiguration
import seed.config.PostMarkEmailConfiguration

var testConfig: IPostMarkEmailConfiguration = PostMarkEmailConfiguration(
    baseConfig = Configuration(
        keycloakRealm = "seed",
        keycloakClientId = "seed"
    ),
    postmarkSenderEmail = "unknown-user@noumenadigital.com"
)

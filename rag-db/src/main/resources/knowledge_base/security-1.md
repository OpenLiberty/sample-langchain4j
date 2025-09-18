:problem: Every time a user tries to log in via SAML, it fails and gets an FFDC with an java.lang.InternalError exception. The exception message is "Unexpected CryptoAPI failure generating seed"
:answer: change the secure random source by modifying the `securerandom.source` security property to `file:/dev/urandom` in the `java.security` file in the JRE.
Read https://www.ibm.com/docs/en/sdk-java-technology/8?topic=guide-securerandom-provider for more information about IBM SecureRandom Provider.

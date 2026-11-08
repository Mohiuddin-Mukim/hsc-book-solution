package bd.hscsolution.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "hsc.security")
public record SecurityProperties(String jwtSecret, long accessTokenMinutes, long refreshTokenDays,
                                 String corsOrigins, String bootstrapAdminEmail, String bootstrapAdminPassword,
                                 String bootstrapAdminName, int loginRateLimit, long loginRateWindowSeconds) {
}

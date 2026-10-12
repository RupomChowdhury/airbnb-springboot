package dev.rupom.project.airbnb.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "AirBNB Clone Enterprise REST API",
                version = "1.0.0",
                description = "Production-grade RESTful API simulating Airbnb with dynamic inventory pricing, " +
                        "high-concurrency booking orchestration, 15-minute reservation hold expiration, " +
                        "itemized fee breakdown engine, escrow host payouts, verified-stay reviews with subratings, " +
                        "wishlists, and guest-host threaded messaging.",
                contact = @Contact(name = "Rupom", email = "developer@rupom.dev"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
        ),
        security = {
                @SecurityRequirement(name = "BearerAuthentication")
        }
)
@SecurityScheme(
        name = "BearerAuthentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Enter JWT Bearer token to authorize requests across protected endpoints."
)
public class OpenApiConfig {
}

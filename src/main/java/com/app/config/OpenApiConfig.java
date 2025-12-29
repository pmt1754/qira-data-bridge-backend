package com.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort);
        server.setDescription("Development Server");
        
        Contact contact = new Contact();
        contact.setName("QIRA Data Bridge Support");
        contact.setEmail("support@example.com");
        
        License license = new License();
        license.setName("MIT License");
        license.setUrl("https://opensource.org/licenses/MIT");
        
        Info info = new Info()
            .title("QIRA Data Bridge API")
            .version("1.0.0")
            .description("REST API for QIRA ticket ingestion, reporting, and data export.\n\n" +
                        "## Features\n" +
                        "- **Monthly Scheduled Ingestion**: Automatically fetches and processes QIRA tickets\n" +
                        "- **Manual Triggers**: Admin endpoints to trigger ingestion on-demand\n" +
                        "- **Excel Export**: Generate reports filtered by date range and team\n" +
                        "- **Job Monitoring**: Track ingestion job status and statistics\n\n" +
                        "## Authentication\n" +
                        "Currently no authentication required (configure as needed for production)")
            .contact(contact)
            .license(license);
        
        return new OpenAPI()
            .info(info)
            .servers(List.of(server));
    }
}

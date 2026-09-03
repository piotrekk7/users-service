package app.auditservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoClientConfig extends AbstractMongoClientConfiguration {

    @Value("#{environment.SPRING_DATA_MONGODB_URI ?: 'mongodb://localhost:27017/auditdb'}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        return connectionString.getDatabase() != null ? connectionString.getDatabase() : "auditdb";
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        return MongoClients.create(settings);
    }
}

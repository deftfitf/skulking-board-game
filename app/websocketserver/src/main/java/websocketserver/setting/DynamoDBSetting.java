package websocketserver.setting;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "websocketserver.amazon.dynamodb")
public class DynamoDBSetting {
    private String region;
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;

    private TableSetting gameRoomTable;

    @Data
    public static class TableSetting {
        String tableName;
    }
}

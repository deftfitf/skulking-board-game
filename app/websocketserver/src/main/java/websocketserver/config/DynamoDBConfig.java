package websocketserver.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import dynamodbdao.GameRoomDynamoDBDao;
import lombok.extern.slf4j.Slf4j;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import websocketserver.setting.DynamoDBSetting;

@Slf4j
@Configuration
@EnableDynamoDBRepositories(basePackages = "websocketserver.repository")
@EnableConfigurationProperties(DynamoDBSetting.class)
public class DynamoDBConfig {

    @Bean
    public AmazonDynamoDB amazonDynamoDB(
            DynamoDBSetting dynamoDBSetting
    ) {
        final var endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(
                        dynamoDBSetting.getEndpoint(),
                        dynamoDBSetting.getRegion());

        /*
         * DynamoDBオブジェクトの生成に利用したAWS SDKのAmazonDynamoDBClientBuilderは
         * 1. 環境変数AWS_ACCESS_KEY_IDとAWS_SECRET_ACCESS_KEY
         * 2. システムプロパティaws.accessKeyId, aws.secretKey
         * 3. ユーザーのAWS認証情報ファイル
         * 4. AWSインスタンスプロファイルに認証情報
         *
         * 開発環境では, .aws配下の認証情報を取得し,
         * 本番環境では, インスタンスのプロファイル上から認証情報を取得する実装が推奨.
         */
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .build();
    }

    @Bean
    public GameRoomDynamoDBDao gameRoomDynamoDBDao(
            DynamoDBSetting dynamoDBSetting
    ) {
        return new GameRoomDynamoDBDao(
                dynamoDBSetting.getEndpoint(),
                dynamoDBSetting.getAccessKeyId(),
                dynamoDBSetting.getAccessKeySecret(),
                dynamoDBSetting.getGameRoomTable().getTableName());
    }

}

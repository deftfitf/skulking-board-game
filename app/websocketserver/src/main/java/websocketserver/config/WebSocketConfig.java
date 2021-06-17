package websocketserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import websocketserver.handler.WebSocketEndpointHandler;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class WebSocketConfig {
    
    @Bean
    public HandlerMapping handlerMapping(
            WebSocketEndpointHandler webSocketEndpointHandler
    ) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/gameserver", webSocketEndpointHandler);
        int order = -1; // before annotated controllers

        return new SimpleUrlHandlerMapping(map, order);
    }

}

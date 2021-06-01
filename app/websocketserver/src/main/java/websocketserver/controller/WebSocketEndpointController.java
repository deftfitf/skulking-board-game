package websocketserver.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;
import websocketserver.protocol.Greeting;
import websocketserver.protocol.HelloMessage;

@Controller
public class WebSocketEndpointController {

    @MessageMapping("/hello")
    @SendTo("/topic/greetings")
    public Greeting greeting(HelloMessage message) throws Exception {
        Thread.sleep(1000); // simulated delay
        return new Greeting("Hello, " +
                HtmlUtils.htmlEscape(message.getName()) + "!");
    }
    
}

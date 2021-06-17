package websocketserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSession;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableSpringWebSession
public class WebFluxSessionConfig {

    @Bean
    public ReactiveSessionRepository<MapSession> reactiveSessionRepository() {
        final var repository = new ReactiveMapSessionRepository(new ConcurrentHashMap<>());
        repository.setDefaultMaxInactiveInterval(60 * 30);
        return repository;
    }

    @Bean
    public WebSessionIdResolver webSessionIdResolver() {
        final var resolver = new CookieWebSessionIdResolver();
        resolver.setCookieName("SESSION");
        resolver.addCookieInitializer(builder -> builder
                        .path("/")
                        .sameSite("Strict")
//                .secure(true)
        );
        return resolver;
    }

}

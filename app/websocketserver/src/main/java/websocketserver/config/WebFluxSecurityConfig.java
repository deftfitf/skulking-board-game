package websocketserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerFormLoginAuthenticationConverter;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import websocketserver.auth.Role;
import websocketserver.repository.GamePlayerRepository;
import websocketserver.service.GamePlayerReactiveUserDetailsService;

import java.net.URI;

@Configuration
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public ServerCsrfTokenRepository serverCsrfTokenRepository() {
        return new CookieServerCsrfTokenRepository();
    }

    @Bean
    public ServerSecurityContextRepository serverSecurityContextRepository() {
        return new WebSessionServerSecurityContextRepository();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            GamePlayerRepository gamePlayerRepository,
            ServerCsrfTokenRepository serverCsrfTokenRepository,
            ServerSecurityContextRepository serverSecurityContextRepository,
            ServerHttpSecurity http
    ) {
        final var reactiveUserDetailsService = new GamePlayerReactiveUserDetailsService(gamePlayerRepository);
        final var logoutSuccessHandler = new RedirectServerLogoutSuccessHandler();
        logoutSuccessHandler.setLogoutSuccessUrl(URI.create("/"));
        final var securityContextServerLogoutHandler = new SecurityContextServerLogoutHandler();
        securityContextServerLogoutHandler.setSecurityContextRepository(serverSecurityContextRepository);

        http.authorizeExchange(exchanges -> exchanges
                .pathMatchers("/", "/static/**", "/error", "/players/register", "/login", "/logout").permitAll()
                .pathMatchers("/checkLogin").hasRole(Role.PLAYER.name())
                .pathMatchers("/players/mypage").hasRole(Role.PLAYER.name())
                .pathMatchers("/gamerooms").hasRole(Role.PLAYER.name())
                .pathMatchers("/gameserver").hasRole(Role.PLAYER.name())
                .anyExchange().authenticated())

                .httpBasic().disable()
                .formLogin().disable()
                .logout(logoutSpec -> logoutSpec
                        .logoutUrl("/logout")
                        .logoutHandler(securityContextServerLogoutHandler)
                        .logoutSuccessHandler(logoutSuccessHandler))

                .csrf(csrfSpec -> csrfSpec
                        .csrfTokenRepository(serverCsrfTokenRepository)
                        .requireCsrfProtectionMatcher(ServerWebExchangeMatchers
                                .matchers(
                                        ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, "/players/mypage", "/gameserver"),
                                        ServerWebExchangeMatchers.pathMatchers(HttpMethod.PUT, "/players/mypage", "/gameserver"),
                                        ServerWebExchangeMatchers.pathMatchers(HttpMethod.DELETE, "/players/mypage", "/gameserver"))))

                .addFilterAt(
                        authenticationWebFilter(reactiveUserDetailsService, serverSecurityContextRepository),
                        SecurityWebFiltersOrder.AUTHENTICATION)
                .addFilterAfter(csrfFilter(), SecurityWebFiltersOrder.CSRF);
        return http.build();
    }

    private AuthenticationWebFilter authenticationWebFilter(
            ReactiveUserDetailsService reactiveUserDetailsService,
            ServerSecurityContextRepository serverSecurityContextRepository
    ) {
        final var authenticationWebFilter = new AuthenticationWebFilter(
                new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService));

        authenticationWebFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers("/login"));
        authenticationWebFilter.setAuthenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/login"));
        authenticationWebFilter.setAuthenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"));
        authenticationWebFilter.setServerAuthenticationConverter(new ServerFormLoginAuthenticationConverter());
        authenticationWebFilter.setSecurityContextRepository(serverSecurityContextRepository);

        return authenticationWebFilter;
    }

    private WebFilter csrfFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            final Mono<CsrfToken> csrfTokenMono = exchange.getAttribute(CsrfToken.class.getName());
            if (csrfTokenMono != null) {
                return csrfTokenMono.then(chain.filter(exchange));
            }
            return chain.filter(exchange);
        };
    }

}

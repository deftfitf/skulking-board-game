package websocketserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.security.web.server.csrf.CookieServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import websocketserver.auth.Role;
import websocketserver.repository.GamePlayerRepository;
import websocketserver.service.GamePlayerReactiveUserDetailsService;

@Configuration
@EnableWebFluxSecurity
public class WebFluxSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public ServerCsrfTokenRepository serverCsrfTokenRepository() {
        return CookieServerCsrfTokenRepository.withHttpOnlyFalse();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(
            GamePlayerRepository gamePlayerRepository,
            ServerCsrfTokenRepository serverCsrfTokenRepository,
            ServerHttpSecurity http
    ) {
        final var reactiveUserDetailsService = new GamePlayerReactiveUserDetailsService(gamePlayerRepository);
        http.authorizeExchange(exchanges -> exchanges
                .pathMatchers("/", "/static/**", "/players/register", "/players/login", "/players/logout").permitAll()
                .pathMatchers("/players/mypage").hasRole(Role.PLAYER.name())
                .pathMatchers("/gameserver").hasRole(Role.PLAYER.name())
                .anyExchange().authenticated())

                .httpBasic().disable()
                .formLogin().disable()

                .csrf().csrfTokenRepository(serverCsrfTokenRepository)
                .and()
                .addFilterAt(authenticationWebFilter(reactiveUserDetailsService), SecurityWebFiltersOrder.AUTHENTICATION);
        return http.build();
    }

    private AuthenticationWebFilter authenticationWebFilter(
            ReactiveUserDetailsService reactiveUserDetailsService
    ) {
        final var authenticationWebFilter = new AuthenticationWebFilter(
                new UserDetailsRepositoryReactiveAuthenticationManager(reactiveUserDetailsService));

        authenticationWebFilter.setRequiresAuthenticationMatcher(ServerWebExchangeMatchers.pathMatchers("/players/login"));
        authenticationWebFilter.setAuthenticationFailureHandler(new RedirectServerAuthenticationFailureHandler("/players/login"));
        authenticationWebFilter.setAuthenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("/"));
        authenticationWebFilter.setServerAuthenticationConverter(new ServerFormLoginAuthenticationConverter());
        authenticationWebFilter.setSecurityContextRepository(new WebSessionServerSecurityContextRepository());

        return authenticationWebFilter;
    }

}

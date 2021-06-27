package websocketserver.auth;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import websocketserver.model.GamePlayer;

import java.security.Principal;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class ReactorNettyRequestUpgradeStrategyWithPrincipal extends ReactorNettyRequestUpgradeStrategy {

    @NonNull
    private final ServerSecurityContextRepository serverSecurityContextRepository;

    @Override
    public Mono<Void> upgrade(ServerWebExchange exchange, WebSocketHandler handler,
                              @Nullable String subProtocol, Supplier<HandshakeInfo> handshakeInfoFactory) {
        return super.upgrade(exchange, handler, subProtocol, () -> {
            final var handShakeInfo = handshakeInfoFactory.get();
            final var gamePlayerPrincipal = serverSecurityContextRepository
                    .load(exchange)
                    .map(SecurityContext::getAuthentication)
                    .map(Authentication::getPrincipal)
                    .cast(GamePlayer.class)
                    .map(player -> (Principal) player::getPlayerId);

            return new HandshakeInfo(
                    handShakeInfo.getUri(),
                    handShakeInfo.getHeaders(),
                    gamePlayerPrincipal,
                    handShakeInfo.getSubProtocol(),
                    handShakeInfo.getRemoteAddress(),
                    handShakeInfo.getAttributes(),
                    handShakeInfo.getLogPrefix()
            );
        });
    }

}

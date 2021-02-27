package z.zaifapiclient.cashtxapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetBalanceRequest {
    String method = "get_info";
}

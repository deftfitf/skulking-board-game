package z.zaifapiclient.cashtxapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetBalanceRequestLight {
    String method = "get_info2";
}

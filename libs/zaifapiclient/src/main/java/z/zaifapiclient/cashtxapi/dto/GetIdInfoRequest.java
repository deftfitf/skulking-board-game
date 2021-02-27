package z.zaifapiclient.cashtxapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetIdInfoRequest {
    String method = "get_id_info";
}

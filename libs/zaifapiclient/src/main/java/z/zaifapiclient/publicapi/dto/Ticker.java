package z.zaifapiclient.publicapi.dto;

import lombok.Data;

@Data
public class Ticker {
    float last;
    float high;
    float low;
    float vwap;
    float volume;
    float bid;
    float ask;
}

package z.zaifapiclient.publicapi.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderBook {

    List<List<Float>> asks;
    List<List<Float>> bids;

}

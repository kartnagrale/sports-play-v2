package com.neml.badminton.service;

import com.neml.badminton.entity.Gender;
import java.math.BigDecimal;

/** Pure rule evaluation used for bid, sale and authoritative UI eligibility. All money is rupees. */
public final class AuctionBidRules {
    private AuctionBidRules() {}
    public record Rules(int requiredPlayers,int maximumPlayers,int minimumMale,int minimumFemale,
                        BigDecimal minimumBid,BigDecimal maximumBid,Integer maximumPremiumPurchases) {}
    public static String unavailable(Rules r,int male,int female,int premium,Gender gender,BigDecimal amount,BigDecimal purse) {
        if(amount==null||amount.signum()<=0||amount.stripTrailingZeros().scale()>2) return "Bid must be a positive rupee amount with at most two decimal places";
        if(amount.compareTo(r.minimumBid())<0)return "Bid is below the configured minimum player bid";
        if(r.maximumBid()!=null&&amount.compareTo(r.maximumBid())>0)return "Bid exceeds the configured maximum of ₹"+r.maximumBid().toPlainString();
        if(r.maximumBid()!=null&&r.maximumPremiumPurchases()!=null&&amount.compareTo(r.maximumBid())==0&&premium>=r.maximumPremiumPurchases())return "Premium purchase limit of "+r.maximumPremiumPurchases()+" reached";
        int filled=male+female;
        if(filled>=r.maximumPlayers())return "Team squad is full";
        int nextMale=male+(gender==Gender.MALE?1:0), nextFemale=female+(gender==Gender.FEMALE?1:0);
        int slots=r.maximumPlayers()-filled-1;
        if(Math.max(0,r.minimumMale()-nextMale)+Math.max(0,r.minimumFemale()-nextFemale)>slots)
            return "Purchase would make the gender minimums impossible";
        int purchasesStillRequired=Math.max(0,r.requiredPlayers()-filled-1);
        BigDecimal reserve=r.minimumBid().multiply(BigDecimal.valueOf(purchasesStillRequired));
        if(purse.subtract(amount).compareTo(reserve)<0)return "Insufficient purse: reserve ₹"+reserve.toPlainString()+" for "+purchasesStillRequired+" remaining required purchases";
        return null;
    }
}

package com.agonyforge.arbitrader.service.model;

import com.agonyforge.arbitrader.DecimalConstants;
import com.agonyforge.arbitrader.config.FeeComputation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class EntryTradeVolume extends TradeVolume {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeVolume.class);

    private final int intermediateScale;

    //The exit target spread
    BigDecimal exitSpread;

     EntryTradeVolume(FeeComputation longFeeComputation, FeeComputation shortFeeComputation, BigDecimal longMaxExposure,
                      BigDecimal shortMaxExposure, BigDecimal longPrice, BigDecimal shortPrice, ExchangeFee longFee,
                      ExchangeFee shortFee, BigDecimal exitSpread, int longScale, int shortScale) {

        this.longFeeComputation=longFeeComputation;
        this.shortFeeComputation=shortFeeComputation;
        if(longFeeComputation == FeeComputation.SERVER) {
            this.longFee=longFee.getTotalFee();
        } else {
            this.longFee= getFeeAdjustedForBuy(FeeComputation.CLIENT, longFee, longScale);
            this.longBaseFee = longFee.getTotalFee();
        }
        if(shortFeeComputation == FeeComputation.SERVER) {
         this.shortFee=shortFee.getTotalFee();
        } else {
            this.shortFee = getFeeAdjustedForSell(FeeComputation.CLIENT, shortFee, shortScale);
            this.shortBaseFee = shortFee.getTotalFee();
        }
        this.longScale=longScale;
        this.shortScale=shortScale;
        this.intermediateScale = getIntermediateScale(Math.max(longScale,shortScale));
        this.exitSpread = exitSpread;
        if(getShortToLongVolumeTargetRatio(longFee.getTotalFee(),shortFee.getTotalFee(),exitSpread,intermediateScale).compareTo(BigDecimal.ONE)>0) {
            this.longVolume = getLongVolumeFromExposures(longMaxExposure, shortMaxExposure, longPrice, shortPrice, this.longFee, this.shortFee, this.exitSpread, this.intermediateScale);
            this.shortVolume = getShortVolumeFromLong(longVolume, this.longFee, this.shortFee, this.exitSpread, this.intermediateScale);
        } else {
            this.shortVolume = getShortVolumeFromExposures(longMaxExposure, shortMaxExposure, longPrice, shortPrice, this.longFee, this.shortFee, this.exitSpread, this.intermediateScale);
            this.longVolume = getLongVolumeFromShort(shortVolume, this.longFee, this.shortFee, this.exitSpread, this.intermediateScale);
        }
        LOGGER.debug("Instantiate EntryTradeVolume with longVolume {} and shortVolume {}, for parameters: \n" +
            "longFeeComputation: {}|shortFeeComputation: {}|longMaxExposure: {}|shortMaxExposure: {}|longPrice: {}|shortPrice: {}|longFee: {}|shortFee: {}|exitSpread: {}|longScale: {}|shortScale: {}",
            this.longVolume.toPlainString(),
            this.shortVolume.toPlainString(),
            longFeeComputation,
            shortFeeComputation,
            longMaxExposure,
            shortMaxExposure,
            longPrice,
            shortPrice,
            longFee,
            shortFee,
            exitSpread,
            longScale,
            shortScale);
        this.longOrderVolume=longVolume;
        this.shortOrderVolume=shortVolume;
    }

    /**
     * Calculates and assign the volume to trade on the long exchange such as:
     * - the total price does not exceed the long exchange maximum exposure
     * - the total price to trade on the short exchange does not exceed the short exchange maximum exposure
     * @see #getShortVolumeFromLong and #getFeeFactor
     * Detailed maths: https://github.com/scionaltera/arbitrader/issues/325
     */
    static BigDecimal getLongVolumeFromExposures(BigDecimal longMaxExposure, BigDecimal shortMaxExposure, BigDecimal longPrice, BigDecimal shortPrice, BigDecimal longFee, BigDecimal shortFee, BigDecimal exitSpread, int intermediateScale) {
        //volume limit induced by the maximum exposure on the long exchange
        BigDecimal longVolume1 = longMaxExposure.divide(longPrice,intermediateScale,RoundingMode.HALF_EVEN);

        //volume limit induced by the maximum exposure on the short exchange: shortVolume * shortPrice == shortMaxExposure
        //to respect market neutrality: shortVolume = longVolume2 / shortToLongVolumeTargetRatio
        BigDecimal longVolume2 = getShortToLongVolumeTargetRatio(longFee, shortFee, exitSpread, intermediateScale).multiply(shortMaxExposure).divide(shortPrice,intermediateScale,RoundingMode.HALF_EVEN);
        return longVolume1.min(longVolume2);
    }

    /**
     * Calculates and assign the volume to trade on the short exchange such as:
     * - the total price does not exceed the short exchange maximum exposure
     * - the total price to trade on the long exchange does not exceed the long exchange maximum exposure
     * @see #getShortVolumeFromLong and #getShortToLongVolumeTargetRatio
     * Detailed maths: https://github.com/scionaltera/arbitrader/issues/325
     */
    static BigDecimal getShortVolumeFromExposures(BigDecimal longMaxExposure, BigDecimal shortMaxExposure, BigDecimal longPrice, BigDecimal shortPrice, BigDecimal longFee, BigDecimal shortFee, BigDecimal exitSpread, int intermediateScale) {
        //volume limit induced by the maximum exposure on the long exchange
        BigDecimal shortVolume1 = shortMaxExposure.divide(shortPrice,intermediateScale,RoundingMode.HALF_EVEN);

        //volume limit induced by the maximum exposure on the long exchange: longVolume * longPrice == longMaxExposure
        //to respect market neutrality: longVolume = shortVolume2 * shortToLongVolumeTargetRatio
        BigDecimal shortVolume2 = shortMaxExposure.divide(getShortToLongVolumeTargetRatio(longFee, shortFee, exitSpread, intermediateScale).multiply(longPrice),intermediateScale,RoundingMode.HALF_EVEN);
        return shortVolume1.min(shortVolume2);
    }

    /**
     * Calculates a market neutral volume to trade on the short exchange from the volume to trade on the long exchange
     * @see #getLongVolumeFromShort and #getFeeFactor
     */
    static BigDecimal getShortVolumeFromLong(BigDecimal longVolume, BigDecimal longFee, BigDecimal shortFee, BigDecimal exitSpread, int intermediateScale) {
        BigDecimal targetRatio =getShortToLongVolumeTargetRatio(longFee, shortFee, exitSpread, intermediateScale);
        return longVolume.divide(targetRatio, intermediateScale, RoundingMode.HALF_EVEN);
    }

    /**
     * Calculates a market neutral volume to trade on the long exchange from the volume to trade on the short exchange
     */
    static BigDecimal getLongVolumeFromShort(BigDecimal shortVolume, BigDecimal longFee, BigDecimal shortFee, BigDecimal exitSpread, int intermediateScale) {
        return shortVolume.multiply(getShortToLongVolumeTargetRatio(longFee, shortFee, exitSpread, intermediateScale));
    }

    /**
     * Set the target ratio between the short and long exchange volume to compensate for the exit fees
     * Trading the same amount on both exchanges is not market neutral. A higher volume traded on the long exchange
     * is required to compensate for the fees that could increase if the price increases.
     * Detailed maths: https://github.com/scionaltera/arbitrader/issues/325
     */
    static BigDecimal getShortToLongVolumeTargetRatio(BigDecimal longFee, BigDecimal shortFee, BigDecimal exitSpread, int intermediateScale) {
        return (BigDecimal.ONE.add(shortFee)).multiply(BigDecimal.ONE.add(exitSpread)).divide(BigDecimal.ONE.subtract(longFee),intermediateScale, RoundingMode.HALF_EVEN);
    }

    /**
     * Check if the trade is still market neutral enough
     * @return true if the market neutrality rating is between 0 and 2.
     */
    public boolean isMarketNeutral() {
        BigDecimal threshold = BigDecimal.ONE;
        return getMarketNeutralityRating().subtract(BigDecimal.ONE).abs().compareTo(threshold)<=0;
    }

    /**
     * Rate the market neutrality in percentage:
     * 1 means perfect market neutrality
     * 0 means the fees are not compensated
     * 2 means the fees are compensated twice
     * @return the market neutrality rating
     */
    public BigDecimal getMarketNeutralityRating() {
        BigDecimal shortToLongVolumeActualRatio = longVolume.divide(shortVolume, intermediateScale, RoundingMode.HALF_EVEN);
        return (shortToLongVolumeActualRatio.subtract(BigDecimal.ONE)).divide(getShortToLongVolumeTargetRatio(longFee, shortFee, exitSpread, intermediateScale).subtract(BigDecimal.ONE), intermediateScale, RoundingMode.HALF_EVEN);
    }

    /**
     * Retrieve the minimum profit estimation for this trade volume.
     * the estimation is only correct if:
     *  - getMarketNeutralityRating is 1 (or very close to 1)
     *  - the exit prices match exactly the exitTarget spread
     * @param longPrice the long entry price
     * @param shortPrice the short entry price
     * @return the estimated minimum profit
     */
    public BigDecimal getMinimumProfit(BigDecimal longPrice, BigDecimal shortPrice) {
        BigDecimal longEntry = longVolume.multiply(longPrice).multiply(BigDecimal.ONE.add(longFee));
        BigDecimal shortEntry = shortVolume.multiply(shortPrice).multiply(BigDecimal.ONE.subtract(shortFee));
        return shortEntry.subtract(longEntry).setScale(DecimalConstants.USD_SCALE, RoundingMode.FLOOR);
    }

    @Override
    public void adjustOrderVolume(String longExchangeName, String shortExchangeName, BigDecimal longAmountStepSize, BigDecimal shortAmountStepSize) {
        BigDecimal tempLongVolume = this.longVolume;
        BigDecimal tempShortVolume = this.shortVolume;

        //First adjust make sure the base volume is market neutral
        this.longVolume= this.longVolume.setScale(longScale, RoundingMode.HALF_EVEN);
        this.shortVolume = getShortVolumeFromLong(longVolume, this.longFee, this.shortFee, this.exitSpread, this.intermediateScale);
        this.shortVolume= this.shortVolume.setScale(shortScale, RoundingMode.HALF_EVEN);
        //For exchanges where feeComputation is set to CLIENT:
        //We need to increase the volume of BUY orders and decrease the volume of SELL orders
        //Because the exchange will buy slightly less volume and sell slightly more as a way to pay the fees
        BigDecimal longBaseFees = getBuyBaseFees(longFeeComputation, longVolume, longBaseFee, false);
        this.longOrderVolume = longVolume.add(longBaseFees);
        BigDecimal shortBaseFees = getSellBaseFees(shortFeeComputation, shortVolume, shortBaseFee,false);
        this.shortOrderVolume = shortVolume.subtract(shortBaseFees);

        if(longFeeComputation == FeeComputation.CLIENT) {
            if(longAmountStepSize != null) {
                throw new IllegalArgumentException("Long exchange FeeComputation.CLIENT and amountStepSize are not compatible.");
            }
        }
        if(shortFeeComputation == FeeComputation.CLIENT && shortAmountStepSize != null) {
            throw new IllegalArgumentException("Short exchange FeeComputation.CLIENT and amountStepSize are not compatible.");
        }

        if(longFeeComputation == FeeComputation.CLIENT) {
            LOGGER.info("{} fees are computed in the client: {} + {} = {}",
                longExchangeName,
                longVolume,
                longBaseFees,
                longOrderVolume);
        }

        if(shortFeeComputation == FeeComputation.CLIENT) {
            LOGGER.info("{} fees are computed in the client: {} + {} = {}",
                shortExchangeName,
                shortVolume,
                shortBaseFees,
                shortOrderVolume);
        }

        // Before executing the order we adjust the step size for each side of the trade (long and short).
        // When adjusting the order volumes, the underlying longVolume and shortVolume need to be adjusted as well as
        // they are used to ensure market neutrality and estimate profits.
        if(longAmountStepSize != null && shortAmountStepSize != null) {
            //Unhappy scenario
            //It will be hard to find a volume that match the step sizes on both exchanges and the market neutrality
            longOrderVolume = roundByStep(longOrderVolume, longAmountStepSize).setScale(longScale, RoundingMode.HALF_EVEN);
            shortOrderVolume = roundByStep(shortOrderVolume, shortAmountStepSize).setScale(shortScale, RoundingMode.HALF_EVEN);
            LOGGER.info("Both exchanges have an amount step size requirements. Market neutrality rating is {}.",
                getMarketNeutralityRating().setScale(3,RoundingMode.HALF_EVEN));
        } else if (longAmountStepSize != null) {
            adjustShortFromLong(longAmountStepSize, longScale, shortScale);
        } else if (shortAmountStepSize != null) {
            adjustLongFromShort(shortAmountStepSize, longScale, shortScale);
        } else if (longScale <= shortScale) {
            adjustShortFromLong(null, longScale, shortScale);
        } else {
            adjustLongFromShort(null, longScale, shortScale);
        }

        if(!tempLongVolume.equals(longOrderVolume)) {
            LOGGER.info("{} long entry trade volumes adjusted: {} -> {} (order volume: {}) ",
                longExchangeName,
                tempLongVolume,
                longVolume,
                longOrderVolume
            );
        }
        if(!tempLongVolume.equals(longOrderVolume)) {
            LOGGER.info("{} short entry trade volumes adjusted: {} -> {} (order volume: {}) ",
                shortExchangeName,
                tempShortVolume,
                shortVolume,
                shortOrderVolume
            );
        }
    }

    /**
     * Adjust the long order volume to the scale, and find the closest short order volume to market neutrality
     *  respecting the short scale
     * @param longAmountStepSize the long exchange's amount step size
     * @param longScale the scale of the volume on the long exchange
     * @param shortScale the scale of the volume on the short exchange
     */
    private void adjustShortFromLong(BigDecimal longAmountStepSize, int longScale, int shortScale) {
        if(longAmountStepSize != null) {
            BigDecimal roundedLongOrderVolume = roundByStep(longOrderVolume, longAmountStepSize);
            LOGGER.debug("Round longOrderVolume by step {}/{} = {}",
                longOrderVolume,
                longAmountStepSize,
                roundedLongOrderVolume);
            this.longOrderVolume = roundedLongOrderVolume;
        }
        this.longOrderVolume = longOrderVolume.setScale(longScale, RoundingMode.HALF_EVEN);
        LOGGER.debug("Scale longOrderVolume with scale {} to {}",
            longScale,
            shortOrderVolume);

        //Adjust other volumes to respect market neutrality
        BigDecimal longBaseFees = getBuyBaseFees(longFeeComputation, longOrderVolume, longBaseFee, true);
        this.longVolume = longOrderVolume.subtract(longBaseFees).setScale(longScale, RoundingMode.HALF_EVEN);
        if(longFeeComputation == FeeComputation.CLIENT) {
            LOGGER.debug("Calculate underlying longVolume = longOrderVolume - longBaseFees: {} - {} = {}",
                longOrderVolume,
                longBaseFees,
                longVolume);
        } else {
            LOGGER.debug("Calculate underlying long volume {}",
                longVolume);
        }

        //Recalculate short volume from long volume
        this.shortVolume = getShortVolumeFromLong(longVolume, longFee, shortFee, exitSpread, intermediateScale).setScale(shortScale, RoundingMode.HALF_EVEN);

        BigDecimal shortBaseFees = getSellBaseFees(shortFeeComputation, shortVolume, shortBaseFee, false);
        this.shortOrderVolume = shortVolume.subtract(shortBaseFees);
        if(shortFeeComputation == FeeComputation.CLIENT) {
            LOGGER.debug("Calculate shortOrderVolume = shortVolume + shortBaseFees: {} + {} = {}",
                shortVolume,
                shortBaseFees,
                shortOrderVolume);
        } else {
            LOGGER.debug("Calculate short order volume {}",
                shortVolume);
        }
    }

    /**
     * Adjust the short order volume to the scale, and find the closest long order volume to market neutrality
     *  respecting the long scale
     * @param shortAmountStepSize the short exchange's amount step size
     * @param longScale the scale of the volume on the long exchange
     * @param shortScale the scale of the volume on the short exchange
     */
    private void adjustLongFromShort(BigDecimal shortAmountStepSize, int longScale, int shortScale) {
        if(shortAmountStepSize != null) {
            //Short exchange has a step size, round the short volume
            BigDecimal roundedShortOrderVolume = roundByStep(shortOrderVolume, shortAmountStepSize);
            LOGGER.debug("Round longOrderVolume by step {}/{} = {}",
                shortOrderVolume,
                shortAmountStepSize,
                roundedShortOrderVolume);
            this.shortOrderVolume=roundedShortOrderVolume;
        }
        this.shortOrderVolume = shortOrderVolume.setScale(shortScale, RoundingMode.HALF_EVEN);
        LOGGER.debug("Scale shortOrderVolume with scale {} to {}",
            shortScale,
            shortOrderVolume);

        //Adjust other volumes to respect market neutrality
        BigDecimal shortBaseFees =getSellBaseFees(shortFeeComputation, shortOrderVolume, shortBaseFee, true);
        this.shortVolume = shortOrderVolume.subtract(shortBaseFees).setScale(shortScale, RoundingMode.HALF_EVEN);
        if(shortFeeComputation == FeeComputation.CLIENT) {
            LOGGER.debug("Calculate underlying shortVolume = shortOrderVolume - shortVolume: {} - {} = {}",
                shortOrderVolume,
                shortBaseFees,
                shortVolume);
        } else {
            LOGGER.debug("Calculate short order volume {}",
                shortVolume);
        }
        //Recalculate long volume from short volume
        this.longVolume = getLongVolumeFromShort(shortVolume, longFee, shortFee, exitSpread, intermediateScale).setScale(longScale, RoundingMode.HALF_EVEN);

        BigDecimal longBaseFees = getBuyBaseFees(longFeeComputation, longVolume, longBaseFee, false);
        this.longOrderVolume = longVolume.add(longBaseFees).setScale(longScale, RoundingMode.HALF_EVEN);
        if(longFeeComputation ==FeeComputation.SERVER) {
            LOGGER.debug("Calculate longOrderVolume = longVolume + longBaseFees: {} + {} = {}",
                longVolume,
                longBaseFees,
                longOrderVolume);
        } else {
            LOGGER.debug("Calculate underlying long volume {}",
                longVolume);
        }
    }

}

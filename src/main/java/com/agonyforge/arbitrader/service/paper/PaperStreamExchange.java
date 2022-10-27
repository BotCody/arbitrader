package com.agonyforge.arbitrader.service.paper;

import com.agonyforge.arbitrader.config.PaperConfiguration;
import com.agonyforge.arbitrader.service.ExchangeService;
import com.agonyforge.arbitrader.service.TickerService;
import info.bitrich.xchangestream.core.ProductSubscription;
import info.bitrich.xchangestream.core.StreamingExchange;
import info.bitrich.xchangestream.core.StreamingMarketDataService;
import io.reactivex.Completable;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.exceptions.ExchangeException;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;
import java.util.List;

public class PaperStreamExchange extends PaperExchange implements StreamingExchange {
    private final StreamingExchange realExchange;

    public PaperStreamExchange(StreamingExchange realExchange, Currency homeCurrency, TickerService tickerService, ExchangeService exchangeService, PaperConfiguration paperConfiguration) {
        super(realExchange, homeCurrency, tickerService, exchangeService, paperConfiguration);
        this.realExchange = realExchange;
    }

    @Override
    public Completable connect(ProductSubscription... args) {
        return realExchange.connect(args);
    }

    @Override
    public Completable disconnect() {
        return realExchange.disconnect();
    }

    @Override
    public boolean isAlive() {
        return realExchange.isAlive();
    }

    @Override
    public StreamingMarketDataService getStreamingMarketDataService() {
        return realExchange.getStreamingMarketDataService();
    }

    @Override
    public void useCompressedMessages(boolean compressedMessages) {
        realExchange.useCompressedMessages(compressedMessages);
    }

    @Override
    public ExchangeSpecification getExchangeSpecification() {
        return realExchange.getExchangeSpecification();
    }

    @Override
    public ExchangeMetaData getExchangeMetaData() {
        return realExchange.getExchangeMetaData();
    }

    @Override
    public List<CurrencyPair> getExchangeSymbols() {
        return realExchange.getExchangeSymbols();
    }

    @Override
    public SynchronizedValueFactory<Long> getNonceFactory() {
        return realExchange.getNonceFactory();
    }

    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {
        return realExchange.getDefaultExchangeSpecification();
    }

    @Override
    public void applySpecification(ExchangeSpecification exchangeSpecification) {
        realExchange.applySpecification(exchangeSpecification);
    }

    @Override
    public void remoteInit() throws IOException, ExchangeException {
        realExchange.remoteInit();
    }
}

package com.agonyforge.arbitrader.service.paper;

import com.agonyforge.arbitrader.config.PaperConfiguration;
import com.agonyforge.arbitrader.service.ExchangeService;
import com.agonyforge.arbitrader.service.TickerService;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.ExchangeMetaData;
import org.knowm.xchange.exceptions.ExchangeException;
import org.knowm.xchange.service.account.AccountService;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.knowm.xchange.service.trade.TradeService;
import si.mazi.rescu.SynchronizedValueFactory;

import java.io.IOException;
import java.util.List;

public class PaperExchange implements Exchange {

    private final Exchange realExchange;
    private final PaperTradeService tradeService;
    private final PaperAccountService accountService;

    public PaperExchange(Exchange exchange, Currency homeCurrency, TickerService tickerService, ExchangeService exchangeService, PaperConfiguration paper) {
        this.realExchange =exchange;
        this.tradeService=new PaperTradeService(this, exchange.getTradeService(), tickerService, exchangeService, paper);
        this.accountService=new PaperAccountService(this, exchange.getAccountService(),homeCurrency, exchangeService, paper);
    }

    public PaperTradeService getPaperTradeService() {
        return tradeService;
    }

    public PaperAccountService getPaperAccountService() {
        return accountService;
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
    public MarketDataService getMarketDataService() {
        return realExchange.getMarketDataService();
    }

    @Override
    public TradeService getTradeService() {
        return tradeService;
    }

    @Override
    public AccountService getAccountService() {
        return accountService;
    }

    @Override
    public void remoteInit() throws IOException, ExchangeException {
        realExchange.remoteInit();
    }
}

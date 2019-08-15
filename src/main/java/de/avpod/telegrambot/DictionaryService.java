package de.avpod.telegrambot;

import com.amazonaws.util.StringUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.vdurmont.emoji.EmojiParser;
import de.avpod.telegrambot.thesarius.Term;
import de.avpod.telegrambot.thesarius.ThesariusResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.*;
import org.asynchttpclient.uri.Uri;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Created by apodznoev
 * date 15.08.2019.
 */
class DictionaryService {
    private static final Logger log = LogManager.getLogger(DictionaryService.class);
    private static final String LIST_SYMBOL = EmojiParser.parseToUnicode(":heavy_minus_sign:");
    private static final String HEADER_SYMBOL = EmojiParser.parseToUnicode(":book: :book: :book:");
    private static final String SUBHEADER_SYMBOL = EmojiParser.parseToUnicode(":memo:");;

    private final DefaultAbsSender responseBot;
    private final AsyncHttpClient httpClient;
    private final String dictionariBaseUri;
    private final ObjectMapper objectMapper;

    DictionaryService(DefaultAbsSender absSender) {
        this.responseBot = absSender;
        this.httpClient = Dsl.asyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .build()
        );
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //dictionariBaseUri = "https://glosbe.com/gapi/translate?from=de&dest=de&format=json&tm=true&phrase=";
        dictionariBaseUri = "https://www.openthesaurus.de/synonyme/search?q=%s&supersynsets=true&subsynsets=true&format=application/json";
    }

    ListenableFuture<Void> requestWordInfo(String word, long chatId) {
        SettableFuture<Void> settableFuture = SettableFuture.create();
        Request request = new RequestBuilder()
                .setUri(Uri.create(dictionariBaseUri.replace("%s", word)))
                .addHeader("accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .setMethod("GET")
                .build();

        httpClient.executeRequest(request, new AsyncCompletionHandler<Void>() {

            @Override
            public Void onCompleted(Response result) {
                try {
                    log.info("Got dictionary response {}", result);
                    ThesariusResponse response = objectMapper.readValue(result.getResponseBody(), ThesariusResponse.class);

                    String formattedResponseText = formatResponse(response);
                    log.info("Formatted response {}", formattedResponseText);
                    SendMessage message = new SendMessage()
                            .setChatId(chatId)
                            .enableMarkdown(true)
                            .setText(formattedResponseText);
                    responseBot.execute(message);
                    settableFuture.set(null);
                } catch (Exception e) {
                    log.error("Cannot send response due to", e);
                    settableFuture.setException(e);
                }
                return null;
            }

        });
        return settableFuture;
    }

    private String formatResponse(ThesariusResponse response) {
        if (response.getSynsets().isEmpty()) {
            return "No data found in dictionary";
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(HEADER_SYMBOL);
        stringBuilder.append(" Gruppe ähnlichen Bedeutungen");
        stringBuilder.append("\n");
        stringBuilder.append("\n");
        AtomicInteger counter = new AtomicInteger(0);

        response.getSynsets().forEach(synset -> {
            stringBuilder.append(SUBHEADER_SYMBOL).append(" ").append(counter.incrementAndGet()).append(".");

            if (!synset.getCategories().isEmpty()) {
                stringBuilder.append(" (");
                Joiner.on(",").appendTo(stringBuilder, synset.getCategories());
                stringBuilder.append(" )");
            }
            stringBuilder.append("\n");
            stringBuilder.append("\n");

            synset.getTerms().forEach(term -> {
                stringBuilder.append(LIST_SYMBOL).append(" ");
                stringBuilder.append(term.getTerm());
                if (!StringUtils.isNullOrEmpty(term.getLevel())) {
                    stringBuilder.append(" ( ").append("_").append(term.getLevel()).append("_").append(" )");
                }
                stringBuilder.append("\n");
            });

            stringBuilder.append("\n");

            if (!synset.getSupersynsets().isEmpty()) {
                stringBuilder.append("```Allgemeine``````Bedeutung:``` ");
                appendTermsList(stringBuilder, synset.getSupersynsets().stream().flatMap(Collection::stream));
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }

            if (!synset.getSubsynsets().isEmpty()) {
                stringBuilder.append("```Konkrete``````Beispiele:``` ");
                appendTermsList(stringBuilder, synset.getSubsynsets().stream().flatMap(Collection::stream));
                stringBuilder.append("\n");
                stringBuilder.append("\n");
            }

            stringBuilder.append("---");
            stringBuilder.append("\n");
            stringBuilder.append("\n");
        });

        return stringBuilder.toString();
    }

    private void appendTermsList(StringBuilder stringBuilder, Stream<Term> termStream) {
        termStream.forEach(synset -> {
            stringBuilder.append(synset.getTerm());
            if (!StringUtils.isNullOrEmpty(synset.getLevel())) {
                stringBuilder.append(" ( ").append("_").append(synset.getLevel()).append("_").append(" )");
            }
            stringBuilder.append(", ");
        });
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
    }
}

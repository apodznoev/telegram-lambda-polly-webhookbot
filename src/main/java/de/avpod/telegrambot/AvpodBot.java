package de.avpod.telegrambot;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AvpodBot extends TelegramWebhookBot implements RequestHandler<Object, BotApiMethod> {

    private static final Logger log = LogManager.getLogger(AvpodBot.class);


    public static void main(String[] args) throws InterruptedException {
        log.info("Running main");
        Thread.currentThread().join();
    }
    @Override
    public BotApiMethod handleRequest(Object input, Context context) {
        log.info("Got invocation with input {}", input);
        ObjectMapper mapper = new ObjectMapper();
        Update mapped = mapper.convertValue(input, Update.class);
        return onWebhookUpdateReceived(mapped);
    }

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            log.info("Got update without message or query {}", update);
            return null;
        }

        Message message = update.getMessage();
        User user = update.getMessage().getFrom();
        log.info("Got message {} from user {}", message, user);

        String text = message.getText();
        log.info("Text to be processed {}", text);
        generateAudioAsync(user, message.getChatId(), text);
        return new SendMessage(message.getChatId(), "Generating");
    }

    private void generateAudioAsync(User user, long chatId, String text) {
        long chatToResponse = chatId;
        if (chatId == myChatId()) {
            chatToResponse = responseChatId();
        }

        Polly polly = new Polly(this);
        ListenableFuture<Void> synthesizeRequest = polly.synthesizeAsync(text, chatToResponse);

        List<String> words = Arrays.stream(text.trim().split(" "))
                .map(String::trim)
                .collect(Collectors.toList());

        ListenableFuture<Void> dictionaryRequest = Futures.immediateFuture(null);
        if (words.size() == 1) {
            log.info("Got a single word {} trying to check a dictionary", words.get(0));
            DictionaryService dictionary = new DictionaryService(this);
            dictionaryRequest = dictionary.requestWordInfo(words.get(0), chatToResponse);
        }

        try {
            log.info("Waiting for all futures");
            Futures.allAsList(synthesizeRequest, dictionaryRequest).get();
            log.info("All futures completed");
        } catch (InterruptedException | ExecutionException e) {
            log.error("Cannot wait execution to be finished", e);
        }
    }

    private long myChatId() {
        return Long.parseLong(System.getenv("TELEGRAM_MY_CHAT_ID"));
    }

    private long responseChatId() {
        return Long.parseLong(System.getenv("TELEGRAM_RESPONSE_CHAT_ID"));
    }

    @Override
    public String getBotUsername() {
        return System.getenv("TELEGRAM_BOT_USERNAME");
    }

    @Override
    public String getBotToken() {
        return System.getenv("TELEGRAM_BOT_TOKEN");
    }

    @Override
    public String getBotPath() {
        return System.getenv("BOT_PATH");
    }
}
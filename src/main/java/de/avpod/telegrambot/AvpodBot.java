package de.avpod.telegrambot;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.methods.BotApiMethod;
import org.telegram.telegrambots.api.methods.send.SendAudio;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.User;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;

public class AvpodBot extends TelegramWebhookBot implements RequestHandler<Object, BotApiMethod> {

    private static final Logger log = LogManager.getLogger(AvpodBot.class);

    @Override
    public BotApiMethod onWebhookUpdateReceived(Update update) {
        try {
            if (!update.hasMessage()) {
                log.info("Got update without message or query {}", update);
                return null;
            }

            Message message = update.getMessage();
            User user = update.getMessage().getFrom();
            log.info("Got message {} from user {}", message, user);

            try {
                String text = message.getText();
                log.info("Translating text {} to audio", text);

                PollyDemo pollyDemo = new PollyDemo();
                InputStream is = pollyDemo.synthesize(text);
                SendAudio response = new SendAudio()
                        .setChatId(message.getChatId())
                        .setNewAudio(text.substring(0, Math.min(24, text.length())), is);

                if (message.getChatId() == myChatId()) {
                    response.setChatId(responseChatId());
                }
                this.sendAudio(response);

                return new SendMessage(message.getChatId(), "Success");
            } catch (TelegramApiException e) {
                log.error("Got an exception", e);
                return null;
            }
        } catch (IOException e) {
            log.error("Got a telegram exception", e);
            return null;
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

    @Override
    public BotApiMethod handleRequest(Object input, Context context) {
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("Test simple input:" + input);
        Update mapped = mapper.convertValue(input, Update.class);
        System.out.println("Test mapped input:" + mapped);
        return onWebhookUpdateReceived(mapped);
    }
}
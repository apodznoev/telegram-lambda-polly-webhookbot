package de.avpod.telegrambot;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.polly.AmazonPollyAsync;
import com.amazonaws.services.polly.AmazonPollyAsyncClientBuilder;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.InputStream;

/**
 * Created by apodznoev
 * date 20.06.2019.
 */
class Polly {
    private static final Logger log = LogManager.getLogger(Polly.class);

    private final AmazonPollyAsync polly;
    private final DefaultAbsSender responseBot;

    Polly(DefaultAbsSender responseSender) {
        polly = AmazonPollyAsyncClientBuilder.defaultClient();
        responseBot = responseSender;
    }


    ListenableFuture<Void> synthesizeAsync(String text, long chatId) {
        SynthesizeSpeechRequest synthReq = new SynthesizeSpeechRequest()
                .withText(text)
                .withVoiceId(getVoiceId())
                .withOutputFormat(OutputFormat.Mp3);
        SettableFuture<Void> settableFuture = SettableFuture.create();
        polly.synthesizeSpeechAsync(synthReq, new AsyncHandler<SynthesizeSpeechRequest, SynthesizeSpeechResult>() {
            @Override
            public void onError(Exception e) {
                log.error("Error generating audio", e);
                settableFuture.setException(e);
            }

            @Override
            public void onSuccess(SynthesizeSpeechRequest request, SynthesizeSpeechResult synthesizeSpeechResult) {
                log.info("Audio successfully generated, sending to chat {}", chatId);
                InputStream audioStream = synthesizeSpeechResult.getAudioStream();
                SendAudio response = new SendAudio()
                        .setChatId(chatId)
                        .setAudio(text.substring(0, Math.min(24, text.length())), audioStream);
                try {
                    responseBot.execute(response);
                    settableFuture.set(null);
                } catch (TelegramApiException e) {
                    log.error("Cannot send response due to", e);
                }
            }
        });
        return settableFuture;
    }

    String getVoiceId() {
        return System.getenv("MALE_VOICE_ID");
    }
}

package com.example.SpringDemoBot.service;

import com.example.SpringDemoBot.config.BotConfig;
import com.example.SpringDemoBot.model.User;
import com.example.SpringDemoBot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

//webhook - отвечает на дествия пользователь

@Component
@Slf4j
@ToString
public class TelegramBot extends TelegramLongPollingBot {
    final BotConfig config;
    @Autowired
    private UserRepository repository;
    static final String HELP_TEXT = "This bot demonstrates Spring capabilities.\n\n" +
            "You can execute commands from the main menu by typing commands or by selecting them from the menu on the left" +
            "Type /start to see a welcome message;\n\n" +
            "Type /mydata to see the your details that are stored on our servers\n\n" +
            "Type /help to see what this bot can do";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Get a welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "Get your stored data"));
        listOfCommands.add(new BotCommand("/deletedata", "Delete your stored data"));
        listOfCommands.add(new BotCommand("/help", "Info on how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "Set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Setting bot's command list error: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startCommandRecieved(chatId, update.getMessage().getChat().getFirstName());
                    registerUser(update.getMessage());
                    break;
                case "/help":
                    sendMessage(chatId, HELP_TEXT);
                    break;

                default:
                    sendMessage(chatId, "Sorry, the command was not recognized");
            }
        }
    }

    private void startCommandRecieved(long chatId, String name) {

        //emojipedia.org
        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you!" + ":blush:");
        log.info("Replied to user: " + name);

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup= new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("weather");
        row.add("random joke");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

    private void registerUser(Message message) {
        if (repository.findById(message.getChatId()).isEmpty()) {
            Long chatId = message.getChatId();
            Chat chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            repository.save(user);
            log.info("User saved: " + user);
        }
    }
}

package com.example.demo.service;

import com.example.demo.config.BusinessBotConfig;
import com.example.demo.model.BusinessUserInfo;
import com.example.demo.model.UserInfo;
import com.example.demo.model.UserProfile;
import com.example.demo.repository.BusinessUserRepository;
import com.example.demo.repository.UserProfileRepository;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBusinessBot extends TelegramLongPollingBot {
    private final BusinessUserRepository businessUserRepository;
    private final UserProfileRepository userProfileRepository;

    final BusinessBotConfig botConfig;


    public TelegramBusinessBot(BusinessUserRepository businessUserRepository, UserProfileRepository userProfileRepository, BusinessBotConfig botConfig) {
        this.businessUserRepository = businessUserRepository;
        this.userProfileRepository = userProfileRepository;

        this.botConfig = botConfig;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String msg = update.getMessage().getText();
            BusinessUserInfo existingUser = businessUserRepository.getByChatId(update.getMessage().getChatId());
            if (msg.equals("/start")) {
                try {
                    startMessage(update, update.getMessage().getChatId());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            } else if (existingUser.getNumber() == null) {
                saveNum(update, update.getMessage().getChatId(), existingUser);
            } else {
                findSpec(update, update.getMessage().getChatId());
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();

            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText("Мы вам свяжемся с вами в течении 24 часов. Если у вас есть вопросы, пишите @Kuka055");
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Смм"));
            keyboardRow.add(new KeyboardButton("Дизайнер"));
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Таргетолог"));
            keyboardRow.add(new KeyboardButton("Мобилограф"));
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("It специалист"));
            keyboardRow.add(new KeyboardButton("Монтажер"));
            keyboardRowList.add(keyboardRow);

            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            msg.setReplyMarkup(replyKeyboardMarkup);


            Long personId = Long.valueOf(callbackData);
            UserProfile userProfile = userProfileRepository.findById(personId).orElse(null); // Получаем UserProfile по personId
            assert userProfile != null;
            UserInfo userInfo = userProfile.getUserInfo();
            SendMessage KukaMessage = new SendMessage();
            String message =
                    "Имя: " + userProfile.getName() + "\n" +
                            "username: @" + userInfo.getUsername() + "\n" +
                            "Номер: " + userProfile.getNumber() + "\n" +
                            "Специализация: " + userProfile.getSpecialization() + "\n" +
                            "Описание: " + userProfile.getDescription() + "\n" +
                            "Проекты: " + userProfile.getProjects() + "\n" +
                            "Деньги: " + userProfile.getMoney() + "\n"+
                            "\n"+
                            "Заказчик: "+ update.getCallbackQuery().getFrom().getUserName();

            KukaMessage.setChatId("1399529997");
            KukaMessage.setText(message);

            try {
                execute(KukaMessage);
                execute(msg);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }




        }

    }



    private void saveNum(Update update,Long chatId,BusinessUserInfo businessUserInfo){
        businessUserInfo.setNumber(update.getMessage().getText());
        businessUserRepository.save(businessUserInfo);

        SendMessage profileMessage = new SendMessage();
        profileMessage.setChatId(chatId);
        profileMessage.setText("Выберите специалиста.");
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton("Смм"));
        keyboardRow.add(new KeyboardButton("Дизайнер"));
        keyboardRowList.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton("Таргетолог"));
        keyboardRow.add(new KeyboardButton("Мобилограф"));
        keyboardRowList.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton("It специалист"));
        keyboardRow.add(new KeyboardButton("Монтажер"));
        keyboardRowList.add(keyboardRow);

        replyKeyboardMarkup.setKeyboard(keyboardRowList);
        profileMessage.setReplyMarkup(replyKeyboardMarkup);


        try {
            execute(profileMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private void findSpec(Update update, Long chatId) {
        String searchText = update.getMessage().getText(); // Получаем текст из сообщения пользователя

        List<UserProfile> userProfiles = userProfileRepository.findAll();
        boolean foundSpecialist = false; // Флаг, указывающий, был ли найден хотя бы один специалист

        for (UserProfile userProfile : userProfiles) {
            if (userProfile.getSpecialization().contains(searchText)) {
                foundSpecialist = true; // Устанавливаем флаг, так как был найден хотя бы один специалист

                // Формируем сообщение о профиле пользователя, исключая номер телефона
                String message =
                        "Имя: " + userProfile.getName() + "\n" +
                                "Специализация: " + userProfile.getSpecialization() + "\n" +
                                "Описание: " + userProfile.getDescription() + "\n" +
                                "Проекты: " + userProfile.getProjects() + "\n";

                // Отправляем сообщение пользователю
                sendMessage(chatId, message, userProfile);
            }
        }

        // Если не было найдено ни одного специалиста, отправляем сообщение об отсутствии свободных специалистов
        if (!foundSpecialist) {
            String noSpecialistsMessage = "К сожалению, в данный момент нет доступных специалистов по вашему запросу.";
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(noSpecialistsMessage);
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Смм"));
            keyboardRow.add(new KeyboardButton("Дизайнер"));
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("Таргетолог"));
            keyboardRow.add(new KeyboardButton("Мобилограф"));
            keyboardRowList.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("It специалист"));
            keyboardRow.add(new KeyboardButton("Монтажер"));
            keyboardRowList.add(keyboardRow);

            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }

        }
    }







    private void sendMessage(Long chatId, String message,UserProfile userProfile) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString()); // Устанавливаем ID чата, куда отправляем сообщение
        sendMessage.setText(message); // Устанавливаем текст сообщения
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        InlineKeyboardButton button = new InlineKeyboardButton();

        // Устанавливаем текст на кнопке
        button.setText("Показать информацию");
        // Устанавливаем callback_data, который будет отправлен обратно на сервер при нажатии кнопки
        button.setCallbackData(String.valueOf(userProfile.getId()));
        rowInline.add(button);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        sendMessage.setReplyMarkup(markupInline);
        System.out.println(button.getCallbackData());


        try {
            execute(sendMessage); // Отправляем сообщение
        } catch (TelegramApiException e) {
            e.printStackTrace(); // Обрабатываем возможную ошибку при отправке сообщения
        }
    }

    private void startMessage(Update update, Long chatId) throws TelegramApiException {
        BusinessUserInfo existingUser = businessUserRepository.getByChatId(chatId);
        if (existingUser == null) {
            BusinessUserInfo userInfo = new BusinessUserInfo();
            userInfo.setUsername(update.getMessage().getFrom().getUserName());
            userInfo.setFirstName(update.getMessage().getFrom().getFirstName());
            userInfo.setLastName(update.getMessage().getFrom().getLastName() != null ? update.getMessage().getFrom().getLastName() : "null");
            userInfo.setChatId(chatId);
            businessUserRepository.save(userInfo);
            SendMessage welcomeMessage = new SendMessage();
            welcomeMessage.setChatId(chatId);
            welcomeMessage.setText("Добро пожаловать на платформу Tulga.kz.");

            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(chatId);
            profileMessage.setText("Пожалуйста введите свой номер для дальнейшего сотрудничества");

            try {
                execute(welcomeMessage);
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

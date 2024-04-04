package com.example.demo.service;

import com.example.demo.config.BusinessBotConfig;
import com.example.demo.model.*;
import com.example.demo.repository.BusinessUserRepository;
import com.example.demo.repository.OrderRepository;
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
import java.util.Optional;

@Component
public class TelegramBusinessBot extends TelegramLongPollingBot {
    private final BusinessUserRepository businessUserRepository;
    private final UserProfileRepository userProfileRepository;

    final BusinessBotConfig botConfig;
    private final OrderRepository orderRepository;


    public TelegramBusinessBot(BusinessUserRepository businessUserRepository, UserProfileRepository userProfileRepository, BusinessBotConfig botConfig, OrderRepository orderRepository) {
        this.businessUserRepository = businessUserRepository;
        this.userProfileRepository = userProfileRepository;

        this.botConfig = botConfig;
        this.orderRepository = orderRepository;
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
            BusinessUserInfo existingUser = businessUserRepository.getByChatId(update.getMessage().getChatId());
            String msg = update.getMessage().getText();
            if (msg.equals("/start")) {
                try {
                    startMessage(update, update.getMessage().getChatId());
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
            else if (existingUser.getNumber()==null){
                    saveNum(update,update.getMessage().getChatId(),existingUser);
            }
            else if (existingUser.getBotState()==BusinessBotState.CHOICE_WAIT) {
                if(msg.equals("2")){
                    askSepcialist(update,update.getMessage().getChatId(),existingUser);
                }
                else if (msg.equals("1")){
                    askOrderDescription(update,update.getMessage().getChatId(),existingUser);
                }

            } else if (existingUser.getBotState()==BusinessBotState.ORDER_DESCRIPTION_WAIT){
                askDeadline(update,update.getMessage().getChatId(),existingUser);
            } else if (existingUser.getBotState()==BusinessBotState.ORDER_DEADLINE_WAIT){
                askMoney(update,update.getMessage().getChatId(),existingUser);
        } else if (existingUser.getBotState().equals(BusinessBotState.ORDER_SUM_WAIT)){
                goToMenu(update,update.getMessage().getChatId(),existingUser);
            }
            else if (existingUser.getBotState().equals(BusinessBotState.SPECIALIST_WAIT)){
                findSpec(update,update.getMessage().getChatId(),existingUser);
            }

        }   else if (update.getMessage() != null && update.getMessage().hasContact()) {
            BusinessUserInfo existingUser = businessUserRepository.getByChatId(update.getMessage().getChatId());
            saveNum(update, update.getMessage().getChatId(), existingUser);
        }
         else if (update.hasCallbackQuery()) {
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
            keyboardRow.add(new KeyboardButton("1"));
            keyboardRow.add(new KeyboardButton("2"));
            keyboardRowList.add(keyboardRow);

            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(chatId);
            profileMessage.setText("Если вы хотите:\n Чтобы специалист сам обратился , нажмите кнопку 1\nПосмотреть список специалистов с кейсами 2");


            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            msg.setReplyMarkup(replyKeyboardMarkup);


            Long personId = Long.valueOf(callbackData);
            UserProfile userProfile = userProfileRepository.findById(personId).orElse(null); // Получаем UserProfile по personId
            BusinessUserInfo businessUserInfo = businessUserRepository.getByChatId(chatId);
            businessUserInfo.setBotState(BusinessBotState.CHOICE_WAIT);
            businessUserRepository.save(businessUserInfo);
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
                            "-----------------------"+"\n"+
                            "\n"+
                            "Заказчик: "+ update.getCallbackQuery().getFrom().getUserName()+"\n"+
                            "Номер заказчика:" + businessUserInfo.getNumber();





            KukaMessage.setChatId("1399529997");
            KukaMessage.setText(message);

            try {
                execute(KukaMessage);
                execute(msg);
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }




        }

    }




    private void saveNum(Update update,Long chatId,BusinessUserInfo businessUserInfo){
        if (update.hasMessage()&&update.getMessage().hasText()) {
            System.out.println("hello");
            businessUserInfo.setNumber(update.getMessage().getText());
            businessUserInfo.setBotState(BusinessBotState.CHOICE_WAIT);
            businessUserRepository.save(businessUserInfo);

            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(chatId);
            profileMessage.setText("Если вы хотите:\n Чтобы специалист сам обратился , нажмите кнопку 1\nПосмотреть список специалистов с кейсами 2");
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("1"));
            keyboardRow.add(new KeyboardButton("2"));
            keyboardRowList.add(keyboardRow);


            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            profileMessage.setReplyMarkup(replyKeyboardMarkup);


            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else if (update.getMessage().hasContact()) {
            String num = update.getMessage().getContact().getPhoneNumber();
            System.out.println(num);
            businessUserInfo.setNumber(num);
            businessUserInfo.setBotState(BusinessBotState.CHOICE_WAIT);
            businessUserRepository.save(businessUserInfo);

            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(chatId);
            profileMessage.setText("Если вы хотите:\n Чтобы специалист сам обратился , нажмите кнопку 1\nПосмотреть список специалистов с кейсами 2");
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("1"));
            keyboardRow.add(new KeyboardButton("2"));
            keyboardRowList.add(keyboardRow);


            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            profileMessage.setReplyMarkup(replyKeyboardMarkup);


            try {
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }

    }
    private void findSpec(Update update, Long chatId,BusinessUserInfo businessUserInfo) {
        String searchText = update.getMessage().getText(); // Получаем текст из сообщения пользователя

        List<UserProfile> userProfiles = userProfileRepository.findAll();
        boolean foundSpecialist = false; // Флаг, указывающий, был ли найден хотя бы один специалист

        for (UserProfile userProfile : userProfiles) {
            String specialization = userProfile.getSpecialization();
            if (specialization != null && userProfile.getSpecialization().contains(searchText)) {
                foundSpecialist = true; // Устанавливаем флаг, так как был найден хотя бы один специалист

                // Формируем сообщение о профиле пользователя, исключая номер телефона
                String message =
                        "**Имя:** " + userProfile.getName() + "\n" +
                                "**Специализация:** " + userProfile.getSpecialization() + "\n" +
                                "**Описание:** " + userProfile.getDescription() + "\n" +
                                "**Проекты:** " + userProfile.getProjects() + "\n";

                // Отправляем сообщение пользователю
                sendMessage(chatId, message, userProfile);
            }
        }

        // Если не было найдено ни одного специалиста, отправляем сообщение об отсутствии свободных специалистов
        if (!foundSpecialist) {
            String noSpecialistsMessage = "К сожалению, в данный момент нет доступных специалистов по вашему запросу.";
            businessUserInfo.setBotState(BusinessBotState.CHOICE_WAIT);
            businessUserRepository.save(businessUserInfo);
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(noSpecialistsMessage);
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            keyboardRow.add(new KeyboardButton("1"));
            keyboardRow.add(new KeyboardButton("2"));
            keyboardRowList.add(keyboardRow);

            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(chatId);
            profileMessage.setText("Если вы хотите:\n Чтобы специалист сам обратился , нажмите кнопку 1\nПосмотреть список специалистов с кейсами 2");


            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            try {
                execute(sendMessage);
                execute(profileMessage);
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
        button.setText("Связаться");
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

    private void askSepcialist(Update update, Long chatId,BusinessUserInfo businessUserInfo){
        businessUserInfo.setBotState(BusinessBotState.SPECIALIST_WAIT);
        businessUserRepository.save(businessUserInfo);
        String noSpecialistsMessage = "Выберите специалиста по вашему запросу.";
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

    private void askOrderDescription(Update update, Long chatId, BusinessUserInfo businessUserInfo) {
        Order order = new Order();
        order.setDescription("nichego");
        order.setSum("nichego");
        order.setDeadline("nichego");
        order.setBusinessUserInfo(businessUserInfo); // Устанавливаем связь с переданным businessUserInfo
        orderRepository.save(order);

        businessUserInfo.setBotState(BusinessBotState.ORDER_DESCRIPTION_WAIT);
        // Возможно, здесь вам нужно сохранить изменения в businessUserInfo, в зависимости от логики вашего приложения
        businessUserRepository.save(businessUserInfo);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Напишите описание вашего заказа");
        sendMessage.setChatId(chatId);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private void goToMenu(Update update, Long chatId,BusinessUserInfo businessUserInfo) {
        Order order = orderRepository.findByBusinessUserInfo(businessUserInfo);
        order.setSum(update.getMessage().getText());
        orderRepository.save(order);
        businessUserInfo.setBotState(BusinessBotState.CHOICE_WAIT);
        businessUserRepository.save(businessUserInfo);

        SendMessage sendKukaMessage = new SendMessage();
        sendKukaMessage.setChatId("726929243");
        sendKukaMessage.setText("Новый заказ:\n" +
                "Пользователь: " + order.getBusinessUserInfo().getUsername() + "\n" +
                "Его номер" + order.getBusinessUserInfo().getNumber() + "\n" +
                "Описание: " + order.getDescription() + "\n" +
                "Сумма: " + order.getSum());


        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Отлично, мы получили ваш заказ, если возникут вопросы можете написать @Kuka055");
        sendMessage.setChatId(chatId);

        SendMessage profileMessage = new SendMessage();
        profileMessage.setChatId(chatId);
        profileMessage.setText("Если вы хотите:\n Чтобы специалист сам обратился , нажмите кнопку 1\nПосмотреть список специалистов с кейсами 2");
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton("1"));
        keyboardRow.add(new KeyboardButton("2"));
        keyboardRowList.add(keyboardRow);
        try {
            execute(sendMessage);
            execute(profileMessage);
            execute(sendKukaMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }


    }
    private void askMoney(Update update, Long chatId,BusinessUserInfo businessUserInfo){
        Order order = orderRepository.findByBusinessUserInfo(businessUserInfo);
        order.setDeadline(update.getMessage().getText());
        businessUserInfo.setBotState(BusinessBotState.ORDER_SUM_WAIT);
        businessUserRepository.save(businessUserInfo);
        orderRepository.save(order);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Напишите сколько вы готовы заплатить");
        sendMessage.setChatId(chatId);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        List<KeyboardRow> keyboardRowList = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton("до 20.000"));
        keyboardRow.add(new KeyboardButton("20-50k тг"));
        keyboardRowList.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton("50-100к тг"));
        keyboardRow.add(new KeyboardButton("100-150k тг"));
        keyboardRowList.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add(new KeyboardButton("150-250к"));
        keyboardRow.add(new KeyboardButton("от 250к"));
        keyboardRowList.add(keyboardRow);

        replyKeyboardMarkup.setKeyboard(keyboardRowList);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void askDeadline(Update update, Long chatId,BusinessUserInfo businessUserInfo){
        Order order = orderRepository.findByBusinessUserInfo(businessUserInfo);

        order.setDescription(update.getMessage().getText());
        orderRepository.save(order);

        businessUserInfo.setBotState(BusinessBotState.ORDER_DEADLINE_WAIT);
        businessUserRepository.save(businessUserInfo);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setText("Напишите в какие сроки вам нужен завершить этот заказ");
        sendMessage.setChatId(chatId);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
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
            welcomeMessage.setText("Добро пожаловать на платформу Tulga.kz. Tulga.kz - это инструмент для поиска опытных специалистов, готовых воплотить ваши идеи в жизнь. Благодаря безопасным транзакциям, качественному подбору специалистов и удобной коммуникацией , ваш проект будет обязательно выполнен в соответствии с договоренностями.");

            SendMessage profileMessage = new SendMessage();
            profileMessage.setChatId(chatId);
            profileMessage.setText("Пожалуйста введите свой номер для дальнейшего сотрудничества");

            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            replyKeyboardMarkup.setOneTimeKeyboard(true);
            List<KeyboardRow> keyboardRowList = new ArrayList<>();
            KeyboardRow keyboardRow = new KeyboardRow();
            KeyboardButton keyboardButton = new KeyboardButton();
            keyboardButton.setText("Отправить номер");
            keyboardButton.setRequestContact(true);
            keyboardRow.add(keyboardButton);
            keyboardRowList.add(keyboardRow);
            replyKeyboardMarkup.setKeyboard(keyboardRowList);
            profileMessage.setReplyMarkup(replyKeyboardMarkup);

            try {
                execute(welcomeMessage);
                execute(profileMessage);
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
